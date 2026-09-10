# AgentFlow Design Spec

**Date:** 2026-09-10  
**Status:** Revised — pending re-approval before implementation planning  
**Type:** Portfolio modular monolith — insurance agent implementation portal

## 1. Purpose

AgentFlow demonstrates skills relevant to a Technical Implementation Engineer / Technical Consultant role:

- Java, Spring Boot, PostgreSQL, REST APIs
- JavaScript (React) and JSON-driven web forms/workflows
- Outbound REST and legacy SOAP/XML integrations
- Validation, retries, failure handling, and integration observability
- Python import scripting and Postman-based API demonstration
- Production-style troubleshooting narrative suitable for interviews

It is intentionally **not** a multi-tenant SaaS product. Prefer understandable professional engineering over unnecessary abstraction or impressive-but-irrelevant technology.

## 2. Product concept

Fictional scenario: an insurance carrier provides software used by independent agencies. Agencies need slightly different forms and integration behavior. AgentFlow supports **JSON-configured forms and workflows** instead of hard-coding every implementation.

An agent (demo user) can:

1. Enter via a portfolio landing page (no login)
2. Select or create a customer
3. Start a New Auto or New Home policy application
4. Complete a dynamically rendered form
5. Submit for backend validation and processing
6. Trigger REST customer verification and SOAP policy eligibility checks
7. View application status and Integration Activity for troubleshooting

The portal should feel like a small **internal enterprise** application after the landing page.

## 3. Decisions locked in design

| Topic | Decision |
|---|---|
| Authentication | None in MVP |
| App entry | Gatekeeping landing page explaining purpose, stack, architecture, demo path |
| Frontend / backend packaging | Separate Vite + React (JS) SPA and Spring Boot API |
| External systems | Real outbound clients in API; mock services in Docker Compose |
| Failure injection | Isolated from domain model; demo/local profile only + mock admin controls |
| Workflows | Two: Auto Policy + Home Policy (Auto first in build sequence; Home proves config-only extension) |
| Config storage | Versioned JSON files are source of truth; seeded into Postgres; API serves from DB |
| Application ↔ workflow | Application pins `workflow_definition_id` (FK) at create time |
| Form config naming | Custom JSON **form/workflow definition** (`definition_json`) — not formal JSON Schema |
| SOAP stack | Spring Web Services (WSDL-visible) |
| REST client | Spring `WebClient` |
| Error responses | Spring `ProblemDetail` (+ `correlationId`, validation details) |
| Activity attempt status | Per attempt: `SUCCESS` \| `FAILED` only (retries = multiple rows via `attempt`) |
| Submit semantics | Enforce legal transitions; duplicate submit → `409 Conflict` |
| Transactions | No long-running DB transaction across external network calls |
| Repo shape | Flat monorepo (Approach 1) |
| Build (backend) | Maven |
| Language (frontend) | JavaScript (not TypeScript) unless a later need is proven |

## 4. Architecture

```text
[Browser]
   │
   ├─ Landing page (portfolio / docs gate)
   └─ Agent portal (React SPA)
         │  REST/JSON
         ▼
[AgentFlow API — Spring Boot modular monolith]
   │  workflow definitions (DB, seeded from files)
   │  customers / applications / integration activity
   │
   ├─ HTTP JSON ──► [Customer Verification mock]
   └─ SOAP/XML  ──► [Policy Eligibility mock — Spring-WS + WSDL]
         │
         ▼
   [PostgreSQL]
```

### 4.1 Component responsibilities

| Component | Responsibility |
|---|---|
| `frontend/` | Landing page; customer list/create; start Auto/Home workflow; **shared** JSON-driven form renderer; submit; status + Integration Activity; demo failure controls (only when API demo profile is on) |
| `backend/` | REST API; Bean Validation; definition re-validation; orchestration with short transactions; JPA; file→DB seed; REST + SOAP clients; correlation IDs; integration activity; retries; demo failure façade (profile-gated) |
| `mocks/customer-verification/` | Simulated third-party REST verification service + admin fault controls |
| `mocks/policy-eligibility/` | Simulated legacy SOAP eligibility service + WSDL + fault controls |
| `scripts/python/` | CSV validate/transform → AgentFlow REST import |
| `postman/` | Collection + environment for core APIs and failure scenarios |
| `docker-compose.yml` | PostgreSQL + both mocks (API/frontend run locally in primary demo path) |

### 4.2 Backend modular packages

Single deployable Spring Boot app with package boundaries (not microservices):

- `customer` — entities, repos, services, controllers
- `application` — applications, application data, submit orchestration, state-transition rules
- `workflow` — definition entities, file seed, form-definition parsing/validation
- `integration` — activity persistence, REST client, SOAP client, retry policy
- `demo` — failure-injection / scenario helpers enabled only under `local`/`demo` profiles (must not leak into core domain entities)
- `common` — correlation ID filter, `ProblemDetail` customization, shared utilities

## 5. MVP scope

### In scope

- Landing page + agent portal shell
- Customers: list, create, retrieve
- Two workflow JSON definitions (auto + home) with field types: text, number, date, select, checkbox; required; simple validation; optional single-condition `visibleWhen`
- Draft application create/save; submit with orchestration
- Applications **pinned** to a specific `workflow_definition` version via FK
- Statuses: `DRAFT` → `SUBMITTED` → `CUSTOMER_VERIFICATION` → `ELIGIBILITY_CHECK` → `APPROVED` \| `MANUAL_REVIEW` \| `INELIGIBLE` \| `INTEGRATION_FAILURE`
- Enforced submit transition rules (see §9.1)
- Integration Activity timeline UI + API
- Docker Compose Postgres + REST mock + SOAP mock
- Profile-gated demo failure injection (not on the `application` table)
- Python customer CSV import
- Postman collection
- Meaningful automated tests (see §13)
- README with architecture diagram (Mermaid), run/test/demo instructions

### Out of MVP (deliberate)

Kubernetes, Kafka, Redis, GraphQL, multiple deployable business microservices, event sourcing, real auth/SSO, cloud infrastructure, elaborate CI/CD, AI features, generic workflow engines, config admin UI, multi-agency tenancy, repeating form groups / nested sections, full browser E2E suite, polished consumer design system, `POST /api/applications/{id}/retry` (document as natural follow-on).

### Build sequence (implementation plan must follow)

```text
Auto definition + generic renderer
  → Auto draft/save/validate
  → Auto submit orchestration
  → REST integration
  → SOAP integration
  → Observability / failure handling / demo injection
  → Home definition added (config only — no product-specific React forms)
```

Adding Home must be primarily `home-policy-v1.json` (plus seed). If it requires `AutoPolicyForm.jsx` / `HomePolicyForm.jsx`, the architecture failed.

## 6. Data model

### 6.1 `customer`

| Column | Notes |
|---|---|
| `id` | UUID PK |
| `first_name`, `last_name` | required |
| `email` | required, unique |
| `phone` | optional |
| `created_at`, `updated_at` | timestamps |

### 6.2 `workflow_definition`

| Column | Notes |
|---|---|
| `id` | UUID PK |
| `workflow_key` | e.g. `auto-policy`, `home-policy` |
| `title` | display title |
| `version` | integer; unique with `workflow_key` (`workflow_key` + `version`) |
| `definition_json` | JSONB — custom form/workflow definition (not JSON Schema Draft) |
| `active` | boolean; at most one active row per `workflow_key` |
| `created_at` | timestamp |

**Seed behavior:** On startup, read versioned files from `backend/src/main/resources/workflows/`, upsert into DB. Broken seed files fail fast at startup. Runtime API reads **from DB**, not directly from classpath.

### 6.3 `application`

| Column | Notes |
|---|---|
| `id` | UUID PK |
| `customer_id` | FK → customer |
| `workflow_definition_id` | FK → `workflow_definition` (**pinned at create**) |
| `status` | enum/string as listed in §5 |
| `correlation_id` | set on submit (and propagated outbound) |
| `submitted_at` | nullable |
| `created_at`, `updated_at` | timestamps |

**No `simulate_failure` (or similar) column.** Demo tooling stays outside the business entity.

**Create flow:**

```text
POST /api/applications { customerId, workflowKey }
  → resolve currently active workflow_definition for that key
  → persist application with workflow_definition_id
  → all later render / validate / submit uses that pinned definition
```

Denormalizing `workflow_key` onto `application` is optional for query convenience; if present it is derived from the pinned definition and not used as the source of truth for validation.

### 6.4 `application_data`

| Column | Notes |
|---|---|
| `application_id` | PK/FK |
| `payload` | JSONB map of field name → value |
| `updated_at` | timestamp |

JSONB is preferred over EAV for dynamic forms at this scale.

### 6.5 `integration_activity`

| Column | Notes |
|---|---|
| `id` | UUID PK |
| `application_id` | FK |
| `correlation_id` | indexed for troubleshooting |
| `integration_name` | e.g. `CustomerVerification`, `PolicyEligibility` |
| `integration_type` | `REST` \| `SOAP` \| `INTERNAL` |
| `action` | HTTP method+path or SOAP operation |
| `status` | `SUCCESS` \| `FAILED` only |
| `http_status` | nullable |
| `duration_ms` | |
| `attempt` | 1-based; retries = additional rows, not a `RETRY` status |
| `error_message` | nullable, truncated |
| `request_summary`, `response_summary` | short, redacted — not full PII payload dumps |
| `created_at` | timestamp |

Example timeline:

```text
CustomerVerification  FAILED   attempt 1   503   842ms
CustomerVerification  FAILED   attempt 2   503   911ms
CustomerVerification  SUCCESS  attempt 3   200   173ms
```

## 7. REST API surface (AgentFlow)

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/health` | Liveness |
| `GET` | `/api/workflows` | List active workflows |
| `GET` | `/api/workflows/{key}` | Active definition for starting a new application |
| `GET` | `/api/applications/{id}/definition` | Pinned definition for an existing application (draft render / review) |
| `GET` | `/api/customers` | List/search |
| `POST` | `/api/customers` | Create |
| `GET` | `/api/customers/{id}` | Retrieve |
| `POST` | `/api/applications` | Create draft: resolve active definition, pin `workflow_definition_id` |
| `GET` | `/api/applications/{id}` | Status + summary (include workflow key/version from pin) |
| `PUT` | `/api/applications/{id}/data` | Save draft payload (allowed in `DRAFT` only) |
| `POST` | `/api/applications/{id}/submit` | Validate + run integrations (see §9) |
| `GET` | `/api/applications/{id}/activities` | Integration Activity timeline |

**Demo-only (profile `local` or `demo`):** failure injection must not live on the `application` entity. Preferred approaches (either or both, kept in `demo` package):

- Request-scoped cue on submit only when profile active, e.g. header `X-Demo-Scenario: rest-timeout` or JSON body `{ "demoScenario": "soap-fault" }` that is **not** persisted on `application`
- And/or explicit demo controllers such as `/api/demo/failures/customer-verification` and `/api/demo/failures/policy-eligibility` that configure in-process client stubs or forward to mock admin APIs

Outside those profiles, demo headers/bodies are ignored or rejected; demo controllers are not registered.

**Error responses:** Spring `ProblemDetail` (RFC 7807-style), extended with `correlationId` and validation field errors as needed. Prefer framework facilities over a bespoke error DTO hierarchy.

No auth endpoints. No workflow definition CRUD in MVP. No `/retry` endpoint in MVP.

## 8. JSON form / workflow definition

This is a **custom, intentionally small JSON form definition format** used as configuration. It is **not** a claim of compliance with the formal JSON Schema specification (Draft 2020-12, etc.). In code and docs prefer names like `WorkflowDefinition`, `FormDefinition`, `definition_json`, and phrases such as “JSON-defined form configuration.”

Example shape:

```json
{
  "workflowKey": "auto-policy",
  "title": "New Auto Policy",
  "version": 1,
  "fields": [
    {
      "name": "vin",
      "label": "VIN",
      "type": "text",
      "required": true,
      "validation": { "minLength": 11, "maxLength": 17 }
    },
    {
      "name": "coverageType",
      "label": "Coverage Type",
      "type": "select",
      "required": true,
      "options": [
        { "value": "LIABILITY", "label": "Liability" },
        { "value": "FULL", "label": "Full Coverage" }
      ]
    },
    {
      "name": "coverageAmount",
      "label": "Coverage Amount",
      "type": "number",
      "required": true,
      "visibleWhen": { "field": "coverageType", "equals": "FULL" }
    }
  ]
}
```

**MVP field types:** `text`, `number`, `date`, `select`, `checkbox`.  
**MVP rules:** `required`, simple `validation`, optional `visibleWhen` (single condition).

Frontend renders from the **pinned** definition for an application (or the active definition when starting new). Backend **re-validates** against the pinned `definition_json` on submit. Invalid user input does not call integrations.

Home policy definition uses a different field set (e.g. property address, dwelling value, occupancy) to prove configuration-driven variation without product-specific React forms.

## 9. Submit orchestration

Draft saves do **not** call external systems.

On `POST .../submit` (only legal from `DRAFT` — see §9.1):

1. Load application + data + **pinned** `workflow_definition`; validate payload against that definition
2. Set `correlationId` if absent; persist status → `SUBMITTED` (**commit**)
3. Persist status → `CUSTOMER_VERIFICATION` (**commit**); call REST outside any open transaction; persist each attempt result (**commit** per attempt or per finished attempt batch — see §9.2); retry if retryable
4. If verified, persist status → `ELIGIBILITY_CHECK` (**commit**); call SOAP outside any open transaction; persist each attempt (**commit**); retry if retryable
5. Map outcomes and persist terminal status (**commit**):
   - REST call exhausted retries → `INTEGRATION_FAILURE`
   - REST success with `verified: false` → `MANUAL_REVIEW` (skip SOAP)
   - REST success with `verified: true` → proceed to SOAP
   - Eligibility `ELIGIBLE` → `APPROVED`
   - Eligibility `MANUAL_REVIEW` → `MANUAL_REVIEW`
   - Eligibility `INELIGIBLE` → `INELIGIBLE`
   - SOAP exhausted retries → `INTEGRATION_FAILURE`
6. Return representation suitable for the status UI

Orchestration lives in a straightforward Spring `@Service` — not a workflow engine. Do **not** annotate the whole submit method with one long `@Transactional`.

**Business rules (explicit for MVP):**

- `verified: false` → `MANUAL_REVIEW` without calling SOAP
- REST/SOAP transport or 5xx failures after retries → `INTEGRATION_FAILURE`
- `riskScore` may appear in redacted activity summaries for demo realism but does **not** change status in MVP

### 9.1 Submit / state-transition semantics

The status list is **enforceable**, not merely descriptive.

| Current status | `POST .../submit` |
|---|---|
| `DRAFT` | Allowed |
| `SUBMITTED`, `CUSTOMER_VERIFICATION`, `ELIGIBILITY_CHECK` | `409 Conflict` — processing already started or in flight |
| `APPROVED`, `MANUAL_REVIEW`, `INELIGIBLE` | `409 Conflict` — terminal |
| `INTEGRATION_FAILURE` | `409 Conflict` in MVP; future `POST .../retry` may reopen this path |

`PUT .../data` is allowed only in `DRAFT`; otherwise `409 Conflict`.

This protects against double-click, browser retry, and accidental Postman resubmits calling external systems twice. No distributed idempotency layer in MVP.

### 9.2 Transaction boundaries

**Policy:** External network operations do **not** execute inside one long-running database transaction. Application state and integration activity are persisted at explicit orchestration boundaries. Integration attempt records **must survive** downstream failures (including later exceptions).

Conceptual sequence:

```text
Persist SUBMITTED                         → commit
Persist CUSTOMER_VERIFICATION             → commit
Call REST (no open DB transaction)
Persist attempt result (FAILED/SUCCESS)   → commit  (repeat per attempt)
Persist ELIGIBILITY_CHECK                 → commit
Call SOAP (no open DB transaction)
Persist attempt result                    → commit  (repeat per attempt)
Persist terminal status                   → commit
```

Short `@Transactional` methods (or explicit transaction templates) around each persist step are fine. Holding a connection/transaction open across REST/SOAP waits is not.

Interview talking point: troubleshooting rows remain durable even when a later step fails or the JVM errors after a successful outbound call.

## 10. Integrations

### 10.1 REST — Customer Verification

- Real HTTP client via Spring `WebClient` with connect/read timeouts
- JSON request/response DTOs and mapping into an internal result type
- Demo behaviors: HTTP 200 success, 500, timeout, 503/unavailable
- Retries with limited attempts + backoff for timeout and 5xx only; no retry on 4xx
- Each attempt writes an `integration_activity` row with `SUCCESS` or `FAILED`

### 10.2 SOAP — Policy Eligibility

- Spring Web Services; WSDL published by the mock
- Operation such as `CheckEligibility`
- Responses: `ELIGIBLE` \| `MANUAL_REVIEW` \| `INELIGIBLE`, or SOAP fault
- Visible WSDL/XSD, marshalling, request/response mapping, fault handling in the repo
- Retry transport/retryable faults; do not retry definitive business `INELIGIBLE`

### 10.3 Failure injection (isolated from domain)

- **Not** stored on `application`
- Enabled only when Spring profile `local` or `demo` is active
- Portal may send a non-persisted demo scenario on submit (header or body) when talking to a demo-profile API
- Mocks expose admin controls for Postman/scripted faults independently
- Supported scenarios at minimum: `rest-500`, `rest-timeout`, `soap-fault`, `soap-manual-review` (exact names fixed during implementation and documented)
- Talking point: failure injection is deliberately isolated from the business domain and disabled outside demo profiles

## 11. Observability & error handling

- Propagate `correlationId` on outbound REST (header) and SOAP (header or body element)
- Logs include `correlationId`, `applicationId`, integration name, attempt
- Activity UI is the primary interview troubleshooting artifact; logs reinforce it
- Summaries only in DB — redact/truncate sensitive fields (email, VIN, etc.) — PII-aware logging story

| Failure class | API / domain behavior |
|---|---|
| Invalid input / definition validation | `400` ProblemDetail + field errors; no integrations |
| Illegal state transition | `409 Conflict` ProblemDetail |
| Unknown workflow / no active definition | Clear `404`/`422` |
| Broken seed files | Fail startup |
| Retryable integration errors | Multiple `FAILED` attempt rows, then `INTEGRATION_FAILURE` |
| Business decline / manual review | Terminal status + activity |
| Unexpected errors | `500` ProblemDetail with `correlationId` |

## 12. Configuration versioning (interview point)

Because applications pin `workflow_definition_id`:

```text
Monday:   auto-policy v1 active → Application A created (pins v1)
Tuesday:  auto-policy v2 becomes active
Wednesday: Application A submitted → validated against v1, not v2
```

New applications resolve the **active** definition at create time. In-flight drafts remain backwards-compatible with the definition they were created under. That is a deliberate configuration-versioning / compatibility choice, not an accident of “always load latest.”

## 13. Testing strategy

| Layer | Focus |
|---|---|
| Unit | Definition validation; legal transitions (including double-submit); retry classification; mapping helpers |
| Spring integration | Controllers + validation; JPA with Testcontainers PostgreSQL; config seed; pin-at-create behavior when active version changes |
| REST client tests | Success, 500, timeout, multiple FAILED then SUCCESS activity rows |
| SOAP client tests | ELIGIBLE, MANUAL_REVIEW, fault, retry against Spring-WS mock |
| Orchestration | Transaction-boundary behavior where practical (activity retained after forced downstream failure) |
| Frontend | Shared form renderer from fixture JSON (required, select, `visibleWhen`); no product-specific form components |
| Python | CSV validation/transform unit tests; optional HTTP mock for POST |

Prefer tests that prove interview-critical behavior over chasing coverage percentages. No full E2E browser suite in MVP.

## 14. Python utility

- Input: `customers.csv`
- Validate and transform rows
- `POST /api/customers` for valid rows
- Per-row error reporting; non-zero exit on failures
- Complements the Java app; does not own workflow orchestration
- Export is optional stretch after import works

## 15. Postman

Collection + environment variables for base URLs covering:

- Create/retrieve customer
- List/get workflow definition
- Create application, save data, submit, get status, get activities, get pinned definition
- Double-submit → expect `409`
- Failure scenarios via demo profile cues and/or mock admin controls

## 16. Frontend UX notes

- **Landing:** brand/purpose, why portfolio exists, stack, architecture summary, how to demo (including failure controls), CTA into portal
- **Portal:** utilitarian internal-tool layout — customers, applications, dynamic form, status, integration activity
- Dynamic forms must use one shared renderer driven by definition JSON — never per-product form components
- When editing a draft, load definition via the application’s pinned definition endpoint, not “latest active”

## 17. Repository structure

```text
agentflow/
├── README.md
├── docker-compose.yml
├── backend/                      # Spring Boot API (Maven)
├── frontend/                     # Vite + React (JavaScript)
├── mocks/
│   ├── customer-verification/    # mock REST service
│   └── policy-eligibility/       # Spring-WS SOAP mock + WSDL
├── scripts/
│   └── python/                   # CSV import utility
├── postman/                      # collection + environment
└── docs/
    └── superpowers/
        └── specs/
            └── 2026-09-10-agentflow-design.md
```

## 18. Local development (target experience)

1. `docker compose up` — Postgres + REST mock + SOAP mock  
2. Run Spring Boot API with `local` or `demo` profile when failure injection is needed  
3. Run Vite frontend  
4. Open landing page → portal  
5. Import Postman collection as needed  
6. Run Python import against local API  

Exact ports, env vars, and commands will be fixed in the implementation plan and README.

## 19. Implementation planning next step

After this revised spec is accepted as written, create a milestone-based implementation plan (small, testable increments) via the writing-plans skill. Sequencing must put Auto end-to-end before Home-as-config, and must not implement the full system in one pass. The implementation plan itself should be scrutinized for ordering and testability.

## 20. Success criteria for the portfolio

A reviewer should quickly conclude the author can:

- Build a clean Spring Boot modular monolith with JPA and PostgreSQL
- Design REST APIs and consume third-party REST APIs with timeouts/retries
- Integrate with SOAP/XML using Spring-WS in a visible way
- Drive UI forms from JSON configuration validated on both ends
- Pin configuration versions for in-flight work (backwards-compatible drafts)
- Keep DB transactions short across integration boundaries
- Enforce state machines so submit is not accidentally re-entrant
- Isolate demo/failure tooling from the business domain model
- Persist and present integration activity for troubleshooting (with PII-aware summaries)
- Script CSV import in Python and document APIs in Postman
- Test happy paths and failure paths deliberately
- Explain every major technical choice in an interview

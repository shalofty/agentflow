# AgentFlow Design Spec

**Date:** 2026-09-10  
**Status:** Approved for implementation planning  
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
| Failure injection | UI/debug controls **and** mock admin controls |
| Workflows | Two: Auto Policy + Home Policy |
| Config storage | Versioned JSON files are source of truth; seeded into Postgres; API serves from DB |
| SOAP stack | Spring Web Services (WSDL-visible) |
| REST client | Spring `WebClient` |
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
   │  workflow configs (DB, seeded from files)
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
| `frontend/` | Landing page; customer CRUD/list; start Auto/Home workflow; JSON-driven form renderer; submit; status + Integration Activity; demo failure controls |
| `backend/` | REST API; Bean Validation; schema re-validation; simple orchestration service; JPA; config file→DB seed; REST + SOAP clients; correlation IDs; integration activity; retries |
| `mocks/customer-verification/` | Simulated third-party REST verification service + admin fault controls |
| `mocks/policy-eligibility/` | Simulated legacy SOAP eligibility service + WSDL + fault controls |
| `scripts/python/` | CSV validate/transform → AgentFlow REST import |
| `postman/` | Collection + environment for core APIs and failure scenarios |
| `docker-compose.yml` | PostgreSQL + both mocks (API/frontend run locally in primary demo path) |

### 4.2 Backend modular packages

Single deployable Spring Boot app with package boundaries (not microservices):

- `customer` — entities, repos, services, controllers
- `application` — applications, application data, submit orchestration
- `workflow` — definition entities, file seed, schema parsing/validation
- `integration` — activity persistence, REST client, SOAP client, retry policy
- `common` — correlation ID filter, problem JSON errors, shared utilities

## 5. MVP scope

### In scope

- Landing page + agent portal shell
- Customers: list, create, retrieve
- Two workflow JSON configs (auto + home) with field types: text, number, date, select, checkbox; required; simple validation; optional single-condition `visibleWhen`
- Draft application create/save; submit with orchestration
- Statuses: `DRAFT` → `SUBMITTED` → `CUSTOMER_VERIFICATION` → `ELIGIBILITY_CHECK` → `APPROVED` \| `MANUAL_REVIEW` \| `INELIGIBLE` \| `INTEGRATION_FAILURE`
- Integration Activity timeline UI + API
- Docker Compose Postgres + REST mock + SOAP mock
- Python customer CSV import
- Postman collection
- Meaningful automated tests (see §12)
- README with architecture diagram (Mermaid), run/test/demo instructions

### Out of MVP (deliberate)

Kubernetes, Kafka, Redis, GraphQL, multiple deployable business microservices, event sourcing, real auth/SSO, cloud infrastructure, elaborate CI/CD, AI features, generic workflow engines, config admin UI, multi-agency tenancy, repeating form groups / nested sections, full browser E2E suite, polished consumer design system.

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
| `schema_json` | JSONB — form/workflow config |
| `active` | boolean; at most one active row per `workflow_key` |
| `created_at` | timestamp |

**Seed behavior:** On startup, read versioned files from `backend/src/main/resources/workflows/`, upsert into DB. Broken seed files fail fast at startup. Runtime API reads **from DB**, not directly from classpath, so reviewers see both “config as files” and “config served from datastore.”

### 6.3 `application`

| Column | Notes |
|---|---|
| `id` | UUID PK |
| `customer_id` | FK |
| `workflow_key` | references logical workflow |
| `status` | enum/string as listed in §5 |
| `correlation_id` | set on submit (and propagated outbound) |
| `simulate_failure` | nullable demo cue (`rest-timeout`, `rest-500`, `soap-fault`, etc.) |
| `submitted_at` | nullable |
| `created_at`, `updated_at` | timestamps |

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
| `status` | `SUCCESS` \| `FAILED` \| `RETRY` |
| `http_status` | nullable |
| `duration_ms` | |
| `attempt` | 1-based |
| `error_message` | nullable, truncated |
| `request_summary`, `response_summary` | short, redacted — not full PII payload dumps |
| `created_at` | timestamp |

## 7. REST API surface (AgentFlow)

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/health` | Liveness |
| `GET` | `/api/workflows` | List active workflows |
| `GET` | `/api/workflows/{key}` | Schema for UI rendering |
| `GET` | `/api/customers` | List/search |
| `POST` | `/api/customers` | Create |
| `GET` | `/api/customers/{id}` | Retrieve |
| `POST` | `/api/applications` | Create draft (`customerId`, `workflowKey`, optional `simulateFailure`) |
| `GET` | `/api/applications/{id}` | Status + summary |
| `PUT` | `/api/applications/{id}/data` | Save draft payload |
| `POST` | `/api/applications/{id}/submit` | Validate + run integrations |
| `GET` | `/api/applications/{id}/activities` | Integration Activity timeline |

**Error responses:** Consistent JSON problem body with `timestamp`, `correlationId`, `status`, `error`, `message`, `path`, plus field errors when validation fails.

No auth endpoints. No workflow config CRUD in MVP.

## 8. JSON form / workflow schema

Intentionally small. Example shape:

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

Frontend renders from schema. Backend **re-validates** against the stored schema on submit. Invalid user input does not call integrations.

Home policy config uses a different field set (e.g. property address, dwelling value, occupancy) to prove configuration-driven variation.

## 9. Submit orchestration

Draft saves do **not** call external systems.

On `POST .../submit`:

1. Load application + data + workflow schema; validate payload
2. Set `correlationId` if absent; status → `SUBMITTED`
3. Status → `CUSTOMER_VERIFICATION`; call REST client; record activity per attempt; retry if retryable
4. Status → `ELIGIBILITY_CHECK`; call SOAP client; record activity per attempt; retry if retryable
5. Map outcomes:
   - REST call exhausted retries → `INTEGRATION_FAILURE`
   - REST success with `verified: false` → `MANUAL_REVIEW` (skip SOAP)
   - REST success with `verified: true` → proceed to SOAP
   - Eligibility `ELIGIBLE` → `APPROVED`
   - Eligibility `MANUAL_REVIEW` → `MANUAL_REVIEW`
   - Eligibility `INELIGIBLE` → `INELIGIBLE`
   - SOAP exhausted retries → `INTEGRATION_FAILURE`
6. Persist final status and return representation suitable for the status UI

Orchestration lives in a straightforward Spring `@Service` — not a workflow engine.

**Business rules (explicit for MVP):**

- `verified: false` → `MANUAL_REVIEW` without calling SOAP
- REST/SOAP transport or 5xx failures after retries → `INTEGRATION_FAILURE`
- `riskScore` is stored/displayed via activity/summary for demo realism but does **not** change status in MVP

## 10. Integrations

### 10.1 REST — Customer Verification

- Real HTTP client via Spring `WebClient` with connect/read timeouts
- JSON request/response DTOs and mapping into an internal result type
- Demo behaviors: HTTP 200 success, 500, timeout, 503/unavailable
- Retries with limited attempts + backoff for timeout and 5xx only; no retry on 4xx
- Each attempt writes `integration_activity`

### 10.2 SOAP — Policy Eligibility

- Spring Web Services; WSDL published by the mock
- Operation such as `CheckEligibility`
- Responses: `ELIGIBLE` \| `MANUAL_REVIEW` \| `INELIGIBLE`, or SOAP fault
- Visible WSDL/XSD, marshalling, request/response mapping, fault handling in the repo
- Retry transport/retryable faults; do not retry definitive business `INELIGIBLE`

### 10.3 Failure injection

- **Portal:** optional debug control mapped to `application.simulate_failure` / submit cue
- **Mocks:** admin endpoints or equivalent to force fault modes for Postman and scripts
- Supported cues at minimum: `rest-500`, `rest-timeout`, `soap-fault`, `soap-manual-review` (exact names fixed during implementation and documented)

## 11. Observability & error handling

- Propagate `correlationId` on outbound REST (header) and SOAP (header or body element)
- Logs include `correlationId`, `applicationId`, integration name, attempt
- Activity UI is the primary interview troubleshooting artifact; logs reinforce it
- Summaries only in DB — redact/truncate sensitive fields (email, VIN, etc.)

| Failure class | API / domain behavior |
|---|---|
| Invalid input / schema | `400` + field errors; no integrations |
| Unknown workflow / inactive config | Clear `404`/`422` |
| Broken seed files | Fail startup |
| Retryable integration errors | Retry then `INTEGRATION_FAILURE` + activity trail |
| Business decline / manual review | Terminal status + activity |
| Unexpected errors | `500` problem JSON with `correlationId` |

## 12. Testing strategy

| Layer | Focus |
|---|---|
| Unit | Schema validation; status transitions; retry classification; mapping helpers |
| Spring integration | Controllers + validation; JPA with Testcontainers PostgreSQL; config seed |
| REST client tests | Success, 500, timeout, retry/activity assertions against WireMock or mock service |
| SOAP client tests | ELIGIBLE, MANUAL_REVIEW, fault, retry against Spring-WS mock |
| Frontend | Form renderer from fixture JSON (required, select, `visibleWhen`) via React Testing Library |
| Python | CSV validation/transform unit tests; optional HTTP mock for POST |

Prefer tests that prove interview-critical behavior over chasing coverage percentages. No full E2E browser suite in MVP.

## 13. Python utility

- Input: `customers.csv`
- Validate and transform rows
- `POST /api/customers` for valid rows
- Per-row error reporting; non-zero exit on failures
- Complements the Java app; does not own workflow orchestration
- Export is optional stretch after import works

## 14. Postman

Collection + environment variables for base URLs covering:

- Create/retrieve customer
- List/get workflow config
- Create application, save data, submit, get status, get activities
- Failure scenarios via simulate cues and/or mock admin controls

## 15. Frontend UX notes

- **Landing:** brand/purpose, why portfolio exists, stack, architecture summary, how to demo (including failure controls), CTA into portal
- **Portal:** utilitarian internal-tool layout — customers, applications, dynamic form, status, integration activity
- Dynamic forms must be schema-driven, not hard-coded field components per product (shared renderer + config)

## 16. Repository structure

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

## 17. Local development (target experience)

1. `docker compose up` — Postgres + REST mock + SOAP mock  
2. Run Spring Boot API  
3. Run Vite frontend  
4. Open landing page → portal  
5. Import Postman collection as needed  
6. Run Python import against local API  

Exact ports, env vars, and commands will be fixed in the implementation plan and README.

## 18. Implementation planning next step

After this spec is accepted as written, create a milestone-based implementation plan (small, testable increments) via the writing-plans skill. Do not implement the full system in one pass.

## 19. Success criteria for the portfolio

A reviewer should quickly conclude the author can:

- Build a clean Spring Boot modular monolith with JPA and PostgreSQL
- Design REST APIs and consume third-party REST APIs with timeouts/retries
- Integrate with SOAP/XML using Spring-WS in a visible way
- Drive UI forms from JSON configuration validated on both ends
- Persist and present integration activity for troubleshooting
- Script CSV import in Python and document APIs in Postman
- Test happy paths and failure paths deliberately
- Explain every major technical choice in an interview

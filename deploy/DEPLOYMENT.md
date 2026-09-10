# AgentFlow cloud deployment proposal (free-only)

Status: **awaiting user approval before any cloud provisioning.**
Budget: **$0 recurring.** No paid plans, disks, cron jobs, or Render Postgres.

Verified 2026-09-10 via MCP (read-only) unless noted.

## Target topology

```text
Cloudflare Pages (static React) + /api proxy Function
Cloudflare Worker: agentflow-customer-verification  (POST /verify)
Cloudflare Worker: agentflow-policy-eligibility     (POST /ws + public WSDL/XSD)
        │
        ▼
Render Free web service: agentflow-api  (Spring Boot, plan free, 512 MB)
        │
        ▼
Neon Free project: agentflow  (org Stephan Haloftis / org-floral-block-85781342)
```

Koyeb: excluded. Render Postgres: not used. Neon org `org-curly-lake-90935950` (XALO): do not touch.

## Account verification (this session)

| Provider | Check | Result |
| --- | --- | --- |
| Neon | `list_organizations` | Personal Free `org-floral-block-85781342` present; Vercel org also present (ignored) |
| Neon | `list_projects` on personal org | **Empty** (ready for AgentFlow-only project) |
| Cloudflare Bindings | `workers_list` | **Empty** inventory; MCP connected |
| GitHub | `get_me` | Authenticated as **shalofty** |
| Render | `list_workspaces` | **Unauthorized** — complete Render MCP Authenticate/Connect in Cursor Settings, then re-check workspace `tea-dahdcde743jc73cgl4dg` |
| Wrangler CLI | `whoami` | Not verified yet (separate from MCP) |
| Cloudflare email verify | Dashboard modal | **User must clear** before Workers/Pages deploy |

## Exact free resources (ceiling)

1. **One** Neon Free project named `agentflow` in `org-floral-block-85781342` (region preference: `aws-us-east-1` to align with Render Virginia, or `aws-us-west-2` if Render Oregon).
2. **One** Render Free web service `agentflow-api` (`plan: free`, no disk, no cron, no Render DB).
3. **Two** Cloudflare Workers (names already in repo wrangler.toml).
4. **One** Cloudflare Pages project for the Vite frontend + `/api/*` proxy.

## Render API service (proposed)

| Field | Value |
| --- | --- |
| Name | `agentflow-api` |
| Workspace | My Workspace `tea-dahdcde743jc73cgl4dg` |
| Plan | **`free`** (explicit; never Starter) |
| Region | `virginia` (or `oregon` — pick one and match Neon) |
| Runtime | `docker` (Java 21; MCP native runtimes have no Java) |
| Dockerfile | `backend/Dockerfile` |
| Health check | `GET /api/health` |
| Repo | GitHub under **shalofty** (name/visibility TBD — see checkpoints) |
| Branch | `master` |
| Auto-deploy | `yes` after first successful deploy |

### Environment variables (names only; secrets never committed)

| Key | Source |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `demo` |
| `SERVER_PORT` | Render-injected `PORT` (Spring Boot maps `SERVER_PORT`) |
| `SPRING_DATASOURCE_URL` | Neon JDBC URL with TLS (`sslmode=require`); no localhost |
| `SPRING_DATASOURCE_USERNAME` | Neon role |
| `SPRING_DATASOURCE_PASSWORD` | Neon password (Render secret) |
| `AGENTFLOW_MOCK_SHARED_SECRET` | Same value as Worker `MOCK_SHARED_SECRET` |
| `AGENTFLOW_INTEGRATIONS_CUSTOMER_VERIFICATION_BASE_URL` | REST Worker HTTPS origin |
| `AGENTFLOW_INTEGRATIONS_POLICY_ELIGIBILITY_URL` | SOAP Worker HTTPS origin + `/ws` |
| `JAVA_TOOL_OPTIONS` | e.g. `-XX:MaxRAMPercentage=75.0 -XX:+UseSerialGC` (512 MB proof) |

Relaxed Spring binding note: confirm Boot maps `AGENTFLOW_INTEGRATIONS_*` to `agentflow.integrations.*` (relaxed binding). If not, use `application-demo.yml` overrides or dotted env forms Render accepts.

## Cloudflare Workers (proposed)

| Worker | Routes / paths | Secrets / vars |
| --- | --- | --- |
| `agentflow-customer-verification` | `POST /verify` | Secret `MOCK_SHARED_SECRET`; remove empty plaintext `[vars]` conflict |
| `agentflow-policy-eligibility` | `POST /ws`; public `GET` WSDL/XSD | Secret `MOCK_SHARED_SECRET`; var `SOAP_PUBLIC_BASE_URL` = deployed HTTPS origin |

## Cloudflare Pages (proposed)

| Item | Value |
| --- | --- |
| Build | `npm ci && npm run build` with **Pages root directory** `frontend` |
| Output | `dist` |
| Function | `frontend/functions/api/[[path]].js` proxies `/api/*` → Render |
| Env | `AGENTFLOW_API_ORIGIN` = Render HTTPS origin (no secrets) |

Cold-start UX: frontend shows wake/progress for `/api/health` before enabling mutating forms; avoid blind retry of POST/PUT while waking.

## Local scaffolding in this repo (reviewable, not provisioned)

- `backend/Dockerfile` — multi-stage Java 21 image, listens on `$PORT`
- `render.yaml` — Blueprint declaring **only** the Free web service (no databases)
- `frontend/functions/api/[[path]].js` — Pages Function proxy stub
- Wrangler: drop empty `MOCK_SHARED_SECRET` from `[vars]`; document secrets via `wrangler secret put`

## Acceptance checks (after you approve and we provision)

1. Process RSS under ~512 MB on Render Free during submit workflow.
2. Flyway + seeded workflows on Neon TLS.
3. Real REST/SOAP paths, secret gate, public WSDL, demo scenarios.
4. Pages `/api` proxy + cold-start behavior.
5. Re-run Worker compatibility + SOAP client tests against deployed Worker URLs where practical.
6. Read back resource IDs/URLs/plans; confirm all Free.

## User checkpoints before provisioning

1. **Render MCP**: Authenticate/Connect in Cursor until `list_workspaces` returns My Workspace.
2. **Cloudflare**: Click the Workers/Pages email verification link; confirm Workers Free plan in dashboard.
3. **GitHub**: Confirm intended repo (suggest `shalofty/agentflow`, public for portfolio) — local git currently has **no remotes**. Approve create+push and Render GitHub access.
4. **Region pair**: Approve Render `virginia` + Neon `aws-us-east-1` (recommended) or Oregon/`aws-us-west-2`.
5. **Approve this document** to authorize provisioning of the four free resource types above only.

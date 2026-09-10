# AgentFlow cloud deployment (free-only)

Status: **provisioned 2026-09-10** (live health checks passed).
Budget: **$0 recurring.** No paid plans, disks, cron jobs, or Render Postgres.

## Live URLs

| Resource | URL / ID |
| --- | --- |
| GitHub | https://github.com/shalofty/agentflow (public) |
| Pages | https://agentflow-95x.pages.dev |
| API (Render Free) | https://agentflow-api-1b7a.onrender.com |
| REST Worker | https://agentflow-customer-verification.stephanhaloftis.workers.dev |
| SOAP Worker | https://agentflow-policy-eligibility.stephanhaloftis.workers.dev |
| Neon project | `agentflow` / `billowing-cloud-69792598` in org-floral-block-85781342 (`aws-us-east-1`) |
| Render service | `agentflow-api` / `srv-dahe2ph5efls73dmp520` (plan **free**, virginia) |

## Topology

```text
Browser
  → Cloudflare Pages (agentflow-95x SPA)
      + Pages Function /api/* proxy
        → Render Free web service: agentflow-api
            → Neon Free project: agentflow
            → Cloudflare Worker: agentflow-customer-verification (REST)
            → Cloudflare Worker: agentflow-policy-eligibility (SOAP)
```

The Workers are **outbound** dependencies of the Render API (not callers into Render). The Pages Function only proxies browser `/api` traffic to Render.

## Verified

- `GET` Render `/api/health` → `{"status":"UP"}` (after Free cold start)
- Pages `/api/health` proxy → same
- Worker health / public WSDL `200`
- Worker `POST` without `X-AgentFlow-Mock-Key` → `401`

Secrets (`AGENTFLOW_MOCK_SHARED_SECRET`, Neon password) are configured in provider secret stores only — never committed.

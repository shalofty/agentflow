# Local Worker adapters (Cloudflare)

Adapters that preserve AgentFlow's REST and SOAP contracts for Cloudflare Workers
and local proof. Production hosting for the API is **Render Free** (not Koyeb).
See `deploy/DEPLOYMENT.md` before provisioning anything.

## Layout

- `customer-verification/` — `POST /verify` (shared secret required)
- `policy-eligibility/` — `POST /ws` (shared secret required); public `GET` WSDL/XSD

## Local proof

```bash
cd deploy/workers/customer-verification && cp .dev.vars.example .dev.vars && npm install && npm run local-server
cd deploy/workers/policy-eligibility && cp .dev.vars.example .dev.vars && npm install && npm run local-server
# or: npm run dev  (wrangler --local / Miniflare)
```

From `backend/`:

```bash
./mvnw test -Dgroups=worker
./mvnw test -Dtest=PolicyEligibilitySoapClientTest,CustomerVerificationClientTest
```

Secret header: `X-AgentFlow-Mock-Key`  
Demo scenario header (authenticated POSTs only): `X-AgentFlow-Demo-Scenario`

Cloud: `wrangler secret put MOCK_SHARED_SECRET` — never commit secrets. Set `SOAP_PUBLIC_BASE_URL` to the deployed Worker HTTPS origin.

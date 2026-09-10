/**
 * Cloudflare Worker adapter for AgentFlow Customer Verification (REST).
 * Local: npx wrangler dev --local --port 8791
 *
 * POST /verify requires X-AgentFlow-Mock-Key when MOCK_SHARED_SECRET is set.
 * Demo scenarios via X-AgentFlow-Demo-Scenario (only on authenticated POSTs).
 */

const DEMO_HEADER = "X-AgentFlow-Demo-Scenario";
const KEY_HEADER = "X-AgentFlow-Mock-Key";

export default {
  async fetch(request, env) {
    return handleRequest(request, env);
  },
};

export async function handleRequest(request, env) {
  const url = new URL(request.url);

  if (request.method === "GET" && (url.pathname === "/" || url.pathname === "/health")) {
    return json(200, { service: "customer-verification", ok: true });
  }

  if (request.method === "POST" && url.pathname === "/verify") {
    const authError = requireMockKey(request, env);
    if (authError) {
      return authError;
    }

    const scenario = (request.headers.get(DEMO_HEADER) || "").toLowerCase();
    if (scenario === "rest-500") {
      return json(500, { error: "Simulated internal error" });
    }
    if (scenario === "rest-timeout") {
      // Wall-clock wait; Workers CPU time is not charged while awaiting.
      await sleep(3500);
    }

    let body;
    try {
      body = await request.json();
    } catch {
      return json(400, { error: "Invalid JSON" });
    }

    const customerId = body.customerId || "unknown";
    return json(200, { customerId, verified: true, riskScore: 27 });
  }

  return json(404, { error: "Not found" });
}

function requireMockKey(request, env) {
  const expected = env.MOCK_SHARED_SECRET;
  if (!expected) {
    return json(503, { error: "MOCK_SHARED_SECRET is not configured" });
  }
  const provided = request.headers.get(KEY_HEADER);
  if (!provided || provided !== expected) {
    return json(401, { error: "Unauthorized" });
  }
  return null;
}

function json(status, body) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

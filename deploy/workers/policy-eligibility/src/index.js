/**
 * Cloudflare Worker adapter for AgentFlow Policy Eligibility (SOAP 1.1).
 * Local: npx wrangler dev --local --port 8792
 *
 * POST /ws requires X-AgentFlow-Mock-Key when MOCK_SHARED_SECRET is set.
 * GET WSDL/XSD remain public.
 */

import { WSDL, XSD } from "./contract.js";

const NS = "http://agentflow.com/policy-eligibility";
const DEMO_HEADER = "X-AgentFlow-Demo-Scenario";
const KEY_HEADER = "X-AgentFlow-Mock-Key";

export default {
  async fetch(request, env) {
    return handleRequest(request, env);
  },
};

export async function handleRequest(request, env) {
  const url = new URL(request.url);
  const path = url.pathname.replace(/\/+$/, "") || "/";

  if (request.method === "GET" && (path === "/" || path === "/health")) {
    return json(200, { service: "policy-eligibility", ok: true });
  }

  if (request.method === "GET" && path === "/ws/policyEligibility.wsdl") {
    const base = (env.SOAP_PUBLIC_BASE_URL || url.origin).replace(/\/+$/, "");
    return xml(200, WSDL(base + "/ws"));
  }

  if (
    request.method === "GET" &&
    (path === "/ws/policy-eligibility.xsd" || path === "/xsd/policy-eligibility.xsd")
  ) {
    return xml(200, XSD);
  }

  // Spring-WS clients POST to /ws; some also request ?wsdl
  if (request.method === "GET" && (path === "/ws" || path === "/ws/")) {
    if (url.searchParams.has("wsdl")) {
      const base = (env.SOAP_PUBLIC_BASE_URL || url.origin).replace(/\/+$/, "");
      return xml(200, WSDL(base + "/ws"));
    }
    return json(200, { service: "policy-eligibility", endpoint: "/ws" });
  }

  if (request.method === "POST" && (path === "/ws" || path === "/ws/")) {
    const authError = requireMockKey(request, env);
    if (authError) {
      return authError;
    }

    const scenario = (request.headers.get(DEMO_HEADER) || "").toLowerCase();
    const body = await request.text();

    if (scenario === "soap-fault") {
      return soapFault("Simulated policy eligibility service fault");
    }

    const applicationId = extractTag(body, "applicationId") || "unknown";
    const riskScore = Number.parseInt(extractTag(body, "riskScore") || "0", 10);
    const correlationId = extractCorrelationId(body);

    let decision;
    if (scenario === "soap-manual-review") {
      decision = "MANUAL_REVIEW";
    } else {
      decision = decisionFor(riskScore);
    }

    return soapResponse(applicationId, decision, correlationId);
  }

  return json(404, { error: "Not found" });
}

function decisionFor(riskScore) {
  if (riskScore >= 80) {
    return "INELIGIBLE";
  }
  if (riskScore >= 50) {
    return "MANUAL_REVIEW";
  }
  return "ELIGIBLE";
}

function requireMockKey(request, env) {
  const expected = env.MOCK_SHARED_SECRET;
  if (!expected) {
    return json(503, { error: "MOCK_SHARED_SECRET is not configured" });
  }
  const provided = request.headers.get(KEY_HEADER);
  if (!provided || provided !== expected) {
    return new Response(
      soapFaultBody("Unauthorized"),
      {
        status: 401,
        headers: {
          "Content-Type": 'text/xml; charset=utf-8',
        },
      },
    );
  }
  return null;
}

function soapResponse(applicationId, decision, correlationId) {
  const header =
    correlationId != null
      ? `<soapenv:Header><correlationId xmlns="${NS}">${escapeXml(correlationId)}</correlationId></soapenv:Header>`
      : "<soapenv:Header/>";
  // Children stay unqualified (empty namespace) to match AgentFlow JAXB bindings /
  // Spring-WS mock payloads. Only the root element carries the target namespace.
  const envelope = `<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
  ${header}
  <soapenv:Body>
    <pol:CheckEligibilityResponse xmlns:pol="${NS}">
      <applicationId>${escapeXml(applicationId)}</applicationId>
      <decision>${decision}</decision>
    </pol:CheckEligibilityResponse>
  </soapenv:Body>
</soapenv:Envelope>`;
  return new Response(envelope, {
    status: 200,
    headers: {
      "Content-Type": "text/xml; charset=utf-8",
    },
  });
}

function soapFault(message) {
  return new Response(soapFaultBody(message), {
    status: 500,
    headers: {
      "Content-Type": "text/xml; charset=utf-8",
    },
  });
}

function soapFaultBody(message) {
  return `<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
  <soapenv:Body>
    <soapenv:Fault>
      <faultcode>soapenv:Server</faultcode>
      <faultstring>${escapeXml(message)}</faultstring>
    </soapenv:Fault>
  </soapenv:Body>
</soapenv:Envelope>`;
}

function extractTag(xml, localName) {
  const re = new RegExp(
    `<(?:[\\w.-]+:)?${localName}(?:\\s[^>]*)?>([^<]*)</(?:[\\w.-]+:)?${localName}>`,
    "i",
  );
  const match = xml.match(re);
  return match ? match[1].trim() : null;
}

function extractCorrelationId(xml) {
  return extractTag(xml, "correlationId");
}

function escapeXml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&apos;");
}

function json(status, body) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function xml(status, body) {
  return new Response(body, {
    status,
    headers: { "Content-Type": "text/xml; charset=utf-8" },
  });
}

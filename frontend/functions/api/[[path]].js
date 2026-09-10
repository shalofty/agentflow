/**
 * Cloudflare Pages Function: proxy /api/* to the Render Free AgentFlow API.
 * Set AGENTFLOW_API_ORIGIN in Pages env (e.g. https://agentflow-api.onrender.com).
 * Cold starts can take tens of seconds; the frontend should poll /api/health first.
 */
export async function onRequest(context) {
  const origin = context.env.AGENTFLOW_API_ORIGIN;
  if (!origin) {
    return new Response(
      JSON.stringify({
        title: "Misconfigured",
        detail: "AGENTFLOW_API_ORIGIN is not set",
      }),
      { status: 503, headers: { "Content-Type": "application/json" } },
    );
  }

  const incoming = new URL(context.request.url);
  const upstream = new URL(incoming.pathname + incoming.search, origin.replace(/\/+$/, "/"));

  const headers = new Headers(context.request.headers);
  headers.delete("host");
  headers.set("X-Forwarded-Proto", "https");

  const init = {
    method: context.request.method,
    headers,
    redirect: "manual",
  };
  if (context.request.method !== "GET" && context.request.method !== "HEAD") {
    init.body = context.request.body;
  }

  // Long timeout to tolerate Render Free wake-up; frontend must not blind-retry mutations.
  const response = await fetch(upstream, init);
  return new Response(response.body, {
    status: response.status,
    statusText: response.statusText,
    headers: response.headers,
  });
}

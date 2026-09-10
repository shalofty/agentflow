/**
 * Cloudflare Pages Function: proxy /api/* to the Render Free AgentFlow API.
 * Set AGENTFLOW_API_ORIGIN in Pages env (e.g. https://agentflow-api-1b7a.onrender.com).
 * Cold starts can take tens of seconds; the frontend polls /api/health first.
 */
export async function onRequest(context) {
  const origin = context.env.AGENTFLOW_API_ORIGIN;
  if (!origin) {
    return new Response(
      JSON.stringify({
        title: "Misconfigured",
        detail: "AGENTFLOW_API_ORIGIN is not set",
        status: 503,
      }),
      { status: 503, headers: { "Content-Type": "application/problem+json" } },
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
    // Tolerate Render Free wake-up; frontend must not blind-retry mutations.
    signal: AbortSignal.timeout(90_000),
  };
  if (context.request.method !== "GET" && context.request.method !== "HEAD") {
    init.body = context.request.body;
  }

  try {
    const response = await fetch(upstream, init);
    return new Response(response.body, {
      status: response.status,
      statusText: response.statusText,
      headers: response.headers,
    });
  } catch (error) {
    const detail =
      error?.name === "TimeoutError" || error?.name === "AbortError"
        ? "Upstream API timed out (free host may still be waking)."
        : "Unable to reach upstream API.";
    return new Response(
      JSON.stringify({
        title: "Bad Gateway",
        detail,
        status: 502,
      }),
      { status: 502, headers: { "Content-Type": "application/problem+json" } },
    );
  }
}

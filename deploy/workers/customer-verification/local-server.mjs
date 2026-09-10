/**
 * Same handler as the Worker, served via Node for hermetic JUnit ProcessBuilder proofs
 * when wrangler/workerd is unavailable. Prefer `npm run dev` (wrangler --local) for
 * the primary Worker-runtime proof.
 */
import http from "node:http";
import { handleRequest } from "./src/index.js";

const PORT = Number(process.env.PORT || 8791);
const env = {
  MOCK_SHARED_SECRET: process.env.MOCK_SHARED_SECRET || "local-proof-secret",
};

const server = http.createServer(async (req, res) => {
  try {
    const chunks = [];
    for await (const chunk of req) {
      chunks.push(chunk);
    }
    const body = Buffer.concat(chunks);
    const url = `http://127.0.0.1:${PORT}${req.url}`;
    const headers = new Headers();
    for (const [key, value] of Object.entries(req.headers)) {
      if (value !== undefined) {
        headers.set(key, Array.isArray(value) ? value.join(", ") : value);
      }
    }
    const request = new Request(url, {
      method: req.method,
      headers,
      body: ["GET", "HEAD"].includes(req.method) ? undefined : body,
    });
    const response = await handleRequest(request, env);
    const responseBody = Buffer.from(await response.arrayBuffer());
    const outHeaders = {};
    response.headers.forEach((value, key) => {
      outHeaders[key] = value;
    });
    res.writeHead(response.status, outHeaders);
    res.end(responseBody);
  } catch (err) {
    res.writeHead(500, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ error: String(err) }));
  }
});

server.listen(PORT, "127.0.0.1", () => {
  console.log(`customer-verification local-server on ${PORT}`);
});

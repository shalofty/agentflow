const http = require("http");

const PORT = process.env.PORT || 8091;
let faultMode = "none";

function readBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    req.on("data", (chunk) => chunks.push(chunk));
    req.on("end", () => {
      try {
        resolve(JSON.parse(Buffer.concat(chunks).toString("utf8") || "{}"));
      } catch (err) {
        reject(err);
      }
    });
    req.on("error", reject);
  });
}

const server = http.createServer(async (req, res) => {
  if (req.method === "POST" && req.url === "/admin/faults") {
    try {
      const body = await readBody(req);
      const mode = body.mode || "none";
      if (!["none", "http500", "timeout", "http503"].includes(mode)) {
        res.writeHead(400, { "Content-Type": "application/json" });
        res.end(JSON.stringify({ error: "Invalid mode" }));
        return;
      }
      faultMode = mode;
      res.writeHead(200, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ mode: faultMode }));
    } catch {
      res.writeHead(400, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ error: "Invalid JSON" }));
    }
    return;
  }

  if (req.method === "POST" && req.url === "/verify") {
    let body;
    try {
      body = await readBody(req);
    } catch {
      res.writeHead(400, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ error: "Invalid JSON" }));
      return;
    }

    if (faultMode === "http500") {
      res.writeHead(500, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ error: "Simulated internal error" }));
      return;
    }
    if (faultMode === "http503") {
      res.writeHead(503, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ error: "Simulated service unavailable" }));
      return;
    }
    if (faultMode === "timeout") {
      return;
    }

    const customerId = body.customerId || "unknown";
    res.writeHead(200, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ customerId, verified: true, riskScore: 27 }));
    return;
  }

  res.writeHead(404, { "Content-Type": "application/json" });
  res.end(JSON.stringify({ error: "Not found" }));
});

server.listen(PORT, () => {
  console.log(`Customer verification mock listening on ${PORT}`);
});

package com.agentflow.integration.worker;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Starts the Worker handler via Node local-server (same {@code handleRequest} export as wrangler).
 * Prefer this for hermetic JUnit; run {@code npm run dev} separately for workerd/wrangler proof.
 */
final class WorkerLocalProcess implements AutoCloseable {

  private final Process process;
  private final int port;
  private final Path workerDir;

  private WorkerLocalProcess(Process process, int port, Path workerDir) {
    this.process = process;
    this.port = port;
    this.workerDir = workerDir;
  }

  static WorkerLocalProcess start(Path workerDir, String mockSecret) throws Exception {
    int port = freePort();
    Path nodeModules = workerDir.resolve("node_modules");
    if (!Files.isDirectory(nodeModules)) {
      Process install =
          new ProcessBuilder("npm", "install", "--no-fund", "--no-audit")
              .directory(workerDir.toFile())
              .redirectErrorStream(true)
              .start();
      if (!install.waitFor(120, TimeUnit.SECONDS) || install.exitValue() != 0) {
        throw new IllegalStateException(
            "npm install failed in " + workerDir + ": " + new String(install.getInputStream().readAllBytes()));
      }
    }

    List<String> command = new ArrayList<>();
    command.add("node");
    command.add("local-server.mjs");

    ProcessBuilder builder =
        new ProcessBuilder(command)
            .directory(workerDir.toFile())
            .redirectErrorStream(true);
    builder.environment().put("PORT", String.valueOf(port));
    builder.environment().put("MOCK_SHARED_SECRET", mockSecret);
    builder.environment().put("SOAP_PUBLIC_BASE_URL", "http://127.0.0.1:" + port);

    Process process = builder.start();
    WorkerLocalProcess started = new WorkerLocalProcess(process, port, workerDir);
    try {
      started.awaitReady();
      return started;
    } catch (Exception ex) {
      started.close();
      throw ex;
    }
  }

  int port() {
    return port;
  }

  String baseUrl() {
    return "http://127.0.0.1:" + port;
  }

  private void awaitReady() throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
    StringBuilder log = new StringBuilder();
    Thread logDrain =
        new Thread(
            () -> {
              try {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = process.getInputStream().read(buffer)) >= 0) {
                  log.append(new String(buffer, 0, read));
                }
              } catch (IOException ignored) {
                // process closed
              }
            },
            "worker-log-" + port);
    logDrain.setDaemon(true);
    logDrain.start();

    while (System.nanoTime() < deadline) {
      if (!process.isAlive()) {
        throw new IllegalStateException(
            "Worker process exited early for " + workerDir + ": " + log);
      }
      try {
        HttpResponse<String> response =
            client.send(
                HttpRequest.newBuilder(URI.create(baseUrl() + "/health"))
                    .timeout(Duration.ofMillis(500))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
          return;
        }
      } catch (Exception ignored) {
        Thread.sleep(100);
      }
    }
    throw new IllegalStateException(
        "Timed out waiting for worker at " + baseUrl() + ". Log:\n" + log);
  }

  private static int freePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }

  @Override
  public void close() {
    process.destroy();
    try {
      if (!process.waitFor(5, TimeUnit.SECONDS)) {
        process.destroyForcibly();
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      process.destroyForcibly();
    }
  }
}

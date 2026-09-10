package com.agentflow.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("demo")
@Testcontainers(disabledWithoutDocker = true)
class DemoFailureIT {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("agentflow")
          .withUsername("agentflow")
          .withPassword("agentflow");

  static final MockWebServer verificationServer = new MockWebServer();
  static final HttpServer eligibilityServer;

  static {
    try {
      verificationServer.setDispatcher(
          new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
              String scenario = request.getHeader("X-AgentFlow-Demo-Scenario");
              if ("rest-500".equalsIgnoreCase(scenario)) {
                return new MockResponse()
                    .setResponseCode(500)
                    .setBody("{\"error\":\"simulated\"}");
              }
              if ("rest-timeout".equalsIgnoreCase(scenario)) {
                return new MockResponse().setBodyDelay(5, TimeUnit.SECONDS).setBody("{}");
              }
              String body = request.getBody().readUtf8();
              String customerId = "00000000-0000-0000-0000-000000000001";
              int idx = body.indexOf("customerId");
              if (idx >= 0) {
                int start = body.indexOf('"', idx + 11) + 1;
                int end = body.indexOf('"', start);
                if (start > 0 && end > start) {
                  customerId = body.substring(start, end);
                }
              }
              return new MockResponse()
                  .setHeader("Content-Type", "application/json")
                  .setBody(
                      "{\"customerId\":\""
                          + customerId
                          + "\",\"verified\":true,\"riskScore\":25}");
            }
          });
      verificationServer.start();

      eligibilityServer = HttpServer.create(new InetSocketAddress(0), 0);
      eligibilityServer.createContext(
          "/ws",
          exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
              exchange.sendResponseHeaders(404, -1);
              exchange.close();
              return;
            }
            String scenario =
                exchange.getRequestHeaders().getFirst("X-AgentFlow-Demo-Scenario");
            byte[] requestBytes = exchange.getRequestBody().readAllBytes();
            String requestXml = new String(requestBytes, StandardCharsets.UTF_8);
            String applicationId = extract(requestXml, "applicationId");

            String responseXml;
            int status;
            if ("soap-fault".equalsIgnoreCase(scenario)) {
              status = 500;
              responseXml =
                  """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                    <soapenv:Body>
                      <soapenv:Fault>
                        <faultcode>soapenv:Server</faultcode>
                        <faultstring>Simulated policy eligibility service fault</faultstring>
                      </soapenv:Fault>
                    </soapenv:Body>
                  </soapenv:Envelope>
                  """;
            } else {
              status = 200;
              String decision =
                  "soap-manual-review".equalsIgnoreCase(scenario) ? "MANUAL_REVIEW" : "ELIGIBLE";
              responseXml =
                  """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                    <soapenv:Body>
                      <pol:CheckEligibilityResponse xmlns:pol="http://agentflow.com/policy-eligibility">
                        <applicationId>%s</applicationId>
                        <decision>%s</decision>
                      </pol:CheckEligibilityResponse>
                    </soapenv:Body>
                  </soapenv:Envelope>
                  """
                      .formatted(applicationId == null ? "unknown" : applicationId, decision);
            }
            byte[] bytes = responseXml.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/xml; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
              os.write(bytes);
            }
          });
      eligibilityServer.setExecutor(Executors.newCachedThreadPool());
      eligibilityServer.start();
    } catch (IOException ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  @AfterAll
  static void stopMocks() throws IOException {
    verificationServer.shutdown();
    eligibilityServer.stop(0);
  }

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("agentflow.integrations.customer-verification.retry-backoff-ms", () -> 1);
    registry.add(
        "agentflow.integrations.customer-verification.base-url",
        () -> verificationServer.url("/").toString().replaceAll("/$", ""));
    registry.add("agentflow.integrations.customer-verification.read-timeout-ms", () -> 800);
    registry.add(
        "agentflow.integrations.policy-eligibility.url",
        () -> "http://127.0.0.1:" + eligibilityServer.getAddress().getPort() + "/ws");
  }

  @Autowired TestRestTemplate rest;

  @Test
  void demoProfileReportsEnabled() {
    var response = rest.getForEntity("/api/demo/enabled", Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsEntry("enabled", true);
  }

  @Test
  void rest500HeaderEndsInIntegrationFailureWithFailedActivities() {
    UUID applicationId = createValidDraft();

    var response = submitWithHeader(applicationId, "rest-500");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsEntry("status", "INTEGRATION_FAILURE");
    assertThat(getActivities(applicationId))
        .extracting(activity -> activity.get("status"))
        .containsExactly("FAILED", "FAILED", "FAILED");
    assertThat(getActivities(applicationId))
        .extracting(activity -> activity.get("httpStatus"))
        .containsExactly(500, 500, 500);
  }

  @Test
  void restTimeoutBodyEndsInIntegrationFailure() {
    UUID applicationId = createValidDraft();

    var response =
        rest.postForEntity(
            "/api/applications/" + applicationId + "/submit",
            Map.of("demoScenario", "rest-timeout"),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsEntry("status", "INTEGRATION_FAILURE");
    assertThat(getActivities(applicationId))
        .extracting(activity -> activity.get("error"))
        .allMatch(error -> error.toString().contains("timed out"));
  }

  @Test
  void soapFaultEndsInIntegrationFailureAfterRestSuccess() {
    UUID applicationId = createValidDraft();

    var response = submitWithHeader(applicationId, "soap-fault");

    assertThat(response.getBody()).containsEntry("status", "INTEGRATION_FAILURE");
    assertThat(getActivities(applicationId))
        .extracting(activity -> activity.get("status"))
        .containsExactly("SUCCESS", "FAILED", "FAILED", "FAILED");
  }

  @Test
  void soapManualReviewEndsInManualReview() {
    UUID applicationId = createValidDraft();

    var response = submitWithHeader(applicationId, "soap-manual-review");

    assertThat(response.getBody()).containsEntry("status", "MANUAL_REVIEW");
    assertThat(getActivities(applicationId))
        .extracting(activity -> activity.get("status"))
        .containsExactly("SUCCESS", "SUCCESS");
  }

  private org.springframework.http.ResponseEntity<Map> submitWithHeader(
      UUID applicationId, String scenario) {
    var headers = new HttpHeaders();
    headers.set("X-Demo-Scenario", scenario);
    return rest.exchange(
        "/api/applications/" + applicationId + "/submit",
        HttpMethod.POST,
        new HttpEntity<>(null, headers),
        Map.class);
  }

  private UUID createValidDraft() {
    UUID customerId = createCustomer();
    var created =
        rest.postForEntity(
            "/api/applications",
            Map.of("customerId", customerId, "workflowKey", "auto-policy"),
            Map.class);
    UUID applicationId = UUID.fromString(created.getBody().get("id").toString());
    var payload =
        Map.of(
            "vin", "1HGCM82633A123456",
            "vehicleYear", 2020,
            "coverageType", "LIABILITY",
            "hasGarage", false,
            "effectiveDate", "2026-01-01");
    rest.exchange(
        "/api/applications/" + applicationId + "/data",
        HttpMethod.PUT,
        new HttpEntity<>(Map.of("payload", payload)),
        Map.class);
    return applicationId;
  }

  private UUID createCustomer() {
    var created =
        rest.postForEntity(
            "/api/customers",
            Map.of(
                "firstName", "Demo",
                "lastName", "Failure",
                "email", "demo-" + UUID.randomUUID() + "@example.com"),
            Map.class);
    return UUID.fromString(created.getBody().get("id").toString());
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> getActivities(UUID applicationId) {
    return rest.getForObject(
        "/api/applications/" + applicationId + "/activities", List.class);
  }

  private static String extract(String xml, String tag) {
    java.util.regex.Matcher matcher =
        java.util.regex.Pattern.compile(
                "<(?:[\\w.-]+:)?" + tag + "(?:\\s[^>]*)?>([^<]*)</(?:[\\w.-]+:)?" + tag + ">",
                java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(xml);
    return matcher.find() ? matcher.group(1) : null;
  }
}

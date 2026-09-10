package com.agentflow.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agentflow.integration.RetryableIntegrationException;
import com.agentflow.integration.eligibility.EligibilityResult;
import com.agentflow.integration.eligibility.PolicyEligibilityClient;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class ApplicationSubmitIT {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("agentflow")
          .withUsername("agentflow")
          .withPassword("agentflow");

  static final MockWebServer verificationServer = startVerificationServer();

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add(
        "agentflow.integrations.customer-verification.base-url",
        () -> verificationServer.url("/").toString());
    registry.add("agentflow.integrations.customer-verification.retry-backoff-ms", () -> 1);
  }

  @Autowired TestRestTemplate rest;
  @MockBean PolicyEligibilityClient eligibilityClient;

  @AfterAll
  static void stopServer() throws IOException {
    verificationServer.shutdown();
  }

  @Test
  void submitHappyPathEndsApprovedWithActivityAndCorrelationId() throws InterruptedException {
    UUID appId = createValidDraft();
    when(eligibilityClient.check(org.mockito.ArgumentMatchers.any()))
        .thenReturn(EligibilityResult.ELIGIBLE);
    verificationServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(
                """
                {"customerId":"%s","verified":true,"riskScore":12}
                """
                    .formatted(getApplication(appId).get("customerId"))));

    var headers = new HttpHeaders();
    headers.set("X-Correlation-Id", "submit-it-correlation");
    var response =
        rest.exchange(
            "/api/applications/" + appId + "/submit",
            HttpMethod.POST,
            new HttpEntity<>(null, headers),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody())
        .containsEntry("status", "APPROVED")
        .containsEntry("correlationId", "submit-it-correlation");
    assertThat(verificationServer.takeRequest().getHeader("X-Correlation-Id"))
        .isEqualTo("submit-it-correlation");

    var activities = getActivities(appId);
    assertThat(activities)
        .extracting(row -> row.get("integration"))
        .containsExactly("CustomerVerification", "PolicyEligibility");
    assertThat(activities)
        .extracting(row -> row.get("integrationType"))
        .containsExactly("REST", "SOAP");
    assertThat(activities)
        .extracting(row -> row.get("status"))
        .containsExactly("SUCCESS", "SUCCESS");
    assertThat(activities).extracting(row -> row.get("attempt")).containsExactly(1, 1);
  }

  @Test
  void retryableFailuresCreateFailedRowsBeforeSuccess() {
    UUID appId = createValidDraft();
    when(eligibilityClient.check(org.mockito.ArgumentMatchers.any()))
        .thenReturn(EligibilityResult.ELIGIBLE);
    verificationServer.enqueue(new MockResponse().setResponseCode(503));
    verificationServer.enqueue(new MockResponse().setResponseCode(500));
    verificationServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(
                """
                {"customerId":"%s","verified":true,"riskScore":8}
                """
                    .formatted(getApplication(appId).get("customerId"))));

    var response = rest.postForEntity("/api/applications/" + appId + "/submit", null, Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsEntry("status", "APPROVED");
    var verificationActivities =
        getActivities(appId).stream()
            .filter(row -> row.get("integration").equals("CustomerVerification"))
            .toList();
    assertThat(verificationActivities)
        .extracting(row -> row.get("status"))
        .containsExactly("FAILED", "FAILED", "SUCCESS");
    assertThat(verificationActivities)
        .extracting(row -> row.get("httpStatus"))
        .containsExactly(503, 500, 200);
  }

  @Test
  void exhaustedSoapFaultsEndIntegrationFailureWithFailedActivities() {
    UUID appId = createValidDraft();
    when(eligibilityClient.check(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new RetryableIntegrationException("Policy eligibility SOAP fault"));
    verificationServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(
                """
                {"customerId":"%s","verified":true,"riskScore":27}
                """
                    .formatted(getApplication(appId).get("customerId"))));

    var response = rest.postForEntity("/api/applications/" + appId + "/submit", null, Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsEntry("status", "INTEGRATION_FAILURE");
    var eligibilityActivities =
        getActivities(appId).stream()
            .filter(row -> row.get("integration").equals("PolicyEligibility"))
            .toList();
    assertThat(eligibilityActivities)
        .extracting(row -> row.get("status"))
        .containsExactly("FAILED", "FAILED", "FAILED");
    assertThat(eligibilityActivities)
        .extracting(row -> row.get("integrationType"))
        .containsOnly("SOAP");
    assertThat(eligibilityActivities)
        .extracting(row -> row.get("attempt"))
        .containsExactly(1, 2, 3);
  }

  @Test
  void unverifiedCustomerEndsManualReviewWithoutEligibilityCall() {
    UUID appId = createValidDraft();
    verificationServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(
                """
                {"customerId":"%s","verified":false,"riskScore":70}
                """
                    .formatted(getApplication(appId).get("customerId"))));

    var response = rest.postForEntity("/api/applications/" + appId + "/submit", null, Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsEntry("status", "MANUAL_REVIEW");
    verify(eligibilityClient, never()).check(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void doubleSubmitReturnsConflictWithoutAnotherVerificationCall() {
    UUID appId = createValidDraft();
    when(eligibilityClient.check(org.mockito.ArgumentMatchers.any()))
        .thenReturn(EligibilityResult.ELIGIBLE);
    verificationServer.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(
                """
                {"customerId":"%s","verified":true,"riskScore":10}
                """
                    .formatted(getApplication(appId).get("customerId"))));

    assertThat(rest.postForEntity("/api/applications/" + appId + "/submit", null, Map.class)
            .getStatusCode())
        .isEqualTo(HttpStatus.OK);
    int requestCount = verificationServer.getRequestCount();

    var second = rest.postForEntity("/api/applications/" + appId + "/submit", null, Map.class);

    assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(verificationServer.getRequestCount()).isEqualTo(requestCount);
  }

  private UUID createValidDraft() {
    UUID customerId = createCustomer();
    var created =
        rest.postForEntity(
            "/api/applications",
            Map.of("customerId", customerId, "workflowKey", "auto-policy"),
            Map.class);
    UUID appId = UUID.fromString(created.getBody().get("id").toString());
    var payload =
        Map.of(
            "vin", "1HGCM82633A123456",
            "vehicleYear", 2020,
            "coverageType", "LIABILITY",
            "hasGarage", false,
            "effectiveDate", "2026-01-01");
    var saved =
        rest.exchange(
            "/api/applications/" + appId + "/data",
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("payload", payload)),
            Map.class);
    assertThat(saved.getStatusCode()).isEqualTo(HttpStatus.OK);
    return appId;
  }

  private UUID createCustomer() {
    var body =
        Map.of(
            "firstName", "Submit",
            "lastName", "Test",
            "email", "submit-" + UUID.randomUUID() + "@example.com");
    var created = rest.postForEntity("/api/customers", body, Map.class);
    return UUID.fromString(created.getBody().get("id").toString());
  }

  private Map<?, ?> getApplication(UUID appId) {
    return rest.getForObject("/api/applications/" + appId, Map.class);
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> getActivities(UUID appId) {
    return rest.getForObject("/api/applications/" + appId + "/activities", List.class);
  }

  private static MockWebServer startVerificationServer() {
    var server = new MockWebServer();
    try {
      server.start();
      return server;
    } catch (IOException ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }
}

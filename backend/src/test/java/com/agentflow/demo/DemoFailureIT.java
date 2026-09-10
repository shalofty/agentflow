package com.agentflow.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
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

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("agentflow.integrations.customer-verification.retry-backoff-ms", () -> 1);
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
}

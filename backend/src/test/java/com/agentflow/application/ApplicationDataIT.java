package com.agentflow.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ApplicationDataIT {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("agentflow")
          .withUsername("agentflow")
          .withPassword("agentflow");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired TestRestTemplate rest;
  @Autowired ApplicationRepository applicationRepository;
  @Autowired TransactionTemplate transactionTemplate;

  @Test
  void rejectsSaveWhenRequiredFieldMissing() {
    var customerId = createCustomer();
    var appId = createApplication(customerId);

    var response =
        rest.exchange(
            "/api/applications/" + appId + "/data",
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("payload", Map.of("coverageType", "LIABILITY"))),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    @SuppressWarnings("unchecked")
    var errors = (Map<String, String>) response.getBody().get("errors");
    assertThat(errors).containsKey("vin");
  }

  @Test
  void rejectsDataMutationWhenNotDraft() {
    var customerId = createCustomer();
    var appId = createApplication(customerId);

    transactionTemplate.executeWithoutResult(
        status -> {
          var application = applicationRepository.findById(appId).orElseThrow();
          application.setStatus(ApplicationStatus.SUBMITTED);
          applicationRepository.save(application);
        });

    var response =
        rest.exchange(
            "/api/applications/" + appId + "/data",
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of(
                    "payload",
                    Map.of(
                        "vin", "1HGCM82633A123456",
                        "vehicleYear", 2020,
                        "coverageType", "LIABILITY",
                        "hasGarage", false,
                        "effectiveDate", "2026-01-01"))),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  private UUID createCustomer() {
    var body =
        Map.of(
            "firstName", "Test",
            "lastName", "User",
            "email", "test-" + UUID.randomUUID() + "@example.com");
    var created = rest.postForEntity("/api/customers", body, Map.class);
    return UUID.fromString(created.getBody().get("id").toString());
  }

  private UUID createApplication(UUID customerId) {
    var created =
        rest.postForEntity(
            "/api/applications",
            Map.of("customerId", customerId, "workflowKey", "auto-policy"),
            Map.class);
    return UUID.fromString(created.getBody().get("id").toString());
  }
}

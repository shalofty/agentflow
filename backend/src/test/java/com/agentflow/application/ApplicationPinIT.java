package com.agentflow.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentflow.workflow.WorkflowDefinition;
import com.agentflow.workflow.WorkflowDefinitionRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ApplicationPinIT {

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
  @Autowired WorkflowDefinitionRepository workflowRepository;
  @Autowired TransactionTemplate transactionTemplate;

  @Test
  void createApplicationPinsActiveWorkflowDefinition() {
    var customerId = createCustomer();
    var created =
        rest.postForEntity(
            "/api/applications",
            Map.of("customerId", customerId, "workflowKey", "auto-policy"),
            Map.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody().get("status")).isEqualTo("DRAFT");
    assertThat(created.getBody().get("workflowKey")).isEqualTo("auto-policy");
    assertThat(created.getBody().get("workflowVersion")).isEqualTo(1);
    assertThat(created.getBody().get("workflowDefinitionId")).isNotNull();
  }

  @Test
  void draftKeepsPinnedDefinitionWhenNewerVersionBecomesActive() {
    var customerId = createCustomer();
    var created =
        rest.postForEntity(
            "/api/applications",
            Map.of("customerId", customerId, "workflowKey", "auto-policy"),
            Map.class);
    var appId = created.getBody().get("id");

    activateAutoPolicyV2();

    var def = rest.getForEntity("/api/applications/" + appId + "/definition", Map.class);
    assertThat(def.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(def.getBody().get("version")).isEqualTo(1);

    var active = rest.getForEntity("/api/workflows/auto-policy", Map.class);
    assertThat(active.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(active.getBody().get("version")).isEqualTo(2);
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

  private void activateAutoPolicyV2() {
    transactionTemplate.executeWithoutResult(
        status -> {
          workflowRepository.deactivateAllActiveForKey("auto-policy");
          workflowRepository.save(
              new WorkflowDefinition(
                  UUID.randomUUID(),
                  "auto-policy",
                  "New Auto Policy",
                  2,
                  """
                  {
                    "workflowKey": "auto-policy",
                    "title": "New Auto Policy",
                    "version": 2,
                    "fields": [
                      { "name": "vin", "label": "VIN", "type": "text", "required": true }
                    ]
                  }
                  """,
                  true,
                  Instant.now()));
        });
  }
}

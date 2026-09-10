package com.agentflow.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class HomeConfigOnlyProofIT {

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

  @Test
  void homePolicyServedFromConfigurationOnly() {
    var workflow = rest.getForEntity("/api/workflows/home-policy", Map.class);
    assertThat(workflow.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(workflow.getBody().get("workflowKey")).isEqualTo("home-policy");
    assertThat(workflow.getBody().get("title")).isEqualTo("New Home Policy");

    @SuppressWarnings("unchecked")
    var definitionJson = (Map<String, Object>) workflow.getBody().get("definitionJson");
    @SuppressWarnings("unchecked")
    var fields = (List<Map<String, Object>>) definitionJson.get("fields");
    assertThat(fields).extracting(field -> field.get("name"))
        .contains("address", "dwellingValue", "occupancyType")
        .doesNotContain("vin", "vehicleYear");
  }

  @Test
  void createApplicationWithHomePolicyPinsHomeDefinition() {
    var customerId = createCustomer();
    var created =
        rest.postForEntity(
            "/api/applications",
            Map.of("customerId", customerId, "workflowKey", "home-policy"),
            Map.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(created.getBody().get("workflowKey")).isEqualTo("home-policy");
    assertThat(created.getBody().get("workflowVersion")).isEqualTo(1);

    var appId = created.getBody().get("id");
    var definition = rest.getForEntity("/api/applications/" + appId + "/definition", Map.class);
    assertThat(definition.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(definition.getBody().get("workflowKey")).isEqualTo("home-policy");
  }

  private UUID createCustomer() {
    var body =
        Map.of(
            "firstName", "Home",
            "lastName", "Owner",
            "email", "home-" + UUID.randomUUID() + "@example.com");
    var created = rest.postForEntity("/api/customers", body, Map.class);
    return UUID.fromString(created.getBody().get("id").toString());
  }
}

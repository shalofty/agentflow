package com.agentflow.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class WorkflowDefinitionIT {

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
  void seedsAutoPolicyAndServesActiveDefinition() {
    var res = rest.getForEntity("/api/workflows/auto-policy", Map.class);
    assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(res.getBody().get("workflowKey")).isEqualTo("auto-policy");
    assertThat(res.getBody().get("version")).isEqualTo(1);
  }

  @Test
  void seedsHomePolicyAndServesActiveDefinition() {
    var res = rest.getForEntity("/api/workflows/home-policy", Map.class);
    assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(res.getBody().get("workflowKey")).isEqualTo("home-policy");
    assertThat(res.getBody().get("version")).isEqualTo(1);
    assertThat(res.getBody().get("title")).isEqualTo("New Home Policy");
  }

  @Test
  void listsActiveWorkflowSummaries() {
    var res = rest.getForEntity("/api/workflows", List.class);
    assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(res.getBody()).isNotEmpty();
  }
}

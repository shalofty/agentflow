package com.agentflow.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WorkflowDefinitionSeeder implements ApplicationRunner {

  private final WorkflowDefinitionRepository repository;
  private final ObjectMapper objectMapper;

  public WorkflowDefinitionSeeder(
      WorkflowDefinitionRepository repository, ObjectMapper objectMapper) {
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) throws Exception {
    var resolver = new PathMatchingResourcePatternResolver();
    var resources = resolver.getResources("classpath:workflows/*.json");
    for (var resource : resources) {
      seedFromResource(resource.getContentAsString(StandardCharsets.UTF_8));
    }
  }

  private void seedFromResource(String json) throws IOException {
    JsonNode root = objectMapper.readTree(json);
    validateDefinitionStructure(root);

    var workflowKey = requiredText(root, "workflowKey");
    var title = requiredText(root, "title");
    var version = root.get("version").asInt();
    var canonicalJson = objectMapper.writeValueAsString(root);

    repository.deactivateAllActiveForKey(workflowKey);

    var existing = repository.findByWorkflowKeyAndVersion(workflowKey, version);
    if (existing.isPresent()) {
      var definition = existing.get();
      definition.setTitle(title);
      definition.setDefinitionJson(canonicalJson);
      definition.setActive(true);
      repository.save(definition);
      return;
    }

    repository.save(
        new WorkflowDefinition(
            UUID.randomUUID(),
            workflowKey,
            title,
            version,
            canonicalJson,
            true,
            Instant.now()));
  }

  private void validateDefinitionStructure(JsonNode root) {
    if (!root.hasNonNull("workflowKey")
        || !root.hasNonNull("title")
        || !root.hasNonNull("version")
        || !root.has("fields")
        || !root.get("fields").isArray()) {
      throw new IllegalStateException("Invalid workflow definition file: missing required fields");
    }

    var fields = root.get("fields");
    for (var field : fields) {
      if (!field.hasNonNull("name") || !field.hasNonNull("type")) {
        throw new IllegalStateException("Invalid workflow definition file: field missing name/type");
      }
    }
  }

  private String requiredText(JsonNode root, String field) {
    var node = root.get(field);
    if (node == null || node.isNull() || node.asText().isBlank()) {
      throw new IllegalStateException("Invalid workflow definition file: missing " + field);
    }
    return node.asText();
  }
}

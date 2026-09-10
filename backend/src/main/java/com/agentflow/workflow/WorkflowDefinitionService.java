package com.agentflow.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowDefinitionService {

  private final WorkflowDefinitionRepository repository;
  private final ObjectMapper objectMapper;

  public WorkflowDefinitionService(
      WorkflowDefinitionRepository repository, ObjectMapper objectMapper) {
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public List<WorkflowSummaryResponse> listActive() {
    return repository.findAllByActiveTrue().stream().map(WorkflowSummaryResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public WorkflowDetailResponse getActiveByKey(String key) {
    var definition =
        repository
            .findByWorkflowKeyAndActiveTrue(key)
            .orElseThrow(() -> new WorkflowNotFoundException(key));
    return WorkflowDetailResponse.from(definition, parseDefinitionJson(definition));
  }

  private JsonNode parseDefinitionJson(WorkflowDefinition definition) {
    try {
      return objectMapper.readTree(definition.getDefinitionJson());
    } catch (Exception ex) {
      throw new IllegalStateException(
          "Stored definition JSON is invalid for workflow " + definition.getWorkflowKey(), ex);
    }
  }
}

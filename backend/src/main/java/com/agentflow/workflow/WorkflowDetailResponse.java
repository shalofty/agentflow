package com.agentflow.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public record WorkflowDetailResponse(
    UUID id, String workflowKey, String title, int version, JsonNode definitionJson) {

  public static WorkflowDetailResponse from(WorkflowDefinition definition, JsonNode definitionJson) {
    return new WorkflowDetailResponse(
        definition.getId(),
        definition.getWorkflowKey(),
        definition.getTitle(),
        definition.getVersion(),
        definitionJson);
  }
}

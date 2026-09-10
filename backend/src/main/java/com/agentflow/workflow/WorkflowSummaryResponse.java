package com.agentflow.workflow;

import java.util.UUID;

public record WorkflowSummaryResponse(UUID id, String key, String title, int version) {

  static WorkflowSummaryResponse from(WorkflowDefinition definition) {
    return new WorkflowSummaryResponse(
        definition.getId(),
        definition.getWorkflowKey(),
        definition.getTitle(),
        definition.getVersion());
  }
}

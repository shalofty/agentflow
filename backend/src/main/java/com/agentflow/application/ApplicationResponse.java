package com.agentflow.application;

import com.agentflow.workflow.WorkflowDefinition;
import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
    UUID id,
    UUID customerId,
    UUID workflowDefinitionId,
    String workflowKey,
    int workflowVersion,
    ApplicationStatus status,
    String correlationId,
    Instant submittedAt,
    Instant createdAt,
    Instant updatedAt) {

  static ApplicationResponse from(Application application, WorkflowDefinition definition) {
    return new ApplicationResponse(
        application.getId(),
        application.getCustomerId(),
        application.getWorkflowDefinitionId(),
        definition.getWorkflowKey(),
        definition.getVersion(),
        application.getStatus(),
        application.getCorrelationId(),
        application.getSubmittedAt(),
        application.getCreatedAt(),
        application.getUpdatedAt());
  }
}

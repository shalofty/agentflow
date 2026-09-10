package com.agentflow.integration.activity;

import java.time.Instant;
import java.util.UUID;

public record IntegrationActivityResponse(
    UUID id,
    String integration,
    String integrationType,
    String action,
    IntegrationActivityStatus status,
    Integer httpStatus,
    long durationMs,
    int attempt,
    String errorMessage,
    String requestSummary,
    String responseSummary,
    Instant createdAt) {

  static IntegrationActivityResponse from(IntegrationActivity activity) {
    return new IntegrationActivityResponse(
        activity.getId(),
        activity.getIntegrationName(),
        activity.getIntegrationType(),
        activity.getAction(),
        activity.getStatus(),
        activity.getHttpStatus(),
        activity.getDurationMs(),
        activity.getAttempt(),
        activity.getErrorMessage(),
        activity.getRequestSummary(),
        activity.getResponseSummary(),
        activity.getCreatedAt());
  }
}

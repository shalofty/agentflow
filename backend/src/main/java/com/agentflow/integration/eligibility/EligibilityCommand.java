package com.agentflow.integration.eligibility;

import java.util.UUID;

public record EligibilityCommand(
    UUID applicationId,
    UUID customerId,
    String correlationId,
    int riskScore,
    String demoScenario) {

  public EligibilityCommand(
      UUID applicationId, UUID customerId, String correlationId, int riskScore) {
    this(applicationId, customerId, correlationId, riskScore, null);
  }
}

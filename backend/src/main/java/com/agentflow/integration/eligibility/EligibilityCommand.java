package com.agentflow.integration.eligibility;

import java.util.UUID;

public record EligibilityCommand(UUID applicationId, UUID customerId, String correlationId) {}

package com.agentflow.integration.rest;

import java.util.UUID;

public record VerifyCommand(UUID customerId, String correlationId) {}

package com.agentflow.integration.rest;

import java.util.UUID;

public record VerifyResponse(UUID customerId, boolean verified, int riskScore) {}

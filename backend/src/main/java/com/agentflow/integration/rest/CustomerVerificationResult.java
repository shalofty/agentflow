package com.agentflow.integration.rest;

import java.util.UUID;

public record CustomerVerificationResult(UUID customerId, boolean verified, int riskScore) {}

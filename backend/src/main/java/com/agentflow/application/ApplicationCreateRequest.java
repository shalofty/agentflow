package com.agentflow.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ApplicationCreateRequest(@NotNull UUID customerId, @NotBlank String workflowKey) {}

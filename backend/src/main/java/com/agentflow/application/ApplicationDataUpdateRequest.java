package com.agentflow.application;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record ApplicationDataUpdateRequest(@NotNull Map<String, Object> payload) {}

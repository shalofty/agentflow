package com.agentflow.application;

import java.util.UUID;

public class ApplicationNotFoundException extends RuntimeException {

  private final UUID id;

  public ApplicationNotFoundException(UUID id) {
    super("Application not found");
    this.id = id;
  }

  public UUID getId() {
    return id;
  }
}

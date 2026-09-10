package com.agentflow.application;

import java.util.UUID;

public class ApplicationStateTransitionException extends RuntimeException {

  private final UUID id;
  private final ApplicationStatus status;

  public ApplicationStateTransitionException(UUID id, ApplicationStatus status) {
    super("Application " + id + " cannot be submitted from status " + status);
    this.id = id;
    this.status = status;
  }

  public UUID getId() {
    return id;
  }

  public ApplicationStatus getStatus() {
    return status;
  }
}

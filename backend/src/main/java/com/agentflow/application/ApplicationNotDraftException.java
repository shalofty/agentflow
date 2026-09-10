package com.agentflow.application;

import java.util.UUID;

public class ApplicationNotDraftException extends RuntimeException {

  private final UUID id;
  private final ApplicationStatus status;

  public ApplicationNotDraftException(UUID id, ApplicationStatus status) {
    super("Application " + id + " is not in DRAFT status (current: " + status + ")");
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

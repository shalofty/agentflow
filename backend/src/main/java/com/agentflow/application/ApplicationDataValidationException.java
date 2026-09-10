package com.agentflow.application;

import com.agentflow.workflow.ValidationError;
import java.util.List;

public class ApplicationDataValidationException extends RuntimeException {

  private final List<ValidationError> errors;

  public ApplicationDataValidationException(List<ValidationError> errors) {
    super("Application data validation failed");
    this.errors = List.copyOf(errors);
  }

  public List<ValidationError> getErrors() {
    return errors;
  }
}

package com.agentflow.workflow;

public record ValidationError(String field, String label, String message) {

  public ValidationError(String field, String message) {
    this(field, field, message);
  }

  public String displayLabel() {
    return label == null || label.isBlank() ? field : label;
  }
}

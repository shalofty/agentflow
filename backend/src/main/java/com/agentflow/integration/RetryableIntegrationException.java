package com.agentflow.integration;

public class RetryableIntegrationException extends RuntimeException {

  public RetryableIntegrationException(String message) {
    super(message);
  }

  public RetryableIntegrationException(String message, Throwable cause) {
    super(message, cause);
  }
}

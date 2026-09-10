package com.agentflow.integration;

public class RetryableIntegrationException extends RuntimeException {

  private final Integer httpStatus;

  public RetryableIntegrationException(String message) {
    this(message, null, null);
  }

  public RetryableIntegrationException(String message, Throwable cause) {
    this(message, cause, null);
  }

  public RetryableIntegrationException(String message, Integer httpStatus) {
    this(message, null, httpStatus);
  }

  public RetryableIntegrationException(String message, Throwable cause, Integer httpStatus) {
    super(message, cause);
    this.httpStatus = httpStatus;
  }

  public Integer getHttpStatus() {
    return httpStatus;
  }
}

package com.agentflow.customer;

public class DuplicateEmailException extends RuntimeException {

  private final String email;

  DuplicateEmailException(String email) {
    super("Duplicate email");
    this.email = email;
  }

  public String getEmail() {
    return email;
  }
}

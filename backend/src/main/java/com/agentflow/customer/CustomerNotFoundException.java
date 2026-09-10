package com.agentflow.customer;

import java.util.UUID;

public class CustomerNotFoundException extends RuntimeException {

  private final UUID id;

  CustomerNotFoundException(UUID id) {
    super("Customer not found");
    this.id = id;
  }

  public UUID getId() {
    return id;
  }
}

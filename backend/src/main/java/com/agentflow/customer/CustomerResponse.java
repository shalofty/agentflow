package com.agentflow.customer;

import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(
    UUID id,
    String firstName,
    String lastName,
    String email,
    String phone,
    Instant createdAt,
    Instant updatedAt) {

  static CustomerResponse from(Customer customer) {
    return new CustomerResponse(
        customer.getId(),
        customer.getFirstName(),
        customer.getLastName(),
        customer.getEmail(),
        customer.getPhone(),
        customer.getCreatedAt(),
        customer.getUpdatedAt());
  }
}

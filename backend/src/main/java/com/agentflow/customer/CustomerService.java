package com.agentflow.customer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

  private final CustomerRepository repository;

  public CustomerService(CustomerRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public CustomerResponse create(CustomerRequest request) {
    if (repository.existsByEmail(request.email())) {
      throw new DuplicateEmailException(request.email());
    }

    var now = Instant.now();
    var customer =
        new Customer(
            UUID.randomUUID(),
            request.firstName(),
            request.lastName(),
            request.email(),
            request.phone(),
            now,
            now);

    try {
      return CustomerResponse.from(repository.save(customer));
    } catch (DataIntegrityViolationException ex) {
      throw new DuplicateEmailException(request.email());
    }
  }

  @Transactional(readOnly = true)
  public List<CustomerResponse> list() {
    return repository.findAll().stream().map(CustomerResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public CustomerResponse getById(UUID id) {
    return repository
        .findById(id)
        .map(CustomerResponse::from)
        .orElseThrow(() -> new CustomerNotFoundException(id));
  }
}

package com.agentflow.common;

import com.agentflow.customer.CustomerNotFoundException;
import com.agentflow.customer.DuplicateEmailException;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail onValidation(MethodArgumentNotValidException ex) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    pd.setTitle("Validation failed");
    Map<String, String> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(e -> e.getField(), e -> e.getDefaultMessage(), (a, b) -> a));
    pd.setProperty("errors", errors);
    return pd;
  }

  @ExceptionHandler(CustomerNotFoundException.class)
  ProblemDetail onCustomerNotFound(CustomerNotFoundException ex) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    pd.setTitle("Customer not found");
    pd.setDetail("No customer with id " + ex.getId());
    return pd;
  }

  @ExceptionHandler(DuplicateEmailException.class)
  ProblemDetail onDuplicateEmail(DuplicateEmailException ex) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    pd.setTitle("Duplicate email");
    pd.setDetail("A customer with email " + ex.getEmail() + " already exists");
    return pd;
  }
}

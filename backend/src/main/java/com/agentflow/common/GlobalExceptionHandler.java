package com.agentflow.common;

import com.agentflow.application.ApplicationDataValidationException;
import com.agentflow.application.ApplicationNotDraftException;
import com.agentflow.application.ApplicationNotFoundException;
import com.agentflow.customer.CustomerNotFoundException;
import com.agentflow.customer.DuplicateEmailException;
import com.agentflow.workflow.WorkflowNotFoundException;
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

  @ExceptionHandler(WorkflowNotFoundException.class)
  ProblemDetail onWorkflowNotFound(WorkflowNotFoundException ex) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    pd.setTitle("Workflow not found");
    pd.setDetail("No active workflow definition for key " + ex.getWorkflowKey());
    return pd;
  }

  @ExceptionHandler(ApplicationNotFoundException.class)
  ProblemDetail onApplicationNotFound(ApplicationNotFoundException ex) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    pd.setTitle("Application not found");
    pd.setDetail("No application with id " + ex.getId());
    return pd;
  }

  @ExceptionHandler(ApplicationDataValidationException.class)
  ProblemDetail onApplicationDataValidation(ApplicationDataValidationException ex) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    pd.setTitle("Validation failed");
    Map<String, String> errors =
        ex.getErrors().stream()
            .collect(Collectors.toMap(e -> e.field(), e -> e.message(), (a, b) -> a));
    pd.setProperty("errors", errors);
    return pd;
  }

  @ExceptionHandler(ApplicationNotDraftException.class)
  ProblemDetail onApplicationNotDraft(ApplicationNotDraftException ex) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    pd.setTitle("Application not in draft");
    pd.setDetail(
        "Application " + ex.getId() + " cannot be modified in status " + ex.getStatus());
    return pd;
  }
}

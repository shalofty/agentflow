package com.agentflow.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentflow.workflow.WorkflowNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void problemDetailIncludesCorrelationIdFromMdc() {
    MDC.put("correlationId", "corr-problem");

    var problem = handler.onWorkflowNotFound(new WorkflowNotFoundException("missing-workflow"));

    assertThat(problem.getProperties()).containsEntry("correlationId", "corr-problem");
  }

  @Test
  void unexpectedExceptionReturnsInternalServerProblemDetail() {
    MDC.put("correlationId", "corr-unexpected");

    var problem = handler.onUnexpected(new IllegalStateException("database password leaked"));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    assertThat(problem.getTitle()).isEqualTo("Internal server error");
    assertThat(problem.getDetail()).doesNotContain("database password leaked");
    assertThat(problem.getProperties()).containsEntry("correlationId", "corr-unexpected");
  }
}

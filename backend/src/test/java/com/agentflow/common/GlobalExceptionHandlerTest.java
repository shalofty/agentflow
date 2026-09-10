package com.agentflow.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentflow.workflow.WorkflowNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

  @Test
  void unknownResourceReturnsNotFound() {
    var problem =
        handler.onNoResourceFound(
            new NoResourceFoundException(HttpMethod.GET, "api/no-such-endpoint"));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(problem.getTitle()).isEqualTo("Not found");
    assertThat(problem.getDetail()).isEqualTo("No endpoint matches this request");
  }

  @Test
  void typeMismatchReturnsBadRequest() {
    var problem =
        handler.onTypeMismatch(
            new MethodArgumentTypeMismatchException(
                "not-a-uuid", java.util.UUID.class, "id", null, null));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problem.getTitle()).isEqualTo("Invalid request parameter");
    assertThat(problem.getDetail()).contains("'id'");
  }

  @Test
  void missingParameterReturnsBadRequest() {
    var problem =
        handler.onMissingParameter(
            new MissingServletRequestParameterException("customerId", "UUID"));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problem.getTitle()).isEqualTo("Missing request parameter");
    assertThat(problem.getDetail()).contains("customerId");
  }
}

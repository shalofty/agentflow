package com.agentflow.application;

import com.agentflow.integration.RetryableIntegrationException;
import com.agentflow.integration.activity.IntegrationActivity;
import com.agentflow.integration.activity.IntegrationActivityService;
import com.agentflow.integration.activity.IntegrationActivityStatus;
import com.agentflow.integration.eligibility.EligibilityCommand;
import com.agentflow.integration.eligibility.EligibilityResult;
import com.agentflow.integration.eligibility.PolicyEligibilityClient;
import com.agentflow.integration.rest.CustomerVerificationClient;
import com.agentflow.integration.rest.CustomerVerificationResult;
import com.agentflow.integration.rest.VerifyCommand;
import com.agentflow.workflow.FormDefinitionValidator;
import com.agentflow.workflow.WorkflowDefinition;
import com.agentflow.workflow.WorkflowDefinitionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ApplicationSubmitService {

  private final ApplicationRepository applicationRepository;
  private final ApplicationDataRepository dataRepository;
  private final WorkflowDefinitionRepository definitionRepository;
  private final FormDefinitionValidator validator;
  private final ObjectMapper objectMapper;
  private final CustomerVerificationClient verificationClient;
  private final PolicyEligibilityClient eligibilityClient;
  private final IntegrationActivityService activityService;
  private final ApplicationStateTransitions transitions;
  private final TransactionTemplate transactions;
  private final int maxAttempts;
  private final long retryBackoffMs;

  public ApplicationSubmitService(
      ApplicationRepository applicationRepository,
      ApplicationDataRepository dataRepository,
      WorkflowDefinitionRepository definitionRepository,
      FormDefinitionValidator validator,
      ObjectMapper objectMapper,
      CustomerVerificationClient verificationClient,
      PolicyEligibilityClient eligibilityClient,
      IntegrationActivityService activityService,
      ApplicationStateTransitions transitions,
      TransactionTemplate transactions,
      @Value("${agentflow.integrations.customer-verification.max-attempts:3}") int maxAttempts,
      @Value("${agentflow.integrations.customer-verification.retry-backoff-ms:100}") long retryBackoffMs) {
    this.applicationRepository = applicationRepository;
    this.dataRepository = dataRepository;
    this.definitionRepository = definitionRepository;
    this.validator = validator;
    this.objectMapper = objectMapper;
    this.verificationClient = verificationClient;
    this.eligibilityClient = eligibilityClient;
    this.activityService = activityService;
    this.transitions = transitions;
    this.transactions = transactions;
    this.maxAttempts = maxAttempts;
    this.retryBackoffMs = retryBackoffMs;
  }

  public ApplicationResponse submit(UUID applicationId, String correlationId) {
    SubmitContext context = claimAndValidate(applicationId, correlationId);
    updateStatus(applicationId, ApplicationStatus.CUSTOMER_VERIFICATION);

    CustomerVerificationResult verification = verifyCustomer(context);
    if (verification == null) {
      return updateStatus(applicationId, ApplicationStatus.INTEGRATION_FAILURE);
    }
    if (!verification.verified()) {
      return updateStatus(applicationId, ApplicationStatus.MANUAL_REVIEW);
    }

    updateStatus(applicationId, ApplicationStatus.ELIGIBILITY_CHECK);
    EligibilityResult eligibility = checkEligibility(context, verification.riskScore());
    if (eligibility == null) {
      return updateStatus(applicationId, ApplicationStatus.INTEGRATION_FAILURE);
    }
    return updateStatus(applicationId, mapEligibility(eligibility));
  }

  private SubmitContext claimAndValidate(UUID applicationId, String correlationId) {
    return transactions.execute(
        ignored -> {
          Application application =
              applicationRepository
                  .findLockedById(applicationId)
                  .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
          transitions.requireSubmittable(applicationId, application.getStatus());

          WorkflowDefinition definition =
              definitionRepository
                  .findById(application.getWorkflowDefinitionId())
                  .orElseThrow(
                      () ->
                          new IllegalStateException(
                              "Pinned workflow definition "
                                  + application.getWorkflowDefinitionId()
                                  + " not found"));
          ApplicationData data =
              dataRepository
                  .findById(applicationId)
                  .orElseThrow(
                      () ->
                          new IllegalStateException(
                              "Application data missing for application " + applicationId));
          validate(definition.getDefinitionJson(), data.getPayload());

          Instant now = Instant.now();
          application.setCorrelationId(correlationId);
          application.setSubmittedAt(now);
          application.setStatus(ApplicationStatus.SUBMITTED);
          application.setUpdatedAt(now);
          applicationRepository.save(application);
          return new SubmitContext(applicationId, application.getCustomerId(), correlationId);
        });
  }

  private void validate(String definitionJson, String payloadJson) {
    try {
      Map<String, Object> definition =
          objectMapper.readValue(definitionJson, new TypeReference<>() {});
      Map<String, Object> payload = objectMapper.readValue(payloadJson, new TypeReference<>() {});
      var errors = validator.validate(definition, payload);
      if (!errors.isEmpty()) {
        throw new ApplicationDataValidationException(errors);
      }
    } catch (ApplicationDataValidationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalStateException("Stored application JSON is invalid", ex);
    }
  }

  private CustomerVerificationResult verifyCustomer(SubmitContext context) {
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      long started = System.nanoTime();
      try {
        CustomerVerificationResult result =
            verificationClient.verify(
                new VerifyCommand(context.customerId(), context.correlationId()));
        activityService.record(
            activity(
                context,
                "CustomerVerification",
                "REST",
                "POST /verify",
                IntegrationActivityStatus.SUCCESS,
                200,
                elapsedMs(started),
                attempt,
                null,
                "customerId=" + context.customerId(),
                "verified=" + result.verified() + ", riskScore=" + result.riskScore()));
        return result;
      } catch (RetryableIntegrationException ex) {
        recordFailure(context, "CustomerVerification", "REST", "POST /verify", attempt, started, ex);
        if (attempt < maxAttempts) {
          backoff();
        }
      } catch (RuntimeException ex) {
        recordFailure(context, "CustomerVerification", "REST", "POST /verify", attempt, started, ex);
        return null;
      }
    }
    return null;
  }

  private EligibilityResult checkEligibility(SubmitContext context, int riskScore) {
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      long started = System.nanoTime();
      try {
        EligibilityResult result =
            eligibilityClient.check(
                new EligibilityCommand(
                    context.applicationId(),
                    context.customerId(),
                    context.correlationId(),
                    riskScore));
        activityService.record(
            activity(
                context,
                "PolicyEligibility",
                "SOAP",
                "CheckEligibility",
                IntegrationActivityStatus.SUCCESS,
                null,
                elapsedMs(started),
                attempt,
                null,
                "applicationId=" + context.applicationId(),
                "result=" + result));
        return result;
      } catch (RetryableIntegrationException ex) {
        recordFailure(
            context, "PolicyEligibility", "SOAP", "CheckEligibility", attempt, started, ex);
        if (attempt < maxAttempts) {
          backoff();
        }
      } catch (RuntimeException ex) {
        recordFailure(
            context, "PolicyEligibility", "SOAP", "CheckEligibility", attempt, started, ex);
        return null;
      }
    }
    return null;
  }

  private void recordFailure(
      SubmitContext context,
      String name,
      String type,
      String action,
      int attempt,
      long started,
      RuntimeException error) {
    Integer httpStatus =
        error instanceof RetryableIntegrationException retryable
            ? retryable.getHttpStatus()
            : null;
    activityService.record(
        activity(
            context,
            name,
            type,
            action,
            IntegrationActivityStatus.FAILED,
            httpStatus,
            elapsedMs(started),
            attempt,
            error.getMessage(),
            name.equals("CustomerVerification")
                ? "customerId=" + context.customerId()
                : "applicationId=" + context.applicationId(),
            null));
  }

  private IntegrationActivity activity(
      SubmitContext context,
      String name,
      String type,
      String action,
      IntegrationActivityStatus status,
      Integer httpStatus,
      long durationMs,
      int attempt,
      String error,
      String request,
      String response) {
    return new IntegrationActivity(
        context.applicationId(),
        context.correlationId(),
        name,
        type,
        action,
        status,
        httpStatus,
        durationMs,
        attempt,
        error,
        request,
        response);
  }

  private ApplicationResponse updateStatus(UUID applicationId, ApplicationStatus status) {
    return transactions.execute(
        ignored -> {
          Application application =
              applicationRepository
                  .findById(applicationId)
                  .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
          application.setStatus(status);
          application.setUpdatedAt(Instant.now());
          applicationRepository.save(application);
          WorkflowDefinition definition =
              definitionRepository
                  .findById(application.getWorkflowDefinitionId())
                  .orElseThrow(IllegalStateException::new);
          return ApplicationResponse.from(application, definition);
        });
  }

  private void backoff() {
    try {
      Thread.sleep(retryBackoffMs);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new RetryableIntegrationException("Integration retry interrupted", ex);
    }
  }

  private static long elapsedMs(long started) {
    return Duration.ofNanos(System.nanoTime() - started).toMillis();
  }

  private static ApplicationStatus mapEligibility(EligibilityResult result) {
    return switch (result) {
      case ELIGIBLE -> ApplicationStatus.APPROVED;
      case MANUAL_REVIEW -> ApplicationStatus.MANUAL_REVIEW;
      case INELIGIBLE -> ApplicationStatus.INELIGIBLE;
    };
  }

  private record SubmitContext(UUID applicationId, UUID customerId, String correlationId) {}
}

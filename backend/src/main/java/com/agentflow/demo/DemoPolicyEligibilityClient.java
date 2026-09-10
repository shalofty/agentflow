package com.agentflow.demo;

import com.agentflow.integration.RetryableIntegrationException;
import com.agentflow.integration.eligibility.EligibilityCommand;
import com.agentflow.integration.eligibility.EligibilityResult;
import com.agentflow.integration.eligibility.PolicyEligibilityClient;
import com.agentflow.integration.soap.PolicyEligibilitySoapClient;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Primary
@Component
@Profile({"local", "demo"})
final class DemoPolicyEligibilityClient implements PolicyEligibilityClient {

  private final PolicyEligibilitySoapClient delegate;

  DemoPolicyEligibilityClient(PolicyEligibilitySoapClient delegate) {
    this.delegate = delegate;
  }

  @Override
  public EligibilityResult check(EligibilityCommand command) {
    return DemoScenarioContext.current()
        .map(this::simulate)
        .orElseGet(() -> delegate.check(command));
  }

  private EligibilityResult simulate(DemoScenario scenario) {
    return switch (scenario) {
      case SOAP_FAULT ->
          throw new RetryableIntegrationException("Demo policy eligibility SOAP fault");
      case SOAP_MANUAL_REVIEW -> EligibilityResult.MANUAL_REVIEW;
      case REST_500, REST_TIMEOUT ->
          throw new IllegalStateException("REST demo scenario reached SOAP integration");
    };
  }
}

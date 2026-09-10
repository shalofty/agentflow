package com.agentflow.demo;

import com.agentflow.integration.eligibility.EligibilityCommand;
import com.agentflow.integration.eligibility.EligibilityResult;
import com.agentflow.integration.eligibility.PolicyEligibilityClient;
import com.agentflow.integration.soap.PolicyEligibilitySoapClient;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Demo-profile primary bean. Always delegates so SOAP demo scenarios are applied by the mock /
 * Worker from the explicit header on the command, not from ThreadLocal.
 */
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
    return delegate.check(command);
  }
}

package com.agentflow.integration.eligibility;

import org.springframework.stereotype.Component;

@Component
public class StubPolicyEligibilityClient implements PolicyEligibilityClient {

  @Override
  public EligibilityResult check(EligibilityCommand command) {
    return EligibilityResult.ELIGIBLE;
  }
}

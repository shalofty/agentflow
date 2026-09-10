package com.agentflow.integration.eligibility;

public interface PolicyEligibilityClient {
  EligibilityResult check(EligibilityCommand command);
}

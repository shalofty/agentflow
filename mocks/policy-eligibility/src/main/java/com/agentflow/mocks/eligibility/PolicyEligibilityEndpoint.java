package com.agentflow.mocks.eligibility;

import com.agentflow.mocks.eligibility.soap.CheckEligibilityRequest;
import com.agentflow.mocks.eligibility.soap.CheckEligibilityResponse;
import com.agentflow.mocks.eligibility.soap.EligibilityDecision;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class PolicyEligibilityEndpoint {

  public static final String NAMESPACE_URI =
      "http://agentflow.com/policy-eligibility";

  private final FaultModeService faultModeService;

  public PolicyEligibilityEndpoint(FaultModeService faultModeService) {
    this.faultModeService = faultModeService;
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "CheckEligibilityRequest")
  @ResponsePayload
  public CheckEligibilityResponse checkEligibility(
      @RequestPayload CheckEligibilityRequest request) {
    if (faultModeService.getMode() == FaultMode.FAULT) {
      throw new PolicyEligibilitySoapFault(
          "Simulated policy eligibility service fault");
    }

    var response = new CheckEligibilityResponse();
    response.setApplicationId(request.getApplicationId());
    response.setDecision(decisionFor(request.getRiskScore()));
    return response;
  }

  private EligibilityDecision decisionFor(int riskScore) {
    if (faultModeService.getMode() == FaultMode.MANUAL_REVIEW) {
      return EligibilityDecision.MANUAL_REVIEW;
    }
    if (riskScore >= 80) {
      return EligibilityDecision.INELIGIBLE;
    }
    if (riskScore >= 50) {
      return EligibilityDecision.MANUAL_REVIEW;
    }
    return EligibilityDecision.ELIGIBLE;
  }
}

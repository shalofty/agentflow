package com.agentflow.mocks.eligibility;

import com.agentflow.mocks.eligibility.soap.CheckEligibilityRequest;
import com.agentflow.mocks.eligibility.soap.CheckEligibilityResponse;
import com.agentflow.mocks.eligibility.soap.EligibilityDecision;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class PolicyEligibilityEndpoint {

  public static final String NAMESPACE_URI =
      "http://agentflow.com/policy-eligibility";

  private final FaultModeService faultModeService;
  private final HttpServletRequest httpRequest;

  public PolicyEligibilityEndpoint(
      FaultModeService faultModeService, HttpServletRequest httpRequest) {
    this.faultModeService = faultModeService;
    this.httpRequest = httpRequest;
  }

  @PayloadRoot(namespace = NAMESPACE_URI, localPart = "CheckEligibilityRequest")
  @ResponsePayload
  public CheckEligibilityResponse checkEligibility(
      @RequestPayload CheckEligibilityRequest request) {
    String scenario = headerScenario();
    if ("soap-fault".equals(scenario) || faultModeService.getMode() == FaultMode.FAULT) {
      throw new PolicyEligibilitySoapFault(
          "Simulated policy eligibility service fault");
    }

    var response = new CheckEligibilityResponse();
    response.setApplicationId(request.getApplicationId());
    response.setDecision(decisionFor(request.getRiskScore(), scenario));
    return response;
  }

  private EligibilityDecision decisionFor(int riskScore, String scenario) {
    if ("soap-manual-review".equals(scenario)
        || faultModeService.getMode() == FaultMode.MANUAL_REVIEW) {
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

  private String headerScenario() {
    String value = httpRequest.getHeader("X-AgentFlow-Demo-Scenario");
    return value == null ? "" : value.toLowerCase();
  }
}

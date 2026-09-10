package com.agentflow.mocks.eligibility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agentflow.mocks.eligibility.soap.CheckEligibilityRequest;
import com.agentflow.mocks.eligibility.soap.EligibilityDecision;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class PolicyEligibilityEndpointTest {

  private final FaultModeService faultModeService = new FaultModeService();
  private final MockHttpServletRequest httpRequest = new MockHttpServletRequest();
  private final PolicyEligibilityEndpoint endpoint =
      new PolicyEligibilityEndpoint(faultModeService, httpRequest);

  @Test
  void mapsRiskScoreToEachEligibilityDecision() {
    assertThat(endpoint.checkEligibility(request(49)).getDecision())
        .isEqualTo(EligibilityDecision.ELIGIBLE);
    assertThat(endpoint.checkEligibility(request(50)).getDecision())
        .isEqualTo(EligibilityDecision.MANUAL_REVIEW);
    assertThat(endpoint.checkEligibility(request(80)).getDecision())
        .isEqualTo(EligibilityDecision.INELIGIBLE);
  }

  @Test
  void adminManualReviewModeOverridesRiskScore() {
    faultModeService.setMode(FaultMode.MANUAL_REVIEW);

    assertThat(endpoint.checkEligibility(request(10)).getDecision())
        .isEqualTo(EligibilityDecision.MANUAL_REVIEW);
  }

  @Test
  void requestScopedManualReviewHeaderOverridesRiskScore() {
    httpRequest.addHeader("X-AgentFlow-Demo-Scenario", "soap-manual-review");

    assertThat(endpoint.checkEligibility(request(10)).getDecision())
        .isEqualTo(EligibilityDecision.MANUAL_REVIEW);
  }

  @Test
  void adminFaultModeRaisesVisibleSoapFault() {
    faultModeService.setMode(FaultMode.FAULT);

    assertThatThrownBy(() -> endpoint.checkEligibility(request(10)))
        .isInstanceOf(PolicyEligibilitySoapFault.class)
        .hasMessage("Simulated policy eligibility service fault");
  }

  private CheckEligibilityRequest request(int riskScore) {
    var request = new CheckEligibilityRequest();
    request.setApplicationId("application-123");
    request.setRiskScore(riskScore);
    return request;
  }
}

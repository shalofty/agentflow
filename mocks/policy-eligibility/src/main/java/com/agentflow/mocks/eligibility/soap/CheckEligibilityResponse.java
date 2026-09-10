package com.agentflow.mocks.eligibility.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"applicationId", "decision"})
@XmlRootElement(
    name = "CheckEligibilityResponse",
    namespace = "http://agentflow.com/policy-eligibility")
public class CheckEligibilityResponse {

  @XmlElement(required = true)
  private String applicationId;

  @XmlElement(required = true)
  private EligibilityDecision decision;

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }

  public EligibilityDecision getDecision() {
    return decision;
  }

  public void setDecision(EligibilityDecision decision) {
    this.decision = decision;
  }
}

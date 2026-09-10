package com.agentflow.mocks.eligibility.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"applicationId", "riskScore"})
@XmlRootElement(
    name = "CheckEligibilityRequest",
    namespace = "http://agentflow.com/policy-eligibility")
public class CheckEligibilityRequest {

  @XmlElement(required = true)
  private String applicationId;

  private int riskScore;

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }

  public int getRiskScore() {
    return riskScore;
  }

  public void setRiskScore(int riskScore) {
    this.riskScore = riskScore;
  }
}

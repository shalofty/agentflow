package com.agentflow.integration.soap;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

@XmlType(
    name = "EligibilityDecision",
    namespace = PolicyEligibilitySoapClient.NAMESPACE_URI)
@XmlEnum
public enum EligibilityDecision {
  ELIGIBLE,
  MANUAL_REVIEW,
  INELIGIBLE
}

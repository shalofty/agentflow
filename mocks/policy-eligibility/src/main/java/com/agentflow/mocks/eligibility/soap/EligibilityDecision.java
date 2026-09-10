package com.agentflow.mocks.eligibility.soap;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

@XmlType(name = "EligibilityDecision")
@XmlEnum
public enum EligibilityDecision {
  ELIGIBLE,
  MANUAL_REVIEW,
  INELIGIBLE
}

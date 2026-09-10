package com.agentflow.demo;

import java.util.Arrays;
import java.util.Optional;

public enum DemoScenario {
  REST_500("rest-500"),
  REST_TIMEOUT("rest-timeout"),
  SOAP_FAULT("soap-fault"),
  SOAP_MANUAL_REVIEW("soap-manual-review");

  private final String value;

  DemoScenario(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }

  public static Optional<DemoScenario> from(String value) {
    return Arrays.stream(values())
        .filter(scenario -> scenario.value.equalsIgnoreCase(value))
        .findFirst();
  }
}

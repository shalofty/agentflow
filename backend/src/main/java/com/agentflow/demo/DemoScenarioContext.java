package com.agentflow.demo;

import java.util.Optional;

final class DemoScenarioContext {

  private static final ThreadLocal<DemoScenario> CURRENT = new ThreadLocal<>();

  private DemoScenarioContext() {}

  static Optional<DemoScenario> current() {
    return Optional.ofNullable(CURRENT.get());
  }

  static void set(DemoScenario scenario) {
    CURRENT.set(scenario);
  }

  static void clear() {
    CURRENT.remove();
  }
}

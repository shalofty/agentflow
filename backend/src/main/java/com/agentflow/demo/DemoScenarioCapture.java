package com.agentflow.demo;

import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Captures the request-scoped demo scenario on the inbound thread. Callers must copy the value
 * onto outbound commands before any reactive/async transport work.
 */
@Component
@Profile({"local", "demo"})
public class DemoScenarioCapture {

  public Optional<String> peek() {
    return DemoScenarioContext.current().map(DemoScenario::value);
  }
}

package com.agentflow.demo;

import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
@Profile({"local", "demo"})
final class DemoFailureController {

  @GetMapping("/enabled")
  Map<String, Object> enabled() {
    return Map.of(
        "enabled", true,
        "scenarios", List.of(DemoScenario.values()).stream().map(DemoScenario::value).toList());
  }
}

package com.agentflow.mocks.eligibility;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class FaultModeController {

  private final FaultModeService faultModeService;

  public FaultModeController(FaultModeService faultModeService) {
    this.faultModeService = faultModeService;
  }

  @PostMapping("/faults")
  ResponseEntity<Map<String, String>> setFaultMode(
      @RequestBody FaultModeRequest request) {
    try {
      var mode = FaultMode.valueOf(request.mode().toUpperCase());
      faultModeService.setMode(mode);
      return ResponseEntity.ok(Map.of("mode", mode.name().toLowerCase()));
    } catch (IllegalArgumentException | NullPointerException exception) {
      return ResponseEntity.badRequest().body(Map.of(
          "error", "mode must be none, fault, or manual_review"));
    }
  }

  record FaultModeRequest(String mode) {}
}

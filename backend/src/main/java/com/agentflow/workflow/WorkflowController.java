package com.agentflow.workflow;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

  private final WorkflowDefinitionService service;

  public WorkflowController(WorkflowDefinitionService service) {
    this.service = service;
  }

  @GetMapping
  public List<WorkflowSummaryResponse> list() {
    return service.listActive();
  }

  @GetMapping("/{key}")
  public WorkflowDetailResponse getByKey(@PathVariable String key) {
    return service.getActiveByKey(key);
  }
}

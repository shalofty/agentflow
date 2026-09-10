package com.agentflow.application;

import com.agentflow.workflow.WorkflowDetailResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

  private final ApplicationService service;

  public ApplicationController(ApplicationService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApplicationResponse create(@Valid @RequestBody ApplicationCreateRequest request) {
    return service.create(request);
  }

  @GetMapping("/{id}")
  public ApplicationResponse getById(@PathVariable UUID id) {
    return service.getById(id);
  }

  @GetMapping("/{id}/definition")
  public WorkflowDetailResponse getDefinition(@PathVariable UUID id) {
    return service.getPinnedDefinition(id);
  }
}

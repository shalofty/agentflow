package com.agentflow.application;

import com.agentflow.common.CorrelationIdFilter;
import com.agentflow.integration.activity.IntegrationActivityResponse;
import com.agentflow.integration.activity.IntegrationActivityService;
import com.agentflow.workflow.WorkflowDetailResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

  private final ApplicationService service;
  private final ApplicationSubmitService submitService;
  private final IntegrationActivityService activityService;

  public ApplicationController(
      ApplicationService service,
      ApplicationSubmitService submitService,
      IntegrationActivityService activityService) {
    this.service = service;
    this.submitService = submitService;
    this.activityService = activityService;
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

  @PutMapping("/{id}/data")
  public ApplicationResponse saveData(
      @PathVariable UUID id, @Valid @RequestBody ApplicationDataUpdateRequest request) {
    return service.saveData(id, request.payload());
  }

  @PostMapping("/{id}/submit")
  public ApplicationResponse submit(@PathVariable UUID id, HttpServletRequest request) {
    return submitService.submit(
        id, (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE));
  }

  @GetMapping("/{id}/activities")
  public List<IntegrationActivityResponse> activities(@PathVariable UUID id) {
    return activityService.list(id);
  }
}

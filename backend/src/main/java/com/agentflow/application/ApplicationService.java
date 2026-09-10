package com.agentflow.application;

import com.agentflow.customer.CustomerService;
import com.agentflow.workflow.WorkflowDefinitionRepository;
import com.agentflow.workflow.WorkflowDefinitionService;
import com.agentflow.workflow.WorkflowNotFoundException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService {

  private final ApplicationRepository applicationRepository;
  private final ApplicationDataRepository applicationDataRepository;
  private final CustomerService customerService;
  private final WorkflowDefinitionRepository workflowDefinitionRepository;
  private final WorkflowDefinitionService workflowDefinitionService;

  public ApplicationService(
      ApplicationRepository applicationRepository,
      ApplicationDataRepository applicationDataRepository,
      CustomerService customerService,
      WorkflowDefinitionRepository workflowDefinitionRepository,
      WorkflowDefinitionService workflowDefinitionService) {
    this.applicationRepository = applicationRepository;
    this.applicationDataRepository = applicationDataRepository;
    this.customerService = customerService;
    this.workflowDefinitionRepository = workflowDefinitionRepository;
    this.workflowDefinitionService = workflowDefinitionService;
  }

  @Transactional
  public ApplicationResponse create(ApplicationCreateRequest request) {
    customerService.getById(request.customerId());

    var definition =
        workflowDefinitionRepository
            .findByWorkflowKeyAndActiveTrue(request.workflowKey())
            .orElseThrow(() -> new WorkflowNotFoundException(request.workflowKey()));

    var now = Instant.now();
    var application =
        new Application(
            UUID.randomUUID(),
            request.customerId(),
            definition.getId(),
            ApplicationStatus.DRAFT,
            now,
            now);
    applicationRepository.save(application);
    applicationDataRepository.save(new ApplicationData(application.getId(), "{}", now));

    return ApplicationResponse.from(application, definition);
  }

  @Transactional(readOnly = true)
  public ApplicationResponse getById(UUID id) {
    var application =
        applicationRepository
            .findById(id)
            .orElseThrow(() -> new ApplicationNotFoundException(id));
    var definition = loadDefinition(application.getWorkflowDefinitionId());
    return ApplicationResponse.from(application, definition);
  }

  @Transactional(readOnly = true)
  public com.agentflow.workflow.WorkflowDetailResponse getPinnedDefinition(UUID id) {
    var application =
        applicationRepository
            .findById(id)
            .orElseThrow(() -> new ApplicationNotFoundException(id));
    return workflowDefinitionService.getById(application.getWorkflowDefinitionId());
  }

  private com.agentflow.workflow.WorkflowDefinition loadDefinition(UUID definitionId) {
    return workflowDefinitionRepository
        .findById(definitionId)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Pinned workflow definition " + definitionId + " not found"));
  }
}

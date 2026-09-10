package com.agentflow.application;

import com.agentflow.customer.CustomerService;
import com.agentflow.workflow.FormDefinitionValidator;
import com.agentflow.workflow.WorkflowDefinitionRepository;
import com.agentflow.workflow.WorkflowDefinitionService;
import com.agentflow.workflow.WorkflowNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
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
  private final FormDefinitionValidator formDefinitionValidator;
  private final ObjectMapper objectMapper;

  public ApplicationService(
      ApplicationRepository applicationRepository,
      ApplicationDataRepository applicationDataRepository,
      CustomerService customerService,
      WorkflowDefinitionRepository workflowDefinitionRepository,
      WorkflowDefinitionService workflowDefinitionService,
      FormDefinitionValidator formDefinitionValidator,
      ObjectMapper objectMapper) {
    this.applicationRepository = applicationRepository;
    this.applicationDataRepository = applicationDataRepository;
    this.customerService = customerService;
    this.workflowDefinitionRepository = workflowDefinitionRepository;
    this.workflowDefinitionService = workflowDefinitionService;
    this.formDefinitionValidator = formDefinitionValidator;
    this.objectMapper = objectMapper;
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

  @Transactional
  public ApplicationResponse saveData(UUID id, Map<String, Object> payload) {
    var application =
        applicationRepository
            .findById(id)
            .orElseThrow(() -> new ApplicationNotFoundException(id));

    if (application.getStatus() != ApplicationStatus.DRAFT) {
      throw new ApplicationNotDraftException(id, application.getStatus());
    }

    var definition = loadDefinition(application.getWorkflowDefinitionId());
    var definitionMap = parseDefinitionJson(definition.getDefinitionJson());
    var errors = formDefinitionValidator.validate(definitionMap, payload);
    if (!errors.isEmpty()) {
      throw new ApplicationDataValidationException(errors);
    }

    var data =
        applicationDataRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Application data missing for application " + id));

    var now = Instant.now();
    try {
      data.setPayload(objectMapper.writeValueAsString(payload));
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to serialize application payload", ex);
    }
    data.setUpdatedAt(now);
    application.setUpdatedAt(now);
    applicationDataRepository.save(data);
    applicationRepository.save(application);

    return ApplicationResponse.from(application, definition);
  }

  private Map<String, Object> parseDefinitionJson(String definitionJson) {
    try {
      return objectMapper.readValue(definitionJson, new TypeReference<>() {});
    } catch (Exception ex) {
      throw new IllegalStateException("Stored definition JSON is invalid", ex);
    }
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

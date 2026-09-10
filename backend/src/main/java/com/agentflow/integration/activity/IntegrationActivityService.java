package com.agentflow.integration.activity;

import com.agentflow.application.ApplicationNotFoundException;
import com.agentflow.application.ApplicationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntegrationActivityService {

  private final IntegrationActivityRepository repository;
  private final ApplicationRepository applicationRepository;

  public IntegrationActivityService(
      IntegrationActivityRepository repository, ApplicationRepository applicationRepository) {
    this.repository = repository;
    this.applicationRepository = applicationRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(IntegrationActivity activity) {
    repository.save(activity);
  }

  @Transactional(readOnly = true)
  public List<IntegrationActivityResponse> list(UUID applicationId) {
    if (!applicationRepository.existsById(applicationId)) {
      throw new ApplicationNotFoundException(applicationId);
    }
    return repository.findByApplicationIdOrderByCreatedAtAsc(applicationId).stream()
        .map(IntegrationActivityResponse::from)
        .toList();
  }
}

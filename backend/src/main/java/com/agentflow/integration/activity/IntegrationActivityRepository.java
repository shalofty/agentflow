package com.agentflow.integration.activity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationActivityRepository
    extends JpaRepository<IntegrationActivity, UUID> {
  List<IntegrationActivity> findByApplicationIdOrderByCreatedAtAsc(UUID applicationId);
}

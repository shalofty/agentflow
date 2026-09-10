package com.agentflow.application;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationDataRepository extends JpaRepository<ApplicationData, UUID> {}

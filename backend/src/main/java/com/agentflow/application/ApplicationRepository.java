package com.agentflow.application;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select application from Application application where application.id = :id")
  Optional<Application> findLockedById(UUID id);
}

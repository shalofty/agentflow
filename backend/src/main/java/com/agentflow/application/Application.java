package com.agentflow.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "application")
public class Application {

  @Id private UUID id;

  @Column(name = "customer_id", nullable = false)
  private UUID customerId;

  @Column(name = "workflow_definition_id", nullable = false)
  private UUID workflowDefinitionId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private ApplicationStatus status;

  @Column(name = "correlation_id", length = 64)
  private String correlationId;

  @Column(name = "submitted_at")
  private Instant submittedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Application() {}

  public Application(
      UUID id,
      UUID customerId,
      UUID workflowDefinitionId,
      ApplicationStatus status,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.customerId = customerId;
    this.workflowDefinitionId = workflowDefinitionId;
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public UUID getWorkflowDefinitionId() {
    return workflowDefinitionId;
  }

  public ApplicationStatus getStatus() {
    return status;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public Instant getSubmittedAt() {
    return submittedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setStatus(ApplicationStatus status) {
    this.status = status;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }

  public void setSubmittedAt(Instant submittedAt) {
    this.submittedAt = submittedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}

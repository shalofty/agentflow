package com.agentflow.integration.activity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "integration_activity")
public class IntegrationActivity {

  @Id private UUID id;

  @Column(name = "application_id", nullable = false)
  private UUID applicationId;

  @Column(name = "correlation_id", nullable = false, length = 64)
  private String correlationId;

  @Column(name = "integration_name", nullable = false, length = 80)
  private String integrationName;

  @Column(name = "integration_type", nullable = false, length = 20)
  private String integrationType;

  @Column(nullable = false, length = 160)
  private String action;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private IntegrationActivityStatus status;

  @Column(name = "http_status")
  private Integer httpStatus;

  @Column(name = "duration_ms", nullable = false)
  private long durationMs;

  @Column(nullable = false)
  private int attempt;

  @Column(name = "error_message", length = 500)
  private String errorMessage;

  @Column(name = "request_summary", length = 500)
  private String requestSummary;

  @Column(name = "response_summary", length = 500)
  private String responseSummary;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected IntegrationActivity() {}

  public IntegrationActivity(
      UUID applicationId,
      String correlationId,
      String integrationName,
      String integrationType,
      String action,
      IntegrationActivityStatus status,
      Integer httpStatus,
      long durationMs,
      int attempt,
      String errorMessage,
      String requestSummary,
      String responseSummary) {
    this.id = UUID.randomUUID();
    this.applicationId = applicationId;
    this.correlationId = correlationId;
    this.integrationName = integrationName;
    this.integrationType = integrationType;
    this.action = action;
    this.status = status;
    this.httpStatus = httpStatus;
    this.durationMs = durationMs;
    this.attempt = attempt;
    this.errorMessage = truncate(errorMessage);
    this.requestSummary = truncate(requestSummary);
    this.responseSummary = truncate(responseSummary);
    this.createdAt = Instant.now();
  }

  private static String truncate(String value) {
    return value == null || value.length() <= 500 ? value : value.substring(0, 500);
  }

  public UUID getId() { return id; }
  public UUID getApplicationId() { return applicationId; }
  public String getCorrelationId() { return correlationId; }
  public String getIntegrationName() { return integrationName; }
  public String getIntegrationType() { return integrationType; }
  public String getAction() { return action; }
  public IntegrationActivityStatus getStatus() { return status; }
  public Integer getHttpStatus() { return httpStatus; }
  public long getDurationMs() { return durationMs; }
  public int getAttempt() { return attempt; }
  public String getErrorMessage() { return errorMessage; }
  public String getRequestSummary() { return requestSummary; }
  public String getResponseSummary() { return responseSummary; }
  public Instant getCreatedAt() { return createdAt; }
}

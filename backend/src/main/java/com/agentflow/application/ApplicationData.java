package com.agentflow.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "application_data")
public class ApplicationData {

  @Id
  @Column(name = "application_id")
  private UUID applicationId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private String payload;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected ApplicationData() {}

  public ApplicationData(UUID applicationId, String payload, Instant updatedAt) {
    this.applicationId = applicationId;
    this.payload = payload;
    this.updatedAt = updatedAt;
  }

  public UUID getApplicationId() {
    return applicationId;
  }

  public String getPayload() {
    return payload;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}

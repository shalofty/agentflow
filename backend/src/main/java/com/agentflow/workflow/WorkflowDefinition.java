package com.agentflow.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "workflow_definition")
public class WorkflowDefinition {

  @Id private UUID id;

  @Column(name = "workflow_key", nullable = false, length = 100)
  private String workflowKey;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false)
  private int version;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "definition_json", nullable = false, columnDefinition = "jsonb")
  private String definitionJson;

  @Column(nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected WorkflowDefinition() {}

  public WorkflowDefinition(
      UUID id,
      String workflowKey,
      String title,
      int version,
      String definitionJson,
      boolean active,
      Instant createdAt) {
    this.id = id;
    this.workflowKey = workflowKey;
    this.title = title;
    this.version = version;
    this.definitionJson = definitionJson;
    this.active = active;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public String getWorkflowKey() {
    return workflowKey;
  }

  public String getTitle() {
    return title;
  }

  public int getVersion() {
    return version;
  }

  public String getDefinitionJson() {
    return definitionJson;
  }

  public boolean isActive() {
    return active;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  void setActive(boolean active) {
    this.active = active;
  }

  void setTitle(String title) {
    this.title = title;
  }

  void setDefinitionJson(String definitionJson) {
    this.definitionJson = definitionJson;
  }
}

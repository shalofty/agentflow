package com.agentflow.workflow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, UUID> {

  Optional<WorkflowDefinition> findByWorkflowKeyAndVersion(String workflowKey, int version);

  Optional<WorkflowDefinition> findByWorkflowKeyAndActiveTrue(String workflowKey);

  List<WorkflowDefinition> findAllByActiveTrue();

  @Modifying
  @Query(
      "UPDATE WorkflowDefinition w SET w.active = false WHERE w.workflowKey = :key AND w.active = true")
  void deactivateAllActiveForKey(@Param("key") String workflowKey);
}

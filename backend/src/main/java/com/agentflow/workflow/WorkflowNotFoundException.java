package com.agentflow.workflow;

public class WorkflowNotFoundException extends RuntimeException {

  private final String workflowKey;

  public WorkflowNotFoundException(String workflowKey) {
    super("No active workflow definition for key: " + workflowKey);
    this.workflowKey = workflowKey;
  }

  public String getWorkflowKey() {
    return workflowKey;
  }
}

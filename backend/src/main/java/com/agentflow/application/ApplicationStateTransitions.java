package com.agentflow.application;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStateTransitions {

  public void requireSubmittable(UUID applicationId, ApplicationStatus currentStatus) {
    if (currentStatus != ApplicationStatus.DRAFT) {
      throw new ApplicationStateTransitionException(applicationId, currentStatus);
    }
  }
}

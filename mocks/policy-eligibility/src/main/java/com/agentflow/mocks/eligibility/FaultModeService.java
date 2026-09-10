package com.agentflow.mocks.eligibility;

import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

@Service
public class FaultModeService {

  private final AtomicReference<FaultMode> mode =
      new AtomicReference<>(FaultMode.NONE);

  public FaultMode getMode() {
    return mode.get();
  }

  public void setMode(FaultMode mode) {
    this.mode.set(mode);
  }
}

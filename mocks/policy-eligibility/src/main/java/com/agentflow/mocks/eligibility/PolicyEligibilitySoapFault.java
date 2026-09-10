package com.agentflow.mocks.eligibility;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

@SoapFault(faultCode = FaultCode.SERVER)
public class PolicyEligibilitySoapFault extends RuntimeException {

  public PolicyEligibilitySoapFault(String message) {
    super(message);
  }
}

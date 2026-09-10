package com.agentflow.demo;

import com.agentflow.integration.RetryableIntegrationException;
import com.agentflow.integration.rest.CustomerVerificationClient;
import com.agentflow.integration.rest.CustomerVerificationResult;
import com.agentflow.integration.rest.VerifyCommand;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Primary
@Component
@Profile({"local", "demo"})
final class DemoCustomerVerificationClient extends CustomerVerificationClient {

  DemoCustomerVerificationClient(
      @Qualifier("customerVerificationWebClient") WebClient webClient) {
    super(webClient);
  }

  @Override
  public CustomerVerificationResult verify(VerifyCommand command) {
    return DemoScenarioContext.current()
        .map(scenario -> simulate(scenario, command))
        .orElseGet(() -> super.verify(command));
  }

  private CustomerVerificationResult simulate(DemoScenario scenario, VerifyCommand command) {
    return switch (scenario) {
      case REST_500 ->
          throw new RetryableIntegrationException(
              "Demo customer verification returned 500", 500);
      case REST_TIMEOUT ->
          throw new RetryableIntegrationException(
              "Demo customer verification request timed out");
      case SOAP_FAULT, SOAP_MANUAL_REVIEW ->
          new CustomerVerificationResult(command.customerId(), true, 25);
    };
  }
}

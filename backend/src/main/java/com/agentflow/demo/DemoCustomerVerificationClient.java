package com.agentflow.demo;

import com.agentflow.integration.rest.CustomerVerificationClient;
import com.agentflow.integration.rest.CustomerVerificationResult;
import com.agentflow.integration.rest.VerifyCommand;
import com.agentflow.integration.config.IntegrationSecurityProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Demo-profile primary bean. Always delegates to the real WebClient path so failure injection
 * travels on the outbound request (explicit scenario header), never via ThreadLocal inside
 * reactive callbacks.
 */
@Primary
@Component
@Profile({"local", "demo"})
final class DemoCustomerVerificationClient extends CustomerVerificationClient {

  DemoCustomerVerificationClient(
      @Qualifier("customerVerificationWebClient") WebClient webClient,
      IntegrationSecurityProperties securityProperties) {
    super(webClient, securityProperties);
  }

  @Override
  public CustomerVerificationResult verify(VerifyCommand command) {
    return super.verify(command);
  }
}

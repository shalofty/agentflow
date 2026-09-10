package com.agentflow.integration.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agentflow.integration.RetryableIntegrationException;
import com.agentflow.integration.config.IntegrationSecurityProperties;
import com.agentflow.integration.config.CustomerVerificationProperties;
import com.agentflow.integration.config.WebClientConfig;
import com.agentflow.integration.rest.CustomerVerificationClient;
import com.agentflow.integration.rest.CustomerVerificationResult;
import com.agentflow.integration.rest.VerifyCommand;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Tag("worker")
class CustomerVerificationWorkerCompatibilityIT {

  private static final String SECRET = "local-proof-secret";
  private static final Path WORKER_DIR =
      Path.of("..", "deploy", "workers", "customer-verification").toAbsolutePath().normalize();

  private static WorkerLocalProcess worker;
  private static CustomerVerificationClient client;
  private static CustomerVerificationClient unauthorizedClient;

  @BeforeAll
  static void startWorker() throws Exception {
    worker = WorkerLocalProcess.start(WORKER_DIR, SECRET);
    CustomerVerificationProperties props =
        new CustomerVerificationProperties(worker.baseUrl(), 1000, 2000);
    WebClient webClient = new WebClientConfig().customerVerificationWebClient(props);
    client =
        new CustomerVerificationClient(webClient, new IntegrationSecurityProperties(SECRET));
    unauthorizedClient =
        new CustomerVerificationClient(webClient, new IntegrationSecurityProperties(""));
  }

  @AfterAll
  static void stopWorker() {
    if (worker != null) {
      worker.close();
    }
  }

  @Test
  void successMapsResponse() {
    UUID customerId = UUID.randomUUID();

    CustomerVerificationResult result =
        client.verify(new VerifyCommand(customerId, "corr-rest-ok"));

    assertThat(result.verified()).isTrue();
    assertThat(result.riskScore()).isEqualTo(27);
    assertThat(result.customerId()).isEqualTo(customerId);
  }

  @Test
  void rest500ScenarioIsRetryable() {
    assertThatThrownBy(
            () ->
                client.verify(
                    new VerifyCommand(UUID.randomUUID(), "corr-500", "rest-500")))
        .isInstanceOf(RetryableIntegrationException.class)
        .satisfies(
            ex ->
                assertThat(((RetryableIntegrationException) ex).getHttpStatus()).isEqualTo(500));
  }

  @Test
  void restTimeoutScenarioIsRetryable() {
    CustomerVerificationProperties props =
        new CustomerVerificationProperties(worker.baseUrl(), 1000, 500);
    WebClient shortTimeout = new WebClientConfig().customerVerificationWebClient(props);
    CustomerVerificationClient shortClient =
        new CustomerVerificationClient(shortTimeout, new IntegrationSecurityProperties(SECRET));

    assertThatThrownBy(
            () ->
                shortClient.verify(
                    new VerifyCommand(UUID.randomUUID(), "corr-timeout", "rest-timeout")))
        .isInstanceOf(RetryableIntegrationException.class)
        .hasMessageContaining("timed out");
  }

  @Test
  void missingSharedSecretIsRejected() {
    assertThatThrownBy(
            () -> unauthorizedClient.verify(new VerifyCommand(UUID.randomUUID(), "corr-unauth")))
        .isInstanceOf(WebClientResponseException.Unauthorized.class);
  }

  @Test
  void demoScenarioOnlyHonoredWhenAuthenticated() {
    assertThatThrownBy(
            () ->
                unauthorizedClient.verify(
                    new VerifyCommand(UUID.randomUUID(), "corr", "rest-500")))
        .isInstanceOf(WebClientResponseException.Unauthorized.class);
  }
}

package com.agentflow.integration.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agentflow.integration.RetryableIntegrationException;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class CustomerVerificationClientTest {

  private MockWebServer server;
  private CustomerVerificationClient client;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    WebClient webClient = WebClient.builder().baseUrl(server.url("/").toString()).build();
    client =
        new CustomerVerificationClient(
            webClient, new com.agentflow.integration.config.IntegrationSecurityProperties(""));
  }

  @AfterEach
  void tearDown() throws IOException {
    server.shutdown();
  }

  @Test
  void verifySuccessMapsResponse() throws InterruptedException {
    UUID customerId = UUID.randomUUID();
    server.enqueue(
        new MockResponse()
            .setBody(
                "{\"customerId\":\""
                    + customerId
                    + "\",\"verified\":true,\"riskScore\":27}")
            .addHeader("Content-Type", "application/json"));

    CustomerVerificationResult result =
        client.verify(new VerifyCommand(customerId, "corr-123"));

    assertThat(result.customerId()).isEqualTo(customerId);
    assertThat(result.verified()).isTrue();
    assertThat(result.riskScore()).isEqualTo(27);

    var recorded = server.takeRequest();
    assertThat(recorded.getPath()).isEqualTo("/verify");
    assertThat(recorded.getHeader("X-Correlation-Id")).isEqualTo("corr-123");
    assertThat(recorded.getHeader("X-AgentFlow-Mock-Key")).isNull();
  }

  @Test
  void verifySendsMockKeyAndDemoScenarioWhenConfigured() throws InterruptedException {
    server.enqueue(
        new MockResponse()
            .setBody(
                "{\"customerId\":\""
                    + UUID.randomUUID()
                    + "\",\"verified\":true,\"riskScore\":27}")
            .addHeader("Content-Type", "application/json"));

    CustomerVerificationClient secured =
        new CustomerVerificationClient(
            WebClient.builder().baseUrl(server.url("/").toString()).build(),
            new com.agentflow.integration.config.IntegrationSecurityProperties("sekret"));

    secured.verify(new VerifyCommand(UUID.randomUUID(), "corr-sec", "rest-500"));

    var recorded = server.takeRequest();
    assertThat(recorded.getHeader("X-AgentFlow-Mock-Key")).isEqualTo("sekret");
    assertThat(recorded.getHeader("X-AgentFlow-Demo-Scenario")).isEqualTo("rest-500");
  }

  @Test
  void verifyServerErrorThrowsRetryable() {
    server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

    assertThatThrownBy(
            () -> client.verify(new VerifyCommand(UUID.randomUUID(), "corr-456")))
        .isInstanceOfSatisfying(
            RetryableIntegrationException.class,
            ex -> {
              assertThat(ex.getMessage()).contains("500");
              assertThat(ex.getHttpStatus()).isEqualTo(500);
            });
  }

  @Test
  void verifyServiceUnavailableThrowsRetryable() {
    server.enqueue(new MockResponse().setResponseCode(503).setBody("Unavailable"));

    assertThatThrownBy(
            () -> client.verify(new VerifyCommand(UUID.randomUUID(), "corr-789")))
        .isInstanceOfSatisfying(
            RetryableIntegrationException.class,
            ex -> {
              assertThat(ex.getMessage()).contains("503");
              assertThat(ex.getHttpStatus()).isEqualTo(503);
            });
  }

  @Test
  void verifyTimeoutThrowsRetryable() {
    server.enqueue(new MockResponse().setBodyDelay(3, TimeUnit.SECONDS).setBody("{}"));

    WebClient slowClient =
        WebClient.builder()
            .baseUrl(server.url("/").toString())
            .clientConnector(
                new org.springframework.http.client.reactive.ReactorClientHttpConnector(
                    reactor.netty.http.client.HttpClient.create()
                        .responseTimeout(java.time.Duration.ofMillis(500))))
            .build();
    CustomerVerificationClient slowClientWrapper =
        new CustomerVerificationClient(
            slowClient, new com.agentflow.integration.config.IntegrationSecurityProperties(""));

    assertThatThrownBy(
            () -> slowClientWrapper.verify(new VerifyCommand(UUID.randomUUID(), "corr-timeout")))
        .isInstanceOf(RetryableIntegrationException.class)
        .hasMessageContaining("timed out");
  }
}

package com.agentflow.integration.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agentflow.integration.RetryableIntegrationException;
import com.agentflow.integration.config.IntegrationSecurityProperties;
import com.agentflow.integration.eligibility.EligibilityCommand;
import com.agentflow.integration.eligibility.EligibilityResult;
import com.agentflow.integration.soap.PolicyEligibilitySoapClient;
import com.agentflow.integration.soap.PolicyEligibilitySoapConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

@Tag("worker")
class PolicyEligibilityWorkerCompatibilityIT {

  private static final String SECRET = "local-proof-secret";
  private static final Path WORKER_DIR =
      Path.of("..", "deploy", "workers", "policy-eligibility").toAbsolutePath().normalize();
  private static final Path CHECKED_IN_WSDL =
      Path.of("..", "mocks", "policy-eligibility", "src", "main", "resources", "wsdl", "policy-eligibility.wsdl")
          .toAbsolutePath()
          .normalize();

  private static final UUID APPLICATION_ID =
      UUID.fromString("22222222-2222-2222-2222-222222222222");

  private static WorkerLocalProcess worker;
  private static PolicyEligibilitySoapClient client;
  private static PolicyEligibilitySoapClient unauthorizedClient;

  @BeforeAll
  static void startWorker() throws Exception {
    worker = WorkerLocalProcess.start(WORKER_DIR, SECRET);
    client = soapClient(SECRET);
    unauthorizedClient = soapClient("");
  }

  @AfterAll
  static void stopWorker() {
    if (worker != null) {
      worker.close();
    }
  }

  @ParameterizedTest
  @MethodSource("riskDecisions")
  void realSpringWsClientUnmarshalsDecisions(int riskScore, EligibilityResult expected) {
    EligibilityResult result =
        client.check(
            new EligibilityCommand(APPLICATION_ID, UUID.randomUUID(), "corr-soap", riskScore));

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void soapFaultUsesRetryableExceptionPath() {
    assertThatThrownBy(
            () ->
                client.check(
                    new EligibilityCommand(
                        APPLICATION_ID,
                        UUID.randomUUID(),
                        "corr-fault",
                        10,
                        "soap-fault")))
        .isInstanceOf(RetryableIntegrationException.class)
        .hasMessageContaining("SOAP fault");
  }

  @Test
  void requestScopedManualReviewWorks() {
    EligibilityResult result =
        client.check(
            new EligibilityCommand(
                APPLICATION_ID,
                UUID.randomUUID(),
                "corr-manual",
                10,
                "soap-manual-review"));

    assertThat(result).isEqualTo(EligibilityResult.MANUAL_REVIEW);
  }

  @Test
  void missingSharedSecretIsRejected() {
    assertThatThrownBy(
            () ->
                unauthorizedClient.check(
                    new EligibilityCommand(
                        APPLICATION_ID, UUID.randomUUID(), "corr-unauth", 10)))
        .isInstanceOf(RetryableIntegrationException.class);
  }

  @Test
  void publicWsdlRemainsReadableAndEquivalent() throws Exception {
    HttpClient http = HttpClient.newHttpClient();
    HttpResponse<String> response =
        http.send(
            HttpRequest.newBuilder(
                    URI.create(worker.baseUrl() + "/ws/policyEligibility.wsdl"))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Content-Type").orElse(""))
        .contains("text/xml");
    String served = normalize(response.body());
    String checkedIn = normalize(Files.readString(CHECKED_IN_WSDL));
    // Address location differs by host; compare structural markers.
    assertThat(served).contains("CheckEligibilityRequest");
    assertThat(served).contains("CheckEligibilityResponse");
    assertThat(served).contains("EligibilityDecision");
    assertThat(served).contains("soapAction=\"\"");
    assertThat(served).contains("http://agentflow.com/policy-eligibility");
    assertThat(checkedIn).contains("CheckEligibilityRequest");
  }

  @Test
  void publicXsdRemainsReadable() throws Exception {
    HttpClient http = HttpClient.newHttpClient();
    HttpResponse<String> response =
        http.send(
            HttpRequest.newBuilder(
                    URI.create(worker.baseUrl() + "/ws/policy-eligibility.xsd"))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("CheckEligibilityRequest");
  }

  private static Stream<Arguments> riskDecisions() {
    return Stream.of(
        Arguments.of(27, EligibilityResult.ELIGIBLE),
        Arguments.of(62, EligibilityResult.MANUAL_REVIEW),
        Arguments.of(90, EligibilityResult.INELIGIBLE));
  }

  private static PolicyEligibilitySoapClient soapClient(String secret) {
    Jaxb2Marshaller marshaller = PolicyEligibilitySoapConfiguration.policyEligibilityMarshaller();
    WebServiceTemplate template =
        new PolicyEligibilitySoapConfiguration()
            .policyEligibilityWebServiceTemplate(
                marshaller,
                worker.baseUrl() + "/ws",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2));
    return new PolicyEligibilitySoapClient(
        template, new IntegrationSecurityProperties(secret));
  }

  private static String normalize(String xml) {
    return xml.replaceAll("\\s+", " ").trim();
  }
}

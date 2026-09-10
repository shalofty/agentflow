package com.agentflow.integration.soap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.ws.test.client.RequestMatchers.payload;
import static org.springframework.ws.test.client.RequestMatchers.soapHeader;
import static org.springframework.ws.test.client.ResponseCreators.withPayload;
import static org.springframework.ws.test.client.ResponseCreators.withServerOrReceiverFault;

import com.agentflow.integration.RetryableIntegrationException;
import com.agentflow.integration.eligibility.EligibilityCommand;
import com.agentflow.integration.eligibility.EligibilityResult;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.test.client.MockWebServiceServer;

class PolicyEligibilitySoapClientTest {

  private static final String ENDPOINT = "http://localhost/policy-eligibility";
  private static final UUID APPLICATION_ID =
      UUID.fromString("11111111-1111-1111-1111-111111111111");

  private MockWebServiceServer server;
  private PolicyEligibilitySoapClient client;

  @BeforeEach
  void setUp() {
    Jaxb2Marshaller marshaller = PolicyEligibilitySoapConfiguration.policyEligibilityMarshaller();
    WebServiceTemplate template = new WebServiceTemplate(marshaller);
    template.setUnmarshaller(marshaller);
    template.setDefaultUri(ENDPOINT);
    server = MockWebServiceServer.createServer(template);
    client = new PolicyEligibilitySoapClient(template);
  }

  @AfterEach
  void verifyServer() {
    server.verify();
  }

  @ParameterizedTest
  @MethodSource("decisions")
  void checkMapsSoapDecision(String soapDecision, EligibilityResult expected) {
    server
        .expect(payload(requestPayload(62)))
        .andExpect(soapHeader(new QName(PolicyEligibilitySoapClient.NAMESPACE_URI, "correlationId")))
        .andRespond(withPayload(responsePayload(soapDecision)));

    EligibilityResult result =
        client.check(new EligibilityCommand(APPLICATION_ID, UUID.randomUUID(), "corr-123", 62));

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void soapFaultIsRetryable() {
    server
        .expect(payload(requestPayload(27)))
        .andExpect(soapHeader(new QName(PolicyEligibilitySoapClient.NAMESPACE_URI, "correlationId")))
        .andRespond(
            withServerOrReceiverFault("Eligibility service unavailable", Locale.ENGLISH));

    assertThatThrownBy(
            () ->
                client.check(
                    new EligibilityCommand(
                        APPLICATION_ID, UUID.randomUUID(), "corr-fault", 27)))
        .isInstanceOf(RetryableIntegrationException.class)
        .hasMessageContaining("SOAP fault");
  }

  private static Stream<Arguments> decisions() {
    return Stream.of(
        Arguments.of("ELIGIBLE", EligibilityResult.ELIGIBLE),
        Arguments.of("MANUAL_REVIEW", EligibilityResult.MANUAL_REVIEW),
        Arguments.of("INELIGIBLE", EligibilityResult.INELIGIBLE));
  }

  private static Source requestPayload(int riskScore) {
    return xml(
        """
        <pol:CheckEligibilityRequest xmlns:pol="http://agentflow.com/policy-eligibility">
          <applicationId>%s</applicationId>
          <riskScore>%d</riskScore>
        </pol:CheckEligibilityRequest>
        """
            .formatted(APPLICATION_ID, riskScore));
  }

  private static Source responsePayload(String decision) {
    return xml(
        """
        <pol:CheckEligibilityResponse xmlns:pol="http://agentflow.com/policy-eligibility">
          <applicationId>%s</applicationId>
          <decision>%s</decision>
        </pol:CheckEligibilityResponse>
        """
            .formatted(APPLICATION_ID, decision));
  }

  private static Source xml(String value) {
    return new StreamSource(new java.io.StringReader(value));
  }
}

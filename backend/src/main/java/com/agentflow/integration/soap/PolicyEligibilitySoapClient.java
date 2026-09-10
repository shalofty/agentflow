package com.agentflow.integration.soap;

import com.agentflow.integration.RetryableIntegrationException;
import com.agentflow.integration.config.IntegrationSecurityProperties;
import com.agentflow.integration.eligibility.EligibilityCommand;
import com.agentflow.integration.eligibility.EligibilityResult;
import java.io.StringReader;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.WebServiceFaultException;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.transport.HeadersAwareSenderWebServiceConnection;
import org.springframework.ws.transport.WebServiceConnection;
import org.springframework.ws.transport.context.TransportContext;
import org.springframework.ws.transport.context.TransportContextHolder;

@Component
public class PolicyEligibilitySoapClient
    implements com.agentflow.integration.eligibility.PolicyEligibilityClient {

  public static final String NAMESPACE_URI = "http://agentflow.com/policy-eligibility";
  public static final String DEMO_SCENARIO_HEADER = "X-AgentFlow-Demo-Scenario";
  public static final String MOCK_KEY_HEADER = "X-AgentFlow-Mock-Key";

  private final WebServiceTemplate webServiceTemplate;
  private final IntegrationSecurityProperties securityProperties;

  public PolicyEligibilitySoapClient(
      @Qualifier("policyEligibilityWebServiceTemplate") WebServiceTemplate webServiceTemplate,
      IntegrationSecurityProperties securityProperties) {
    this.webServiceTemplate = webServiceTemplate;
    this.securityProperties = securityProperties;
  }

  @Override
  public EligibilityResult check(EligibilityCommand command) {
    // Capture scenario/secret on the calling thread before SOAP transport may switch threads.
    final String correlationId = command.correlationId();
    final String demoScenario = command.demoScenario();
    final String mockKey =
        securityProperties.hasMockSharedSecret() ? securityProperties.mockSharedSecret() : null;

    CheckEligibilityRequest request = new CheckEligibilityRequest();
    request.setApplicationId(command.applicationId().toString());
    request.setRiskScore(command.riskScore());

    try {
      Object payload =
          webServiceTemplate.marshalSendAndReceive(
              request,
              message -> {
                addCorrelationHeader((SoapMessage) message, correlationId);
                addHttpHeaders(demoScenario, mockKey);
              });
      if (!(payload instanceof CheckEligibilityResponse response)
          || response.getDecision() == null) {
        throw new RetryableIntegrationException(
            "Policy eligibility returned an empty SOAP response");
      }
      return EligibilityResult.valueOf(response.getDecision().name());
    } catch (RetryableIntegrationException ex) {
      throw ex;
    } catch (WebServiceFaultException ex) {
      throw new RetryableIntegrationException(
          "Policy eligibility SOAP fault: " + ex.getMessage(), ex);
    } catch (WebServiceIOException ex) {
      throw new RetryableIntegrationException(
          "Policy eligibility SOAP transport failure: " + ex.getMessage(), ex);
    }
  }

  private static void addHttpHeaders(String demoScenario, String mockKey) {
    TransportContext transportContext = TransportContextHolder.getTransportContext();
    if (transportContext == null) {
      return;
    }
    WebServiceConnection connection = transportContext.getConnection();
    if (!(connection instanceof HeadersAwareSenderWebServiceConnection headersConnection)) {
      return;
    }
    try {
      if (mockKey != null) {
        headersConnection.addRequestHeader(MOCK_KEY_HEADER, mockKey);
      }
      if (demoScenario != null && !demoScenario.isBlank()) {
        headersConnection.addRequestHeader(DEMO_SCENARIO_HEADER, demoScenario);
      }
    } catch (Exception ex) {
      throw new IllegalStateException("Could not write SOAP HTTP headers", ex);
    }
  }

  private static void addCorrelationHeader(SoapMessage message, String correlationId) {
    try {
      String header =
          """
          <correlationId xmlns="%s">%s</correlationId>
          """
              .formatted(NAMESPACE_URI, escapeXml(correlationId));
      TransformerFactory.newInstance()
          .newTransformer()
          .transform(
              new StreamSource(new StringReader(header)),
              message.getSoapHeader().getResult());
    } catch (Exception ex) {
      throw new IllegalStateException("Could not write SOAP correlation header", ex);
    }
  }

  private static String escapeXml(String value) {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }
}

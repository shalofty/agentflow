package com.agentflow.integration.soap;

import com.agentflow.integration.RetryableIntegrationException;
import com.agentflow.integration.eligibility.EligibilityCommand;
import com.agentflow.integration.eligibility.EligibilityResult;
import java.io.StringReader;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.WebServiceFaultException;
import org.springframework.ws.client.WebServiceTransportException;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.SoapMessage;

@Component
public class PolicyEligibilitySoapClient
    implements com.agentflow.integration.eligibility.PolicyEligibilityClient {

  public static final String NAMESPACE_URI =
      "http://agentflow.com/policy-eligibility";

  private final WebServiceTemplate webServiceTemplate;

  public PolicyEligibilitySoapClient(
      @Qualifier("policyEligibilityWebServiceTemplate")
          WebServiceTemplate webServiceTemplate) {
    this.webServiceTemplate = webServiceTemplate;
  }

  @Override
  public EligibilityResult check(EligibilityCommand command) {
    CheckEligibilityRequest request = new CheckEligibilityRequest();
    request.setApplicationId(command.applicationId().toString());
    request.setRiskScore(command.riskScore());

    try {
      Object payload =
          webServiceTemplate.marshalSendAndReceive(
              request, message -> addCorrelationHeader((SoapMessage) message, command.correlationId()));
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
    } catch (WebServiceTransportException ex) {
      throw new RetryableIntegrationException(
          "Policy eligibility SOAP transport failure: " + ex.getMessage(), ex);
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

package com.agentflow.integration.soap;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

@Configuration
public class PolicyEligibilitySoapConfiguration {

  @Bean
  public static Jaxb2Marshaller policyEligibilityMarshaller() {
    Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
    marshaller.setClassesToBeBound(
        CheckEligibilityRequest.class,
        CheckEligibilityResponse.class,
        EligibilityDecision.class);
    return marshaller;
  }

  @Bean
  public WebServiceTemplate policyEligibilityWebServiceTemplate(
      Jaxb2Marshaller policyEligibilityMarshaller,
      @Value("${agentflow.integrations.policy-eligibility.url:http://localhost:8092/ws}")
          String url,
      @Value("${agentflow.integrations.policy-eligibility.connect-timeout:1s}")
          Duration connectTimeout,
      @Value("${agentflow.integrations.policy-eligibility.read-timeout:2s}")
          Duration readTimeout) {
    HttpUrlConnectionMessageSender messageSender = new HttpUrlConnectionMessageSender();
    messageSender.setConnectionTimeout(connectTimeout);
    messageSender.setReadTimeout(readTimeout);

    WebServiceTemplate template = new WebServiceTemplate(policyEligibilityMarshaller);
    template.setUnmarshaller(policyEligibilityMarshaller);
    template.setDefaultUri(url);
    template.setMessageSender(messageSender);
    return template;
  }
}

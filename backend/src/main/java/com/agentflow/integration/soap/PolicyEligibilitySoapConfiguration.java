package com.agentflow.integration.soap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

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
  WebServiceTemplate policyEligibilityWebServiceTemplate(
      Jaxb2Marshaller policyEligibilityMarshaller,
      @Value("${agentflow.integrations.policy-eligibility.url:http://localhost:8092/ws}")
          String url) {
    WebServiceTemplate template = new WebServiceTemplate(policyEligibilityMarshaller);
    template.setUnmarshaller(policyEligibilityMarshaller);
    template.setDefaultUri(url);
    return template;
  }
}

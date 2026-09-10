package com.agentflow.mocks.eligibility;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

@EnableWs
@Configuration
public class WebServiceConfiguration {

  @Bean
  ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
      ApplicationContext applicationContext) {
    var servlet = new MessageDispatcherServlet();
    servlet.setApplicationContext(applicationContext);
    servlet.setTransformWsdlLocations(true);
    return new ServletRegistrationBean<>(servlet, "/ws/*");
  }

  @Bean(name = "policyEligibility")
  DefaultWsdl11Definition policyEligibilityWsdl(XsdSchema policyEligibilitySchema) {
    var definition = new DefaultWsdl11Definition();
    definition.setPortTypeName("PolicyEligibilityPort");
    definition.setLocationUri("/ws");
    definition.setTargetNamespace(PolicyEligibilityEndpoint.NAMESPACE_URI);
    definition.setSchema(policyEligibilitySchema);
    return definition;
  }

  @Bean
  XsdSchema policyEligibilitySchema() {
    return new SimpleXsdSchema(
        new ClassPathResource("xsd/policy-eligibility.xsd"));
  }
}

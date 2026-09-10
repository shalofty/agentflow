package com.agentflow.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agentflow.integrations")
public record IntegrationSecurityProperties(String mockSharedSecret) {

  public IntegrationSecurityProperties {
    if (mockSharedSecret == null) {
      mockSharedSecret = "";
    }
  }

  public boolean hasMockSharedSecret() {
    return !mockSharedSecret.isBlank();
  }
}

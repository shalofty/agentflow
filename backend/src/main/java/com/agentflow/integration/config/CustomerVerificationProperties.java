package com.agentflow.integration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agentflow.integrations.customer-verification")
public record CustomerVerificationProperties(
    String baseUrl, int connectTimeoutMs, int readTimeoutMs) {

  public CustomerVerificationProperties {
    if (connectTimeoutMs <= 0) {
      connectTimeoutMs = 1000;
    }
    if (readTimeoutMs <= 0) {
      readTimeoutMs = 2000;
    }
  }
}

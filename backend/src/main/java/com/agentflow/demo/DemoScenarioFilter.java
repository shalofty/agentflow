package com.agentflow.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Profile({"local", "demo"})
final class DemoScenarioFilter extends OncePerRequestFilter {

  private static final String HEADER = "X-Demo-Scenario";
  private final ObjectMapper objectMapper;

  DemoScenarioFilter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      readScenario(request).ifPresent(DemoScenarioContext::set);
      filterChain.doFilter(request, response);
    } finally {
      DemoScenarioContext.clear();
    }
  }

  private java.util.Optional<DemoScenario> readScenario(HttpServletRequest request)
      throws IOException {
    if (!"POST".equals(request.getMethod())
        || !request.getRequestURI().matches("/api/applications/[^/]+/submit")) {
      return java.util.Optional.empty();
    }

    String header = request.getHeader(HEADER);
    if (header != null && !header.isBlank()) {
      return DemoScenario.from(header);
    }
    if (request.getContentLengthLong() <= 0) {
      return java.util.Optional.empty();
    }
    var body = objectMapper.readTree(request.getInputStream());
    return body.hasNonNull("demoScenario")
        ? DemoScenario.from(body.get("demoScenario").asText())
        : java.util.Optional.empty();
  }
}

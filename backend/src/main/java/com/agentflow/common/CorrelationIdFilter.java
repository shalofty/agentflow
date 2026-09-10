package com.agentflow.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-Correlation-Id";
  public static final String ATTRIBUTE = CorrelationIdFilter.class.getName() + ".correlationId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String supplied = request.getHeader(HEADER);
    String correlationId =
        supplied == null || supplied.isBlank()
            ? UUID.randomUUID().toString()
            : supplied.substring(0, Math.min(supplied.length(), 64));
    request.setAttribute(ATTRIBUTE, correlationId);
    response.setHeader(HEADER, correlationId);
    MDC.put("correlationId", correlationId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove("correlationId");
    }
  }
}

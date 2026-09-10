package com.agentflow.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lightweight free-tier abuse bound for public demo customer creation. Not a substitute for auth.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class DemoAbuseLimitFilter extends OncePerRequestFilter {

  private static final int MAX_CUSTOMER_CREATES_PER_WINDOW = 30;
  private static final long WINDOW_MS = 60 * 60 * 1000L;

  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if ("POST".equalsIgnoreCase(request.getMethod())
        && "/api/customers".equals(request.getRequestURI())) {
      String key = clientKey(request);
      if (!allow(key)) {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response
            .getWriter()
            .write(
                """
                {"title":"Too Many Requests","detail":"Customer creation rate limit exceeded for this demo. Try again later.","status":429}
                """);
        return;
      }
    }
    filterChain.doFilter(request, response);
  }

  private boolean allow(String key) {
    long now = Instant.now().toEpochMilli();
    Window window =
        windows.compute(
            key,
            (ignored, existing) -> {
              if (existing == null || now - existing.startedAtMs > WINDOW_MS) {
                return new Window(now);
              }
              return existing;
            });
    return window.count.incrementAndGet() <= MAX_CUSTOMER_CREATES_PER_WINDOW;
  }

  private static String clientKey(HttpServletRequest request) {
    String forwarded = request.getHeader("CF-Connecting-IP");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.trim();
    }
    String xff = request.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      return xff.split(",")[0].trim();
    }
    return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
  }

  private static final class Window {
    private final long startedAtMs;
    private final AtomicInteger count = new AtomicInteger();

    private Window(long startedAtMs) {
      this.startedAtMs = startedAtMs;
    }
  }
}

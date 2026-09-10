package com.agentflow.integration.rest;

import com.agentflow.integration.RetryableIntegrationException;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.TimeoutException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
public class CustomerVerificationClient {

  private final WebClient webClient;

  public CustomerVerificationClient(
      @Qualifier("customerVerificationWebClient") WebClient webClient) {
    this.webClient = webClient;
  }

  public CustomerVerificationResult verify(VerifyCommand command) {
    try {
      VerifyResponse response =
          webClient
              .post()
              .uri("/verify")
              .header("X-Correlation-Id", command.correlationId())
              .bodyValue(new VerifyRequest(command.customerId()))
              .retrieve()
              .onStatus(
                  HttpStatusCode::is5xxServerError,
                  clientResponse ->
                      clientResponse
                          .bodyToMono(String.class)
                          .defaultIfEmpty("")
                          .flatMap(
                              body ->
                                  Mono.error(
                                      new RetryableIntegrationException(
                                          "Customer verification returned "
                                              + clientResponse.statusCode()
                                              + (body.isBlank() ? "" : ": " + body)))))
              .bodyToMono(VerifyResponse.class)
              .block();

      if (response == null) {
        throw new RetryableIntegrationException("Customer verification returned empty response");
      }

      return new CustomerVerificationResult(
          response.customerId(), response.verified(), response.riskScore());
    } catch (RetryableIntegrationException ex) {
      throw ex;
    } catch (WebClientResponseException ex) {
      if (ex.getStatusCode().is5xxServerError()) {
        throw new RetryableIntegrationException(
            "Customer verification returned " + ex.getStatusCode(), ex);
      }
      if (isTimeout(ex)) {
        throw new RetryableIntegrationException("Customer verification request timed out", ex);
      }
      throw ex;
    } catch (WebClientRequestException | TimeoutException ex) {
      throw new RetryableIntegrationException("Customer verification request timed out", ex);
    } catch (Exception ex) {
      if (isTimeout(ex)) {
        throw new RetryableIntegrationException("Customer verification request timed out", ex);
      }
      throw ex;
    }
  }

  private static boolean isTimeout(Throwable ex) {
    Throwable current = ex;
    while (current != null) {
      if (current instanceof ReadTimeoutException
          || current instanceof TimeoutException
          || current instanceof java.util.concurrent.TimeoutException) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }
}

package com.agentflow.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class ConcurrentSaveSubmitIT {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("agentflow")
          .withUsername("agentflow")
          .withPassword("agentflow");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired ApplicationService applicationService;
  @Autowired ApplicationRepository applicationRepository;
  @Autowired ApplicationDataRepository applicationDataRepository;
  @Autowired com.agentflow.customer.CustomerService customerService;
  @Autowired TransactionTemplate transactionTemplate;

  @Test
  void lateSaveCannotMutateAfterConcurrentClaim() throws Exception {
    UUID customerId =
        customerService
            .create(
                new com.agentflow.customer.CustomerRequest(
                    "Race", "Test", "race-" + UUID.randomUUID() + "@example.com", null))
            .id();
    UUID applicationId =
        applicationService
            .create(new ApplicationCreateRequest(customerId, "auto-policy"))
            .id();

    Map<String, Object> completePayload =
        Map.of(
            "vin", "1HGCM82633A123456",
            "vehicleYear", 2020,
            "coverageType", "LIABILITY",
            "hasGarage", false,
            "effectiveDate", "2026-01-01");
    applicationService.saveData(applicationId, completePayload);

    CountDownLatch claimReady = new CountDownLatch(1);
    AtomicReference<Exception> saveError = new AtomicReference<>();

    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      Future<?> claimFuture =
          pool.submit(
              () ->
                  transactionTemplate.executeWithoutResult(
                      status -> {
                        try {
                          Application locked =
                              applicationRepository
                                  .findLockedById(applicationId)
                                  .orElseThrow();
                          claimReady.countDown();
                          // Keep the lock long enough for the save to block, then claim.
                          Thread.sleep(400);
                          locked.setStatus(ApplicationStatus.SUBMITTED);
                          applicationRepository.save(locked);
                        } catch (InterruptedException ex) {
                          Thread.currentThread().interrupt();
                          throw new IllegalStateException(ex);
                        }
                      }));

      Future<?> saveFuture =
          pool.submit(
              () -> {
                try {
                  assertThat(claimReady.await(10, TimeUnit.SECONDS)).isTrue();
                  Thread.sleep(50);
                  Map<String, Object> mutated =
                      Map.of(
                          "vin", "MUTATEDVIN0000001",
                          "vehicleYear", 1999,
                          "coverageType", "LIABILITY",
                          "hasGarage", true,
                          "effectiveDate", "2099-12-31");
                  applicationService.saveData(applicationId, mutated);
                } catch (Exception ex) {
                  saveError.set(ex);
                }
              });

      claimFuture.get(30, TimeUnit.SECONDS);
      saveFuture.get(30, TimeUnit.SECONDS);
    } finally {
      pool.shutdownNow();
    }

    assertThat(saveError.get()).isInstanceOf(ApplicationNotDraftException.class);

    Application application = applicationRepository.findById(applicationId).orElseThrow();
    assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);

    String payload = applicationDataRepository.findById(applicationId).orElseThrow().getPayload();
    assertThat(payload).doesNotContain("MUTATEDVIN");
    assertThat(payload).contains("1HGCM82633A123456");
  }

  @Test
  void saveAgainstSubmittedStatusFailsImmediately() {
    UUID customerId =
        customerService
            .create(
                new com.agentflow.customer.CustomerRequest(
                    "Late", "Save", "late-" + UUID.randomUUID() + "@example.com", null))
            .id();
    UUID applicationId =
        applicationService
            .create(new ApplicationCreateRequest(customerId, "auto-policy"))
            .id();
    applicationService.saveData(
        applicationId,
        Map.of(
            "vin", "1HGCM82633A123456",
            "vehicleYear", 2020,
            "coverageType", "LIABILITY",
            "hasGarage", false,
            "effectiveDate", "2026-01-01"));

    transactionTemplate.executeWithoutResult(
        status -> {
          Application application = applicationRepository.findById(applicationId).orElseThrow();
          application.setStatus(ApplicationStatus.APPROVED);
          applicationRepository.save(application);
        });

    assertThatThrownBy(
            () ->
                applicationService.saveData(
                    applicationId, Map.of("vin", "SHOULDNOTSAVE00001")))
        .isInstanceOf(ApplicationNotDraftException.class);
  }
}

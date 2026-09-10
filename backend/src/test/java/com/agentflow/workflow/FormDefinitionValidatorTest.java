package com.agentflow.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class FormDefinitionValidatorTest {

  private FormDefinitionValidator validator;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    validator = new FormDefinitionValidator(objectMapper);
  }

  @Test
  void requiresVin() throws IOException {
    var def = loadAutoDefinition();
    var errors = validator.validate(def, Map.of("coverageType", "LIABILITY"));
    assertThat(errors).anyMatch(e -> e.field().equals("vin"));
  }

  @Test
  void coverageAmountRequiredWhenFullCoverage() throws IOException {
    var def = loadAutoDefinition();
    var errors =
        validator.validate(
            def,
            Map.of(
                "vin", "1HGCM82633A123456",
                "vehicleYear", 2020,
                "coverageType", "FULL",
                "hasGarage", true,
                "effectiveDate", "2026-01-01"));
    assertThat(errors).anyMatch(e -> e.field().equals("coverageAmount"));
  }

  @Test
  void coverageAmountNotRequiredWhenLiability() throws IOException {
    var def = loadAutoDefinition();
    var errors =
        validator.validate(
            def,
            Map.of(
                "vin", "1HGCM82633A123456",
                "vehicleYear", 2020,
                "coverageType", "LIABILITY",
                "hasGarage", false,
                "effectiveDate", "2026-01-01"));
    assertThat(errors).noneMatch(e -> e.field().equals("coverageAmount"));
  }

  @Test
  void rejectsSelectValueNotPresentInOptions() throws IOException {
    var errors =
        validator.validate(
            loadAutoDefinition(),
            Map.of(
                "vin", "1HGCM82633A123456",
                "vehicleYear", 2020,
                "coverageType", "UNKNOWN",
                "hasGarage", false,
                "effectiveDate", "2026-01-01"));

    assertThat(errors)
        .anyMatch(
            error ->
                error.field().equals("coverageType")
                    && error.message().equals("Must be one of the configured options"));
  }

  @Test
  void rejectsDateThatIsNotAnIsoLocalDate() throws IOException {
    var errors =
        validator.validate(
            loadAutoDefinition(),
            Map.of(
                "vin", "1HGCM82633A123456",
                "vehicleYear", 2020,
                "coverageType", "LIABILITY",
                "hasGarage", false,
                "effectiveDate", "01/31/2026"));

    assertThat(errors)
        .anyMatch(
            error ->
                error.field().equals("effectiveDate")
                    && error.message().equals("Must be a valid ISO date"));
  }

  @Test
  void acceptsValidSelectAndIsoLocalDate() throws IOException {
    var errors =
        validator.validate(
            loadAutoDefinition(),
            Map.of(
                "vin", "1HGCM82633A123456",
                "vehicleYear", 2020,
                "coverageType", "LIABILITY",
                "hasGarage", false,
                "effectiveDate", "2026-01-31"));

    assertThat(errors)
        .noneMatch(
            error ->
                error.field().equals("coverageType") || error.field().equals("effectiveDate"));
  }

  private Map<String, Object> loadAutoDefinition() throws IOException {
    try (var in = new ClassPathResource("workflows/auto-policy-v1.json").getInputStream()) {
      return objectMapper.readValue(in, Map.class);
    }
  }
}

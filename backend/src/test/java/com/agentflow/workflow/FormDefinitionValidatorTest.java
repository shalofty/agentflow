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

  private Map<String, Object> loadAutoDefinition() throws IOException {
    try (var in = new ClassPathResource("workflows/auto-policy-v1.json").getInputStream()) {
      return objectMapper.readValue(in, Map.class);
    }
  }
}

package com.agentflow.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class FormDefinitionValidator {

  private final ObjectMapper objectMapper;

  public FormDefinitionValidator(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** Full validation including required fields (used at submit). */
  public List<ValidationError> validate(Map<String, Object> definition, Map<String, ?> payload) {
    return validate(definition, payload, true);
  }

  /**
   * @param requireComplete when false, blank required fields are allowed (draft saves); supplied
   *     values are still type/format validated.
   */
  public List<ValidationError> validate(
      Map<String, Object> definition, Map<String, ?> payload, boolean requireComplete) {
    JsonNode definitionNode = objectMapper.valueToTree(definition);
    return validateNode(definitionNode, payload, requireComplete);
  }

  public List<ValidationError> validateNode(JsonNode definition, Map<String, ?> payload) {
    return validateNode(definition, payload, true);
  }

  public List<ValidationError> validateNode(
      JsonNode definition, Map<String, ?> payload, boolean requireComplete) {
    var errors = new ArrayList<ValidationError>();
    var fields = definition.get("fields");
    if (fields == null || !fields.isArray()) {
      return errors;
    }

    for (var field : fields) {
      var name = field.get("name").asText();
      var label = field.path("label").asText(name);
      if (!isVisible(field, payload)) {
        continue;
      }

      var value = payload.get(name);
      if (requireComplete && field.path("required").asBoolean(false) && isBlank(value)) {
        errors.add(new ValidationError(name, label, "Field is required"));
        continue;
      }

      if (!isBlank(value)) {
        validateFieldValue(field, name, label, value, errors);
      }
    }

    return errors;
  }

  private boolean isVisible(JsonNode field, Map<String, ?> payload) {
    var visibleWhen = field.get("visibleWhen");
    if (visibleWhen == null || visibleWhen.isNull()) {
      return true;
    }
    var dependentField = visibleWhen.get("field").asText();
    var expected = visibleWhen.get("equals");
    var actual = payload.get(dependentField);
    return valuesEqual(actual, expected);
  }

  private void validateFieldValue(
      JsonNode field,
      String name,
      String label,
      Object value,
      List<ValidationError> errors) {
    var validation = field.get("validation");
    var type = field.get("type").asText();
    switch (type) {
      case "text" -> {
        if (validation != null && !validation.isNull()) {
          validateText(validation, name, label, value, errors);
        }
      }
      case "number" -> {
        if (validation != null && !validation.isNull()) {
          validateNumber(validation, name, label, value, errors);
        }
      }
      case "select" -> validateSelect(field, name, label, value, errors);
      case "date" -> validateDate(name, label, value, errors);
      default -> {}
    }
  }

  private void validateSelect(
      JsonNode field,
      String name,
      String label,
      Object value,
      List<ValidationError> errors) {
    var options = field.get("options");
    boolean valid =
        options != null
            && options.isArray()
            && java.util.stream.StreamSupport.stream(options.spliterator(), false)
                .map(option -> option.get("value"))
                .filter(java.util.Objects::nonNull)
                .anyMatch(optionValue -> valuesEqual(value, optionValue));
    if (!valid) {
      errors.add(new ValidationError(name, label, "Must be one of the configured options"));
    }
  }

  private void validateDate(
      String name, String label, Object value, List<ValidationError> errors) {
    try {
      LocalDate.parse(String.valueOf(value));
    } catch (DateTimeParseException ex) {
      errors.add(new ValidationError(name, label, "Must be a valid ISO date"));
    }
  }

  private void validateText(
      JsonNode validation,
      String name,
      String label,
      Object value,
      List<ValidationError> errors) {
    var text = String.valueOf(value);
    if (validation.has("minLength")) {
      int min = validation.get("minLength").asInt();
      if (text.length() < min) {
        errors.add(new ValidationError(name, label, "Must be at least " + min + " characters"));
      }
    }
    if (validation.has("maxLength")) {
      int max = validation.get("maxLength").asInt();
      if (text.length() > max) {
        errors.add(new ValidationError(name, label, "Must be at most " + max + " characters"));
      }
    }
  }

  private void validateNumber(
      JsonNode validation,
      String name,
      String label,
      Object value,
      List<ValidationError> errors) {
    Double number = toDouble(value);
    if (number == null) {
      errors.add(new ValidationError(name, label, "Must be a number"));
      return;
    }
    if (validation.has("min")) {
      double min = validation.get("min").asDouble();
      if (number < min) {
        errors.add(new ValidationError(name, label, "Must be at least " + min));
      }
    }
    if (validation.has("max")) {
      double max = validation.get("max").asDouble();
      if (number > max) {
        errors.add(new ValidationError(name, label, "Must be at most " + max));
      }
    }
  }

  private Double toDouble(Object value) {
    if (value instanceof Number number) {
      return number.doubleValue();
    }
    try {
      return Double.parseDouble(String.valueOf(value));
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private boolean isBlank(Object value) {
    if (value == null) {
      return true;
    }
    if (value instanceof String s) {
      return s.isBlank();
    }
    return false;
  }

  private boolean valuesEqual(Object actual, JsonNode expected) {
    if (actual == null) {
      return expected.isNull();
    }
    if (expected.isBoolean()) {
      return Boolean.valueOf(String.valueOf(actual)).equals(expected.booleanValue());
    }
    if (expected.isNumber()) {
      Double actualNumber = toDouble(actual);
      return actualNumber != null && actualNumber.equals(expected.doubleValue());
    }
    return String.valueOf(actual).equals(expected.asText());
  }
}

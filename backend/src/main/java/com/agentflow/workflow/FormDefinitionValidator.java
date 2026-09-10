package com.agentflow.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

  public List<ValidationError> validate(Map<String, Object> definition, Map<String, ?> payload) {
    JsonNode definitionNode = objectMapper.valueToTree(definition);
    return validateNode(definitionNode, payload);
  }

  public List<ValidationError> validateNode(JsonNode definition, Map<String, ?> payload) {
    var errors = new ArrayList<ValidationError>();
    var fields = definition.get("fields");
    if (fields == null || !fields.isArray()) {
      return errors;
    }

    for (var field : fields) {
      var name = field.get("name").asText();
      if (!isVisible(field, payload)) {
        continue;
      }

      var value = payload.get(name);
      if (field.path("required").asBoolean(false) && isBlank(value)) {
        errors.add(new ValidationError(name, "Field is required"));
        continue;
      }

      if (!isBlank(value)) {
        validateFieldValue(field, name, value, errors);
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
      JsonNode field, String name, Object value, List<ValidationError> errors) {
    var validation = field.get("validation");
    if (validation == null || validation.isNull()) {
      return;
    }

    var type = field.get("type").asText();
    switch (type) {
      case "text" -> validateText(validation, name, value, errors);
      case "number" -> validateNumber(validation, name, value, errors);
      default -> {}
    }
  }

  private void validateText(
      JsonNode validation, String name, Object value, List<ValidationError> errors) {
    var text = String.valueOf(value);
    if (validation.has("minLength")) {
      int min = validation.get("minLength").asInt();
      if (text.length() < min) {
        errors.add(new ValidationError(name, "Must be at least " + min + " characters"));
      }
    }
    if (validation.has("maxLength")) {
      int max = validation.get("maxLength").asInt();
      if (text.length() > max) {
        errors.add(new ValidationError(name, "Must be at most " + max + " characters"));
      }
    }
  }

  private void validateNumber(
      JsonNode validation, String name, Object value, List<ValidationError> errors) {
    Double number = toDouble(value);
    if (number == null) {
      errors.add(new ValidationError(name, "Must be a number"));
      return;
    }
    if (validation.has("min")) {
      double min = validation.get("min").asDouble();
      if (number < min) {
        errors.add(new ValidationError(name, "Must be at least " + min));
      }
    }
    if (validation.has("max")) {
      double max = validation.get("max").asDouble();
      if (number > max) {
        errors.add(new ValidationError(name, "Must be at most " + max));
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

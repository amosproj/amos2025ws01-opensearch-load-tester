package com.opensearchloadtester.metricsreporter.controller;

import com.opensearchloadtester.common.dto.FinishLoadTestDto;
import com.opensearchloadtester.common.dto.MetricsDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Bean Validation constraints on DTOs used by ReportController.
 *
 * These tests validate that the DTO validation annotations are correctly defined.
 * The actual enforcement via Spring MVC (@Valid on controller parameters) is tested
 * implicitly when the application runs, as the @ExceptionHandler in ReportController
 * handles validation errors.
 *
 * Note: MockMvc-based integration tests for @Valid on List<@Valid T> parameters
 * have limitations in the current Spring Boot test slice configuration.
 * Full HTTP-stack validation is covered in ReportControllerHttpValidationTest.
 */
class ReportControllerValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ==================== MetricsDto Validation Tests ====================

    @ParameterizedTest
    @NullAndEmptySource
    void metricsDto_isInvalid_forNullOrEmptyLoadGeneratorId(String loadGeneratorId) {
        MetricsDto dto = new MetricsDto(loadGeneratorId, "query_type_test", 10L, 10L, 3, 200);

        Set<ConstraintViolation<MetricsDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("loadGeneratorId"));
    }

    @Test
    void metricsDto_isInvalid_forBlankQueryType() {
        MetricsDto dto = new MetricsDto("lg-1", "  ", 10L, 10L, 3, 200);

        Set<ConstraintViolation<MetricsDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("queryType"));
    }

    @Test
    void metricsDto_isInvalid_forNegativeRequestDuration() {
        MetricsDto dto = new MetricsDto("lg-1", "query_type_test", -5L, 10L, 3, 200);

        Set<ConstraintViolation<MetricsDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("requestDurationMillis"));
    }

    @Test
    void metricsDto_isInvalid_forNegativeQueryDuration() {
        MetricsDto dto = new MetricsDto("lg-1", "query_type_test", 10L, -5L, 3, 200);

        Set<ConstraintViolation<MetricsDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("queryDurationMillis"));
    }

    @Test
    void metricsDto_isInvalid_forHttpStatusCodeBelow100() {
        MetricsDto dto = new MetricsDto("lg-1", "query_type_test", 10L, 10L, 3, 50);

        Set<ConstraintViolation<MetricsDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("httpStatusCode"));
    }

    @Test
    void metricsDto_isInvalid_forHttpStatusCodeAbove599() {
        MetricsDto dto = new MetricsDto("lg-1", "query_type_test", 10L, 10L, 3, 600);

        Set<ConstraintViolation<MetricsDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("httpStatusCode"));
    }

    @Test
    void metricsDto_isValid_forCorrectValues() {
        MetricsDto dto = new MetricsDto("lg-1", "query_type_test", 100L, 50L, 10, 200);

        Set<ConstraintViolation<MetricsDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void metricsDto_isValid_forNullQueryDuration() {
        // queryDurationMillis is nullable (Long), so null is valid
        MetricsDto dto = new MetricsDto("lg-1", "query_type_test", 100L, null, 10, 200);

        Set<ConstraintViolation<MetricsDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void metricsDto_isValid_forBoundaryHttpStatusCodes() {
        // Test boundary values: 100 and 599
        MetricsDto dto100 = new MetricsDto("lg-1", "query_type_test", 100L, 50L, 10, 100);
        MetricsDto dto599 = new MetricsDto("lg-1", "query_type_test", 100L, 50L, 10, 599);

        assertThat(validator.validate(dto100)).isEmpty();
        assertThat(validator.validate(dto599)).isEmpty();
    }

    // ==================== FinishLoadTestDto Validation Tests ====================

    @ParameterizedTest
    @NullAndEmptySource
    void finishLoadTestDto_isInvalid_forNullOrEmptyLoadGeneratorId(String loadGeneratorId) {
        FinishLoadTestDto dto = new FinishLoadTestDto(loadGeneratorId, true, null);

        Set<ConstraintViolation<FinishLoadTestDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("loadGeneratorId"));
    }

    @Test
    void finishLoadTestDto_isValid_forCorrectValues() {
        FinishLoadTestDto dto = new FinishLoadTestDto("lg-1", true, null);

        Set<ConstraintViolation<FinishLoadTestDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    void finishLoadTestDto_isValid_withErrorMessage() {
        FinishLoadTestDto dto = new FinishLoadTestDto("lg-1", false, "Connection timeout");

        Set<ConstraintViolation<FinishLoadTestDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }
}

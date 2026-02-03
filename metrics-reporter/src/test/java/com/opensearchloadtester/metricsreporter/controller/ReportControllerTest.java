package com.opensearchloadtester.metricsreporter.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

import com.opensearchloadtester.common.dto.FinishLoadTestDto;
import com.opensearchloadtester.common.dto.MetricsDto;
import com.opensearchloadtester.metricsreporter.config.ShutdownAfterResponseInterceptor;
import com.opensearchloadtester.metricsreporter.dto.StatisticsDto;
import com.opensearchloadtester.metricsreporter.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

  private static final String LOAD_GENERATOR_ID = "lg-1";

  @Mock private ReportService reportService;

  @InjectMocks private ReportController reportController;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(reportController, "expectedLoadGenerators", 1);
  }

  // ==================== submitMetrics Tests ====================

  @Test
  void submitMetrics_returnsOk_forValidMetrics() throws Exception {
    List<MetricsDto> metrics =
        List.of(new MetricsDto(LOAD_GENERATOR_ID, "query_type_test", 120L, 80L, 5, 200));

    ResponseEntity<String> response = reportController.submitMetrics(metrics);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(reportService).processMetrics(metrics);
  }

  @Test
  void submitMetrics_returnsBadRequest_forEmptyPayload() throws Exception {
    List<MetricsDto> metrics = List.of();

    ResponseEntity<String> response = reportController.submitMetrics(metrics);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isEqualTo("Invalid metrics payload\n");
    verify(reportService, never()).processMetrics(anyList());
  }

  /**
   * Tests the controller's null-check for the metrics list. Note: In production, a missing/empty
   * HTTP body would cause HttpMessageNotReadableException before reaching the controller. However,
   * a JSON body containing literal "null" would be deserialized as null and reach this code path.
   */
  @Test
  void submitMetrics_returnsBadRequest_forNullJsonBody() throws Exception {
    ResponseEntity<String> response = reportController.submitMetrics(null);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isEqualTo("Invalid metrics payload\n");
    verify(reportService, never()).processMetrics(anyList());
  }

  @Test
  void submitMetrics_returnsBadRequest_forMixedLoadGeneratorIds() throws Exception {
    List<MetricsDto> metrics =
        List.of(
            new MetricsDto("lg-1", "query_type_test", 100L, 50L, 4, 200),
            new MetricsDto("lg-2", "query_type_test", 180L, 90L, 2, 200));

    ResponseEntity<String> response = reportController.submitMetrics(metrics);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isEqualTo("Invalid metrics payload\n");
    verify(reportService, never()).processMetrics(anyList());
  }

  @Test
  void submitMetrics_returnsConflict_afterLoadTestFinished() throws Exception {
    // First, complete a successful run
    List<MetricsDto> metrics =
        List.of(new MetricsDto(LOAD_GENERATOR_ID, "query_type_test", 100L, 50L, 4, 200));

    StatisticsDto statistics =
        new StatisticsDto(
            LocalDateTime.now(),
            new StatisticsDto.DurationStats(100.0, 100L, 100L),
            new StatisticsDto.DurationStats(50.0, 50L, 50L),
            1L,
            0L,
            List.of(LOAD_GENERATOR_ID));

    when(reportService.finalizeReports(anySet())).thenReturn(statistics);
    when(reportService.getResultsJsonPath()).thenReturn(Path.of("out/query_results.json"));
    when(reportService.getStatisticsReportPath()).thenReturn(Path.of("out/statistics.json"));

    HttpServletRequest request = mock(HttpServletRequest.class);
    reportController.submitMetrics(metrics);
    reportController.finish(new FinishLoadTestDto(LOAD_GENERATOR_ID, true, null), request);

    // Now try to submit more metrics
    ResponseEntity<String> lateResponse = reportController.submitMetrics(metrics);

    assertThat(lateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(lateResponse.getBody()).contains("Run already finalized");
  }

  @Test
  void submitMetrics_returnsInternalServerError_whenPersistingFails() throws Exception {
    List<MetricsDto> metrics =
        List.of(new MetricsDto(LOAD_GENERATOR_ID, "query_type_test", 120L, 80L, 5, 200));

    doThrow(new IOException("Disk full")).when(reportService).processMetrics(metrics);

    ResponseEntity<String> response = reportController.submitMetrics(metrics);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).contains("Failed to persist metrics");
  }

  // ==================== finish Tests ====================

  @Test
  void finish_generatesReports_whenAllReplicasFinished() throws Exception {
    List<MetricsDto> metrics =
        List.of(
            new MetricsDto(LOAD_GENERATOR_ID, "query_type_test", 100L, 50L, 4, 200),
            new MetricsDto(LOAD_GENERATOR_ID, "query_type_test", 180L, 90L, 2, 500));

    StatisticsDto statistics =
        new StatisticsDto(
            LocalDateTime.now(),
            new StatisticsDto.DurationStats(140.0, 100L, 180L),
            new StatisticsDto.DurationStats(70.0, 50L, 90L),
            2L,
            1L,
            List.of(LOAD_GENERATOR_ID));

    when(reportService.finalizeReports(anySet())).thenReturn(statistics);
    when(reportService.getResultsJsonPath()).thenReturn(Path.of("out/query_results.json"));
    when(reportService.getStatisticsReportPath()).thenReturn(Path.of("out/statistics.json"));

    HttpServletRequest request = mock(HttpServletRequest.class);
    ResponseEntity<String> submitResponse = reportController.submitMetrics(metrics);
    ResponseEntity<String> finishResponse =
        reportController.finish(new FinishLoadTestDto(LOAD_GENERATOR_ID, true, null), request);

    assertThat(submitResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(finishResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    verify(reportService).processMetrics(metrics);
    verify(reportService).finalizeReports(anySet());
    verify(request)
        .setAttribute(eq(ShutdownAfterResponseInterceptor.SHUTDOWN_AFTER_RESPONSE), eq(true));
    verify(request)
        .setAttribute(
            eq(ShutdownAfterResponseInterceptor.EXIT_CODE),
            eq(ShutdownAfterResponseInterceptor.EXIT_OK));
  }

  @Test
  void finish_setsExitCodeForFailedLoadGenerator() throws Exception {
    List<MetricsDto> metrics =
        List.of(new MetricsDto(LOAD_GENERATOR_ID, "query_type_test", 100L, 50L, 4, 200));

    StatisticsDto statistics =
        new StatisticsDto(
            LocalDateTime.now(),
            new StatisticsDto.DurationStats(100.0, 100L, 100L),
            new StatisticsDto.DurationStats(50.0, 50L, 50L),
            1L,
            0L,
            List.of(LOAD_GENERATOR_ID));

    when(reportService.finalizeReports(anySet())).thenReturn(statistics);
    when(reportService.getResultsJsonPath()).thenReturn(Path.of("out/query_results.json"));
    when(reportService.getStatisticsReportPath()).thenReturn(Path.of("out/statistics.json"));

    HttpServletRequest request = mock(HttpServletRequest.class);
    reportController.submitMetrics(metrics);
    reportController.finish(
        new FinishLoadTestDto(LOAD_GENERATOR_ID, false, "Connection timeout"), request);

    verify(request)
        .setAttribute(
            eq(ShutdownAfterResponseInterceptor.EXIT_CODE),
            eq(ShutdownAfterResponseInterceptor.EXIT_LOAD_GENERATOR_FAILED));
  }

  @Test
  void finish_isIdempotent_forSameLoadGenerator() throws Exception {
    ReflectionTestUtils.setField(reportController, "expectedLoadGenerators", 2);

    List<MetricsDto> metrics =
        List.of(new MetricsDto(LOAD_GENERATOR_ID, "query_type_test", 100L, 50L, 4, 200));

    HttpServletRequest request = mock(HttpServletRequest.class);
    reportController.submitMetrics(metrics);

    // First finish call
    ResponseEntity<String> firstResponse =
        reportController.finish(new FinishLoadTestDto(LOAD_GENERATOR_ID, true, null), request);

    // Second finish call (idempotent)
    ResponseEntity<String> secondResponse =
        reportController.finish(new FinishLoadTestDto(LOAD_GENERATOR_ID, true, null), request);

    assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    // finalizeReports should not be called since not all load generators finished
    verify(reportService, never()).finalizeReports(anySet());
  }

  @Test
  void finish_returnsInternalServerError_whenReportGenerationFails() throws Exception {
    List<MetricsDto> metrics =
        List.of(new MetricsDto(LOAD_GENERATOR_ID, "query_type_test", 100L, 50L, 4, 200));

    when(reportService.finalizeReports(anySet())).thenThrow(new IOException("Disk full"));

    HttpServletRequest request = mock(HttpServletRequest.class);
    reportController.submitMetrics(metrics);
    ResponseEntity<String> response =
        reportController.finish(new FinishLoadTestDto(LOAD_GENERATOR_ID, true, null), request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).contains("Fatal error: failed to generate reports");
    verify(request)
        .setAttribute(
            eq(ShutdownAfterResponseInterceptor.EXIT_CODE),
            eq(ShutdownAfterResponseInterceptor.EXIT_INTERNAL_ERROR));
  }
}

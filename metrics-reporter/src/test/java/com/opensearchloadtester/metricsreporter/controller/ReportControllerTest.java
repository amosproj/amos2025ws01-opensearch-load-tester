package com.opensearchloadtester.metricsreporter.controller;

import com.opensearchloadtester.common.dto.MetricsDto;
import com.opensearchloadtester.metricsreporter.config.ShutdownAfterResponseInterceptor;
import com.opensearchloadtester.metricsreporter.dto.StatisticsDto;
import com.opensearchloadtester.metricsreporter.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    private static final String LOAD_GENERATOR_ID = "lg-1";

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reportController, "expectedReplicas", 1);
        ReflectionTestUtils.setField(reportController, "jsonExportEnabled", false);
    }

    @Test
    void submitMetrics_returnsBadRequest_forInvalidMetricsEntry() {
        List<MetricsDto> metrics = List.of(
                new MetricsDto("", "query_type_test", 10L, 10L, 3, 200)
        );

        ResponseEntity<String> response = reportController.submitMetrics(metrics);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(reportService);
    }

    @Test
    void submitMetrics_waitsUntilAllReplicasReport() throws Exception {
        ReflectionTestUtils.setField(reportController, "expectedReplicas", 2);

        List<MetricsDto> metrics = List.of(
                new MetricsDto(LOAD_GENERATOR_ID, "query_type_test", 120L, 80L, 5, 200)
        );

        ResponseEntity<String> response = reportController.submitMetrics(metrics);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void finish_generatesReports_whenAllReplicasFinished() throws Exception {
        ReflectionTestUtils.setField(reportController, "expectedReplicas", 1);
        ReflectionTestUtils.setField(reportController, "jsonExportEnabled", true);

        List<MetricsDto> metrics = List.of(
                new MetricsDto(LOAD_GENERATOR_ID, "query_type_test", 100L, 50L, 4, 200),
                new MetricsDto(LOAD_GENERATOR_ID, "query_type_test", 180L, 90L, 2, 500)
        );

        StatisticsDto statistics = new StatisticsDto(
                LocalDateTime.now(),
                new StatisticsDto.DurationStats(140.0, 100L, 180L),
                new StatisticsDto.DurationStats(70.0, 50L, 90L),
                2,
                1,
                List.of(LOAD_GENERATOR_ID)
        );

        when(reportService.finalizeReports(anySet())).thenReturn(statistics);
        when(reportService.getFullJsonReportPath()).thenReturn(Path.of("out/query_results.json"));
        when(reportService.getStatisticsReportPath()).thenReturn(Path.of("out/statistics.json"));

        HttpServletRequest request = mock(HttpServletRequest.class);
        ResponseEntity<String> submitResponse = reportController.submitMetrics(metrics);
        ResponseEntity<String> finishResponse = reportController.finish(LOAD_GENERATOR_ID, request);

        assertThat(submitResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(finishResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        verify(reportService).processMetrics(metrics);
        verify(reportService).finalizeReports(anySet());
        verify(request).setAttribute(
                eq(ShutdownAfterResponseInterceptor.SHUTDOWN_AFTER_RESPONSE),
                eq(true)
        );
    }
}

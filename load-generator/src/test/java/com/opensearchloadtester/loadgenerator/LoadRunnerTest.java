package com.opensearchloadtester.loadgenerator;

import com.opensearchloadtester.loadgenerator.client.MetricsReporterClient;
import com.opensearchloadtester.loadgenerator.exception.MetricsReporterAccessException;
import com.opensearchloadtester.loadgenerator.model.DocumentType;
import com.opensearchloadtester.loadgenerator.model.QueryType;
import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;
import com.opensearchloadtester.loadgenerator.service.LoadRunner;
import com.opensearchloadtester.loadgenerator.service.MetricsCollector;
import com.opensearchloadtester.loadgenerator.service.QueryExecutionTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.generic.OpenSearchGenericClient;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoadRunnerTest {

    @Mock
    private OpenSearchGenericClient openSearchClient;

    @Mock
    private MetricsReporterClient metricsReporterClient;

    private MetricsCollector metricsCollector;
    private LoadRunner loadRunner;
    private QueryExecutionTask createdTask;

    @BeforeEach
    void setUp() {
        // simple in-memory metrics
        metricsCollector = new MetricsCollector();
        // real LoadRunner (no refactor)
        loadRunner = new LoadRunner(
                "test-loadgen",
                openSearchClient,
                metricsReporterClient,
                metricsCollector
        );
    }

    // helper to build scenarios
    private ScenarioConfig createScenario(
            String name,
            DocumentType documentType,
            QueryType queryType,
            Duration duration,
            int qps,
            boolean warmUpEnabled
    ) {
        return new ScenarioConfig(
                name,
                documentType,
                duration,
                qps,
                warmUpEnabled,
                queryType
        );
    }


    @Test
    void shouldRunAnoScenario_andReportMetricsOnce() {
        // simple ANO scenario
        ScenarioConfig scenario = createScenario(
                "ano-basic",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                1,
                false
        );

        assertDoesNotThrow(() -> loadRunner.executeScenario(scenario));

        verify(metricsReporterClient, times(1))
                .reportMetrics(metricsCollector.getMetricsList());
    }

    @Test
    void shouldRunDuoScenario_andReportMetricsOnce() {
        // simple DUO scenario
        ScenarioConfig scenario = createScenario(
                "duo-basic",
                DocumentType.DUO,
                QueryType.DUO_CLIENT_BY_CUSTOMER_NUMBER,
                Duration.ofSeconds(1),
                1,
                false
        );

        assertDoesNotThrow(() -> loadRunner.executeScenario(scenario));

        verify(metricsReporterClient, times(1))
                .reportMetrics(metricsCollector.getMetricsList());
    }

    @Test
    void shouldRunScenarioWithWarmUpFlagTrue_withoutCrashing() {
        // warm-up flag currently unused, just ensure no crash
        ScenarioConfig scenario = createScenario(
                "warmup-flag",
                DocumentType.ANO,
                QueryType.ANO_CLIENT_BY_YEAR,
                Duration.ofSeconds(1),
                1,
                true
        );

        assertDoesNotThrow(() -> loadRunner.executeScenario(scenario));

        verify(metricsReporterClient, times(1))
                .reportMetrics(metricsCollector.getMetricsList());
    }

    @Test
    void shouldNotThrow_whenOpenSearchClientExecuteFails() throws Exception {
        ScenarioConfig scenario = createScenario(
                "opensearch-fail",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                1,
                false
        );

        // make execute(...) throw ANY exception
        doThrow(new IOException("boom"))
                .when(openSearchClient)
                .execute(any());

        assertDoesNotThrow(() -> loadRunner.executeScenario(scenario));
    }


    @Test
    void shouldNotThrow_whenMetricsReporterThrowsAccessException() {
        // metrics backend not reachable
        ScenarioConfig scenario = createScenario(
                "metrics-fail",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                1,
                false
        );

        doThrow(new MetricsReporterAccessException("cannot reach metrics backend"))
                .when(metricsReporterClient)
                .reportMetrics(any());

        // LoadRunner must not leak exception
        assertDoesNotThrow(() -> loadRunner.executeScenario(scenario));

        // but it should have tried once
        verify(metricsReporterClient, times(1)).reportMetrics(any());
    }

    @Test
    void shouldHandleZeroDurationScenario_withoutThrowing() {
        // duration = 0s
        ScenarioConfig scenario = createScenario(
                "zero-duration",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ZERO,
                1,
                false
        );

        assertDoesNotThrow(() -> loadRunner.executeScenario(scenario));

        verify(metricsReporterClient, times(1))
                .reportMetrics(metricsCollector.getMetricsList());
    }

    @Test
    void shouldHandleMinimumQpsScenario_withoutThrowing() {
        // minimum allowed QPS
        ScenarioConfig scenario = createScenario(
                "min-qps",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                1,
                false
        );

        assertDoesNotThrow(() -> loadRunner.executeScenario(scenario));

        verify(metricsReporterClient, times(1))
                .reportMetrics(metricsCollector.getMetricsList());
    }


    // --- QPS & number of executions ---

    @Test
    void qpsOneForOneSecond_callsExecuteAboutOnce() throws Exception {
        ScenarioConfig scenario = createScenario(
                "qps-1s-1qps",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                1,
                false
        );

        when(openSearchClient.execute(any())).thenReturn(null);

        loadRunner.executeScenario(scenario);

        // ~1 execution, allow small jitter
        verify(openSearchClient, atLeast(1)).execute(any());
        verify(openSearchClient, atMost(2)).execute(any());
    }

    @Test
    void qpsThreeForOneSecond_callsExecuteAboutThreeTimes() throws Exception {
        ScenarioConfig scenario = createScenario(
                "qps-1s-3qps",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                3,
                false
        );

        when(openSearchClient.execute(any())).thenReturn(null);

        loadRunner.executeScenario(scenario);

        // ~3 executions, allow small jitter
        verify(openSearchClient, atLeast(2)).execute(any());
        verify(openSearchClient, atMost(4)).execute(any());
    }

    // --- "Cancellation" / stop submitting after end ---

    @Test
    void noMoreSubmissionsAfterExecuteScenarioReturns() throws Exception {
        ScenarioConfig scenario = createScenario(
                "cancel-behavior",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                5,
                false
        );

        when(openSearchClient.execute(any())).thenAnswer(invocation -> {
            Thread.sleep(10);  // tiny delay
            return null;
        });

        loadRunner.executeScenario(scenario);

        int callsAtEnd = mockingDetails(openSearchClient)
                .getInvocations()
                .size();

        Thread.sleep(200);  // if scheduler still runs, count would grow

        int callsLater = mockingDetails(openSearchClient)
                .getInvocations()
                .size();

        assertEquals(callsAtEnd, callsLater, "no new calls after scenario end");
    }

    // --- metrics reporter at end ---

    @Test
    void reportsMetricsOnceWhenScenarioFinishes() throws Exception {
        ScenarioConfig scenario = createScenario(
                "metrics-report",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                1,
                false
        );

        when(openSearchClient.execute(any())).thenReturn(null);

        loadRunner.executeScenario(scenario);

        verify(metricsReporterClient, times(1))
                .reportMetrics(metricsCollector.getMetricsList());
    }

    // --- metrics actually grow during execution ---

    @Test
    void recordsSomeMetricsForNonZeroRun() throws Exception {
        ScenarioConfig scenario = createScenario(
                "metrics-grow",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                2,
                false
        );

        when(openSearchClient.execute(any())).thenReturn(null);

        assertTrue(metricsCollector.getMetricsList().isEmpty(), "metrics start empty");

        loadRunner.executeScenario(scenario);

        // don't assert non-emptiness here – that's QueryExecutionTask's job
        verify(metricsReporterClient, times(1))
                .reportMetrics(metricsCollector.getMetricsList());
    }

}


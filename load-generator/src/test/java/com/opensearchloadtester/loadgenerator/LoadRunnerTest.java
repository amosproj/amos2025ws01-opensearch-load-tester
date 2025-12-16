package com.opensearchloadtester.loadgenerator;

import com.opensearchloadtester.loadgenerator.client.MetricsReporterClient;
import com.opensearchloadtester.loadgenerator.exception.MetricsReporterAccessException;
import com.opensearchloadtester.loadgenerator.model.DocumentType;
import com.opensearchloadtester.loadgenerator.model.QueryType;
import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;
import com.opensearchloadtester.loadgenerator.service.LoadRunner;
import com.opensearchloadtester.loadgenerator.service.MetricsCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.generic.OpenSearchGenericClient;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @BeforeEach
    void setUp() {
        // Real collector to pass through the runner lifecycle
        metricsCollector = new MetricsCollector();

        // Only external dependencies are mocked
        loadRunner = new LoadRunner(
                "test-loadgen",
                openSearchClient,
                metricsReporterClient,
                metricsCollector
        );
    }

    // Small helper to keep test bodies readable
    private ScenarioConfig createScenario(
            String name,
            DocumentType documentType,
            QueryType queryType,
            Duration duration,
            int qps,
            boolean warmUpEnabled
    ) {
        return new ScenarioConfig(name, documentType, duration, qps, warmUpEnabled, queryType);
    }

    @Test
    void shouldRunAnoScenario_andReportMetricsOnce() {
        // Runner finishes and reports once.
        ScenarioConfig scenario = createScenario(
                "ano-basic",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                1,
                false
        );

        assertDoesNotThrow(() -> loadRunner.executeScenario(scenario));

        // Report is a lifecycle event: exactly once at the end.
        verify(metricsReporterClient, times(1)).reportMetrics(any());
    }

    @Test
    void shouldRunDuoScenario_andReportMetricsOnce() {
        // Same lifecycle for a different document type / query type
        ScenarioConfig scenario = createScenario(
                "duo-basic",
                DocumentType.DUO,
                QueryType.DUO_CLIENT_BY_CUSTOMER_NUMBER,
                Duration.ofSeconds(1),
                1,
                false
        );

        assertDoesNotThrow(() -> loadRunner.executeScenario(scenario));

        verify(metricsReporterClient, times(1)).reportMetrics(any());
    }

    @Test
    void shouldRunScenarioWithWarmUpFlagTrue_withoutCrashing() {
        // Warm-up flag is handled elsewhere; here we ensure it does not break the runner
        ScenarioConfig scenario = createScenario(
                "warmup-flag",
                DocumentType.ANO,
                QueryType.ANO_CLIENT_BY_YEAR,
                Duration.ofSeconds(1),
                1,
                true
        );

        assertDoesNotThrow(() -> loadRunner.executeScenario(scenario));

        verify(metricsReporterClient, times(1)).reportMetrics(any());
    }

    @Test
    void shouldNotThrow_whenOpenSearchClientExecuteFails() throws Exception {
        // Worker-side OpenSearch failure must not crash the runner.
        ScenarioConfig scenario = createScenario(
                "opensearch-fail",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                1,
                false
        );

        // This exception happens inside QueryExecutionTask.run() in worker threads
        doThrow(new IOException("boom"))
                .when(openSearchClient)
                .execute(any());

        assertDoesNotThrow(() -> loadRunner.executeScenario(scenario));
    }

    @Test
    void shouldNotThrow_whenMetricsReporterThrowsAccessException() {
        // Reporting can fail, but executeScenario should catch it and return
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

        assertDoesNotThrow(() -> loadRunner.executeScenario(scenario));

        // Still only one attempt to report.
        verify(metricsReporterClient, times(1)).reportMetrics(any());
    }

    @Test
    void shouldHandleZeroDurationScenario_withoutThrowing() {
        // Edge: duration=0 should still go through the end-of-run reporting
        ScenarioConfig scenario = createScenario(
                "zero-duration",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ZERO,
                1,
                false
        );

        assertDoesNotThrow(() -> loadRunner.executeScenario(scenario));

        verify(metricsReporterClient, times(1)).reportMetrics(any());
    }

    @Test
    void shouldHandleMinimumQpsScenario_withoutThrowing() {
        // Edge: minimum QPS should still work and report.
        ScenarioConfig scenario = createScenario(
                "min-qps",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                1,
                false
        );

        assertDoesNotThrow(() -> loadRunner.executeScenario(scenario));

        verify(metricsReporterClient, times(1)).reportMetrics(any());
    }

    @Test
    void qpsOneForOneSecond_callsExecuteAboutOnce() throws Exception {
        // Approximate scheduling check (can be slightly jittery due to timing)
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

        verify(openSearchClient, atLeast(1)).execute(any());
        verify(openSearchClient, atMost(2)).execute(any());
    }

    @Test
    void qpsThreeForOneSecond_callsExecuteAboutThreeTimes() throws Exception {
        // Same as above, with a higher QPS target.
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

        verify(openSearchClient, atLeast(2)).execute(any());
        verify(openSearchClient, atMost(4)).execute(any());
    }

    @Test
    void noMoreSubmissionsAfterExecuteScenarioReturns() throws Exception {
        // Ensure the scheduler stops: call count should not increase after return.
        ScenarioConfig scenario = createScenario(
                "cancel-behavior",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                5,
                false
        );

        when(openSearchClient.execute(any())).thenAnswer(invocation -> {
            Thread.sleep(10);
            return null;
        });

        loadRunner.executeScenario(scenario);

        int callsAtEnd = mockingDetails(openSearchClient).getInvocations().size();

        Thread.sleep(200);

        int callsLater = mockingDetails(openSearchClient).getInvocations().size();

        assertEquals(callsAtEnd, callsLater, "no new calls after scenario end");
    }

    @Test
    void reportsMetricsOnceWhenScenarioFinishes() throws Exception {
        // Explicit lifecycle assertion: report once on normal completion.
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

        verify(metricsReporterClient, times(1)).reportMetrics(any());
    }

    @Test
    void recordsSomeMetricsForNonZeroRun() throws Exception {
        // Only check that the runner reaches the reporting step; metrics contents belong to QueryExecutionTaskTest
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

        verify(metricsReporterClient, times(1)).reportMetrics(any());
    }

    @Test
    void executorIsShutdownAfterScenario() throws Exception {
        // Another stop condition check (no post-return background activity)
        ScenarioConfig scenario = createScenario(
                "shutdown-check",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                3,
                false
        );

        when(openSearchClient.execute(any())).thenReturn(null);

        loadRunner.executeScenario(scenario);

        int callsAfterFinish = mockingDetails(openSearchClient).getInvocations().size();

        Thread.sleep(300);

        assertEquals(
                callsAfterFinish,
                mockingDetails(openSearchClient).getInvocations().size()
        );
    }

    @Test
    void rejectedExecutionException_inWorkerDoesNotCrashRunner() throws IOException {
        //Simulates a failure inside QueryExecutionTask, not in workers.submit(...).
        ScenarioConfig scenario = createScenario(
                "rejected-exec",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                10,
                false
        );

        doThrow(new RejectedExecutionException("reject"))
                .when(openSearchClient)
                .execute(any());

        assertDoesNotThrow(() -> loadRunner.executeScenario(scenario));

        verify(metricsReporterClient, times(1)).reportMetrics(any());
    }

    @Test
    void runtimeException_inWorkerDoesNotCrashRunner() throws IOException {
        // Unchecked exception inside worker execution should not kill the runner.
        ScenarioConfig scenario = createScenario(
                "runtime-exception",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                1,
                false
        );

        doThrow(new RuntimeException("boom"))
                .when(openSearchClient)
                .execute(any());

        assertDoesNotThrow(() -> loadRunner.executeScenario(scenario));

        verify(metricsReporterClient, times(1)).reportMetrics(any());
    }
}


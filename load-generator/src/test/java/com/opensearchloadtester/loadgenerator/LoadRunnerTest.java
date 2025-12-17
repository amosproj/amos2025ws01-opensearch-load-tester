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
import org.opensearch.client.opensearch.generic.Response;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoadRunnerTest {

    private static final int NUMBER_LOAD_GENERATORS = 1; // deterministic qpsPerLoadGen

    @Mock
    private OpenSearchGenericClient openSearchClient;

    @Mock
    private MetricsReporterClient metricsReporterClient;

    private MetricsCollector metricsCollector;
    private LoadRunner loadRunner;

    @BeforeEach
    void setUp() {
        metricsCollector = new MetricsCollector();

        loadRunner = new LoadRunner(
                "test-loadgen",
                NUMBER_LOAD_GENERATORS,
                openSearchClient,
                metricsReporterClient,
                metricsCollector
        );
    }

    private ScenarioConfig createScenario(
            String name,
            DocumentType documentType,
            QueryType queryType,
            Duration duration,
            int qps,
            boolean warmUpEnabled
    ) {
        return new ScenarioConfig(name, documentType, duration, qps, warmUpEnabled, List.of(queryType));
    }

    private void stubOpenSearchStatus(int status) throws Exception {
        // Keep QueryExecutionTask simple: only status is used
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(status);
        when(openSearchClient.execute(any())).thenReturn(response);
    }

    @Test
    void anoScenario_runs_andReportsOnce() throws Exception {
        // Normal lifecycle
        ScenarioConfig scenario = createScenario(
                "ano-basic",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                1,
                false
        );

        stubOpenSearchStatus(500);

        assertDoesNotThrow(() -> loadRunner.executeScenario(scenario));
        verify(metricsReporterClient, times(1)).sendMetrics(any());
    }

    @Test
    void duoScenario_runs_andReportsOnce() throws Exception {
        // Same lifecycle with different index/query.
        ScenarioConfig scenario = createScenario(
                "duo-basic",
                DocumentType.DUO,
                QueryType.DUO_CLIENT_BY_CUSTOMER_NUMBER,
                Duration.ofSeconds(1),
                1,
                false
        );

        stubOpenSearchStatus(500);

        assertDoesNotThrow(() -> loadRunner.executeScenario(scenario));
        verify(metricsReporterClient, times(1)).sendMetrics(any());
    }

    @Test
    void warmUpFlag_doesNotAffectRunner() throws Exception {
        // Warm-up is handled elsewhere; runner must still finish
        ScenarioConfig scenario = createScenario(
                "warmup-flag",
                DocumentType.ANO,
                QueryType.ANO_CLIENT_BY_YEAR,
                Duration.ofSeconds(1),
                1,
                true
        );

        stubOpenSearchStatus(500);

        assertDoesNotThrow(() -> loadRunner.executeScenario(scenario));
        verify(metricsReporterClient, times(1)).sendMetrics(any());
    }

    @Test
    void openSearchIOException_doesNotCrashRunner() throws Exception {
        // Exceptions inside QueryExecutionTask must not crash executeScenario
        ScenarioConfig scenario = createScenario(
                "opensearch-fail",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                1,
                false
        );

        doThrow(new IOException("boom")).when(openSearchClient).execute(any());

        assertDoesNotThrow(() -> loadRunner.executeScenario(scenario));

    }

    @Test
    void metricsReporterFailure_isHandled_andAttemptedOnce() throws Exception {
        // Reporting failure must not crash runner
        ScenarioConfig scenario = createScenario(
                "metrics-fail",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                1,
                false
        );

        stubOpenSearchStatus(500);
        doThrow(new MetricsReporterAccessException("down")).when(metricsReporterClient).sendMetrics(any());

        assertDoesNotThrow(() -> loadRunner.executeScenario(scenario));
        verify(metricsReporterClient, times(1)).sendMetrics(any());
    }

    @Test
    void qpsOneSecond_oneQps_executesAtLeastOnce() throws Exception {
        // Scheduling is jittery; only assert a minimal bound
        ScenarioConfig scenario = createScenario(
                "qps-1s-1qps",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                1,
                false
        );

        stubOpenSearchStatus(500);

        loadRunner.executeScenario(scenario);

        verify(openSearchClient, atLeast(1)).execute(any());
        verify(openSearchClient, atMost(3)).execute(any());
    }

    @Test
    void qpsOneSecond_threeQps_executesRoughlyInRange() throws Exception {

        ScenarioConfig scenario = createScenario(
                "qps-1s-3qps",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                3,
                false
        );

        stubOpenSearchStatus(500);

        loadRunner.executeScenario(scenario);

        verify(openSearchClient, atLeast(1)).execute(any());
        verify(openSearchClient, atMost(8)).execute(any()); // wide range on purpose
    }

    @Test
    void schedulerStops_afterExecuteScenarioReturns() throws Exception {
        // No new calls should happen after method returns
        ScenarioConfig scenario = createScenario(
                "cancel-behavior",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                5,
                false
        );

        when(openSearchClient.execute(any())).thenAnswer(inv -> {
            Thread.sleep(10);
            Response resp = mock(Response.class);
            when(resp.getStatus()).thenReturn(500);
            return resp;
        });

        loadRunner.executeScenario(scenario);

        int callsAtEnd = mockingDetails(openSearchClient).getInvocations().size();
        Thread.sleep(250);
        int callsLater = mockingDetails(openSearchClient).getInvocations().size();

        assertEquals(callsAtEnd, callsLater, "no new calls after return");
    }

    @Test
    void rejectedExecutionException_fromOpenSearch_doesNotCrashRunner() throws Exception {
        // This simulates an exception inside QueryExecutionTask, not submit()
        ScenarioConfig scenario = createScenario(
                "rejected-exec",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                10,
                false
        );

        doThrow(new RejectedExecutionException("reject")).when(openSearchClient).execute(any());

        assertDoesNotThrow(() -> loadRunner.executeScenario(scenario));
        verify(metricsReporterClient, times(1)).sendMetrics(any());
    }

    @Test
    void runtimeException_fromOpenSearch_doesNotCrashRunner() throws Exception {
        // Unchecked exceptions should be contained
        ScenarioConfig scenario = createScenario(
                "runtime-exception",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                1,
                false
        );

        doThrow(new RuntimeException("boom")).when(openSearchClient).execute(any());

        assertDoesNotThrow(() -> loadRunner.executeScenario(scenario));
        verify(metricsReporterClient, times(1)).sendMetrics(any());
    }

    @Test
    void qpsLowerThanReplicas_divByZero_isCaught_andNoMetricsSent() {

        LoadRunner runner = new LoadRunner(
                "test-loadgen",
                2,
                openSearchClient,
                metricsReporterClient,
                metricsCollector
        );

        ScenarioConfig scenario = createScenario(
                "qps-too-low",
                DocumentType.ANO,
                QueryType.ANO_PAYROLL_RANGE,
                Duration.ofSeconds(1),
                1,
                false
        );

        assertDoesNotThrow(() -> runner.executeScenario(scenario));
        verify(metricsReporterClient, never()).sendMetrics(any());
    }

    @Test
    void multipleQueryTypes_runs_andReportsOnce() throws Exception {
        // Ensure QueryExecutionTask can be created with multiple types
        ScenarioConfig scenario = new ScenarioConfig(
                "multi-qtypes",
                DocumentType.ANO,
                Duration.ofSeconds(1),
                2,
                false,
                List.of(QueryType.ANO_PAYROLL_RANGE, QueryType.ANO_CLIENT_BY_YEAR)
        );

        stubOpenSearchStatus(500);

        assertDoesNotThrow(() -> loadRunner.executeScenario(scenario));
        verify(metricsReporterClient, times(1)).sendMetrics(any());
    }

    @Test
    void shutdownExecutorService_forcesShutdownNow_whenNotTerminating() throws Exception {

        ExecutorService stubborn = mock(ExecutorService.class);
        when(stubborn.awaitTermination(anyLong(), any())).thenReturn(false);

        var m = LoadRunner.class.getDeclaredMethod("shutdownExecutorService", ExecutorService.class);
        m.setAccessible(true);
        m.invoke(loadRunner, stubborn);

        verify(stubborn).shutdown();
        verify(stubborn).shutdownNow();
        verify(stubborn, times(2)).awaitTermination(anyLong(), any());
    }

    @Test
    void shutdownExecutorService_restoresInterruptFlag_onInterruptedException() throws Exception {

        ExecutorService exec = mock(ExecutorService.class);
        when(exec.awaitTermination(anyLong(), any())).thenThrow(new InterruptedException("interrupted"));

        var m = LoadRunner.class.getDeclaredMethod("shutdownExecutorService", ExecutorService.class);
        m.setAccessible(true);

        Thread.interrupted();
        m.invoke(loadRunner, exec);

        verify(exec).shutdown();
        verify(exec).shutdownNow();
        assertTrue(Thread.currentThread().isInterrupted(), "interrupt flag should be set");
    }
}



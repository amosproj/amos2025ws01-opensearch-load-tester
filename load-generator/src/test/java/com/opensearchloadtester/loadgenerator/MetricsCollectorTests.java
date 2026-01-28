package com.opensearchloadtester.loadgenerator;

import com.opensearchloadtester.common.dto.MetricsDto;
import com.opensearchloadtester.loadgenerator.client.MetricsReporterClient;
import com.opensearchloadtester.loadgenerator.service.MetricsCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MetricsCollectorTests {

    private MetricsCollector metricsCollector;
    private MetricsReporterClient mockClient;

    @BeforeEach
    void setUp() {
        mockClient = mock(MetricsReporterClient.class);
        // Use batch size of 1 so metrics are sent immediately for easier testing
        metricsCollector = new MetricsCollector(mockClient, 1, true);
    }

    /**
     * Verifies that calling {@link MetricsCollector#appendMetrics(MetricsDto)}
     * successfully sends the metrics to the reporter client
     * <br><br>
     * The test checks that:
     * <ul>
     *     <li>The client receives exactly one batch</li>
     *     <li>The batch contains the exact same instance that was provided</li>
     * </ul>
     */
    @Test
    void testAppendMetrics_singleAdd() {
        MetricsDto dto = new MetricsDto();

        metricsCollector.appendMetrics(dto);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MetricsDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(mockClient, times(1)).sendMetrics(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertSame(dto, captor.getValue().get(0));
    }

    /**
     * Ensures that multiple sequential calls to
     * {@link MetricsCollector#appendMetrics(MetricsDto)} correctly send
     * all provided metrics objects to the reporter client
     * <br><br>
     * The test checks that:
     * <ul>
     *     <li>The number of sent batches matches the number of inserted objects (batch size = 1)</li>
     *     <li>Each inserted {@link MetricsDto} is sent to the client</li>
     * </ul>
     */
    @Test
    void testAppendMetrics_multipleAdds() {
        Random random = new Random();
        int metricsAmount = random.nextInt(10, 100);
        ArrayList<MetricsDto> metricsList = new ArrayList<>();

        for (int i = 0; i < metricsAmount; i++) {
            metricsList.add(new MetricsDto());
        }

        for (int i = 0; i < metricsAmount; i++) {
            metricsCollector.appendMetrics(metricsList.get(i));
        }

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MetricsDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(mockClient, times(metricsAmount)).sendMetrics(captor.capture());

        List<List<MetricsDto>> allBatches = captor.getAllValues();
        for (int i = 0; i < metricsAmount; i++) {
            assertTrue(allBatches.get(i).contains(metricsList.get(i)));
        }
    }

    /**
     * Verifies that {@link MetricsCollector#appendMetrics(MetricsDto)} is thread-safe
     * due to its synchronized buffer access
     * <br><br>
     * The test launches multiple concurrent threads, each adding a metrics object.
     * After all threads finish, the client must have received exactly as many
     * metrics as the number of executed threads
     * <br><br>
     * This ensures that no data races or lost updates occur under concurrent access
     */
    @Test
    void testAppendMetrics_threadSafety() throws InterruptedException {
        Random random = new Random();
        int threadCount = random.nextInt(10, 100);
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                metricsCollector.appendMetrics(new MetricsDto());
                latch.countDown();
            });
        }
        latch.await();
        executor.shutdown();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MetricsDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(mockClient, times(threadCount)).sendMetrics(captor.capture());

        int totalMetricsSent = captor.getAllValues().stream()
                .mapToInt(List::size)
                .sum();
        assertEquals(threadCount, totalMetricsSent);
    }

    /**
     * Verifies that metrics are batched correctly when batch size is greater than 1
     * <br><br>
     * The test checks that:
     * <ul>
     *     <li>No metrics are sent until the batch size is reached</li>
     *     <li>When batch size is reached, all metrics are sent together</li>
     * </ul>
     */
    @Test
    void testAppendMetrics_batching() {
        // Create collector with batch size of 5
        MetricsReporterClient batchClient = mock(MetricsReporterClient.class);
        MetricsCollector batchCollector = new MetricsCollector(batchClient, 5, true);

        // Add 4 metrics - should not trigger send yet
        for (int i = 0; i < 4; i++) {
            batchCollector.appendMetrics(new MetricsDto());
        }
        verify(batchClient, never()).sendMetrics(any());

        // Add 5th metric - should trigger batch send
        batchCollector.appendMetrics(new MetricsDto());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MetricsDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(batchClient, times(1)).sendMetrics(captor.capture());
        assertEquals(5, captor.getValue().size());
    }

    /**
     * Verifies that flush() sends remaining buffered metrics
     */
    @Test
    void testFlush() {
        // Create collector with batch size of 10 (won't be reached)
        MetricsReporterClient flushClient = mock(MetricsReporterClient.class);
        MetricsCollector flushCollector = new MetricsCollector(flushClient, 10, true);

        // Add 3 metrics - won't trigger automatic send
        for (int i = 0; i < 3; i++) {
            flushCollector.appendMetrics(new MetricsDto());
        }
        verify(flushClient, never()).sendMetrics(any());

        // Flush should send remaining metrics
        flushCollector.flush();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MetricsDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(flushClient, times(1)).sendMetrics(captor.capture());
        assertEquals(3, captor.getValue().size());
    }

    /**
     * Verifies that disabled collector does not send any metrics
     */
    @Test
    void testDisabledCollector() {
        MetricsReporterClient disabledClient = mock(MetricsReporterClient.class);
        MetricsCollector disabledCollector = new MetricsCollector(disabledClient, 1, false);

        disabledCollector.appendMetrics(new MetricsDto());
        disabledCollector.flush();

        verify(disabledClient, never()).sendMetrics(any());
    }
}

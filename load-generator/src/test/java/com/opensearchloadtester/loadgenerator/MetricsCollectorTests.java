package com.opensearchloadtester.loadgenerator;

import com.opensearchloadtester.common.dto.MetricsDto;
import com.opensearchloadtester.loadgenerator.service.MetricsCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class MetricsCollectorTests {

    private MetricsCollector metricsCollector;

    @BeforeEach
    void setUp() {
        metricsCollector = new MetricsCollector();
    }

    /**
     * Verifies that calling {@link MetricsCollector#appendMetrics(MetricsDto)}
     * successfully adds a metrics object to the internal list
     * <br><br>
     * The test checks that:
     * <ul>
     *     <li>The list size increases to one</li>
     *     <li>The stored object is the exact same instance that was provided</li>
     * </ul>
     */
    @Test
    void testAppendMetrics_singleAdd() {
        MetricsDto dto = new MetricsDto();

        metricsCollector.appendMetrics(dto);

        assertEquals(1, metricsCollector.getMetricsList().size());
        assertSame(dto, metricsCollector.getMetricsList().get(0));
    }

    /**
     * Ensures that multiple sequential calls to
     * {@link MetricsCollector#appendMetrics(MetricsDto)} correctly append
     * all provided metrics objects to the internal list
     * <br><br>
     * The test checks that:
     * <ul>
     *     <li>The list size matches the number of inserted objects</li>
     *     <li>Each inserted {@link MetricsDto} is present in the list</li>
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

        assertEquals(metricsAmount, metricsCollector.getMetricsList().size());

        for (int i = 0; i < metricsAmount; i++) {
            assertTrue(metricsCollector.getMetricsList().contains(metricsList.get(i)));
        }
    }

    /**
     * Verifies that {@link MetricsCollector#appendMetrics(MetricsDto)} is thread-safe
     * due to its synchronized method declaration
     * <br><br>
     * The test launches multiple concurrent threads, each adding a metrics object.
     * After all threads finish, the internal list must contain exactly as many
     * elements as the number of executed threads
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

        assertEquals(threadCount, metricsCollector.getMetricsList().size());
    }

    /**
     * Confirms that {@link MetricsCollector#getMetricsList()} always returns
     * the same underlying list instance instead of creating new copies
     * <br><br>
     * This ensures that external callers can reliably inspect the internal state,
     * and that list modifications across calls remain consistent
     */
    @Test
    void testGetMetricsList() {
        assertNotNull(metricsCollector.getMetricsList());
        assertSame(metricsCollector.getMetricsList(), metricsCollector.getMetricsList());
    }
}

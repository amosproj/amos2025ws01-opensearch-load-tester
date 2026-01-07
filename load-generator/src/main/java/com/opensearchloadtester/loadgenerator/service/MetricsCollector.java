package com.opensearchloadtester.loadgenerator.service;

import com.opensearchloadtester.loadgenerator.client.MetricsReporterClient;
import com.opensearchloadtester.common.dto.MetricsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class MetricsCollector {

    private final MetricsReporterClient metricsReporterClient;
    private final int batchSize;
    private final boolean enabled;

    private final List<MetricsDto> buffer = new ArrayList<>();

    @Autowired
    public MetricsCollector(
            MetricsReporterClient metricsReporterClient,
            @Value("${metrics.batch.size:100}") int batchSize
    ) {
        this.metricsReporterClient = metricsReporterClient;
        this.batchSize = batchSize;
        this.enabled = true;
    }

    // Extra constructor for warm-up, enabled configurable
    public MetricsCollector(MetricsReporterClient metricsReporterClient, int batchSize, boolean enabled) {
        this.metricsReporterClient = metricsReporterClient;
        this.batchSize = batchSize;
        this.enabled = enabled;
    }

    public void appendMetrics(MetricsDto metricsDto) {
        if (!enabled) return;

        List<MetricsDto> toSend = null;

        synchronized (buffer) {
            buffer.add(metricsDto);
            if (buffer.size() >= batchSize) {
                toSend = new ArrayList<>(buffer);
                buffer.clear();
            }
        }

        if (toSend != null) {
            sendBatchSafely(toSend);
        }
    }

    public void flush() {
        if (!enabled) return;

        List<MetricsDto> toSend;

        synchronized (buffer) {
            if (buffer.isEmpty()) return;
            toSend = new ArrayList<>(buffer);
            buffer.clear();
        }

        sendBatchSafely(toSend);
    }

    private void sendBatchSafely(List<MetricsDto> batch) {
        try {
            metricsReporterClient.sendMetrics(batch);
            log.debug("Sent metrics batch size={}", batch.size());
        } catch (Exception e) {
            log.error("Failed to send metrics batch (size={}): {}", batch.size(), e.getMessage(), e);
        }
    }
}

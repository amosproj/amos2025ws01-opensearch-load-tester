package com.opensearchloadtester.loadgenerator.service;

import com.opensearchloadtester.common.dto.Metrics;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MetricsCollector {

    @Getter
    private final Metrics metrics = new Metrics(System.getenv("HOSTNAME"));

    public void appendMetrics(String requestType, long roundtripMilSec, String jsonResponse) {
        metrics.addMetrics(requestType, roundtripMilSec, jsonResponse);
    }
}

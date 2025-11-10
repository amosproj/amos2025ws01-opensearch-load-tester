package com.opensearchloadtester.loadgenerator.controller;

import com.opensearchloadtester.loadgenerator.service.MetricsCollectorService;

import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class MetricsCollectorController {

    private final MetricsCollectorService service;

    public MetricsCollectorController(MetricsCollectorService service) {
        this.service = service;
    }

    public void recordMetrics(String requestType, int roundtripMilSec, String jsonResponse) {
        service.appendMetrics(requestType, roundtripMilSec,  jsonResponse);
    }

    public File getMetricsFile() {
        return service.getMetricFile();
    }
}

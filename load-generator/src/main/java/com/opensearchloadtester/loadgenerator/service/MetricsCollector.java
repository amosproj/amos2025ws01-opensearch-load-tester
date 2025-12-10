package com.opensearchloadtester.loadgenerator.service;

import com.opensearchloadtester.common.dto.MetricsDto;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class MetricsCollector {

    @Getter
    private final List<MetricsDto> metricsList = new ArrayList<>();

    public synchronized void appendMetrics(MetricsDto metricsDto) {
        metricsList.add(metricsDto);
    }
}

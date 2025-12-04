package com.opensearchloadtester.loadgenerator.service;

import com.opensearchloadtester.common.dto.MetricsDto;
import com.opensearchloadtester.common.dto.LoadGeneratorReportDto;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MetricsCollector {

    @Getter
    private final LoadGeneratorReportDto report = new LoadGeneratorReportDto(System.getenv("HOSTNAME"));

    public void appendMetrics(MetricsDto metricsDto) {
        report.addMetrics(metricsDto);
    }
}

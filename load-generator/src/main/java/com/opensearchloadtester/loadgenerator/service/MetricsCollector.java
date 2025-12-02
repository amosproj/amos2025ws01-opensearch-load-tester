package com.opensearchloadtester.loadgenerator.service;

import com.opensearchloadtester.common.dto.MetricsDto;
import com.opensearchloadtester.common.dto.ReportDto;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MetricsCollector {

    @Getter
    private final ReportDto report = new ReportDto(System.getenv("HOSTNAME"));

    public void appendMetrics(MetricsDto metricsDto) {
        report.addMetrics(metricsDto);
    }
}

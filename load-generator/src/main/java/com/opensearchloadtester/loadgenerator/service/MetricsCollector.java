package com.opensearchloadtester.loadgenerator.service;

import com.opensearchloadtester.common.dto.MetricsDto;
import com.opensearchloadtester.common.dto.LoadGeneratorReportDto;
import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MetricsCollector {

    @Getter
    private final LoadGeneratorReportDto report;

    public MetricsCollector(@Value("${HOSTNAME}") String loadGeneratorId,
                            ScenarioConfig scenarioConfig) {
        this.report = new LoadGeneratorReportDto(
                loadGeneratorId,
                scenarioConfig.getName(),
                scenarioConfig.getQuery().getType().name()
        );
    }

    public void appendMetrics(MetricsDto metricsDto) {
        report.addMetrics(metricsDto);
    }
}

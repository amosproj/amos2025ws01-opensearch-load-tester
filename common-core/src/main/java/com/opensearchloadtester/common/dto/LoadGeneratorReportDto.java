package com.opensearchloadtester.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class LoadGeneratorReportDto {

    private String loadGeneratorId;
    private String scenario;
    private String queryType;
    private final List<MetricsDto> metricsList = new ArrayList<>();

    public LoadGeneratorReportDto(String loadGeneratorId, String scenario, String queryType) {
        this.loadGeneratorId = loadGeneratorId;
        this.scenario = scenario;
        this.queryType = queryType;
    }

    public synchronized void addMetrics(MetricsDto metricsDto) {
        metricsList.add(metricsDto);
    }
}

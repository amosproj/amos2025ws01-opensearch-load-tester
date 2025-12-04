package com.opensearchloadtester.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoadGeneratorReportDto {

    private String loadGeneratorId;
    private final List<MetricsDto> metricsList = new ArrayList<>();

    public synchronized void addMetrics(MetricsDto metricsDto) {
        metricsList.add(metricsDto);
    }
}

package com.opensearchloadtester.metricsreporter.service;

import com.opensearchloadtester.common.dto.MetricsDto;
import com.opensearchloadtester.metricsreporter.dto.StatisticsDto;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
class StatsAccumulator {

    private long totalQueries = 0;
    private long totalErrors = 0;

    private long requestDurationCount = 0;
    private long requestDurationSum = 0;
    private long requestDurationMin = Long.MAX_VALUE;
    private long requestDurationMax = Long.MIN_VALUE;

    private long queryDurationCount = 0;
    private long queryDurationSum = 0;
    private long queryDurationMin = Long.MAX_VALUE;
    private long queryDurationMax = Long.MIN_VALUE;

    void update(List<MetricsDto> results) {
        for (MetricsDto result : results) {
            totalQueries++;

            if (result.getHttpStatusCode() >= 400) {
                totalErrors++;
            }

            Long requestDurationMs = result.getRequestDurationMillis();
            if (requestDurationMs != null) {
                requestDurationCount++;
                requestDurationSum += requestDurationMs;
                requestDurationMin = Math.min(requestDurationMin, requestDurationMs);
                requestDurationMax = Math.max(requestDurationMax, requestDurationMs);
            }

            Long queryDurationMs = result.getQueryDurationMillis();
            if (queryDurationMs != null && queryDurationMs >= 0) {
                queryDurationCount++;
                queryDurationSum += queryDurationMs;
                queryDurationMin = Math.min(queryDurationMin, queryDurationMs);
                queryDurationMax = Math.max(queryDurationMax, queryDurationMs);
            }
        }
    }

    StatisticsDto toStatistics(LocalDateTime generatedAt, Set<String> loadGeneratorInstances) {
        StatisticsDto.DurationStats requestDuration = new StatisticsDto.DurationStats();
        if (requestDurationCount > 0) {
            requestDuration.setAverage(requestDurationSum / (double) requestDurationCount);
            requestDuration.setMin(requestDurationMin);
            requestDuration.setMax(requestDurationMax);
        } else {
            requestDuration.setAverage(0.0);
            requestDuration.setMin(0L);
            requestDuration.setMax(0L);
        }

        StatisticsDto.DurationStats queryDuration = new StatisticsDto.DurationStats();
        if (queryDurationCount > 0) {
            queryDuration.setAverage(queryDurationSum / (double) queryDurationCount);
            queryDuration.setMin(queryDurationMin);
            queryDuration.setMax(queryDurationMax);
        } else {
            queryDuration.setAverage(0.0);
            queryDuration.setMin(0L);
            queryDuration.setMax(0L);
        }

        return new StatisticsDto(
                generatedAt,
                requestDuration,
                queryDuration,
                totalQueries,
                totalErrors,
                new ArrayList<>(loadGeneratorInstances)
        );
    }
}
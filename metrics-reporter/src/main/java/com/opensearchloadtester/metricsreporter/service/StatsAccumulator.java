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
            if (queryDurationMs != null) {
                queryDurationCount++;
                queryDurationSum += queryDurationMs;
                queryDurationMin = Math.min(queryDurationMin, queryDurationMs);
                queryDurationMax = Math.max(queryDurationMax, queryDurationMs);
            }
        }
    }

    StatisticsDto toStatistics(LocalDateTime generatedAt, Set<String> loadGeneratorInstances) {
        return new StatisticsDto(
                generatedAt,
                buildDurationStats(requestDurationCount, requestDurationSum, requestDurationMin, requestDurationMax),
                buildDurationStats(queryDurationCount, queryDurationSum, queryDurationMin, queryDurationMax),
                totalQueries,
                totalErrors,
                new ArrayList<>(loadGeneratorInstances)
        );
    }

    private StatisticsDto.DurationStats buildDurationStats(long count, long sum, long min, long max) {
        if (count > 0) {
            return new StatisticsDto.DurationStats(sum / (double) count, min, max);
        }
        return new StatisticsDto.DurationStats(0.0, 0L, 0L);
    }
}

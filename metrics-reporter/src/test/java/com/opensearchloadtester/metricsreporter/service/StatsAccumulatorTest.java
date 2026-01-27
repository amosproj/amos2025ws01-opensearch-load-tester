package com.opensearchloadtester.metricsreporter.service;

import com.opensearchloadtester.common.dto.MetricsDto;
import com.opensearchloadtester.metricsreporter.dto.StatisticsDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class StatsAccumulatorTest {

    @Test
    void update_accumulatesBasicStats() {
        StatsAccumulator accumulator = new StatsAccumulator();

        accumulator.update(List.of(
                new MetricsDto("lg-1", "match_all", 100L, 50L, 10, 200),
                new MetricsDto("lg-1", "match_all", 300L, 150L, 5, 500)
        ));

        assertThat(accumulator.getTotalQueries()).isEqualTo(2L);
        assertThat(accumulator.getTotalErrors()).isEqualTo(1L);
        assertThat(accumulator.getRequestDurationMin()).isEqualTo(100L);
        assertThat(accumulator.getRequestDurationMax()).isEqualTo(300L);
        assertThat(accumulator.getQueryDurationMin()).isEqualTo(50L);
        assertThat(accumulator.getQueryDurationMax()).isEqualTo(150L);
    }

    @Test
    void update_handlesNullDurations() {
        StatsAccumulator accumulator = new StatsAccumulator();

        accumulator.update(List.of(
                new MetricsDto("lg-1", "match_all", null, null, 10, 200)
        ));

        assertThat(accumulator.getTotalQueries()).isEqualTo(1L);
        assertThat(accumulator.getRequestDurationCount()).isEqualTo(0L);
        assertThat(accumulator.getQueryDurationCount()).isEqualTo(0L);
    }

    @Test
    void update_skipsNegativeQueryDuration() {
        StatsAccumulator accumulator = new StatsAccumulator();

        accumulator.update(List.of(
                new MetricsDto("lg-1", "match_all", 100L, -1L, 10, 200)
        ));

        assertThat(accumulator.getTotalQueries()).isEqualTo(1L);
        assertThat(accumulator.getRequestDurationCount()).isEqualTo(1L);
        assertThat(accumulator.getQueryDurationCount()).isEqualTo(0L);
    }

    @Test
    void toStatistics_withNoData_returnsZeroDefaults() {
        StatsAccumulator accumulator = new StatsAccumulator();

        StatisticsDto stats = accumulator.toStatistics(LocalDateTime.now(), Set.of("lg-1"));

        assertThat(stats.getTotalQueries()).isEqualTo(0L);
        assertThat(stats.getTotalErrors()).isEqualTo(0L);
        assertThat(stats.getRequestDurationMs().getAverage()).isEqualTo(0.0);
        assertThat(stats.getRequestDurationMs().getMin()).isEqualTo(0L);
        assertThat(stats.getRequestDurationMs().getMax()).isEqualTo(0L);
        assertThat(stats.getQueryDurationMs().getAverage()).isEqualTo(0.0);
        assertThat(stats.getQueryDurationMs().getMin()).isEqualTo(0L);
        assertThat(stats.getQueryDurationMs().getMax()).isEqualTo(0L);
        assertThat(stats.getLoadGeneratorInstances()).containsExactly("lg-1");
    }

    @Test
    void toStatistics_calculatesCorrectAverages() {
        StatsAccumulator accumulator = new StatsAccumulator();

        accumulator.update(List.of(
                new MetricsDto("lg-1", "match_all", 100L, 50L, 10, 200),
                new MetricsDto("lg-1", "match_all", 300L, 150L, 5, 200)
        ));

        StatisticsDto stats = accumulator.toStatistics(LocalDateTime.now(), Set.of("lg-1"));

        assertThat(stats.getTotalQueries()).isEqualTo(2L);
        assertThat(stats.getTotalErrors()).isEqualTo(0L);
        assertThat(stats.getRequestDurationMs().getAverage()).isEqualTo(200.0);
        assertThat(stats.getQueryDurationMs().getAverage()).isEqualTo(100.0);
    }

    @Test
    void update_countsErrorsForStatusCodesAbove400() {
        StatsAccumulator accumulator = new StatsAccumulator();

        accumulator.update(List.of(
                new MetricsDto("lg-1", "match_all", 10L, 5L, 0, 200),
                new MetricsDto("lg-1", "match_all", 10L, 5L, 0, 400),
                new MetricsDto("lg-1", "match_all", 10L, 5L, 0, 404),
                new MetricsDto("lg-1", "match_all", 10L, 5L, 0, 500)
        ));

        assertThat(accumulator.getTotalQueries()).isEqualTo(4L);
        assertThat(accumulator.getTotalErrors()).isEqualTo(3L);
    }
}
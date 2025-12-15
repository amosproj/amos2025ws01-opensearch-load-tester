package com.opensearchloadtester.metricsreporter.service;

import com.opensearchloadtester.common.dto.LoadTestSyncStatusDto;
import com.opensearchloadtester.common.utils.TimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
public class LoadTestStartSyncService {

    private static final long START_DELAY_MILLIS = 2_000L; // 2 seconds

    private final int expectedLoadGenerators;
    private final Set<String> readyLoadGenerators = new HashSet<>();
    private Long plannedStartTimeMillis = null;

    public LoadTestStartSyncService(
            @Value("${load.generator.replicas}") int expectedLoadGenerators
    ) {
        this.expectedLoadGenerators = expectedLoadGenerators;
    }

    public synchronized void markReady(String loadGeneratorId) {
        if (readyLoadGenerators.contains(loadGeneratorId)) {
            log.debug("Load Generator with id '{}' was already marked as READY", loadGeneratorId);
            return;
        }

        readyLoadGenerators.add(loadGeneratorId);

        log.info("Load Generator with id '{}' marked as READY ({}/{})",
                loadGeneratorId, readyLoadGenerators.size(), expectedLoadGenerators);

        if (readyLoadGenerators.size() == expectedLoadGenerators && plannedStartTimeMillis == null) {
            plannedStartTimeMillis = System.currentTimeMillis() + START_DELAY_MILLIS;
            log.info("All Load Generators are ready. Planned load test start in {} seconds at {}",
                    Duration.ofMillis(START_DELAY_MILLIS).toSeconds(),
                    TimeFormatter.formatEpochMillisToUtcString(plannedStartTimeMillis));
        }
    }

    public synchronized LoadTestSyncStatusDto getStatus() {
        boolean isStartAllowed = readyLoadGenerators.size() == expectedLoadGenerators;

        return new LoadTestSyncStatusDto(
                expectedLoadGenerators,
                readyLoadGenerators.size(),
                isStartAllowed,
                plannedStartTimeMillis
        );
    }
}

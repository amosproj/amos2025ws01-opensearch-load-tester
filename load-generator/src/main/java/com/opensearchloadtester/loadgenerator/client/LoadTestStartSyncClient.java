package com.opensearchloadtester.loadgenerator.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensearchloadtester.common.dto.LoadTestStartSyncStatusDto;
import com.opensearchloadtester.common.utils.TimeFormatter;
import com.opensearchloadtester.loadgenerator.exception.LoadTestStartSyncException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Client for synchronizing the global load test start via the Metrics Reporter.
 *
 * <p>
 * Registers the Load Generator as READY and polls the shared synchronization status
 * from the Metrics Reporter until all Load Generators are allowed to start simultaneously
 * or a timeout is reached.
 * </p>
 */
@Slf4j
@Component
public class LoadTestStartSyncClient {

    private static final long STATUS_POLL_INTERVAL_MILLIS = 500L;
    private static final long MAX_WAIT_FOR_START_MILLIS = 3 * 60_000L; // 3 minutes

    private final String syncEndpointUrl;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LoadTestStartSyncClient(@Value("${metrics-reporter.url}") String metricsReporterBaseUrl) {
        this.syncEndpointUrl = metricsReporterBaseUrl + "/load-test";
        this.httpClient = HttpClients.createDefault();
    }

    public void registerReady(String loadGeneratorId) {

        // TODO: Validate loadGeneratorId format

        String encodedId = URLEncoder.encode(loadGeneratorId, StandardCharsets.UTF_8);

        // TODO: Validate encodedId format

        // TODO: Validate url format

        String url = syncEndpointUrl + "/ready/" + encodedId;

        try {
            int status = httpClient.execute(new HttpPost(url), HttpResponse::getCode);

            if (status >= 400) {
                throw new LoadTestStartSyncException(
                        String.format("Failed to register READY state (HTTP: %s)", status)
                );
            }

            log.info("Registered as READY at Metrics Reporter");
        } catch (IOException e) {
            throw new LoadTestStartSyncException("I/O error while registering READY state", e);
        }
    }

    public void awaitStartPermission() {
        log.info("Waiting for global load test start");

        long startTime = System.currentTimeMillis();

        while (true) {
            LoadTestStartSyncStatusDto status = fetchStatus();

            if (status.isStartAllowed()) {
                long plannedStart = status.getPlannedStartTimeMillis();
                long now = System.currentTimeMillis();

                if (plannedStart < now) {
                    throw new LoadTestStartSyncException(
                            String.format("Missed global start (planned: %s, now: %s)",
                                    TimeFormatter.formatEpochMillisToUtcString(plannedStart),
                                    TimeFormatter.formatEpochMillisToUtcString(now))
                    );
                }

                sleep(plannedStart - now);
                return;
            }

            if (System.currentTimeMillis() - startTime > MAX_WAIT_FOR_START_MILLIS) {
                throw new LoadTestStartSyncException("Timed out waiting for global start permission");
            }

            log.debug(
                    "Waiting for start (Load Generators ready: {}/{})",
                    status.getReadyLoadGenerators(),
                    status.getExpectedLoadGenerators()
            );

            sleep(STATUS_POLL_INTERVAL_MILLIS);
        }
    }

    private LoadTestStartSyncStatusDto fetchStatus() {
        String url = syncEndpointUrl + "/status";

        try {
            return httpClient.execute(new HttpGet(url), response -> {
                int status = response.getCode();

                if (status >= 400) {
                    throw new LoadTestStartSyncException(
                            String.format("Failed to fetch sync status (HTTP: %s)", status)
                    );
                }

                if (response.getEntity() == null) {
                    throw new LoadTestStartSyncException("Sync status response body is empty");
                }

                try {
                    return objectMapper.readValue(
                            EntityUtils.toString(response.getEntity()),
                            LoadTestStartSyncStatusDto.class
                    );
                } catch (IOException | ParseException e) {
                    throw new LoadTestStartSyncException("Failed to parse sync status response", e);
                }
            });
        } catch (IOException e) {
            throw new LoadTestStartSyncException("I/O error while fetching sync status", e);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LoadTestStartSyncException("Thread interrupted while waiting", e);
        }
    }

    @PreDestroy
    private void close() {
        try {
            httpClient.close();
        } catch (IOException e) {
            log.warn("Failed to close HTTP client", e);
        }
    }
}

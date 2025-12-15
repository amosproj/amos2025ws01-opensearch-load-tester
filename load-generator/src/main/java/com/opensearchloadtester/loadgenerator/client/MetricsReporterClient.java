package com.opensearchloadtester.loadgenerator.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensearchloadtester.common.dto.MetricsDto;
import com.opensearchloadtester.loadgenerator.exception.MetricsReporterAccessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class MetricsReporterClient {

    @Value("${metrics-reporter.url}")
    private String metricsReporterUrl;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Sends the given metrics as JSON to the configured Metrics Reporter service.
     */
    public void reportMetrics(List<MetricsDto> metricsList) {
        String jsonBody;

        try {
            jsonBody = mapper.writeValueAsString(metricsList);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Metrics to JSON: {}", e.getMessage());
            throw new MetricsReporterAccessException("Failed to serialize Metrics to JSON", e);
        }

        String url = metricsReporterUrl + "metrics";

        HttpPost request = new HttpPost(url);
        request.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
        request.setHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());
        request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));

        log.info("Sending metrics to Metrics Reporter at {}", metricsReporterUrl);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            int maxAttempts = 3;

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    int status = httpClient.execute(request, HttpResponse::getCode);

                    if (status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED) {
                        log.info("Reported metrics successfully (status: {})", status);
                        return;
                    }

                    log.warn("Failed to send metrics (attempt: {}/{}, status: {})",
                            attempt, maxAttempts, status);

                    if (attempt == maxAttempts) {
                        log.error(
                                "Stopped sending metrics to Metrics Reporter after {} failed attempts",
                                maxAttempts);
                        throw new MetricsReporterAccessException(
                                String.format(
                                        "Stopped sending metrics to Metrics Reporter after %s failed attempts",
                                        maxAttempts));
                    }
                } catch (IOException e) {
                    log.warn("I/O error while reporting metrics (attempt: {}/{}): {}",
                            attempt, maxAttempts, e.getMessage());

                    if (attempt == maxAttempts) {
                        log.error(
                                "Stopped sending metrics to Metrics Reporter after {} failed attempts due to I/O errors",
                                maxAttempts);
                        throw new MetricsReporterAccessException(
                                String.format(
                                        "Stopped sending metrics to Metrics Reporter after %s failed attempts due to I/O errors",
                                        maxAttempts), e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error while creating or closing HTTP client for Metrics Reporter: {}", e.getMessage());
            throw new MetricsReporterAccessException(
                    "Error while creating or closing HTTP client for Metrics Reporter", e);
        }
    }
}

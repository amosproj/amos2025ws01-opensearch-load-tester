package com.opensearchloadtester.loadgenerator.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensearchloadtester.common.dto.MetricsDto;
import com.opensearchloadtester.loadgenerator.exception.MetricsReporterAccessException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class MetricsReporterClient {

    private final String metricsEndpointUrl;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String finishEndpointBaseUrl;

    public MetricsReporterClient(@Value("${metrics-reporter.url}") String metricsReporterBaseUrl) {
        this.metricsEndpointUrl = metricsReporterBaseUrl + "/metrics";
        this.finishEndpointBaseUrl = metricsReporterBaseUrl + "/finish";
        this.httpClient = HttpClients.createDefault();
    }

    /**
     * Sends the given metrics as JSON to the configured Metrics Reporter service.
     */
    public void sendMetrics(List<MetricsDto> metricsList) {
        String jsonBody;

        try {
            jsonBody = objectMapper.writeValueAsString(metricsList);
        } catch (JsonProcessingException e) {
            throw new MetricsReporterAccessException("Failed to serialize metrics list to JSON", e);
        }

        HttpPost postRequest = new HttpPost(metricsEndpointUrl);
        postRequest.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));

        log.info("Sending metrics to Metrics Reporter at '{}'", metricsEndpointUrl);

        int maxAttempts = 3;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                int status = httpClient.execute(postRequest, HttpResponse::getCode);

                if (status >= 200 && status < 300) {
                    log.debug("Sent metrics successfully");
                    return;
                }

                log.warn("Failed to send metrics (attempt: {}/{}, HTTP: {})",
                        attempt, maxAttempts, status);

                if (attempt == maxAttempts) {
                    throw new MetricsReporterAccessException(
                            String.format(
                                    "Stopped sending metrics after %s failed attempts (last HTTP: %s)",
                                    maxAttempts, status)
                    );
                }
            } catch (IOException e) {
                log.warn("I/O error while sending metrics (attempt: {}/{})",
                        attempt, maxAttempts, e);

                if (attempt == maxAttempts) {
                    throw new MetricsReporterAccessException(
                            String.format(
                                    "Stopped sending metrics after %s failed attempts due to I/O errors",
                                    maxAttempts),
                            e
                    );
                }
            }
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

    public void finish(String loadGeneratorId) {
        String url = finishEndpointBaseUrl + "/" + loadGeneratorId;
        HttpPost postRequest = new HttpPost(url);

        log.info("Sending finish signal to Metrics Reporter at '{}'", url);

        try {
            int status = httpClient.execute(postRequest, HttpResponse::getCode);
            if (status < 200 || status >= 300) {
                throw new MetricsReporterAccessException("Finish call failed (HTTP: " + status + ")");
            }
        } catch (IOException e) {
            throw new MetricsReporterAccessException("Failed to call finish endpoint", e);
        }
    }

}

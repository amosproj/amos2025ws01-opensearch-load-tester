package com.opensearchloadtester.loadgenerator.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensearchloadtester.common.dto.FinishLoadTestDto;
import com.opensearchloadtester.common.dto.MetricsDto;
import com.opensearchloadtester.loadgenerator.exception.MetricsReporterAccessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
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
    private final ObjectMapper objectMapper;
    private final String finishEndpointUrl;

    public MetricsReporterClient(@Value("${metrics-reporter.url}") String metricsReporterBaseUrl,
                                 ObjectMapper objectMapper, CloseableHttpClient httpClient) {
        this.metricsEndpointUrl = metricsReporterBaseUrl + "/metrics";
        this.finishEndpointUrl = metricsReporterBaseUrl + "/finish";
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
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
                    log.info("Sent metrics successfully");
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

    public void finish(String loadGeneratorId, boolean success, String errorMessage) {
        FinishLoadTestDto finishDto = new FinishLoadTestDto(loadGeneratorId, success, errorMessage);

        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(finishDto);
        } catch (JsonProcessingException e) {
            throw new MetricsReporterAccessException("Failed to serialize finish signal to JSON", e);
        }

        HttpPost postRequest = new HttpPost(finishEndpointUrl);
        postRequest.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));

        log.info("Sending finish signal to Metrics Reporter at '{}'", finishEndpointUrl);

        try {
            int status = httpClient.execute(postRequest, HttpResponse::getCode);
            if (status >= 400) {
                throw new MetricsReporterAccessException("Finish call failed (HTTP: " + status + ")");
            }
        } catch (IOException e) {
            throw new MetricsReporterAccessException("I/O error while sending finish signal", e);
        }
    }

}

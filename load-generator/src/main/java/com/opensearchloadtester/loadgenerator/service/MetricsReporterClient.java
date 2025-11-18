package com.opensearchloadtester.loadgenerator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensearchloadtester.loadgenerator.dto.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class MetricsReporterClient {

    @Value("${metrics-reporter.url}")
    private String metricsReporterUrl;

    // Debug test-method to check if implementation works
    // @PostConstruct
//    public void test() {
//        log.info("Created metrics");
//        Metrics metrics = new Metrics();
//        metrics.setRequestType("Filter");
//        metrics.setJsonResponse("abc");
//        metrics.setRoundtripMilSec(1000);
//        metrics.setLoadGeneratorInstance("Loadgenerator 1");
//
//        reportMetrics(metrics);
//    }

    /**
     * This method is called by LoadGenerator after all execution threads are completed.
     * Then an HTTP request with collected metrics data is built and sent to metrics-reporter.
     *
     * @param metrics DTO with all metrics data
     */
    public void reportMetrics(Metrics metrics) {
        log.info("Received command to report Metrics");

        try {
            URL url = new URL(metricsReporterUrl + "addMetrics");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");

            // Metrics -> JSON
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(metrics);

            // Write JSON in request body
            OutputStream os = con.getOutputStream();
            os.write(json.getBytes(StandardCharsets.UTF_8));

            log.info("Sending HTTP request to metrics-reporter...");

            int status = con.getResponseCode();
            while (status != HttpURLConnection.HTTP_OK) {
                log.debug("Received status: {} ... retrying...", status);
                status = con.getResponseCode();
            }
            log.info("Received status: {} ... Done.", status);

            con.disconnect();

        } catch (Exception e) {
            log.error("Error while sending HTTP Request to metrics-reporter", e);
        }
    }
}

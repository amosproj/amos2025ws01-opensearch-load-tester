package com.opensearchloadtester.loadgenerator.service;

import com.opensearchloadtester.loadgenerator.dto.Metrics;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

@Slf4j
@Service
public class MetricsCollectorService {

    private BufferedWriter writer;
    private File csvFile;
    private final Metrics metrics = new Metrics(System.getenv("HOSTNAME"));

    public MetricsCollectorService() {
    }

    @PostConstruct
    public void init() {
//        try {
//            File dataDir = new File("./data");
//            if (!dataDir.exists() && !dataDir.mkdirs()) {
//                log.error("Could not create directory ./data");
//            }
//
//            csvFile = new File("./data/loadgenerator-output.csv");
//            if (csvFile.exists() && !csvFile.delete()) {
//                log.error("Could not delete existing CSV file: {}", csvFile.getAbsolutePath());
//            }
//
//            writer = new BufferedWriter(new FileWriter(csvFile, false));
//            writer.write("loadRunnerInstance;" +
//                    "query;" +
//                    "index;" +
//                    "document_id;" +
//                    "result_count;" +
//                    "max_score;" +
//                    "took_ms;" +
//                    "timed_out;" +
//                    "shards_total;" +
//                    "shards_successful;" +
//                    "shards_failed\n");
//            writer.flush();
//            log.info("Metrics CSV initialized at {}", csvFile.getAbsolutePath());
//
//        } catch (IOException e) {
//            log.error("Could not initialize CSV writer: {}", e.getMessage());
//        }
    }

    public void appendMetrics(String requestType, long roundtripMilSec, String jsonResponse) {

        metrics.addMetrics(requestType, roundtripMilSec, jsonResponse);

        log.info(metrics.toString());
        

//        try {
//            // JSON to Map
//            ObjectMapper objectMapper = new ObjectMapper();
//            Map<String, Object> responseMap = objectMapper.readValue(jsonResponse, Map.class);
//
//            // Extract metrics
//            long took = 0;
//            if (responseMap.get("took") instanceof Number) {
//                took = ((Number) responseMap.get("took")).longValue();
//            }
//
//            boolean timedOut = Boolean.TRUE.equals(responseMap.get("timed_out"));
//
//            Map<String, Object> shards = (Map<String, Object>) responseMap.get("_shards");
//            int totalShards = 0, successfulShards = 0, failedShards = 0;
//            if (shards != null) {
//                totalShards = shards.get("total") instanceof Number ? ((Number) shards.get("total")).intValue() : 0;
//                successfulShards = shards.get("successful") instanceof Number ? ((Number) shards.get("successful")).intValue() : 0;
//                failedShards = shards.get("failed") instanceof Number ? ((Number) shards.get("failed")).intValue() : 0;
//            }
//
//            int totalHits = 0;
//            double maxScore = 0.0;
//            Map<String, Object> hits = (Map<String, Object>) responseMap.get("hits");
//            if (hits != null) {
//                Map<String, Object> totalMap = (Map<String, Object>) hits.get("total");
//                if (totalMap != null && totalMap.get("value") instanceof Number) {
//                    totalHits = ((Number) totalMap.get("value")).intValue();
//                }
//
//                Object maxScoreObj = hits.get("max_score");
//                if (maxScoreObj instanceof Number) {
//                    maxScore = ((Number) maxScoreObj).doubleValue();
//                }
//            }
//
//            // Write metrics to CSV
//            writer.write(String.format(
//                    "%s;%s;%d;%d;%b;%d;%d;%d;%d;%.2f%n",
//                    System.getenv("HOSTNAME"),
//                    requestType,
//                    roundtripMilSec,
//                    took,
//                    timedOut,
//                    totalShards,
//                    successfulShards,
//                    failedShards,
//                    totalHits,
//                    maxScore
//            ));
//            writer.flush();
//
//            log.debug("Metrics appended for requestType='{}'", requestType);
//
//        } catch (IOException e) {
//            log.error("Could not write to CSV: {}", e.getMessage(), e);
//        } catch (Exception e) {
//            log.error("Failed to process JSON response: {}", e.getMessage(), e);
//        }
    }

    public File getMetricFile() {
        return csvFile;
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (writer != null) {
                writer.close();
                log.info("Metrics CSV writer closed");
            }
        } catch (IOException e) {
            log.error("Error closing CSV writer: {}", e.getMessage());
        }
    }
}

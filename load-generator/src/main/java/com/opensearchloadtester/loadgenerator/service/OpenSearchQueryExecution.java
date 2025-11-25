package com.opensearchloadtester.loadgenerator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Executes a single OpenSearch search request based on a JSON template.
 * <p>
 * Responsibilities:
 * - Load the query template JSON from classpath (resources/queries).
 * - Replace {{placeholders}} with runtime parameters.
 * - Send the HTTP POST request to OpenSearch.
 * - Log basic metrics (status, timings, hit count).
 * - Write per-run CSV files with selected fields from each hit.
 */
@Slf4j
public class OpenSearchQueryExecution implements QueryExecution {

    private final String id;
    private final String indexName;
    private final String queryFile;
    private final Map<String, String> params;
    ObjectMapper mapper = new ObjectMapper();

    private final MetricsCollectorService metricsCollectorService;

    @Value("${opensearch.url}")
    String openSearchBaseUrl;

    public OpenSearchQueryExecution(String id,
                                    String indexName,
                                    String queryFile,
                                    Map<String, String> params,
                                    String openSearchBaseUrl,
                                    MetricsCollectorService metricsCollectorService) {
        this.id = id;
        this.indexName = indexName;
        this.queryFile = queryFile;
        this.params = params;
        this.openSearchBaseUrl = openSearchBaseUrl;
        this.metricsCollectorService = metricsCollectorService;
    }

    @Override
    public void run() {

        // High-level marker that this run started
        log.debug("[{}] Starting OpenSearch query {}", id, queryFile);
        try {
            // 1) JSON template from resources/queries/<file>
            ClassPathResource resource = new ClassPathResource(queryFile);
            byte[] bytes = resource.getInputStream().readAllBytes();
            String body = new String(bytes, StandardCharsets.UTF_8);

            // 2) Substitute {{param}} with real values
            for (Map.Entry<String, String> e : params.entrySet()) {
                String placeholder = "{{" + e.getKey() + "}}";
                body = body.replace(placeholder, e.getValue());
            }

            // 3) Prepare HTTP call to OpenSearch


            String url = openSearchBaseUrl + indexName + "/_search";
            RestTemplate restTemplate = new RestTemplate();

            // Configure headers and basic auth for OpenSearch
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth("admin", "admin");

            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            // Measure client-side round-trip time
            long start = System.currentTimeMillis();
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, entity, String.class);
            long tookMs = System.currentTimeMillis() - start;

            metricsCollectorService.appendMetrics(id, tookMs, response.toString());

            int status = response.getStatusCodeValue();
            JsonNode json = mapper.readTree(response.getBody());

            // Extract basic metrics from the OpenSearch response
            int totalHits = json.path("hits").path("total").path("value").asInt();
            long osTook = json.path("took").asLong(-1);

            log.debug("[{}] Status {}, clientTimeMs={}, osTookMs={}, totalHits={}",
                    id, status, tookMs, osTook, totalHits);

        } catch (Exception e) {
            log.error("[{}] Error executing query {}: {}", id, queryFile, e.getMessage(), e);
        }
    }

    @Override
    public String getId() {
        return id;
    }
}

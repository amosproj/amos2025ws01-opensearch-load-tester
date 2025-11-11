package com.opensearchloadtester.loadgenerator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;
/**
 * Executes a single OpenSearch search request based on a JSON template.
 *
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

    public OpenSearchQueryExecution(String id,
                                    String indexName,
                                    String queryFile,
                                    Map<String, String> params) {
        this.id = id;
        this.indexName = indexName;
        this.queryFile = queryFile;
        this.params = params;
    }

    @Override
    public void run() {

        // High-level marker that this run started
        log.info("[{}] Starting OpenSearch query {}", id, queryFile);
        try {
            // 1) JSON template from resources/queries/<file>
            ClassPathResource resource = new ClassPathResource("queries/" + queryFile);
            byte[] bytes = resource.getInputStream().readAllBytes();
            String body = new String(bytes, StandardCharsets.UTF_8);

            // 2) Substitute {{param}} with real values
            for (Map.Entry<String, String> e : params.entrySet()) {
                String placeholder = "{{" + e.getKey() + "}}";
                body = body.replace(placeholder, e.getValue());
            }

            // 3) Prepare HTTP call to OpenSearch
            String openSearchBaseUrl = "http://localhost:9200";
            String url = openSearchBaseUrl + "/" + indexName + "/_search";
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

            int status = response.getStatusCodeValue();
            JsonNode json = mapper.readTree(response.getBody());

            // Extract basic metrics from the OpenSearch response
            int totalHits = json.path("hits").path("total").path("value").asInt();
            long osTook = json.path("took").asLong(-1); // tiempo que dice OpenSearch

            log.info("[{}] Status {}, clientTimeMs={}, osTookMs={}, totalHits={}",
                    id, status, tookMs, osTook, totalHits);

            // Create a per-query-run CSV file: one file per id + timestamp
            String timestamp = java.time.format.DateTimeFormatter
                    .ofPattern("yyyyMMdd_HHmmss")
                    .format(java.time.Instant.now().atZone(java.time.ZoneId.systemDefault()));

            String csvFileName = String.format("query-results/query_results_%s_%s.csv", id, timestamp);
            CsvLogger csvLogger = new CsvLogger(csvFileName, false);


            // Iterate over each hit and log/CSV-export selected fields
            for (JsonNode hit : json.path("hits").path("hits")) {
                JsonNode sourceNode = hit.path("_source");
                String sourceStr = sourceNode.toString();
                log.info("[{}] Hit source: {}", id, sourceStr);

                JsonNode customMeta = sourceNode.path("dss_custom_metadata");
                JsonNode payroll = customMeta.path("payrollinfo");

                String docName = sourceNode.path("dss_document_name").asText(null);
                String payrollType = payroll.path("payroll_type").asText(null);
                String language = payroll.path("language").asText(null);
                String year = payroll.path("accounting_year").asText(null);
                String month = payroll.path("accounting_month").asText(null);


                //  CSV: id query + data
                csvLogger.writeRow(
                        id,
                        indexName,
                        queryFile,
                        docName,
                        payrollType,
                        language,
                        year,
                        month,
                        status,
                        tookMs,
                        osTook,
                        totalHits
                );

            }



        } catch (Exception e) {
            log.error("[{}] Error executing query {}: {}", id, queryFile, e.getMessage(), e);
        }
    }

    @Override
    public String getId() {
        return id;
    }
}

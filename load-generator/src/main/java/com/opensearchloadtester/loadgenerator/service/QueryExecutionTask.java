package com.opensearchloadtester.loadgenerator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.generic.*;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Executes a single OpenSearch query based on a JSON template.
 */
@Slf4j
@RequiredArgsConstructor
public class QueryExecutionTask implements Runnable {

    @Getter
    private final String id;

    private final String index;
    private final String queryTemplatePath;
    private final Map<String, String> queryParams;
    private final OpenSearchGenericClient openSearchClient;
    private final MetricsCollector metricsCollector;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void run() {

        try {
            // Load query template JSON
            String queryTemplate = loadQueryTemplate(queryTemplatePath);

            // Substitute placeholders in query template with provided values
            String query = applyQueryParams(queryTemplate, queryParams);

            // Send query to OpenSearch and measure end-to-end client-side round-trip time
            Request request = Requests.builder()
                    .endpoint("/" + index + "/_search")
                    .method("POST")
                    .json(query)
                    .build();

            long startTime = System.nanoTime();
            Response response = openSearchClient.execute(request);
            long requestDurationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            // Collect performance metrics
            String responseBodyAsString = response.getBody()
                    .map(Body::bodyAsString)
                    .orElse("");

            metricsCollector.appendMetrics(id, requestDurationMillis, responseBodyAsString);

            int status = response.getStatus();

            JsonNode responseBodyAsJsonNode = mapper.readTree(responseBodyAsString);
            int totalHits = responseBodyAsJsonNode.path("hits").path("total").path("value").asInt();
            long openSearchExecutionMillis = responseBodyAsJsonNode.path("took").asLong(-1);

            log.debug("[{}] Status {}, requestDurationMillis={}, openSearchExecutionMillis={}, totalHits={}",
                    id, status, requestDurationMillis, openSearchExecutionMillis, totalHits);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String loadQueryTemplate(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            throw new IllegalStateException(String.format("Query template '%s' not found", path));
        }
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to read query template '{}': {}", path, e.getMessage());
            throw new UncheckedIOException(String.format("Failed to read query template '%s'", path), e);
        }
    }

    private String applyQueryParams(String template, Map<String, String> params) {
        String result = template;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }
}

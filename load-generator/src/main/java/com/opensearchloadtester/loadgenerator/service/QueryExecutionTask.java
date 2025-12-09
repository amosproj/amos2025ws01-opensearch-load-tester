package com.opensearchloadtester.loadgenerator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensearchloadtester.loadgenerator.queries.Query;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.generic.*;

import java.io.IOException;
import java.io.UncheckedIOException;
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
    private final Query query;
    private final OpenSearchGenericClient openSearchClient;
    private final MetricsCollector metricsCollector;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void run() {
        log.debug("[{}] Starting OpenSearch query '{}' in thread '{}'",
                id, query.getQueryTemplatePath(), Thread.currentThread().getName());

        String randomizedQuery = query.generateQuery();

        log.debug("Generated query '{}': {}", query.getQueryTemplatePath(), randomizedQuery);

        try {
            // Send query to OpenSearch and measure end-to-end client-side round-trip time
            Request request = Requests.builder()
                    .endpoint("/" + index + "/_search")
                    .method("POST")
                    .json(randomizedQuery)
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
}

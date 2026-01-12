package com.opensearchloadtester.loadgenerator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensearchloadtester.common.dto.MetricsDto;
import com.opensearchloadtester.loadgenerator.model.QueryType;
import com.opensearchloadtester.loadgenerator.queries.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.generic.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Executes a single OpenSearch query based on a JSON template.
 */
@Slf4j
@RequiredArgsConstructor
public class QueryExecutionTask implements Runnable {

    private final String loadGeneratorId;
    private final String index;
    private final List<QueryType> queryTypes;
    private final OpenSearchGenericClient openSearchClient;
    private final MetricsCollector metricsCollector;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void run() {
        log.debug("Executing query in thread '{}'", Thread.currentThread().getName());

        int queryTypesCount = queryTypes.size();
        QueryType selectedQueryType = queryTypes.get(ThreadLocalRandom.current().nextInt(queryTypesCount));
        Query query = selectedQueryType.createRandomQuery();
        String queryAsJson = query.toJsonString();

        // log.debug("Generated query of type '{}': {}", selectedQueryType.name(), queryAsJson);

        try {
            // Send query to OpenSearch and measure end-to-end client-side round-trip time
            Request request = Requests.builder()
                    .endpoint("/" + index + "/_search")
                    .method("POST")
                    .json(queryAsJson)
                    .build();

            long startTime = System.nanoTime();
            Response response = null;
            int status;
            try {
                response = openSearchClient.execute(request);
                status = response.getStatus();
            } catch (SocketTimeoutException timeoutException) {
                status = 408;
            }
            long requestDurationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

            if (status >= 400) {
                MetricsDto metricsDto = new MetricsDto(
                        loadGeneratorId,
                        selectedQueryType.name(),
                        requestDurationMillis,
                        null,
                        null,
                        status
                );

                metricsCollector.appendMetrics(metricsDto);

                log.debug("Query execution failed (status: {}, requestDurationMillis: {})",
                        status, requestDurationMillis);

                return;
            }

            // Collect performance metrics
            String responseBodyAsString = response.getBody()
                    .map(Body::bodyAsString)
                    .orElseThrow(() -> new IllegalStateException("Response body is missing"));

            JsonNode responseBodyAsJsonNode = mapper.readTree(responseBodyAsString);
            int totalHits = responseBodyAsJsonNode.path("hits").path("total").path("value").asInt();
            long queryDurationMillis = responseBodyAsJsonNode.path("took").asLong(-1);

            MetricsDto metricsDto = new MetricsDto(
                    loadGeneratorId,
                    selectedQueryType.name(),
                    requestDurationMillis,
                    queryDurationMillis,
                    totalHits,
                    status
            );

            metricsCollector.appendMetrics(metricsDto);

            log.debug(
                    "Executed query (status: {}, requestDurationMillis: {}, queryDurationMillis: {}, totalHits: {})",
                    status, requestDurationMillis, queryDurationMillis, totalHits);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

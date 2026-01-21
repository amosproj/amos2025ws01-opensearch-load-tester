package com.opensearchloadtester.loadgenerator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensearchloadtester.common.dto.MetricsDto;
import com.opensearchloadtester.loadgenerator.model.QueryType;
import com.opensearchloadtester.loadgenerator.queries.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.generic.*;
import org.opensearch.client.transport.httpclient5.ResponseException;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    private final Duration timeout;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void run() {
        //log.debug("Executing query in thread '{}'", Thread.currentThread().getName());

        int queryTypesCount = queryTypes.size();
        QueryType selectedQueryType = queryTypes.get(ThreadLocalRandom.current().nextInt(queryTypesCount));
        Query query = selectedQueryType.createRandomQuery();
        String queryAsJson = query.toJsonString();

        // log.debug("Generated query of type '{}': {}", selectedQueryType.name(), queryAsJson);

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
        } catch (Exception e) {
            log.debug("Error while executing query: {} {}", e.getClass(), e.getCause().getMessage());
            if (e.getCause() instanceof TimeoutException
                    || e.getCause() instanceof SocketTimeoutException) {
                status = 408;
            } else if (e.getCause() instanceof ResponseException) {
                ResponseException exep = (ResponseException) e.getCause();
                status = exep.status();
            } else {
                log.error("Error while executing query: ", e);
                status = 500;
            }
        }
        long requestDurationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

        if (status >= 400) {
            if (response != null) {
                String responseBody = response.getBody()
                        .map(Body::bodyAsString)
                        .orElse("no body");
                log.debug("Status: {}, reason: {}, body: {}", status, response.getReason(), responseBody);
            }
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

        JsonNode responseBodyAsJsonNode = null;
        try {
            responseBodyAsJsonNode = mapper.readTree(responseBodyAsString);
        } catch (JsonProcessingException e) {
            log.error("Error while parsing response body: {} {}", e.getClass(), e.getMessage());
            return;
        }
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
    }
}

package com.opensearchloadtester.testdatagenerator.service;

import com.opensearchloadtester.testdatagenerator.exception.OpenSearchDataAccessException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.indices.RefreshRequest;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.opensearch.client.transport.httpclient5.ResponseException;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class OpenSearchDataService {

    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    @Getter
    private volatile int dynamicBatchLimit = Integer.MAX_VALUE;


    private static final int MIN_SUB_BATCH_SIZE = 1_000;
    private static final int ABSOLUTE_MAX_SPLIT_DEPTH = 8;

    private final OpenSearchClient openSearchClient;

    /**
     * Creates an index if it does not exist yet.
     * <p>
     * If {@code indexSettings} or {@code indexMapping} is {@code null},
     * the respective part is omitted and OpenSearch defaults are used.
     *
     * @param indexName     name of the index to create (must not be {@code null} or blank)
     * @param indexSettings optional index settings (may be {@code null})
     * @param indexMapping  optional index mapping (may be {@code null})
     */

    public void createIndex(String indexName,
                            @Nullable IndexSettings indexSettings,
                            @Nullable TypeMapping indexMapping) {
        validateIndexName(indexName);

        if (indexExists(indexName)) {
            log.info("Index '{}' already exists, skipping creation", indexName);
            return;
        }

        try {
            CreateIndexRequest.Builder requestBuilder = new CreateIndexRequest.Builder()
                    .index(indexName);

            if (indexSettings != null) {
                requestBuilder.settings(indexSettings);
            }

            if (indexMapping != null) {
                requestBuilder.mappings(indexMapping);
            }

            openSearchClient.indices().create(requestBuilder.build());

            log.info("Created index '{}'", indexName);
        } catch (Exception e) {
            log.error("Unexpected error while creating index '{}'", indexName, e);
            throw new OpenSearchDataAccessException(
                    String.format("Unexpected error while creating index '%s'", indexName), e);
        }
    }

    public <T> String indexDocument(String indexName, T document) {
        validateIndexName(indexName);
        Objects.requireNonNull(document, "document must not be null");

        try {
            IndexRequest<T> request = new IndexRequest.Builder<T>()
                    .index(indexName)
                    .document(document)
                    .build();

            IndexResponse response = openSearchClient.index(request);

            log.info("Indexed document in index '{}' with id '{}'", indexName, response.id());

            return response.id();
        } catch (Exception e) {
            log.error("Unexpected error while indexing document in index '{}'", indexName, e);
            throw new OpenSearchDataAccessException(
                    String.format("Unexpected error while indexing document in index '%s'", indexName), e);
        }
    }

    //  BULK INDEXING WITH AUTO-SPLIT ON 429

    /**
     * Bulk indexes documents. If OpenSearch responds with HTTP 429, the batch is split
     * into smaller sub-batches (down to MIN_SUB_BATCH_SIZE) and retried.
     * The split/retry logic lives in this public method (not in executeBulk()).
     */

    /**
     * Computes how deep we are allowed to split the initial batch without
     * going below MIN_SUB_BATCH_SIZE and without exceeding ABSOLUTE_MAX_SPLIT_DEPTH.
     */
    private int computeMaxSplitDepth(int initialSize) {
        int depth = 0;
        int size = initialSize;

        while (size > MIN_SUB_BATCH_SIZE * 2 && depth < ABSOLUTE_MAX_SPLIT_DEPTH) {
            size = size / 2;
            depth++;
        }

        log.debug("Computed maxSplitDepth={} for initialSize={}", depth, initialSize);
        return depth;
    }

    public <T> void bulkIndexDocuments(String indexName, List<T> documents) {
        validateIndexName(indexName);
        Objects.requireNonNull(documents, "documents list must not be null");

        if (documents.isEmpty()) {
            log.info("Empty documents list provided, skipping bulk indexing");
            return;
        }

        final int maxSplitDepth = computeMaxSplitDepth(documents.size());

        record Work<T>(List<T> docs, int depth) {}

        var stack = new java.util.ArrayDeque<Work<T>>();
        stack.push(new Work<>(documents, 0));

        while (!stack.isEmpty()) {
            Work<T> work = stack.pop();
            List<T> batch = work.docs();
            int depth = work.depth();

            if (batch == null || batch.isEmpty()) continue;

            try {
                executeBulk(indexName, batch);
                log.debug("Bulk indexed {} documents in index '{}' (depth={})", batch.size(), indexName, depth);

            } catch (ResponseException e) {
                int status = e.status();

                boolean canSplit =
                        status == HTTP_TOO_MANY_REQUESTS
                                && batch.size() > MIN_SUB_BATCH_SIZE
                                && depth < maxSplitDepth;

                if (!canSplit) {
                    log.error("ResponseException while bulk indexing documents in index '{}' (status={})",
                            indexName, status, e);
                    throw new OpenSearchDataAccessException(
                            "Unexpected error while bulk indexing documents in index '" + indexName + "'", e);
                }

                // ---- 429 handling & split logic lives HERE (public method) ----
                int newLimit = batch.size() / 2;
                dynamicBatchLimit = Math.min(dynamicBatchLimit, newLimit);

                log.warn("Adjusted dynamic batch limit to {} based on failing batch size {}",
                        dynamicBatchLimit, batch.size());
                log.warn("Bulk request rejected with 429 for {} docs in index '{}'. Splitting (depth={}/{})...",
                        batch.size(), indexName, depth, maxSplitDepth);

                int mid = batch.size() / 2;

                // IMPORTANT: subList() is a view; copy to avoid retaining huge backing list
                List<T> left  = new ArrayList<>(batch.subList(0, mid));
                List<T> right = new ArrayList<>(batch.subList(mid, batch.size()));

                try {
                    Thread.sleep(200L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }

                // push right then left so left is processed first (LIFO)
                stack.push(new Work<>(right, depth + 1));
                stack.push(new Work<>(left,  depth + 1));

            } catch (IOException e) {
                log.error("I/O error while bulk indexing documents in index '{}'", indexName, e);
                throw new OpenSearchDataAccessException(
                        "I/O error while bulk indexing documents in index '" + indexName + "'", e);

            } catch (OpenSearchDataAccessException e) {
                throw e;

            } catch (Exception e) {
                log.error("Unexpected error while bulk indexing documents in index '{}'", indexName, e);
                throw new OpenSearchDataAccessException(
                        "Unexpected error while bulk indexing documents in index '" + indexName + "'", e);
            }

        }
    }


    // Builds the bulk request from the given documents

    private <T> void executeBulk(String indexName, List<T> documents) throws IOException {
        ArrayList<BulkOperation> ops = new ArrayList<>();

        for (T document : documents) {
            Objects.requireNonNull(document, "document must not be null");

            ops.add(new BulkOperation.Builder()
                    .index(new IndexOperation.Builder<T>()
                            .index(indexName)
                            .document(document)
                            .build())
                    .build());
        }

        BulkRequest request = new BulkRequest.Builder()
                .operations(ops)
                .build();

        BulkResponse response = openSearchClient.bulk(request);

        if (response.errors()) {
            String errorMessages = response.items().stream()
                    .filter(item -> item.error() != null)
                    .map(item -> String.format(
                            "id: %s, status: %d, reason: %s",
                            item.id(), item.status(), item.error().reason()
                    ))
                    .collect(Collectors.joining("; "));

            log.error("Bulk indexing completed with errors for index '{}': {}", indexName, errorMessages);

            throw new OpenSearchDataAccessException(
                    String.format("Bulk indexing completed with errors for index '%s': %s", indexName, errorMessages)
            );
        }
    }

    //  OTHER METHODS (search, delete, refresh...)

    public <T> Optional<T> getDocument(String indexName, String id, Class<T> documentClass) {
        validateIndexName(indexName);
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(documentClass, "documentClass must not be null");

        try {
            GetRequest request = new GetRequest.Builder()
                    .index(indexName)
                    .id(id)
                    .build();

            GetResponse<T> response = openSearchClient.get(request, documentClass);

            if (!response.found()) {
                log.debug("Document with id '{}' not found in index '{}'", id, indexName);
                return Optional.empty();
            }

            return Optional.ofNullable(response.source());
        } catch (Exception e) {
            log.error("Unexpected error while getting document with id '{}' from index '{}'", id, indexName, e);
            throw new OpenSearchDataAccessException(
                    String.format("Unexpected error while getting document with id '%s' from index '%s'", id, indexName), e);
        }
    }

    public <T> List<T> searchByField(String indexName, String fieldName, String fieldValue, Class<T> documentClass) {
        validateIndexName(indexName);
        Objects.requireNonNull(fieldName, "fieldName must not be null");
        Objects.requireNonNull(fieldValue, "fieldValue must not be null");
        Objects.requireNonNull(documentClass, "documentClass must not be null");

        try {
            SearchRequest request = new SearchRequest.Builder()
                    .index(indexName)
                    .query(q -> q
                            .match(m -> m
                                    .field(fieldName)
                                    .query(FieldValue.of(fieldValue))
                            )
                    )
                    .build();

            SearchResponse<T> response = openSearchClient.search(request, documentClass);

            return response.hits()
                    .hits()
                    .stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Unexpected error while searching in index '{}' on field '{}' with value '{}'",
                    indexName, fieldName, fieldValue, e);
            throw new OpenSearchDataAccessException(
                    String.format("Unexpected error while searching in index '%s' on field '%s' with value '%s'",
                            indexName, fieldName, fieldValue), e);
        }
    }

    public void deleteDocument(String indexName, String id) {
        validateIndexName(indexName);
        Objects.requireNonNull(id, "id must not be null");

        try {
            DeleteRequest request = new DeleteRequest.Builder()
                    .index(indexName)
                    .id(id)
                    .build();

            openSearchClient.delete(request);

            log.info("Deleted document with id '{}' from index '{}'", id, indexName);
        } catch (Exception e) {
            log.error("Unexpected error while deleting document with id '{}' from index '{}'", id, indexName, e);
            throw new OpenSearchDataAccessException(
                    String.format("Unexpected error while deleting document with id '%s' from index '%s'", id, indexName), e);
        }
    }

    public void refreshIndex(String indexName) {
        validateIndexName(indexName);

        try {
            RefreshRequest request = new RefreshRequest.Builder()
                    .index(indexName)
                    .build();

            openSearchClient.indices().refresh(request);

            log.info("Refreshed index '{}'", indexName);
        } catch (Exception e) {
            log.error("Unexpected error while refreshing index '{}'", indexName, e);
            throw new OpenSearchDataAccessException(
                    String.format("Unexpected error while refreshing index '%s'", indexName), e);
        }
    }

    private void validateIndexName(String indexName) {
        if (indexName == null || indexName.isBlank()) {
            throw new IllegalArgumentException("indexName must not be null or blank");
        }
    }

    private boolean indexExists(String indexName) {
        try {
            ExistsRequest request = new ExistsRequest.Builder()
                    .index(indexName)
                    .build();

            BooleanResponse response = openSearchClient.indices().exists(request);
            return response.value();
        } catch (Exception e) {
            log.error("Unexpected error while checking if index '{}' exists", indexName, e);
            throw new OpenSearchDataAccessException(
                    String.format("Unexpected error while checking if index '%s' exists", indexName), e);
        }
    }
}

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

    //  INDEX CREATION, GET, ETC

    //Creates an index if it does not exist yet.

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

    //Indexes a single document in the given index.

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
     * It calculates a dynamic maxSplitDepth based on the initial batch size
     * and delegates to the recursive variant. If OpenSearch responds with 429
     * (Too Many Requests), the batch is split into smaller sub-batches
     * (down to MIN_SUB_BATCH_SIZE) and retried.
     */
    public <T> void bulkIndexDocuments(String indexName, List<T> documents) {
        validateIndexName(indexName);
        Objects.requireNonNull(documents, "documents list must not be null");

        if (documents.isEmpty()) {
            log.info("Empty documents list provided, skipping bulk indexing");
            return;
        }

        int maxSplitDepth = computeMaxSplitDepth(documents.size());
        bulkIndexDocuments(indexName, documents, 0, maxSplitDepth);
    }

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

    private <T> void bulkIndexDocuments(String indexName, List<T> documents, int depth, int maxSplitDepth) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        try {
            executeBulk(indexName, documents);
            log.debug("Bulk indexed {} documents in index '{}' (depth={})", documents.size(), indexName, depth);

        } catch (ResponseException e) {
            int status = e.status();  // correct for OpenSearch Java Client HTTP5

            if (status == HTTP_TOO_MANY_REQUESTS
                    && documents.size() > MIN_SUB_BATCH_SIZE
                    && depth < maxSplitDepth) {

                dynamicBatchLimit = Math.min(dynamicBatchLimit, documents.size() / 2);
                log.warn("Adjusted dynamic batch limit to {} based on failing batch size {}",
                        dynamicBatchLimit, documents.size());
                log.warn(
                        "Bulk request rejected with 429 (Too Many Requests) for {} docs in index '{}'. " +
                                "Splitting batch (depth={}/{})...",
                        documents.size(),
                        indexName,
                        depth,
                        maxSplitDepth
                );

                // Split current batch into two sub-batches
                int mid = documents.size() / 2;
                List<T> left  = documents.subList(0, mid);
                List<T> right = documents.subList(mid, documents.size());

                // Optional small pause to give the cluster some breathing room
                try {
                    Thread.sleep(200L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }

                // Retry both halves recursively with increased depth
                bulkIndexDocuments(indexName, left,  depth + 1, maxSplitDepth);
                bulkIndexDocuments(indexName, right, depth + 1, maxSplitDepth);
                return; // important: this batch has been handled via recursive calls

            }

            // Any other status code or we cannot split further → fail normally
            log.error(
                    "ResponseException while bulk indexing documents in index '{}' (status={})",
                    indexName,
                    status,
                    e
            );
            throw new OpenSearchDataAccessException(
                    "Unexpected error while bulk indexing documents in index '" + indexName + "'",
                    e
            );

        } catch (IOException e) {
            log.error("I/O error while bulk indexing documents in index '{}'", indexName, e);
            throw new OpenSearchDataAccessException(
                    "I/O error while bulk indexing documents in index '" + indexName + "'",
                    e
            );

        } catch (Exception e) {
            log.error("Unexpected error while bulk indexing documents in index '{}'", indexName, e);
            throw new OpenSearchDataAccessException(
                    "Unexpected error while bulk indexing documents in index '" + indexName + "'",
                    e
            );
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
                            .build()
                    )
                    .build()
            );
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
                            item.id(),
                            item.status(),
                            item.error().reason()
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

package com.opensearchloadtester.testdatagenerator.service;

import com.opensearchloadtester.testdatagenerator.exception.OpenSearchDataAccessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.indices.RefreshRequest;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class OpenSearchDataService {

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
            throw new OpenSearchDataAccessException(
                    String.format("Unexpected error while creating index '%s'", indexName), e);
        }
    }

    public <T> void bulkIndexDocuments(String indexName, List<T> documents) {
        validateIndexName(indexName);
        Objects.requireNonNull(documents, "documents list must not be null");

        if (documents.isEmpty()) {
            log.info("Empty documents list provided, skipping bulk indexing");
            return;
        }

        try {
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

            log.info("Bulk indexed {} documents in index '{}'", response.items().size(), indexName);
        } catch (Exception e) {
            throw new OpenSearchDataAccessException(
                    String.format("Unexpected error while bulk indexing documents in index '%s'", indexName), e);
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
            throw new OpenSearchDataAccessException(
                    String.format("Unexpected error while checking if index '%s' exists", indexName), e);
        }
    }
}

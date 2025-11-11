package com.opensearchloadtester.testdatagenerator.service;

import com.opensearchloadtester.testdatagenerator.exception.OpenSearchDataAccessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class OpenSearchDataService {

    private final OpenSearchClient openSearchClient;

    public void createIndex(String indexName, IndexSettings indexSettings, TypeMapping indexMapping) {
        validateIndexName(indexName);
        Objects.requireNonNull(indexSettings, "indexSettings must not be null");
        Objects.requireNonNull(indexMapping, "indexMapping must not be null");

        if (indexExists(indexName)) {
            log.info("Index '{}' already exists, skipping creation", indexName);
            return;
        }

        try {
            CreateIndexRequest request = new CreateIndexRequest.Builder()
                    .index(indexName)
                    .settings(indexSettings)
                    .mappings(indexMapping)
                    .build();

            openSearchClient.indices().create(request);

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

            log.info("Bulk indexed {} documents in index '{}'", response.items().size(), indexName);
        } catch (Exception e) {
            log.error("Unexpected error while bulk indexing documents in index '{}'", indexName, e);
            throw new OpenSearchDataAccessException(
                    String.format("Unexpected error while bulk indexing documents in index '%s'", indexName), e);
        }
    }

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
            return response.value(); // true, if index exists
        } catch (Exception e) {
            log.error("Unexpected error while checking if index '{}' exists", indexName, e);
            throw new OpenSearchDataAccessException(
                    String.format("Unexpected error while checking if index '%s' exists", indexName), e);
        }
    }
}

package com.opensearchloadtester.testdatagenerator.service;

import com.opensearchloadtester.testdatagenerator.exception.OpenSearchDataAccessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class OpenSearchDataService {

    private final OpenSearchClient openSearchClient;

    public void createIndex(String indexName) {
        validateIndexName(indexName);

        if (indexExists(indexName)) {
            log.info("Index '{}' already exists, skipping creation", indexName);
            return;
        }

        try {
            CreateIndexRequest request = new CreateIndexRequest.Builder()
                    .index(indexName)
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
                    .filter(Objects::nonNull)
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

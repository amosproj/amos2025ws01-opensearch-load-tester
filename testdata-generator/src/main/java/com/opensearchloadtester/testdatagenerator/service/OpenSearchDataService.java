package com.opensearchloadtester.testdatagenerator.service;

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

import java.io.IOException;
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
        if (indexExists(indexName)) {
            return;
        }

        try {
            CreateIndexRequest request = new CreateIndexRequest.Builder()
                    .index(indexName)
                    .build();

            openSearchClient.indices().create(request);

            log.info("Created index={}", indexName);
        } catch (IOException e) {
            log.error("Error creating index {}", indexName, e);
            throw new RuntimeException("Failed to create index in OpenSearch", e);
        }
    }

    public <T> String indexDocument(String indexName, T document) {
        try {
            IndexRequest<T> request = new IndexRequest.Builder<T>()
                    .index(indexName)
                    .document(document)
                    .build();

            IndexResponse response = openSearchClient.index(request);

            log.info("Indexed document in index={} id={} result={}",
                    indexName, response.id(), response.result());

            return response.id();
        } catch (IOException e) {
            log.error("Error indexing document in index {}", indexName, e);
            throw new RuntimeException("Failed to index document in OpenSearch", e);
        }
    }

    public <T> Optional<T> getDocument(String indexName, String id, Class<T> documentClass) {
        try {
            GetRequest request = new GetRequest.Builder()
                    .index(indexName)
                    .id(id)
                    .build();

            GetResponse<T> response = openSearchClient.get(request, documentClass);

            if (!response.found()) {
                return Optional.empty();
            }

            return Optional.ofNullable(response.source());
        } catch (IOException e) {
            log.error("Error retrieving document with id {} from index {}", id, indexName, e);
            throw new RuntimeException("Failed to get document from OpenSearch", e);
        }
    }

    public <T> List<T> searchByField(String indexName, String fieldName, String fieldValue, Class<T> documentClass) {
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
        } catch (IOException e) {
            log.error("Error searching in index {} on field {} with value {}",
                    indexName, fieldName, fieldValue, e);
            throw new RuntimeException("Failed to search documents in OpenSearch by field", e);
        }
    }

    public void deleteDocument(String indexName, String id) {
        try {
            DeleteRequest request = new DeleteRequest.Builder()
                    .index(indexName)
                    .id(id)
                    .build();

            openSearchClient.delete(request);

            log.info("Deleted document from index={} id={}", indexName, id);
        } catch (IOException e) {
            log.error("Error deleting document with id {} from index {}", id, indexName, e);
            throw new RuntimeException("Failed to delete document from OpenSearch", e);
        }
    }

    private boolean indexExists(String indexName) {
        try {
            ExistsRequest request = new ExistsRequest.Builder()
                    .index(indexName)
                    .build();

            BooleanResponse response = openSearchClient.indices().exists(request);
            return response.value(); // true, if index exists
        } catch (IOException e) {
            log.error("Error checking if index {} exists", indexName, e);
            throw new RuntimeException("Failed to check if index exists in OpenSearch", e);
        }
    }
}

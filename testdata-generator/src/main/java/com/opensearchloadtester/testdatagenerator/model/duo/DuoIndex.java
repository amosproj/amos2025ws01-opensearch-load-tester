package com.opensearchloadtester.testdatagenerator.model.duo;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.indices.IndexSettings;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE) // DuoIndex Objects must not be instantiated
public class DuoIndex {

    private static final String INDEX_NAME = "duo-index";

    public static String getName() {
        return INDEX_NAME;
    }

    public static IndexSettings getSettings() {
        // TODO: add remaining settings
        return new IndexSettings.Builder()
                .numberOfShards(5)
                .numberOfReplicas(1)
                .build();
    }

    public static TypeMapping getMapping() {
        // TODO: add remaining properties
        return new TypeMapping.Builder()
                .properties(Map.ofEntries(
                        Map.entry("id", Property.of(p -> p.keyword(k -> k))),
                        Map.entry("dss_document_name", Property.of(p -> p.text(t -> t))))
                )
                .build();
    }
}

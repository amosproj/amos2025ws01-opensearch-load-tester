package com.opensearchloadtester.testdatagenerator.model.duo;

import com.opensearchloadtester.testdatagenerator.model.Index;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.indices.IndexSettings;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE) // DuoIndex Objects must not be instantiated
public class DuoIndex implements Index {

    private static final String INDEX_NAME = "duo-index";

    private static DuoIndex INSTANCE = null;


    public static DuoIndex getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DuoIndex();
        }
        return INSTANCE;
    }

    @Override
    public String getName() {
        return INDEX_NAME;
    }

    @Override
    public IndexSettings getSettings() {
        // TODO: add remaining settings
        return new IndexSettings.Builder()
                .numberOfShards(5)
                .numberOfReplicas(0)
                .build();
    }

    @Override
    public TypeMapping getMapping() {
        // TODO: add remaining properties
        return new TypeMapping.Builder()
                .properties(Map.ofEntries(
                        Map.entry("id", Property.of(p -> p.keyword(k -> k))),
                        Map.entry("dss_document_name", Property.of(p -> p.text(t -> t))))
                )
                .build();
    }
}

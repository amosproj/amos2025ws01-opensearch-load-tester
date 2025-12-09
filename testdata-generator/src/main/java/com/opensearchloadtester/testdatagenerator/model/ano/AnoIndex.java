package com.opensearchloadtester.testdatagenerator.model.ano;

import com.opensearchloadtester.testdatagenerator.model.Index;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.opensearch.client.opensearch._types.mapping.DynamicMapping;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.indices.IndexSettings;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE) // AnoIndex Objects must not be instantiated
public final class AnoIndex implements Index {

    private static final String INDEX_NAME = "ano-index";

    private static AnoIndex INSTANCE = null;


    public static AnoIndex getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AnoIndex();
        }
        return INSTANCE;
    }

    @Override
    public String getName() {
        return INDEX_NAME;
    }

    @Override
    public IndexSettings getSettings() {

        return new IndexSettings.Builder()
                .analysis(a -> a
                        .charFilter("eszett", cf -> cf.definition(d ->
                                d.mapping(m -> m.mappings("ß => ss"))))
                        .charFilter("hyphens", cf -> cf.definition(d ->
                                d.patternReplace(pr -> pr
                                        .pattern("(\\\\S+)-(\\\\S+)")
                                        .replacement("$1$2"))))
                        .normalizer("german_sorting_din_5007_1", n -> n
                                .custom(cn -> cn
                                        .filter("lowercase", "asciifolding")
                                        .charFilter("eszett", "hyphens"))
                        )
                )
                // not found: query.default_field = "custom_all"
                .numberOfShards(5)
                .numberOfReplicas(1)
                .search(sb -> sb
                        .slowlog(sl -> sl
                                .threshold(th -> th
                                        .query(q -> q.warn(t -> t.time("4s")))
                                        .fetch(f -> f.warn(t -> t.time("1s"))))
                                .level("warn")))
                .indexing(ib -> ib
                        .slowlog(sl -> sl
                                .threshold(th -> th.index(i -> i.warn(t -> t.time("1s"))))
                                .level("warn")
                                .source(10000)
                                .reformat(false)))
                .build();
    }

    @Override
    public TypeMapping getMapping() {
        return new TypeMapping.Builder()
                .dynamic(DynamicMapping.False)
                .dynamicDateFormats(
                        "uuuu-MM-dd'T'HH:mm:ssXXX",
                        "uuuu-MM-dd'T'HH:mm:ss.SSSXXX",
                        "uuuu-MM-dd HH:mm:ss.SS",
                        "uuuu-MM-dd"
                )
                .properties(buildProperties())
                .build();
    }

    private Map<String, Property> buildProperties() {

        Map<String, Property> props = new HashMap<>();

        props.put("custom_all", Property.of(p -> p.text(t -> t
                .analyzer("german")
                .store(true)
        )));

        props.put("contentLength", Property.of(p -> p.long_(l -> l)));

        props.put("contentType", Property.of(p -> p.keyword(k -> k)));

        props.put("dssCreationDatetime", Property.of(p -> p.date(d -> d)));

        props.put("dssCreationUserDisplayName", textWithSortAndRaw());

        props.put("dssCreationUserIdKey", textWithSortAndRaw());

        props.put("dssCustomMetadataPayrollInfo.accountingMonth", Property.of(p -> p.integer(i -> i)));
        props.put("dssCustomMetadataPayrollInfo.accountingYear", Property.of(p -> p.integer(i -> i)));
        props.put("dssCustomMetadataPayrollInfo.firstAccess", Property.of(p -> p.date(d -> d)));
        props.put("dssCustomMetadataPayrollInfo.language", Property.of(p -> p.keyword(k -> k)));
        props.put("dssCustomMetadataPayrollInfo.payrollType", Property.of(p -> p.keyword(k -> k)));
        props.put("dssCustomMetadataPayrollInfo.provisionDate", Property.of(p -> p.date(d -> d)));

        props.put("dssDataspaceId", Property.of(p -> p.keyword(k -> k)));

        props.put("dssDeleteRetentionMinRetention", Property.of(p -> p.date(d -> d)));

        props.put("dssDocumentId", Property.of(p -> p.keyword(k -> k)));

        props.put("dssDocumentName", Property.of(p -> p.text(t -> t
                .index(false)
                .copyTo("customAll")
                .fields("sort", f -> f.keyword(k -> k.normalizer("german_sorting_din_5007_1")))
        )));

        props.put("dssDocumentOrientation", Property.of(p -> p.long_(l -> l)));

        props.put("dssDocumentPath", Property.of(p -> p.keyword(k -> k)));

        props.put("dssDocumentSource", Property.of(p -> p.keyword(k -> k)));

        props.put("dssLastModifiedDatetime", Property.of(p -> p.date(d -> d)));
        props.put("dssLastModifiedUserDatetime", Property.of(p -> p.date(d -> d)));

        props.put("dssLastModifiedUserDisplayName", textWithSortAndRaw());
        props.put("dssLastModifiedUserIdKey", textWithSortAndRaw());

        props.put("dssOriginalFilename", textWithSortAndRaw());

        props.put("dssProcessingFlagOwner", Property.of(p -> p.keyword(k -> k)));

        props.put("dssRecyclebin", Property.of(p -> p.boolean_(b -> b)));

        props.put("dssVersion", Property.of(p -> p.keyword(k -> k)));

        props.put("etag", Property.of(p -> p.keyword(k -> k)));

        props.put("id", Property.of(p -> p.keyword(k -> k)));

        props.put("lastDocumentChange", Property.of(p -> p.date(d -> d)));

        return props;
    }

    /**
     * Helper method for fields with:
     * type = text, index = false, fields.raw = keyword
     * fields.sort = keyword(normalizer = german)
     *
     * @return property for text with sorting
     */
    private Property textWithSortAndRaw() {
        return Property.of(p -> p.text(t -> t
                .index(false)
                .fields("raw", f -> f.keyword(k -> k))
                .fields("sort", f -> f.keyword(k -> k.normalizer("german_sorting_din_5007_1")))
        ));
    }
}

package com.opensearchloadtester.testdatagenerator.model.duo;

import com.opensearchloadtester.testdatagenerator.model.Index;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.opensearch.client.opensearch._types.mapping.DynamicMapping;
import org.opensearch.client.opensearch._types.mapping.IndexOptions;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.indices.IndexSettings;

import java.util.HashMap;
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

    /**
     * Builds correct settings for the Index
     *
     * @return correct IndexSettings
     */
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

    /**
     * Builds correct mapping for the Index
     *
     * @return correct Mapping
     */
    @Override
    public TypeMapping getMapping() {
        return new TypeMapping.Builder()
                .dynamic(DynamicMapping.False)
                .dynamicDateFormats(
                        "uuuu-MM-dd'T'HH:mm:ssXXX",
                        "uuuu-MM-dd'T'HH:mm:ss",
                        "uuuu-MM-dd'T'HH:mm:ss.SSSXXX",
                        "uuuu-MM-dd"
                )
                .source(s -> s.excludes(
                        "dss_creation_datetime",
                        "EINGANGSDATUM",
                        "dss_document_source",
                        "classification_labels",
                        "dss_document_orientation",
                        "COMPANYID",
                        "FILEREF",
                        "dss_change_source",
                        "DATEINAME",
                        "dss_document_path",
                        "dss_last_modified_user_datetime",
                        "dss_delete_retention_min_retention",
                        "dss_recyclebin",
                        "dss_original_filename"
                ))
                .properties(buildProperties())
                .build();
    }

    /**
     * Builds all relevant properties
     *
     * @return Map with all properties
     */
    private Map<String, Property> buildProperties() {
        Map<String, Property> props = new HashMap<>();

        props.put("id", Property.of(p -> p.keyword(k -> k)));
        props.put("customAll", Property.of(p -> p.text(t -> t
                .analyzer("german")
                .indexOptions(IndexOptions.Offsets)
                .store(true)
        )));
        props.put("lastDocumentChange", Property.of(p -> p.date(d -> d)));

        props.put("dssDataspaceId", textWithRaw());
        props.put("dssDocumentId", textWithRaw());

        props.put("dssDocumentName", Property.of(p -> p.text(t -> t
                .index(false)
                .copyTo("customAll")
                .fields("sort", f -> f.keyword(k -> k.normalizer("german_sorting_din_5007_1")))
        )));

        props.put("contentLength", Property.of(p -> p.long_(l -> l)));

        props.put("ocrFulltext", Property.of(p -> p.text(t -> t
                .index(false)
                .copyTo("customAll"
                ))));

        props.put("contentType", textWithRaw());
        props.put("etag", textWithRaw());
        props.put("dssVersion", textWithRaw());
        props.put("dssLastModifiedUserIdKey", textWithRaw());

        props.put("dssLastModifiedDatetime", Property.of(p -> p.date(d -> d)));

        props.put("dssCustomMetadataDuo.bookingState", textWithRaw());
        props.put("dssCustomMetadataDuo.bookingStateChangedAt", Property.of(p -> p.date(d -> d)));
        props.put("dssCustomMetadataDuo.companyId", Property.of(p -> p.long_(l -> l)));
        props.put("dssCustomMetadataDuo.currency", textWithRaw());
        props.put("dssCustomMetadataDuo.customerNumber", Property.of(p -> p.text(t -> t
                .index(false)
                .copyTo("customAll")
                .fields("raw", f -> f.keyword(k -> k))
        )));
        props.put("dssCustomMetadataDuo.deletedAt", Property.of(p -> p.date(d -> d)));
        props.put("dssCustomMetadataDuo.documentType", Property.of(p -> p.integer(i -> i)));
        props.put("dssCustomMetadataDuo.documentCategory", textWithRaw());
        props.put("dssCustomMetadataDuo.documentInvoiceType", Property.of(p -> p.keyword(k -> k)));
        props.put("dssCustomMetadataDuo.einvoiceFulltext", Property.of(p -> p.text(t -> t
                .index(false)
                .copyTo("customAll"
                ))));
        props.put("dssCustomMetadataDuo.hasPositionCorrection", Property.of(p -> p.boolean_(b -> b)));
        props.put("dssCustomMetadataDuo.invoiceBusinessPartner", Property.of(p -> p.text(t -> t
                .index(false)
                .copyTo("customAll")
                .fields("raw", f -> f.keyword(k -> k))
        )));
        props.put("dssCustomMetadataDuo.invoiceBusinessPartnerId", Property.of(p -> p.long_(l -> l)));
        props.put("dssCustomMetadataDuo.invoiceDate", Property.of(p -> p.date(d -> d)));
        props.put("dssCustomMetadataDuo.invoiceNumber", Property.of(p -> p.text(t -> t
                .index(false)
                .copyTo("customAll")
                .fields("raw", f -> f.keyword(k -> k))
        )));
        props.put("dssCustomMetadataDuo.lastModifiedDatetime", Property.of(p -> p.date(d -> d)));
        props.put("dssCustomMetadataDuo.lastModifiedUserIdKey", textWithRaw());
        props.put("dssCustomMetadataDuo.location", textWithRaw());
        props.put("dssCustomMetadataDuo.paidAt", Property.of(p -> p.date(d -> d)));
        props.put("dssCustomMetadataDuo.paidStatus", textWithRaw());

        props.put("dssCustomMetadataDuo.positions.note", Property.of(p -> p.text(t -> t
                .index(false)
                .copyTo("customAll"
                ))));
        props.put("dssCustomMetadataDuo.positions.costCenter1", Property.of(p -> p.text(t -> t
                .index(false)
                .copyTo("customAll")
                .fields("raw", f -> f.keyword(k -> k))
        )));
        props.put("dssCustomMetadataDuo.positions.costCenter2", Property.of(p -> p.text(t -> t
                .index(false)
                .copyTo("customAll")
                .fields("raw", f -> f.keyword(k -> k))
        )));
        props.put("dssCustomMetadataDuo.positions.serviceDate", Property.of(p -> p.date(d -> d)));
        props.put("dssCustomMetadataDuo.totalGrossAmount", Property.of(p -> p.double_(d -> d
                .copyTo("customAll"
                ))));
        props.put("dssCustomMetadataDuo.uploaderScId", textWithRaw());
        props.put("dssCustomMetadataDuo.timeOfUpload", Property.of(p -> p.date(d -> d)));
        props.put("dssCustomMetadataDuo.documentApprovalState", textWithRaw());
        props.put("dssCustomMetadataDuo.transactionIds", textWithRaw());

        props.put("dssCreationUserDisplayName", Property.of(p -> p.text(t -> t
                .index(false)
                .fields("raw", f -> f.keyword(k -> k))
                .fields("sort", f -> f.keyword(k -> k.normalizer("german_sorting_din_5007_1")))
        )));
        props.put("dssLastModifiedUserDisplayName", Property.of(p -> p.text(t -> t
                .index(false)
                .fields("raw", f -> f.keyword(k -> k))
                .fields("sort", f -> f.keyword(k -> k.normalizer("german_sorting_din_5007_1")))
        )));
        props.put("dssCreationUserIdKey", Property.of(p -> p.text(t -> t
                .index(false)
                .fields("raw", f -> f.keyword(k -> k))
                .fields("sort", f -> f.keyword(k -> k.normalizer("german_sorting_din_5007_1")))
        )));

        props.put("dssProcessingFlagOwner", Property.of(p -> p.keyword(k -> k)));

        return props;
    }

    /**
     * Helper method for fields with:
     * type = text, index = false, fields.raw = keyword
     *
     * @return property for text with raw field
     */
    private Property textWithRaw() {
        return Property.of(p -> p.text(t -> t
                .index(false)
                .fields("raw", f -> f.keyword(k -> k))
        ));
    }

}

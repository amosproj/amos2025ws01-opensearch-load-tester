# Complex Queries in OpenSearch (short)

- Cost drivers: fuzziness/slop in full-text, large/nested aggregations, `top_hits` per bucket, leading wildcards, very wide date ranges, highlighting on large fields (e.g., OCR).
- Use wildcards only on keyword fields; a leading `*` is the most expensive variant.
- Large aggs with high `size`/`shard_size` multiply RAM/CPU, especially on high-cardinality fields.

## q1_duo_complex.json (realistic but heavy)

- Hits: `size: 50`, `track_total_hits: true`.
- Query:
    - `multi_match` on `custom_all` (includes `ocr_fulltext`) with `operator: and`, `fuzziness: AUTO`, `slop: 3` → tolerates typos/word gaps, adds cost.
    - `range` on `dss_custom_metadata.duo.invoice_date` (2016–now) → broad time window.
    - `wildcard` on `dss_custom_metadata.duo.invoice_business_partner.raw` with `*holz*` → leading wildcard forces wide term scan; in the sample IP data this field is often null and not a good wildcard target—prefer a populated keyword field instead.
    - `terms` on `document_category.raw` → keeps relevant document types only.
- Highlight: Fast Vector Highlighter on `custom_all`, 5 fragments of 180 chars → expensive on large OCR text.
- Aggregations:
    - `terms by_dataspace` (size 5000) → many buckets.
    - Within: `terms by_partner` (size 5000) → many more buckets.
    - Within: `date_histogram by_month` + sub-aggs: `terms paid_state`, `percentiles` on `total_gross_amount` (drop if not needed), `cardinality` on `invoice_number`, `top_hits` (3 docs) → CPU/RAM intensive.

Notes on wildcard fields:

- `document_path` in the sample IP data is highly structured and not a good candidate for a broad wildcard; prefer exact/prefix filters there.
- `invoice_business_partner.raw` is often null in the sample data and not suited for wildcards; pick a keyword field that is actually populated (e.g., `invoice_number.raw` or another business key).

Notes:

- `percentiles` can be removed if not needed; they add extra CPU/memory per bucket.
- `Highlight` is expensive but maybe not needed, if so drop it

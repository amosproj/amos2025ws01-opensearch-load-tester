#!/usr/bin/env python3
import json
import os
import sys
import urllib.request
from urllib.error import HTTPError
from pathlib import Path

OPENSEARCH_URL = os.getenv("OPENSEARCH_URL", "http://test-target-opensearch:9200").rstrip("/")
OUTPUT_PATH = Path(
    os.getenv(
        "OSB_SEED_VALUES_PATH",
        "/workloads/workloads/amos-load-tester/seed-values.json",
    )
)


def request_search(index: str, body: dict):
    url = f"{OPENSEARCH_URL}/{index}/_search"
    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except HTTPError as exc:
        if exc.code == 404:
            return None
        raise


def extract_terms(response: dict, agg_name: str) -> list:
    buckets = response.get("aggregations", {}).get(agg_name, {}).get("buckets", [])
    return [bucket.get("key") for bucket in buckets if bucket.get("key") is not None]


def main() -> int:
    ano_body = {
        "size": 0,
        "aggs": {
            "creation_user": {"terms": {"field": "dssCreationUserDisplayName.raw", "size": 200}},
            "original_filename": {"terms": {"field": "dssOriginalFilename.raw", "size": 200}},
            "accounting_year": {
                "terms": {"field": "dssCustomMetadataPayrollInfo.accountingYear", "size": 50}
            },
        },
    }

    duo_body = {
        "size": 0,
        "aggs": {
            "invoice_business_partner": {
                "terms": {"field": "dssCustomMetadataDuo.invoiceBusinessPartner.raw", "size": 200}
            },
            "invoice_number": {
                "terms": {"field": "dssCustomMetadataDuo.invoiceNumber.raw", "size": 200}
            },
            "customer_number": {
                "terms": {"field": "dssCustomMetadataDuo.customerNumber.raw", "size": 200}
            },
            "cost_center_1": {
                "terms": {"field": "dssCustomMetadataDuo.positions.costCenter1.raw", "size": 200}
            },
        },
    }

    try:
        ano_response = request_search("ano-index", ano_body)
        duo_response = request_search("duo-index", duo_body)
    except Exception as exc:
        print(f"Failed to sample values from OpenSearch: {exc}")
        return 1

    if ano_response is None and duo_response is None:
        print("Failed to sample values: both ano-index and duo-index are missing.")
        return 1

    seed_values = {
        "ano": {
            "creation_user_display_name": extract_terms(ano_response or {}, "creation_user"),
            "original_filename": extract_terms(ano_response or {}, "original_filename"),
            "accounting_year": extract_terms(ano_response or {}, "accounting_year"),
        },
        "duo": {
            "invoice_business_partner": extract_terms(duo_response or {}, "invoice_business_partner"),
            "invoice_number": extract_terms(duo_response or {}, "invoice_number"),
            "customer_number": extract_terms(duo_response or {}, "customer_number"),
            "cost_center_1": extract_terms(duo_response or {}, "cost_center_1"),
        },
    }

    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT_PATH.write_text(json.dumps(seed_values, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Seed values written to {OUTPUT_PATH}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

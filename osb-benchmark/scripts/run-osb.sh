#!/usr/bin/env bash
set -euo pipefail

WORKLOAD_PATH="${OSB_WORKLOAD_PATH:-/workloads/workloads/amos-load-tester}"
TEST_PROCEDURE="${OSB_TEST_PROCEDURE:-default}"
TARGET_HOSTS="${OSB_TARGET_HOSTS:-http://test-target-opensearch:9200}"
RESULTS_FORMAT="${OSB_RESULTS_FORMAT:-csv}"
RESULTS_CSV="${OSB_RESULTS_CSV:-/reports/osb_summary.csv}"
WORKLOAD_PARAMS="${OSB_WORKLOAD_PARAMS:-}"
CLIENT_OPTIONS="${OSB_CLIENT_OPTIONS:-}"
VALIDATE_QUERIES="${OSB_VALIDATE_QUERIES:-1}"
SAMPLE_VALUES="${OSB_SAMPLE_VALUES:-1}"

if [[ "$SAMPLE_VALUES" == "1" ]]; then
  python3 /workloads/scripts/sample_values.py
fi

if [[ "$VALIDATE_QUERIES" == "1" ]]; then
  python3 /workloads/scripts/validate_queries.py
fi

cmd=(opensearch-benchmark run \
  --pipeline=benchmark-only \
  --workload-path "$WORKLOAD_PATH" \
  --test-procedure "$TEST_PROCEDURE" \
  --target-hosts "$TARGET_HOSTS" \
  --results-file "$RESULTS_CSV" \
  --results-format "$RESULTS_FORMAT")

if [[ -n "$WORKLOAD_PARAMS" ]]; then
  cmd+=(--workload-params "$WORKLOAD_PARAMS")
fi

if [[ -n "$CLIENT_OPTIONS" ]]; then
  cmd+=(--client-options "$CLIENT_OPTIONS")
fi

"${cmd[@]}"

echo "OSB summary written to $RESULTS_CSV"

# metrics-reporter (ReportService)

The metrics-reporter exposes a REST endpoint (`/api/addmetrics`) that receives batches of `Metrics` from the load-generator. `ReportService` streams those metrics to disk and keeps only lightweight aggregates in memory, making it safe for large runs.

## What it writes

- `tmp_query_results.ndjson` – one `QueryResult` per line (compact JSON, append-only).
- `query_results.json` – valid JSON array built from the NDJSON stream, suitable for Grafana import.
- `statistics.json` – summary counters and latency stats (avg/min/max) without embedding all query results.

## How it works

1. `ReportController` validates incoming metrics and passes them to `ReportService.processMetrics`.
2. `ReportService` converts each metric to `QueryResult`, appends to NDJSON, and updates in-memory stats only.
3. On `finalizeReports`, stats are written to `statistics.json` and the NDJSON is streamed into `query_results.json` (no bulk load).

## Configuration

Adjust filenames/paths via properties (defaults in parentheses):

- `report.output.directory` (`../reports`)
- `report.ndjson.filename` (`tmp_query_results.ndjson`)
- `report.resultsjson.filename` (`query_results.json`)
- `report.stats.filename` (`statistics.json`)

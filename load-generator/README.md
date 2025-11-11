# Load Generator 
------------------
# Query Execution Component

## Overview
This component adds functionality to execute parameterized OpenSearch queries via REST endpoints.

## New Features
- **Endpoint** `/api/load-test/run`: executes queries with multiple threads and iterations.
- **QueryRegistry**: maps query IDs (q1–q11) to JSON templates in `resources/queries/`.
- **OpenSearchQueryExecution**: handles parameter substitution, HTTP requests, timing.
- **Parallel execution** using `LoadRunnerService`.

## Usage Example
 *To start the Load Generator*

```bash
mvn spring-boot:run
```
This starts the Load Generator service on port 8081.

*Example for Query 1*

```bash
 curl -X POST "http://localhost:8081/api/load-test/run"   -H "Content-Type: application/json"   -d '{
    "queryId": "q1",
    "threads": 2,
    "iterations": 1,
    "indexName": "ano_test",
    "params": {
      "from": "2024",
      "to": "2025"
    }
  }'
```
Each query template supports its own set of parameters,
which can be customized in the "params" section of the request body.
For example, some queries use category, approval_state, or location instead of from/to.
# OpenSearch Load Tester

This project provides a Docker-based OpenSearch environment for load testing with pre-configured mappings and test scenarios.

## Quick Start

### 1. Start the Environment

```bash
cd opensearch-load-tester/docker
docker-compose up -d
```

### 2. Verify OpenSearch is Running

```bash
curl -X GET "localhost:9200/_cluster/health?pretty"
```

### 3. Create the Articles Index

```bash
curl -X PUT localhost:9200/articles \
  -H 'Content-Type: application/json' \
  --data-binary @mappings/articles.json
```

### 4. Test the Index

```bash
curl -X POST 'localhost:9200/articles/_search?pretty' \
  -H 'Content-Type: application/json' \
  -d '{ "query": { "match_all": {} } }'
```

## Sample Data Operations

### Insert Sample Document

```bash
curl -X POST 'localhost:9200/articles/_doc' \
  -H 'Content-Type: application/json' \
  -d '{
    "id": "1",
    "title": "Test Article",
    "content": "This is the article content for testing",
    "category": "Technology",
    "tags": ["test", "demo"],
    "published": "2024-01-01",
    "location": {"lat": 52.5200, "lon": 13.4050}
  }'
```

### Search Operations

#### Full-text Search (German Analyzer)

```bash
curl -X POST 'localhost:9200/articles/_search?pretty' \
  -H 'Content-Type: application/json' \
  -d '{
    "query": {
      "match": {
        "title": "Test"
      }
    }
  }'
```

#### Category Filter

```bash
curl -X POST 'localhost:9200/articles/_search?pretty' \
  -H 'Content-Type: application/json' \
  -d '{
    "query": {
      "term": {
        "category": "Technology"
      }
    }
  }'
```

#### Complex Query with Aggregations

```bash
curl -X POST 'localhost:9200/articles/_search?pretty' \
  -H 'Content-Type: application/json' \
  -d '{
    "query": {
      "bool": {
        "must": [
          { "match": { "content": "testing" } },
          { "term": { "category": "Technology" } }
        ]
      }
    },
    "aggs": {
      "categories": {
        "terms": { "field": "category" }
      }
    }
  }'
```

## Load Testing Commands

### Bulk Insert Test Data

```bash
# Create multiple documents for load testing
for i in {1..100}; do
  curl -X POST 'localhost:9200/articles/_doc' \
    -H 'Content-Type: application/json' \
    -d "{
      \"id\": \"$i\",
      \"title\": \"Article $i\",
      \"content\": \"Content for article number $i\",
      \"category\": \"Test\",
      \"tags\": [\"load-test\", \"article-$i\"],
      \"published\": \"2024-01-$(printf %02d $((i % 28 + 1)))\"
    }"
done
```

### Performance Monitoring

```bash
# Check cluster health
curl -X GET "localhost:9200/_cluster/health?pretty"

# Get index statistics
curl -X GET "localhost:9200/articles/_stats?pretty"

# Get node stats
curl -X GET "localhost:9200/_nodes/stats?pretty"
```

## Access Points

- **OpenSearch API**: http://localhost:9200
- **OpenSearch Dashboards**: http://localhost:5601

## Index Mapping

The `articles` index includes:

- **id**: keyword field for exact matching
- **title**: text field with German analyzer + keyword field
- **content**: text field with German analyzer
- **category**: keyword field for filtering
- **tags**: keyword field for tagging
- **published**: date field
- **location**: geo_point field for geographic queries

## Cleanup

```bash
# Stop containers
docker-compose down

# Remove index
curl -X DELETE "localhost:9200/articles"

# Remove all data
docker-compose down -v
```

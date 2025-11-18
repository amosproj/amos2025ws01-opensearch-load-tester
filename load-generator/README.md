# 🚀 How to start and test the load-generator

The Load Generator is the component, that can be scaled
and generates the load on the OpenSearch instance. The
execution is performed multithreaded. When every thread
finished its execution, an HTTP request is sent to the
MetricsReporter container.

## Components

- LoadRunner - starts multiple threads with query executions
- QueryExecution - Executes one query and reports metrics to MetricsCollector
- MetricsCollector - Stores all metrics
- MetricsReporterClient - Sends all metrics to MetricsReporter container when
  all threads are finished

## Integration Tests (REST API)

### 1. Start Application

```bash
cd load-generator
./mvnw spring-boot:run
```

Expected Result:

    Application starts without errors
    Port 8081 is reachable
    Logs show: "Started LoadGeneratorApplication"

### 2. Test Health Check Endpoint

```bash
curl http://localhost:8081/api/load-test/health
```

Expected Result:

    HTTP Status: 200 OK
    Response Body: Load Generator is running!\n

### 3. Load Test with Default Parameters

```bash
curl -X POST "http://localhost:8081/api/load-test/start"
```

Expected Result:

    HTTP Status: 200 OK
    Response Body: Load test completed successfully with 5 threads\n
    In logs: "Starting load test with 5 parallel query execution threads"
    In logs: "All 5 query execution threads completed successfully"

### 4. Load Test with Custom Thread Count

```bash
curl -X POST "http://localhost:8081/api/load-test/start?threadCount=3"
```

Expected Result:

    HTTP Status: 200 OK
    Response Body: Load test completed successfully with 3 threads\n
    In logs: "Starting load test with 3 parallel query execution threads"
    In logs: "All 3 query execution threads completed successfully"

### 5. Load Test with Many Threads

```bash
curl -X POST "http://localhost:8081/api/load-test/start?threadCount=10"
```

Expected Result:

    HTTP Status: 200 OK
    Response Body: Load test completed successfully with 10 threads\n
    All 10 threads are executed in parallel

## Functionality Tests

### 1. Test Parallelism

Start multiple load tests simultaneously

```bash
curl -X POST "http://localhost:8081/api/load-test/start?threadCount=5" &
curl -X POST "http://localhost:8081/api/load-test/start?threadCount=3" &
wait
```

Expected Result:

    Both requests are processed
    No race conditions
    Logs show correct thread management

### 2. Thread Pool Behavior

Test with many threads

```bash
curl -X POST "http://localhost:8081/api/load-test/start?threadCount=20"
```

Expected Result:

    All threads are managed correctly
    Executor service is properly shut down
    No hanging threads

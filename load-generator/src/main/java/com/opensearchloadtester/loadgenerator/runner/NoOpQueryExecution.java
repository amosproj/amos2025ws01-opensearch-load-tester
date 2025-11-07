package com.opensearchloadtester.loadgenerator.runner;

import lombok.extern.slf4j.Slf4j;
// PLACEHOLDER IMPLEMENTATION! THIS IS JUST A PLACEHOLDER FOR THE QUERY EXECUTION!

/**
 * Placeholder implementation of QueryExecution for testing purposes.
 * This will be replaced with actual query logic using the shared DbClient library.
 */
@Slf4j
public class NoOpQueryExecution implements QueryExecution {

    private final String id;
    private final int executionTimeMs;

    public NoOpQueryExecution(String id) {
        this(id, 1000); // Default 1 second execution time
    }

    public NoOpQueryExecution(String id, int executionTimeMs) {
        this.id = id;
        this.executionTimeMs = executionTimeMs;
    }

    @Override
    public void run() {
        log.info("QueryExecution {} started", id);
        try {
            // Simulate query execution time
            Thread.sleep(executionTimeMs);
            log.info("QueryExecution {} completed", id);
        } catch (InterruptedException e) {
            log.warn("QueryExecution {} was interrupted", id);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String getId() {
        return id;
    }
}


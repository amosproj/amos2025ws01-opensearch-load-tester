package com.opensearchloadtester.loadgenerator.runner;

/**
 * Factory interface for creating QueryExecution instances.
 * This allows the LoadRunnerService to create multiple query executions
 * without knowing the specific implementation details.
 */
@FunctionalInterface
public interface QueryExecutionFactory {
    
    /**
     * Creates a new QueryExecution instance.
     * 
     * @param threadId The unique identifier for this thread/execution
     * @return A new QueryExecution instance
     */
    QueryExecution create(int threadId);
}


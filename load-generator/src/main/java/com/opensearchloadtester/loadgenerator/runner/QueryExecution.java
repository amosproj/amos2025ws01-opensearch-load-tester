package com.opensearchloadtester.loadgenerator.runner;
// PLACEHOLDER IMPLEMENTATION! THIS IS JUST A PLACEHOLDER FOR THE QUERY EXECUTION!
/**
 * Interface for query execution tasks that will be run in parallel threads.
 * The actual query logic will be implemented later using the shared DbClient library.
 */
public interface QueryExecution extends Runnable {
    
    /**
     * Executes a single query operation.
     * This method will be called by the LoadRunner in a separate thread.
     */
    @Override
    void run();
    
    /**
     * Returns a unique identifier for this query execution.
     * Useful for logging and tracking purposes.
     * 
     * @return unique identifier for this execution
     */
    String getId();
}


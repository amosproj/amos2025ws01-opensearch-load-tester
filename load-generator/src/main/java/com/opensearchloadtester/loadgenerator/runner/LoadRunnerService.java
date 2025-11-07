package com.opensearchloadtester.loadgenerator.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for executing multiple queries simultaneously using thread pools.
 * Manages thread lifecycle and ensures proper shutdown when all queries are completed.
 */
@Slf4j
@Service
public class LoadRunnerService {

    /**
     * Executes n query executions simultaneously and waits for all to complete.
     * 
     * @param queryExecutions List of query executions to run in parallel
     */
    public void executeQueries(List<QueryExecution> queryExecutions) throws InterruptedException {
        if (queryExecutions == null || queryExecutions.isEmpty()) {
            return;
        }

        int threadCount = queryExecutions.size();

        // TODO: resources can be a problem here, use a thread pool with a max size
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        try {
            // Submit all query executions to the thread pool
            for (QueryExecution queryExecution : queryExecutions) {
                executorService.submit(() -> {
                    try {
                        queryExecution.run();
                    } catch (Exception e) {
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Wait for all threads to complete
            boolean completed = latch.await(Long.MAX_VALUE, TimeUnit.SECONDS);
            // TODO: Use this for production to set a timeout
            // boolean completed = latch.await(10, TimeUnit.MINUTES);


        } finally {
            // Shutdown executor service
    /**
     * Executes n query executions simultaneously using a factory to create them.
     * 
     * @param threadCount Number of query execution threads to spawn
     * @param queryExecutionFactory Factory to create query execution instances
     * @throws InterruptedException if the execution is interrupted while waiting
     */
    public void executeQueries(int threadCount, QueryExecutionFactory queryExecutionFactory) 
            throws InterruptedException {
        if (threadCount <= 0) {
            log.warn("Invalid thread count: {}, nothing to execute", threadCount);
            return;
        }

        List<QueryExecution> queryExecutions = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            queryExecutions.add(queryExecutionFactory.create(i));
        }

        executeQueries(queryExecutions);
    }

        }
    }
}


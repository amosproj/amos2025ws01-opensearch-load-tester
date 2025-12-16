package com.opensearchloadtester.loadgenerator.exception;

public class LoadTestStartSyncException extends RuntimeException {

    public LoadTestStartSyncException(String message) {
        super(message);
    }

    public LoadTestStartSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}

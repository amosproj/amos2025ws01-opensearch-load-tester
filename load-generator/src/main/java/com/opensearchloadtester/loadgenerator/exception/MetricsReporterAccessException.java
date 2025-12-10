package com.opensearchloadtester.loadgenerator.exception;

public class MetricsReporterAccessException extends RuntimeException {

    public MetricsReporterAccessException(String message) {
        super(message);
    }

    public MetricsReporterAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}

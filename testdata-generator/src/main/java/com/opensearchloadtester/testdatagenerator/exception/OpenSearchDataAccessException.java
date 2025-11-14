package com.opensearchloadtester.testdatagenerator.exception;

public class OpenSearchDataAccessException extends RuntimeException {

    public OpenSearchDataAccessException(String message) {
        super(message);
    }

    public OpenSearchDataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}

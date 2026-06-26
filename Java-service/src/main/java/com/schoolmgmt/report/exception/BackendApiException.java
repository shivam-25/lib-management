package com.schoolmgmt.report.exception;

import org.springframework.http.HttpStatus;

public class BackendApiException extends RuntimeException {

    private final HttpStatus status;

    public BackendApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

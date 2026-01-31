package com.backend.common.exception;

import org.springframework.http.HttpStatus;

// NO LOMBOK ANNOTATIONS HERE
public class CustomException extends RuntimeException {

    private final HttpStatus status;

    public CustomException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    // MANUALLY WRITTEN GETTER METHOD
    public HttpStatus getStatus() {
        return this.status;
    }
}
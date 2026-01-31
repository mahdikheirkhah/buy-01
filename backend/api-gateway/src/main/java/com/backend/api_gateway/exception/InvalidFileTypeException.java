package com.backend.api_gateway.exception;

public class InvalidFileTypeException extends RuntimeException {
    public InvalidFileTypeException(String message) { super(message); }
}

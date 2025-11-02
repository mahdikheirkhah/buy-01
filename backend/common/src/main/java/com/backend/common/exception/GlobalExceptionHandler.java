// common/src/main/java/com/backend/common/exception/GlobalExceptionHandler.java

package com.backend.common.exception;

import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

// common/src/main/java/com/backend/common/exception/GlobalExceptionHandler.java

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TIMESTAMP = "timestamp";
    private static final String STATUS = "status";
    private static final String ERROR = "error";
    private static final String MESSAGE = "message";

    // ────────────────────────────────────────────────────────────────
    // 1. Custom Exceptions
    // ────────────────────────────────────────────────────────────────
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Map<String, Object>> handleCustom(CustomException ex) {
        return buildResponse(ex.getStatus(), ex.getMessage());
    }

    // ────────────────────────────────────────────────────────────────
    // 2. Spring Security Authentication Exceptions
    // ────────────────────────────────────────────────────────────────
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials() {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound() {
        return buildResponse(HttpStatus.UNAUTHORIZED, "User not found");
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabled() {
        return buildResponse(HttpStatus.FORBIDDEN, "Account is disabled");
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<Map<String, Object>> handleLocked() {
        return buildResponse(HttpStatus.FORBIDDEN, "Account is locked");
    }

    @ExceptionHandler(AccountExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleExpired() {
        return buildResponse(HttpStatus.FORBIDDEN, "Account has expired");
    }

    @ExceptionHandler(CredentialsExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleCredentialsExpired() {
        return buildResponse(HttpStatus.FORBIDDEN, "Credentials have expired");
    }

    // ────────────────────────────────────────────────────────────────
    // 3. Validation
    // ────────────────────────────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        err -> err.getDefaultMessage() != null ? err.getDefaultMessage() : "invalid"
                ));
        return buildFieldErrorResponse(HttpStatus.BAD_REQUEST, errors);
    }

    // ────────────────────────────────────────────────────────────────
    // 4. File Upload
    // ────────────────────────────────────────────────────────────────
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleFileSize() {
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "File size exceeds 2 MB limit.");
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidFileType(InvalidFileTypeException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ────────────────────────────────────────────────────────────────
    // 5. 404
    // ────────────────────────────────────────────────────────────────
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoHandlerFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Endpoint not found: " + ex.getRequestURL());
    }

    // ────────────────────────────────────────────────────────────────
    // 6. Fallback
    // ────────────────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        ex.printStackTrace();
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

    // ────────────────────────────────────────────────────────────────
    // Helper Methods
    // ────────────────────────────────────────────────────────────────
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put(TIMESTAMP, LocalDateTime.now());
        body.put(STATUS, status.value());
        body.put(ERROR, status.getReasonPhrase());
        body.put(MESSAGE, message);
        return new ResponseEntity<>(body, status);
    }

    private ResponseEntity<Map<String, Object>> buildFieldErrorResponse(HttpStatus status, Map<String, String> errors) {
        Map<String, Object> body = new HashMap<>();
        body.put(TIMESTAMP, LocalDateTime.now());
        body.put(STATUS, status.value());
        body.put(ERROR, status.getReasonPhrase());
        body.put("errors", errors);
        return new ResponseEntity<>(body, status);
    }
}
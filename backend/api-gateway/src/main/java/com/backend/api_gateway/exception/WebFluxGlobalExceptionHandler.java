// api-gateway/src/main/java/com/backend/api_gateway/exception/WebFluxGlobalExceptionHandler.java

package com.backend.api_gateway.exception;

import com.backend.api_gateway.exception.CustomException;
import com.backend.api_gateway.exception.InvalidFileTypeException;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.NotFoundException;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;

@ControllerAdvice
public class WebFluxGlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        Map<String, Object> body = new HashMap<>();

        if (ex instanceof CustomException c) {
            status = c.getStatus();
            body.put("message", c.getMessage());
        } else if (ex instanceof MethodArgumentNotValidException m) {
            status = HttpStatus.BAD_REQUEST;
            Map<String, String> errors = new HashMap<>();
            m.getBindingResult().getFieldErrors().forEach(e ->
                    errors.put(e.getField(), e.getDefaultMessage())
            );
            body.put("errors", errors);
        } else if (ex instanceof ConstraintViolationException c) {
            status = HttpStatus.BAD_REQUEST;
            Map<String, String> errors = new HashMap<>();
            c.getConstraintViolations().forEach(v ->
                    errors.put(v.getPropertyPath().toString(), v.getMessage())
            );
            body.put("errors", errors);
        } else if (ex instanceof WebExchangeBindException w) {
            status = HttpStatus.BAD_REQUEST;
            Map<String, String> errors = new HashMap<>();
            w.getFieldErrors().forEach(e ->
                    errors.put(e.getField(), e.getDefaultMessage())
            );
            body.put("errors", errors);
        } else if (ex instanceof MaxUploadSizeExceededException) {
            status = HttpStatus.PAYLOAD_TOO_LARGE;
            body.put("message", "File size exceeds 2 MB limit.");
        } else if (ex instanceof InvalidFileTypeException i) {
            status = HttpStatus.BAD_REQUEST;
            body.put("message", i.getMessage());
        } else if (ex instanceof NotFoundException) {
            status = HttpStatus.NOT_FOUND;
            body.put("message", "Endpoint not found");
        } else {
            System.err.println("Unexpected error: " + ex.getMessage());
            body.put("message", "Unexpected error.");
        }

        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            return exchange.getResponse()
                    .writeWith(Mono.just(exchange.getResponse()
                            .bufferFactory().wrap(
                                    new com.fasterxml.jackson.databind.ObjectMapper()
                                            .writeValueAsBytes(body)
                            )));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
package com.faijan.interviewhub.controller;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return body(400, "Validation failed", "One or more fields are invalid", Map.of("fieldErrors", fields));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> constraintViolation(ConstraintViolationException ex) {
        return body(400, "Validation failed", "One or more request parameters are invalid", Map.of("violations", ex.getConstraintViolations().stream().map(v -> v.getPropertyPath() + ": " + v.getMessage()).toList()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> unreadableRequest(Exception ex) {
        return body(400, "Bad Request", "The request body or parameter has an invalid format", Map.of());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> status(ResponseStatusException ex) {
        int code = ex.getStatusCode().value();
        return ResponseEntity.status(ex.getStatusCode()).body(body(code, ex.getStatusCode().toString(), ex.getReason() == null ? "Request failed" : ex.getReason(), Map.of()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> conflict(DataIntegrityViolationException ex) {
        return body(409, "Conflict", "The change conflicts with an existing record", Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> badRequest(IllegalArgumentException ex) {
        return body(400, "Bad Request", ex.getMessage(), Map.of());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> internal(Exception ex) {
        return body(500, "Internal Server Error", "An unexpected error occurred", Map.of());
    }

    private Map<String, Object> body(int status, String error, String message, Map<String, Object> extra) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("timestamp", Instant.now());
        out.put("status", status);
        out.put("error", error);
        out.put("message", message);
        out.putAll(extra);
        return out;
    }
}

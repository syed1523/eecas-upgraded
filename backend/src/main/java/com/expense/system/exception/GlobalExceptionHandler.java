package com.expense.system.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
                Map<String, Object> errors = new HashMap<>();
                ex.getBindingResult().getFieldErrors()
                                .forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
                errors.put("status", 400);
                errors.put("message", "Validation failed");
                return ResponseEntity.badRequest().body(errors);
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
                return buildError(HttpStatus.FORBIDDEN, ex.getMessage(), ex.getClass().getSimpleName());
        }

        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex) {
                return buildError(HttpStatus.UNAUTHORIZED, ex.getMessage(), ex.getClass().getSimpleName());
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
                return buildError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ex.getClass().getSimpleName());
        }

        private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String message, String type) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", status.value());
                error.put("message", message);
                error.put("type", type);
                return ResponseEntity.status(status).body(error);
        }
}

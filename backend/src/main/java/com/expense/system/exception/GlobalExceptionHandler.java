package com.expense.system.exception;

import org.springframework.http.ResponseEntity;
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

        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
                Map<String, Object> error = new HashMap<>();
                error.put("status", 500);
                error.put("message", ex.getMessage());
                error.put("type", ex.getClass().getSimpleName());
                return ResponseEntity.status(500).body(error);
        }
}

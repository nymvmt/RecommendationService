package com.example.recommendation.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.cors.CorsException;

import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(CorsException.class)
    public ResponseEntity<Map<String, Object>> handleCorsException(CorsException e) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", "CorsException");
        body.put("message", "CORS policy violation: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handle(Exception e) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", e.getClass().getSimpleName());
        body.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
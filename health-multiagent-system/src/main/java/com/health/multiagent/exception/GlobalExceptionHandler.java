package com.health.multiagent.exception;

import com.health.multiagent.model.AssessmentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler global de exceções para a API
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Trata erros de validação
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AssessmentResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation errors: {}", errors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(AssessmentResponse.builder()
                .status("ERROR")
                .message("Validation failed")
                .data(errors)
                .build());
    }
    
    /**
     * Trata exceções gerais
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AssessmentResponse> handleGeneralException(Exception ex) {
        log.error("Unexpected error", ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(AssessmentResponse.builder()
                .status("ERROR")
                .message("An unexpected error occurred: " + ex.getMessage())
                .build());
    }
}


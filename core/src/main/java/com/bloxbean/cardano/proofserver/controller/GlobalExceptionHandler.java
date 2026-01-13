package com.bloxbean.cardano.proofserver.controller;

import com.bloxbean.cardano.proofserver.dto.ErrorResponse;
import com.bloxbean.cardano.proofserver.exception.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the application.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TrieNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTrieNotFound(TrieNotFoundException ex) {
        log.warn("Trie not found: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .code("TRIE_NOT_FOUND")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DuplicateTrieException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateTrie(DuplicateTrieException ex) {
        log.warn("Duplicate trie: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .code("DUPLICATE_TRIE")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(UnsupportedTrieTypeException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedTrieType(UnsupportedTrieTypeException ex) {
        log.warn("Unsupported trie type: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .code("UNSUPPORTED_TRIE_TYPE")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ProviderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProviderNotFound(ProviderNotFoundException ex) {
        log.warn("Provider not found: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .code("PROVIDER_NOT_FOUND")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DataProviderException.class)
    public ResponseEntity<ErrorResponse> handleDataProviderException(DataProviderException ex) {
        log.error("Data provider error: {}", ex.getMessage(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .code("DATA_PROVIDER_ERROR")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(ProofGenerationException.class)
    public ResponseEntity<ErrorResponse> handleProofGenerationException(ProofGenerationException ex) {
        log.error("Proof generation error: {}", ex.getMessage(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .code("PROOF_GENERATION_ERROR")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(TrieOperationException.class)
    public ResponseEntity<ErrorResponse> handleTrieOperationException(TrieOperationException ex) {
        log.error("Trie operation error: {}", ex.getMessage(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .code("TRIE_OPERATION_ERROR")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(SerializationException.class)
    public ResponseEntity<ErrorResponse> handleSerializationException(SerializationException ex) {
        log.error("Serialization error: {}", ex.getMessage(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .code("SERIALIZATION_ERROR")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        ErrorResponse.Builder errorBuilder = ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message("Validation failed");

        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String field = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errorBuilder.fieldError(field, message);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBuilder.build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        ErrorResponse.Builder errorBuilder = ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message("Validation failed");

        ex.getBindingResult().getFieldErrors().forEach(fieldError -> {
            errorBuilder.fieldError(fieldError.getField(), fieldError.getDefaultMessage());
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBuilder.build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .code("INVALID_ARGUMENT")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .code("INTERNAL_ERROR")
                .message("An unexpected error occurred")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

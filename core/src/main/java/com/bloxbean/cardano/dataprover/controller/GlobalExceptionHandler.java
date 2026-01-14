package com.bloxbean.cardano.dataprover.controller;

import com.bloxbean.cardano.dataprover.dto.ErrorResponse;
import com.bloxbean.cardano.dataprover.exception.*;
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

    @ExceptionHandler(MerkleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMerkleNotFound(MerkleNotFoundException ex) {
        log.warn("Merkle not found: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .code("MERKLE_NOT_FOUND")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DuplicateMerkleException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateMerkle(DuplicateMerkleException ex) {
        log.warn("Duplicate merkle: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .code("DUPLICATE_MERKLE")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(UnsupportedMerkleSchemeException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMerkleScheme(UnsupportedMerkleSchemeException ex) {
        log.warn("Unsupported merkle scheme: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .code("UNSUPPORTED_MERKLE_SCHEME")
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

    @ExceptionHandler(MerkleOperationException.class)
    public ResponseEntity<ErrorResponse> handleMerkleOperationException(MerkleOperationException ex) {
        log.error("Merkle operation error: {}", ex.getMessage(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .code("MERKLE_OPERATION_ERROR")
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

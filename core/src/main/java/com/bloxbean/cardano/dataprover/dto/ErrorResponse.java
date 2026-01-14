package com.bloxbean.cardano.dataprover.dto;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Standard error response DTO.
 */
public class ErrorResponse {

    private String code;
    private String message;
    private Map<String, String> fieldErrors;
    private Instant timestamp;

    public ErrorResponse() {
        this.timestamp = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public static class Builder {
        private final ErrorResponse response = new ErrorResponse();

        public Builder code(String code) {
            response.setCode(code);
            return this;
        }

        public Builder message(String message) {
            response.setMessage(message);
            return this;
        }

        public Builder fieldError(String field, String error) {
            if (response.getFieldErrors() == null) {
                response.setFieldErrors(new HashMap<>());
            }
            response.getFieldErrors().put(field, error);
            return this;
        }

        public ErrorResponse build() {
            return response;
        }
    }
}

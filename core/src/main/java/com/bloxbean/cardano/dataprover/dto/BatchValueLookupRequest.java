package com.bloxbean.cardano.dataprover.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO for batch value lookup.
 */
public class BatchValueLookupRequest {

    @NotEmpty(message = "Keys list cannot be empty")
    private List<String> keys;

    public BatchValueLookupRequest() {
    }

    public BatchValueLookupRequest(List<String> keys) {
        this.keys = keys;
    }

    public List<String> getKeys() {
        return keys;
    }

    public void setKeys(List<String> keys) {
        this.keys = keys;
    }
}

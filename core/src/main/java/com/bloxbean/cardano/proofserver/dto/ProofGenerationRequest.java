package com.bloxbean.cardano.proofserver.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for proof generation.
 */
public class ProofGenerationRequest {

    @NotBlank(message = "Key is required")
    private String key;

    private String format = "wire";

    public ProofGenerationRequest() {
    }

    public ProofGenerationRequest(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}

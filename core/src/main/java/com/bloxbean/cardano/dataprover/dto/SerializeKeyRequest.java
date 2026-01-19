package com.bloxbean.cardano.dataprover.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to serialize a domain key to hex format.
 */
public class SerializeKeyRequest {

    @NotBlank(message = "Key is required")
    private String key;

    public SerializeKeyRequest() {
    }

    public SerializeKeyRequest(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}

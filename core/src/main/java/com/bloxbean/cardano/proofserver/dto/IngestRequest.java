package com.bloxbean.cardano.proofserver.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.HashMap;
import java.util.Map;

/**
 * Request DTO for data ingestion.
 */
public class IngestRequest {

    @NotBlank(message = "Provider name is required")
    private String provider;

    private Map<String, Object> config = new HashMap<>();

    public IngestRequest() {
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }
}

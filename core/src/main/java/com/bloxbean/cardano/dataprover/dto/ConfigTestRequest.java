package com.bloxbean.cardano.dataprover.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request DTO for testing provider configuration.
 */
public class ConfigTestRequest {

    @NotNull(message = "Configuration values are required")
    private Map<String, Object> config;

    public ConfigTestRequest() {
    }

    public ConfigTestRequest(Map<String, Object> config) {
        this.config = config;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }
}

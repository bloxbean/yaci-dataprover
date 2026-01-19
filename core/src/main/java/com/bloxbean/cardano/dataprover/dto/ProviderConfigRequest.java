package com.bloxbean.cardano.dataprover.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request DTO for saving provider configuration.
 */
public class ProviderConfigRequest {

    @NotNull(message = "Configuration values are required")
    private Map<String, Object> config;

    public ProviderConfigRequest() {
    }

    public ProviderConfigRequest(Map<String, Object> config) {
        this.config = config;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }
}

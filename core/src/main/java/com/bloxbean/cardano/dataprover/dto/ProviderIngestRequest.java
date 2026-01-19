package com.bloxbean.cardano.dataprover.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.HashMap;
import java.util.Map;

/**
 * Request to ingest data using a provider with optional auto-create merkle.
 */
public class ProviderIngestRequest {

    @NotBlank(message = "Merkle name is required")
    @Pattern(regexp = "^[a-z0-9]([a-z0-9-]{1,62}[a-z0-9])?$",
            message = "Merkle name must be 3-64 lowercase alphanumeric characters or hyphens, cannot start/end with hyphen")
    private String merkleName;

    private boolean createIfNotExists = true;

    private String merkleScheme = "mpf";

    private String merkleDescription;

    @NotBlank(message = "Provider name is required")
    private String provider;

    private Map<String, Object> config = new HashMap<>();

    public ProviderIngestRequest() {
    }

    public String getMerkleName() {
        return merkleName;
    }

    public void setMerkleName(String merkleName) {
        this.merkleName = merkleName;
    }

    public boolean isCreateIfNotExists() {
        return createIfNotExists;
    }

    public void setCreateIfNotExists(boolean createIfNotExists) {
        this.createIfNotExists = createIfNotExists;
    }

    public String getMerkleScheme() {
        return merkleScheme;
    }

    public void setMerkleScheme(String merkleScheme) {
        this.merkleScheme = merkleScheme;
    }

    public String getMerkleDescription() {
        return merkleDescription;
    }

    public void setMerkleDescription(String merkleDescription) {
        this.merkleDescription = merkleDescription;
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

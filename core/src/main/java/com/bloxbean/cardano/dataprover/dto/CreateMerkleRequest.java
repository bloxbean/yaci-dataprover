package com.bloxbean.cardano.dataprover.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.HashMap;
import java.util.Map;

/**
 * Request DTO for creating a new merkle.
 */
public class CreateMerkleRequest {

    @NotBlank(message = "Merkle identifier is required")
    @Pattern(regexp = "^[a-z0-9]([a-z0-9-]{1,62}[a-z0-9])?$",
             message = "Identifier must be 3-64 chars, lowercase letters/numbers/hyphens only")
    private String identifier;

    private String scheme = "mpf";

    private String description;

    private Map<String, Object> metadata = new HashMap<>();

    public CreateMerkleRequest() {
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}

package com.bloxbean.cardano.proofserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.HashMap;
import java.util.Map;

/**
 * Request DTO for creating a new trie.
 */
public class CreateTrieRequest {

    @NotBlank(message = "Trie identifier is required")
    @Pattern(regexp = "^[a-z0-9]([a-z0-9-]{1,62}[a-z0-9])?$",
             message = "Identifier must be 3-64 chars, lowercase letters/numbers/hyphens only")
    private String identifier;

    private String trieType = "mpf";

    private String description;

    private Map<String, Object> metadata = new HashMap<>();

    public CreateTrieRequest() {
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getTrieType() {
        return trieType;
    }

    public void setTrieType(String trieType) {
        this.trieType = trieType;
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

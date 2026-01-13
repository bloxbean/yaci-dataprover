package com.bloxbean.cardano.proofserver.dto;

import com.bloxbean.cardano.proofserver.model.TrieMetadata;
import com.bloxbean.cardano.proofserver.model.TrieStatus;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for trie information.
 */
public class TrieResponse {

    private String identifier;
    private String trieType;
    private String rootHash;
    private Integer recordCount;
    private TrieStatus status;
    private Instant createdAt;
    private Instant lastUpdated;
    private String description;
    private Map<String, Object> metadata;

    public TrieResponse() {
    }

    public static TrieResponse from(TrieMetadata metadata) {
        TrieResponse response = new TrieResponse();
        response.setIdentifier(metadata.getIdentifier());
        response.setTrieType(metadata.getTrieType());
        response.setRootHash(metadata.getRootHash());
        response.setRecordCount(metadata.getRecordCount());
        response.setStatus(metadata.getStatus());
        response.setCreatedAt(metadata.getCreatedAt());
        response.setLastUpdated(metadata.getLastUpdated());
        response.setMetadata(metadata.getMetadata());

        if (metadata.getMetadata() != null && metadata.getMetadata().containsKey("description")) {
            response.setDescription((String) metadata.getMetadata().get("description"));
        }

        return response;
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

    public String getRootHash() {
        return rootHash;
    }

    public void setRootHash(String rootHash) {
        this.rootHash = rootHash;
    }

    public Integer getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(Integer recordCount) {
        this.recordCount = recordCount;
    }

    public TrieStatus getStatus() {
        return status;
    }

    public void setStatus(TrieStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
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

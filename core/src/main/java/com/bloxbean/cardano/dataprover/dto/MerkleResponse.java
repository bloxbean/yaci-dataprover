package com.bloxbean.cardano.dataprover.dto;

import com.bloxbean.cardano.dataprover.model.MerkleMetadata;
import com.bloxbean.cardano.dataprover.model.MerkleStatus;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for merkle information.
 */
public class MerkleResponse {

    private String identifier;
    private String scheme;
    private String rootHash;
    private MerkleStatus status;
    private Instant createdAt;
    private Instant lastUpdated;
    private String description;
    private Map<String, Object> metadata;
    private Boolean storeOriginalKeys;

    public MerkleResponse() {
    }

    public static MerkleResponse from(MerkleMetadata metadata) {
        MerkleResponse response = new MerkleResponse();
        response.setIdentifier(metadata.getIdentifier());
        response.setScheme(metadata.getScheme());
        response.setRootHash(metadata.getRootHash());
        response.setStatus(metadata.getStatus());
        response.setCreatedAt(metadata.getCreatedAt());
        response.setLastUpdated(metadata.getLastUpdated());
        response.setMetadata(metadata.getMetadata());
        response.setStoreOriginalKeys(metadata.getStoreOriginalKeys());

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

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getRootHash() {
        return rootHash;
    }

    public void setRootHash(String rootHash) {
        this.rootHash = rootHash;
    }

    public MerkleStatus getStatus() {
        return status;
    }

    public void setStatus(MerkleStatus status) {
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

    public Boolean getStoreOriginalKeys() {
        return storeOriginalKeys;
    }

    public void setStoreOriginalKeys(Boolean storeOriginalKeys) {
        this.storeOriginalKeys = storeOriginalKeys;
    }
}

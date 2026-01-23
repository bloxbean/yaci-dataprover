package com.bloxbean.cardano.dataprover.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing metadata for a merkle instance.
 * Stores information about the merkle's configuration, status, and statistics.
 */
@Entity
@Table(name = "merkle_metadata")
public class MerkleMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String identifier;

    @Column(name = "scheme", nullable = false, length = 20)
    private String scheme;

    @Column(name = "root_hash", length = 128)
    private String rootHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    @Convert(converter = JsonMapConverter.class)
    @Column(name = "metadata", columnDefinition = "TEXT")
    private Map<String, Object> metadata;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MerkleStatus status;

    @Version
    private Long version;

    @Column(name = "store_original_keys", nullable = false)
    private Boolean storeOriginalKeys = false;

    public MerkleMetadata() {
        this.scheme = "mpf";
        this.status = MerkleStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.metadata = new HashMap<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public MerkleStatus getStatus() {
        return status;
    }

    public void setStatus(MerkleStatus status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Boolean getStoreOriginalKeys() {
        return storeOriginalKeys;
    }

    public void setStoreOriginalKeys(Boolean storeOriginalKeys) {
        this.storeOriginalKeys = storeOriginalKeys;
    }

    public void touch() {
        this.lastUpdated = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final MerkleMetadata metadata = new MerkleMetadata();

        public Builder identifier(String identifier) {
            metadata.setIdentifier(identifier);
            return this;
        }

        public Builder scheme(String scheme) {
            metadata.setScheme(scheme);
            return this;
        }

        public Builder rootHash(String rootHash) {
            metadata.setRootHash(rootHash);
            return this;
        }

        public Builder status(MerkleStatus status) {
            metadata.setStatus(status);
            return this;
        }

        public Builder customMetadata(Map<String, Object> customMetadata) {
            metadata.setMetadata(new HashMap<>(customMetadata));
            return this;
        }

        public Builder addCustomMetadata(String key, Object value) {
            metadata.getMetadata().put(key, value);
            return this;
        }

        public Builder storeOriginalKeys(Boolean storeOriginalKeys) {
            metadata.setStoreOriginalKeys(storeOriginalKeys != null ? storeOriginalKeys : false);
            return this;
        }

        public MerkleMetadata build() {
            return metadata;
        }
    }
}

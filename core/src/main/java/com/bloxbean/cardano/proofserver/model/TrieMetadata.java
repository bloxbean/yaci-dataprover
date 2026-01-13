package com.bloxbean.cardano.proofserver.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing metadata for a trie instance.
 * Stores information about the trie's configuration, status, and statistics.
 */
@Entity
@Table(name = "trie_metadata")
public class TrieMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String identifier;

    @Column(name = "trie_type", nullable = false, length = 20)
    private String trieType;

    @Column(name = "root_hash", length = 128)
    private String rootHash;

    @Column(name = "record_count", nullable = false)
    private Integer recordCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    @Convert(converter = JsonMapConverter.class)
    @Column(name = "metadata", columnDefinition = "TEXT")
    private Map<String, Object> metadata;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrieStatus status;

    @Version
    private Long version;

    public TrieMetadata() {
        this.trieType = "mpf";
        this.recordCount = 0;
        this.status = TrieStatus.ACTIVE;
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

    public TrieStatus getStatus() {
        return status;
    }

    public void setStatus(TrieStatus status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public void touch() {
        this.lastUpdated = Instant.now();
    }

    public void incrementRecordCount(int delta) {
        this.recordCount += delta;
        touch();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final TrieMetadata metadata = new TrieMetadata();

        public Builder identifier(String identifier) {
            metadata.setIdentifier(identifier);
            return this;
        }

        public Builder trieType(String trieType) {
            metadata.setTrieType(trieType);
            return this;
        }

        public Builder rootHash(String rootHash) {
            metadata.setRootHash(rootHash);
            return this;
        }

        public Builder recordCount(Integer recordCount) {
            metadata.setRecordCount(recordCount);
            return this;
        }

        public Builder status(TrieStatus status) {
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

        public TrieMetadata build() {
            return metadata;
        }
    }
}

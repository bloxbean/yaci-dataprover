package com.bloxbean.cardano.dataprover.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Response from provider ingestion with auto-create merkle support.
 */
public class ProviderIngestResponse {
    private String merkleIdentifier;
    private boolean merkleCreated;
    private String provider;
    private int recordsProcessed;
    private int recordsSkipped;
    private String rootHash;
    private long durationMs;
    private List<String> errors = new ArrayList<>();

    public ProviderIngestResponse() {
    }

    private ProviderIngestResponse(Builder builder) {
        this.merkleIdentifier = builder.merkleIdentifier;
        this.merkleCreated = builder.merkleCreated;
        this.provider = builder.provider;
        this.recordsProcessed = builder.recordsProcessed;
        this.recordsSkipped = builder.recordsSkipped;
        this.rootHash = builder.rootHash;
        this.durationMs = builder.durationMs;
        this.errors = builder.errors;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getMerkleIdentifier() {
        return merkleIdentifier;
    }

    public void setMerkleIdentifier(String merkleIdentifier) {
        this.merkleIdentifier = merkleIdentifier;
    }

    public boolean isMerkleCreated() {
        return merkleCreated;
    }

    public void setMerkleCreated(boolean merkleCreated) {
        this.merkleCreated = merkleCreated;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public int getRecordsProcessed() {
        return recordsProcessed;
    }

    public void setRecordsProcessed(int recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }

    public int getRecordsSkipped() {
        return recordsSkipped;
    }

    public void setRecordsSkipped(int recordsSkipped) {
        this.recordsSkipped = recordsSkipped;
    }

    public String getRootHash() {
        return rootHash;
    }

    public void setRootHash(String rootHash) {
        this.rootHash = rootHash;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public static class Builder {
        private String merkleIdentifier;
        private boolean merkleCreated;
        private String provider;
        private int recordsProcessed;
        private int recordsSkipped;
        private String rootHash;
        private long durationMs;
        private List<String> errors = new ArrayList<>();

        public Builder merkleIdentifier(String merkleIdentifier) {
            this.merkleIdentifier = merkleIdentifier;
            return this;
        }

        public Builder merkleCreated(boolean merkleCreated) {
            this.merkleCreated = merkleCreated;
            return this;
        }

        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder recordsProcessed(int recordsProcessed) {
            this.recordsProcessed = recordsProcessed;
            return this;
        }

        public Builder recordsSkipped(int recordsSkipped) {
            this.recordsSkipped = recordsSkipped;
            return this;
        }

        public Builder rootHash(String rootHash) {
            this.rootHash = rootHash;
            return this;
        }

        public Builder durationMs(long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public Builder errors(List<String> errors) {
            this.errors = errors;
            return this;
        }

        public Builder addError(String error) {
            this.errors.add(error);
            return this;
        }

        public ProviderIngestResponse build() {
            return new ProviderIngestResponse(this);
        }
    }
}

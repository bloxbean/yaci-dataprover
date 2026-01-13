package com.bloxbean.cardano.proofserver.dto;

import java.util.List;

/**
 * Response DTO for data ingestion.
 */
public class IngestResponse {

    private String trieIdentifier;
    private String provider;
    private Integer recordsProcessed;
    private Integer recordsSkipped;
    private String rootHash;
    private Long durationMs;
    private List<String> errors;

    public IngestResponse() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTrieIdentifier() {
        return trieIdentifier;
    }

    public void setTrieIdentifier(String trieIdentifier) {
        this.trieIdentifier = trieIdentifier;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Integer getRecordsProcessed() {
        return recordsProcessed;
    }

    public void setRecordsProcessed(Integer recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }

    public Integer getRecordsSkipped() {
        return recordsSkipped;
    }

    public void setRecordsSkipped(Integer recordsSkipped) {
        this.recordsSkipped = recordsSkipped;
    }

    public String getRootHash() {
        return rootHash;
    }

    public void setRootHash(String rootHash) {
        this.rootHash = rootHash;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public static class Builder {
        private final IngestResponse response = new IngestResponse();

        public Builder trieIdentifier(String trieIdentifier) {
            response.setTrieIdentifier(trieIdentifier);
            return this;
        }

        public Builder provider(String provider) {
            response.setProvider(provider);
            return this;
        }

        public Builder recordsProcessed(Integer recordsProcessed) {
            response.setRecordsProcessed(recordsProcessed);
            return this;
        }

        public Builder recordsSkipped(Integer recordsSkipped) {
            response.setRecordsSkipped(recordsSkipped);
            return this;
        }

        public Builder rootHash(String rootHash) {
            response.setRootHash(rootHash);
            return this;
        }

        public Builder durationMs(Long durationMs) {
            response.setDurationMs(durationMs);
            return this;
        }

        public Builder errors(List<String> errors) {
            response.setErrors(errors);
            return this;
        }

        public IngestResponse build() {
            return response;
        }
    }
}

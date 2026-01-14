package com.bloxbean.cardano.dataprover.dto;

import java.util.List;

/**
 * Response DTO for direct entry addition.
 */
public class AddEntriesResponse {

    private String merkleIdentifier;
    private Integer entriesAdded;
    private Integer entriesSkipped;
    private String rootHash;
    private Long durationMs;
    private List<String> errors;

    public AddEntriesResponse() {
    }

    public String getMerkleIdentifier() {
        return merkleIdentifier;
    }

    public void setMerkleIdentifier(String merkleIdentifier) {
        this.merkleIdentifier = merkleIdentifier;
    }

    public Integer getEntriesAdded() {
        return entriesAdded;
    }

    public void setEntriesAdded(Integer entriesAdded) {
        this.entriesAdded = entriesAdded;
    }

    public Integer getEntriesSkipped() {
        return entriesSkipped;
    }

    public void setEntriesSkipped(Integer entriesSkipped) {
        this.entriesSkipped = entriesSkipped;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final AddEntriesResponse response = new AddEntriesResponse();

        public Builder merkleIdentifier(String merkleIdentifier) {
            response.setMerkleIdentifier(merkleIdentifier);
            return this;
        }

        public Builder entriesAdded(Integer entriesAdded) {
            response.setEntriesAdded(entriesAdded);
            return this;
        }

        public Builder entriesSkipped(Integer entriesSkipped) {
            response.setEntriesSkipped(entriesSkipped);
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

        public AddEntriesResponse build() {
            return response;
        }
    }
}

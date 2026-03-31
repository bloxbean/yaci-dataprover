package com.bloxbean.cardano.dataprover.dto;

import java.util.List;

/**
 * Response DTO for merkle tree entries list.
 */
public class MerkleEntriesResponse {

    private String merkleIdentifier;
    private List<MerkleEntryResponse> entries;
    private int totalReturned;
    private boolean hasMore;
    private long computationTimeMs;

    public MerkleEntriesResponse() {
    }

    public MerkleEntriesResponse(String merkleIdentifier, List<MerkleEntryResponse> entries,
                                 int totalReturned, boolean hasMore, long computationTimeMs) {
        this.merkleIdentifier = merkleIdentifier;
        this.entries = entries;
        this.totalReturned = totalReturned;
        this.hasMore = hasMore;
        this.computationTimeMs = computationTimeMs;
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

    public List<MerkleEntryResponse> getEntries() {
        return entries;
    }

    public void setEntries(List<MerkleEntryResponse> entries) {
        this.entries = entries;
    }

    public int getTotalReturned() {
        return totalReturned;
    }

    public void setTotalReturned(int totalReturned) {
        this.totalReturned = totalReturned;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public long getComputationTimeMs() {
        return computationTimeMs;
    }

    public void setComputationTimeMs(long computationTimeMs) {
        this.computationTimeMs = computationTimeMs;
    }

    public static class Builder {
        private String merkleIdentifier;
        private List<MerkleEntryResponse> entries;
        private int totalReturned;
        private boolean hasMore;
        private long computationTimeMs;

        public Builder merkleIdentifier(String merkleIdentifier) {
            this.merkleIdentifier = merkleIdentifier;
            return this;
        }

        public Builder entries(List<MerkleEntryResponse> entries) {
            this.entries = entries;
            return this;
        }

        public Builder totalReturned(int totalReturned) {
            this.totalReturned = totalReturned;
            return this;
        }

        public Builder hasMore(boolean hasMore) {
            this.hasMore = hasMore;
            return this;
        }

        public Builder computationTimeMs(long computationTimeMs) {
            this.computationTimeMs = computationTimeMs;
            return this;
        }

        public MerkleEntriesResponse build() {
            return new MerkleEntriesResponse(merkleIdentifier, entries, totalReturned, hasMore, computationTimeMs);
        }
    }
}

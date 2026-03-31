package com.bloxbean.cardano.dataprover.dto;

/**
 * Response DTO for computed merkle tree size.
 */
public class MerkleSizeResponse {

    private String merkleIdentifier;
    private long size;
    private long computationTimeMs;

    public MerkleSizeResponse() {
    }

    public MerkleSizeResponse(String merkleIdentifier, long size, long computationTimeMs) {
        this.merkleIdentifier = merkleIdentifier;
        this.size = size;
        this.computationTimeMs = computationTimeMs;
    }

    public static MerkleSizeResponse of(String merkleIdentifier, long size, long computationTimeMs) {
        return new MerkleSizeResponse(merkleIdentifier, size, computationTimeMs);
    }

    public String getMerkleIdentifier() {
        return merkleIdentifier;
    }

    public void setMerkleIdentifier(String merkleIdentifier) {
        this.merkleIdentifier = merkleIdentifier;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getComputationTimeMs() {
        return computationTimeMs;
    }

    public void setComputationTimeMs(long computationTimeMs) {
        this.computationTimeMs = computationTimeMs;
    }
}

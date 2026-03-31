package com.bloxbean.cardano.dataprover.dto;

/**
 * Response DTO for a single merkle tree entry.
 */
public class MerkleEntryResponse {

    private String originalKey;  // null if not stored
    private String hashedKey;    // hex encoded
    private String value;        // hex encoded

    public MerkleEntryResponse() {
    }

    public MerkleEntryResponse(String originalKey, String hashedKey, String value) {
        this.originalKey = originalKey;
        this.hashedKey = hashedKey;
        this.value = value;
    }

    public static MerkleEntryResponse of(byte[] originalKey, byte[] hashedKey, byte[] value) {
        return new MerkleEntryResponse(
            originalKey != null ? bytesToHex(originalKey) : null,
            bytesToHex(hashedKey),
            bytesToHex(value)
        );
    }

    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) return null;
        StringBuilder sb = new StringBuilder("0x");
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public String getOriginalKey() {
        return originalKey;
    }

    public void setOriginalKey(String originalKey) {
        this.originalKey = originalKey;
    }

    public String getHashedKey() {
        return hashedKey;
    }

    public void setHashedKey(String hashedKey) {
        this.hashedKey = hashedKey;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

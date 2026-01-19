package com.bloxbean.cardano.dataprover.dto;

/**
 * Response containing serialized key in hex format.
 */
public class SerializeKeyResponse {
    private String originalKey;
    private String serializedKeyHex;
    private int keyLength;

    public SerializeKeyResponse() {
    }

    private SerializeKeyResponse(Builder builder) {
        this.originalKey = builder.originalKey;
        this.serializedKeyHex = builder.serializedKeyHex;
        this.keyLength = builder.keyLength;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getOriginalKey() {
        return originalKey;
    }

    public void setOriginalKey(String originalKey) {
        this.originalKey = originalKey;
    }

    public String getSerializedKeyHex() {
        return serializedKeyHex;
    }

    public void setSerializedKeyHex(String serializedKeyHex) {
        this.serializedKeyHex = serializedKeyHex;
    }

    public int getKeyLength() {
        return keyLength;
    }

    public void setKeyLength(int keyLength) {
        this.keyLength = keyLength;
    }

    public static class Builder {
        private String originalKey;
        private String serializedKeyHex;
        private int keyLength;

        public Builder originalKey(String originalKey) {
            this.originalKey = originalKey;
            return this;
        }

        public Builder serializedKeyHex(String serializedKeyHex) {
            this.serializedKeyHex = serializedKeyHex;
            return this;
        }

        public Builder keyLength(int keyLength) {
            this.keyLength = keyLength;
            return this;
        }

        public SerializeKeyResponse build() {
            return new SerializeKeyResponse(this);
        }
    }
}

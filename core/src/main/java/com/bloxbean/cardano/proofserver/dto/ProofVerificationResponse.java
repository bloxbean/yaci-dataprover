package com.bloxbean.cardano.proofserver.dto;

/**
 * Response DTO for proof verification.
 */
public class ProofVerificationResponse {

    private String key;
    private String value;
    private Boolean verified;
    private String rootHash;

    public ProofVerificationResponse() {
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public String getRootHash() {
        return rootHash;
    }

    public void setRootHash(String rootHash) {
        this.rootHash = rootHash;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ProofVerificationResponse response = new ProofVerificationResponse();

        public Builder key(String key) {
            response.setKey(key);
            return this;
        }

        public Builder value(String value) {
            response.setValue(value);
            return this;
        }

        public Builder verified(Boolean verified) {
            response.setVerified(verified);
            return this;
        }

        public Builder rootHash(String rootHash) {
            response.setRootHash(rootHash);
            return this;
        }

        public ProofVerificationResponse build() {
            return response;
        }
    }
}

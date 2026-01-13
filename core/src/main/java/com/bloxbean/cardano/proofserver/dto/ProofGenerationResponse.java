package com.bloxbean.cardano.proofserver.dto;

/**
 * Response DTO for proof generation.
 */
public class ProofGenerationResponse {

    private String key;
    private String value;
    private String proof;
    private String rootHash;
    private String proofFormat;

    public ProofGenerationResponse() {
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

    public String getProof() {
        return proof;
    }

    public void setProof(String proof) {
        this.proof = proof;
    }

    public String getRootHash() {
        return rootHash;
    }

    public void setRootHash(String rootHash) {
        this.rootHash = rootHash;
    }

    public String getProofFormat() {
        return proofFormat;
    }

    public void setProofFormat(String proofFormat) {
        this.proofFormat = proofFormat;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ProofGenerationResponse response = new ProofGenerationResponse();

        public Builder key(String key) {
            response.setKey(key);
            return this;
        }

        public Builder value(String value) {
            response.setValue(value);
            return this;
        }

        public Builder proof(String proof) {
            response.setProof(proof);
            return this;
        }

        public Builder rootHash(String rootHash) {
            response.setRootHash(rootHash);
            return this;
        }

        public Builder proofFormat(String proofFormat) {
            response.setProofFormat(proofFormat);
            return this;
        }

        public ProofGenerationResponse build() {
            return response;
        }
    }
}

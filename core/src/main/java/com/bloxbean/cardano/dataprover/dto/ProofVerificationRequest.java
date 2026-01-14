package com.bloxbean.cardano.dataprover.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for proof verification.
 */
public class ProofVerificationRequest {

    @NotBlank(message = "Key is required")
    private String key;

    // Value can be null for exclusion proofs (proving non-existence)
    private String value;

    @NotBlank(message = "Proof is required")
    private String proof;

    @NotBlank(message = "Root hash is required")
    private String rootHash;

    public ProofVerificationRequest() {
    }

    public ProofVerificationRequest(String key, String value, String proof, String rootHash) {
        this.key = key;
        this.value = value;
        this.proof = proof;
        this.rootHash = rootHash;
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
}

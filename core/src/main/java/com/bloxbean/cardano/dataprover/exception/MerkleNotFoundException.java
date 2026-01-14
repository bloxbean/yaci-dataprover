package com.bloxbean.cardano.dataprover.exception;

/**
 * Exception thrown when a merkle structure is not found.
 */
public class MerkleNotFoundException extends RuntimeException {

    private final String identifier;

    public MerkleNotFoundException(String identifier) {
        super("Merkle not found: " + identifier);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}

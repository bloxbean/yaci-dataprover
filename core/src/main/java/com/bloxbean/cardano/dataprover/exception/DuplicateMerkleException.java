package com.bloxbean.cardano.dataprover.exception;

/**
 * Exception thrown when attempting to create a merkle with a duplicate identifier.
 */
public class DuplicateMerkleException extends RuntimeException {

    private final String identifier;

    public DuplicateMerkleException(String identifier) {
        super("Merkle already exists: " + identifier);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}

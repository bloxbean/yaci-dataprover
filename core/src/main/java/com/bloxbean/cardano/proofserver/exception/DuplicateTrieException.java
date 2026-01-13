package com.bloxbean.cardano.proofserver.exception;

/**
 * Exception thrown when attempting to create a trie with a duplicate identifier.
 */
public class DuplicateTrieException extends RuntimeException {

    private final String identifier;

    public DuplicateTrieException(String identifier) {
        super("Trie already exists: " + identifier);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}

package com.bloxbean.cardano.proofserver.exception;

/**
 * Exception thrown when a trie is not found.
 */
public class TrieNotFoundException extends RuntimeException {

    private final String identifier;

    public TrieNotFoundException(String identifier) {
        super("Trie not found: " + identifier);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}

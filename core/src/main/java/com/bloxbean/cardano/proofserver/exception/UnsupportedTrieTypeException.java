package com.bloxbean.cardano.proofserver.exception;

/**
 * Exception thrown when an unsupported trie type is requested.
 */
public class UnsupportedTrieTypeException extends RuntimeException {

    private final String trieType;

    public UnsupportedTrieTypeException(String trieType) {
        super("Unsupported trie type: " + trieType);
        this.trieType = trieType;
    }

    public String getTrieType() {
        return trieType;
    }
}

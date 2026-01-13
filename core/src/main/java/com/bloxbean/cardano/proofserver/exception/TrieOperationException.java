package com.bloxbean.cardano.proofserver.exception;

/**
 * Exception thrown when a trie operation fails.
 */
public class TrieOperationException extends RuntimeException {

    public TrieOperationException(String message) {
        super(message);
    }

    public TrieOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

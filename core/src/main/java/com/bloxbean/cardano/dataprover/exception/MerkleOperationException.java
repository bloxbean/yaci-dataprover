package com.bloxbean.cardano.dataprover.exception;

/**
 * Exception thrown when a merkle operation fails.
 */
public class MerkleOperationException extends RuntimeException {

    public MerkleOperationException(String message) {
        super(message);
    }

    public MerkleOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

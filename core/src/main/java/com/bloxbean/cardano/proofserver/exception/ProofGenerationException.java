package com.bloxbean.cardano.proofserver.exception;

/**
 * Exception thrown when proof generation fails.
 */
public class ProofGenerationException extends RuntimeException {

    public ProofGenerationException(String message) {
        super(message);
    }

    public ProofGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}

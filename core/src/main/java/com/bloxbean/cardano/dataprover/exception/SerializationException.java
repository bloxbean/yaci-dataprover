package com.bloxbean.cardano.dataprover.exception;

/**
 * Exception thrown when serialization/deserialization fails.
 */
public class SerializationException extends RuntimeException {

    public SerializationException(String message) {
        super(message);
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.bloxbean.cardano.dataprover.exception;

/**
 * Exception thrown when an unsupported merkle scheme is requested.
 */
public class UnsupportedMerkleSchemeException extends RuntimeException {

    private final String scheme;

    public UnsupportedMerkleSchemeException(String scheme) {
        super("Unsupported merkle scheme: " + scheme);
        this.scheme = scheme;
    }

    public String getScheme() {
        return scheme;
    }
}

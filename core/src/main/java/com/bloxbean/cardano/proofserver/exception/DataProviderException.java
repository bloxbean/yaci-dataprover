package com.bloxbean.cardano.proofserver.exception;

/**
 * Exception thrown when a data provider operation fails.
 */
public class DataProviderException extends RuntimeException {

    private final String providerName;

    public DataProviderException(String providerName, String message) {
        super(message);
        this.providerName = providerName;
    }

    public DataProviderException(String providerName, String message, Throwable cause) {
        super(message, cause);
        this.providerName = providerName;
    }

    public String getProviderName() {
        return providerName;
    }
}

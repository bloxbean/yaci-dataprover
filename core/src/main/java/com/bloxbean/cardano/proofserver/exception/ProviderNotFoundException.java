package com.bloxbean.cardano.proofserver.exception;

/**
 * Exception thrown when a data provider is not found.
 */
public class ProviderNotFoundException extends RuntimeException {

    private final String providerName;

    public ProviderNotFoundException(String providerName) {
        super("Provider not found: " + providerName);
        this.providerName = providerName;
    }

    public String getProviderName() {
        return providerName;
    }
}

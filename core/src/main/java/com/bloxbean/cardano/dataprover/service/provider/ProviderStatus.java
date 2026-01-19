package com.bloxbean.cardano.dataprover.service.provider;

/**
 * Status of a data provider.
 */
public enum ProviderStatus {
    /**
     * Provider is initialized and ready to use.
     */
    AVAILABLE,

    /**
     * Provider is loaded but not configured properly.
     */
    NOT_CONFIGURED,

    /**
     * Provider encountered an error during initialization.
     */
    ERROR
}

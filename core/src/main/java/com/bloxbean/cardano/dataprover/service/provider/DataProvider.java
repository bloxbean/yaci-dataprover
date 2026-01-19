package com.bloxbean.cardano.dataprover.service.provider;

import com.bloxbean.cardano.dataprover.exception.DataProviderException;
import com.bloxbean.cardano.dataprover.exception.SerializationException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Abstraction for data providers that fetch and serialize data for trie ingestion.
 *
 * Implementations provide domain-specific logic for:
 * - Fetching data from external sources
 * - Serializing data to key-value pairs
 * - Validating data quality
 *
 * For plugin-based providers, implement this interface and register via SPI.
 *
 * @param <T> the data type this provider handles
 */
public interface DataProvider<T> {

    /**
     * Returns the unique name of this provider (e.g., "epoch-stake", "json-file").
     */
    String getName();

    /**
     * Returns a human-readable description of this provider.
     */
    String getDescription();

    /**
     * Initializes the provider with configuration.
     * Called once when the provider is loaded from a plugin JAR.
     *
     * @param config provider-specific configuration (e.g., database URL, credentials)
     */
    default void initialize(Map<String, Object> config) {
        // Default implementation does nothing
    }

    /**
     * Fetches data from the configured source.
     *
     * @param config provider-specific fetch configuration (e.g., epoch number, file path)
     * @return list of data items
     * @throws DataProviderException if data fetch fails
     */
    List<T> fetchData(Map<String, Object> config) throws DataProviderException;

    /**
     * Serializes a data item to its key representation.
     *
     * @param data the data item
     * @return the key bytes
     * @throws SerializationException if serialization fails
     */
    byte[] serializeKey(T data) throws SerializationException;

    /**
     * Serializes a data item to its value representation.
     *
     * @param data the data item
     * @return the value bytes (typically CBOR-encoded)
     * @throws SerializationException if serialization fails
     */
    byte[] serializeValue(T data) throws SerializationException;

    /**
     * Validates a data item.
     *
     * @param data the data item to validate
     * @return validation result with errors if any
     */
    ValidationResult validate(T data);

    /**
     * Returns the data type this provider handles.
     * Used for type safety and reflection.
     *
     * @return the class of the data type
     */
    Class<T> getDataType();

    /**
     * Returns metadata about this provider for dynamic UI generation.
     * Override this method to provide configuration schema and key serialization schema.
     *
     * @return provider metadata including config schema
     */
    default ProviderMetadata getMetadata() {
        return ProviderMetadata.builder()
                .name(getName())
                .description(getDescription())
                .dataType(getDataType() != null ? getDataType().getSimpleName() : "Unknown")
                .status(ProviderStatus.AVAILABLE)
                .configSchema(ConfigSchema.builder().fields(Collections.emptyList()).build())
                .build();
    }

    /**
     * Serializes a raw key input (e.g., stake address string) to bytes.
     * Used by the UI to convert user-provided keys to hex format for proof generation.
     *
     * @param keyInput the raw key input from the user
     * @return the serialized key bytes
     * @throws SerializationException if serialization fails
     */
    default byte[] serializeKeyFromInput(String keyInput) throws SerializationException {
        throw new UnsupportedOperationException(
                "Key serialization from raw input is not supported by provider: " + getName());
    }

    /**
     * Returns the schema for connection configuration fields.
     * Override this to expose configurable settings (DB connections, API keys, etc.).
     *
     * @return connection config schema, or null if not configurable via UI
     */
    default ConnectionConfigSchema getConnectionConfigSchema() {
        return null;
    }

    /**
     * Reconfigures the provider with new settings.
     * Called when UI configuration is saved.
     *
     * @param config new configuration values
     * @return true if reconfiguration succeeded
     */
    default boolean reconfigure(Map<String, Object> config) {
        try {
            initialize(config);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tests configuration validity without saving.
     * Used by UI to validate settings before persisting.
     *
     * @param config configuration to test
     * @return test result with success/failure and message
     */
    default ConfigTestResult testConfiguration(Map<String, Object> config) {
        return ConfigTestResult.success();
    }
}

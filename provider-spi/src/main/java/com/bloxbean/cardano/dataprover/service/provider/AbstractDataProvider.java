package com.bloxbean.cardano.dataprover.service.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for data providers.
 * Provides common validation helpers and utilities.
 *
 * @param <T> the data type this provider handles
 */
public abstract class AbstractDataProvider<T> implements DataProvider<T> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected ValidationResult validateNotNull(Object value, String fieldName) {
        if (value == null) {
            return ValidationResult.failure(fieldName + " cannot be null");
        }
        return ValidationResult.success();
    }

    protected ValidationResult validateNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return ValidationResult.failure(fieldName + " cannot be blank");
        }
        return ValidationResult.success();
    }

    protected ValidationResult validateLength(byte[] data, int expectedLength, String fieldName) {
        if (data == null) {
            return ValidationResult.failure(fieldName + " cannot be null");
        }
        if (data.length != expectedLength) {
            return ValidationResult.failure(
                String.format("%s must be %d bytes, got %d", fieldName, expectedLength, data.length)
            );
        }
        return ValidationResult.success();
    }

    protected ValidationResult validateMaxSize(byte[] data, int maxSize, String fieldName) {
        if (data == null) {
            return ValidationResult.failure(fieldName + " cannot be null");
        }
        if (data.length > maxSize) {
            return ValidationResult.failure(
                String.format("%s exceeds max size of %d bytes (got %d)",
                    fieldName, maxSize, data.length)
            );
        }
        return ValidationResult.success();
    }

    protected ValidationResult validatePositive(Number value, String fieldName) {
        if (value == null) {
            return ValidationResult.failure(fieldName + " cannot be null");
        }
        if (value.doubleValue() <= 0) {
            return ValidationResult.failure(fieldName + " must be positive");
        }
        return ValidationResult.success();
    }

    protected ValidationResult validateNonNegative(Number value, String fieldName) {
        if (value == null) {
            return ValidationResult.failure(fieldName + " cannot be null");
        }
        if (value.doubleValue() < 0) {
            return ValidationResult.failure(fieldName + " cannot be negative");
        }
        return ValidationResult.success();
    }

    protected ValidationResult combine(ValidationResult... results) {
        List<String> errors = new ArrayList<>();

        for (ValidationResult result : results) {
            if (!result.isValid()) {
                errors.addAll(result.getErrors());
            }
        }

        return errors.isEmpty()
            ? ValidationResult.success()
            : ValidationResult.failure(errors);
    }

    @SuppressWarnings("unchecked")
    protected <V> V getRequiredConfig(Map<String, Object> config, String key, Class<V> type) {
        Object value = config.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required config key missing: " + key);
        }

        if (!type.isInstance(value)) {
            throw new IllegalArgumentException(
                String.format("Config key '%s' has wrong type: expected %s, got %s",
                    key, type.getSimpleName(), value.getClass().getSimpleName())
            );
        }

        return (V) value;
    }

    @SuppressWarnings("unchecked")
    protected <V> V getOptionalConfig(Map<String, Object> config, String key,
                                     Class<V> type, V defaultValue) {
        Object value = config.get(key);
        if (value == null) {
            return defaultValue;
        }

        if (!type.isInstance(value)) {
            log.warn("Config key '{}' has wrong type, using default", key);
            return defaultValue;
        }

        return (V) value;
    }
}

package com.bloxbean.cardano.dataprover.service.provider;

/**
 * Result of testing a provider's configuration.
 * Contains success/failure status and a message describing the result.
 */
public class ConfigTestResult {
    private final boolean success;
    private final String message;

    private ConfigTestResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static ConfigTestResult success() {
        return new ConfigTestResult(true, "Configuration is valid");
    }

    public static ConfigTestResult success(String message) {
        return new ConfigTestResult(true, message);
    }

    public static ConfigTestResult failure(String message) {
        return new ConfigTestResult(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}

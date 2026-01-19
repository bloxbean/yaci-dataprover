package com.bloxbean.cardano.dataprover.dto;

/**
 * Response DTO for configuration test result.
 */
public class ConfigTestResponse {

    private boolean success;
    private String message;

    public ConfigTestResponse() {
    }

    public ConfigTestResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static ConfigTestResponse fromTestResult(
            com.bloxbean.cardano.dataprover.service.provider.ConfigTestResult result) {
        return new ConfigTestResponse(result.isSuccess(), result.getMessage());
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

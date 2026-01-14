package com.bloxbean.cardano.dataprover.dto;

/**
 * Response DTO for value lookup without proof generation.
 */
public class ValueLookupResponse {

    private String key;
    private String value;
    private boolean found;

    public ValueLookupResponse() {
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ValueLookupResponse response = new ValueLookupResponse();

        public Builder key(String key) {
            response.setKey(key);
            return this;
        }

        public Builder value(String value) {
            response.setValue(value);
            return this;
        }

        public Builder found(boolean found) {
            response.setFound(found);
            return this;
        }

        public ValueLookupResponse build() {
            return response;
        }
    }
}

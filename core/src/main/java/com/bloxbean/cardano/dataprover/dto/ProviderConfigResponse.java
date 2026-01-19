package com.bloxbean.cardano.dataprover.dto;

import com.bloxbean.cardano.dataprover.service.provider.ConnectionConfigSchema;

import java.util.Map;

/**
 * Response DTO for provider configuration.
 */
public class ProviderConfigResponse {

    private String providerName;
    private Map<String, Object> config;
    private String source;
    private ConnectionConfigSchema schema;

    public ProviderConfigResponse() {
    }

    private ProviderConfigResponse(Builder builder) {
        this.providerName = builder.providerName;
        this.config = builder.config;
        this.source = builder.source;
        this.schema = builder.schema;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public ConnectionConfigSchema getSchema() {
        return schema;
    }

    public void setSchema(ConnectionConfigSchema schema) {
        this.schema = schema;
    }

    public static class Builder {
        private String providerName;
        private Map<String, Object> config;
        private String source;
        private ConnectionConfigSchema schema;

        public Builder providerName(String providerName) {
            this.providerName = providerName;
            return this;
        }

        public Builder config(Map<String, Object> config) {
            this.config = config;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder schema(ConnectionConfigSchema schema) {
            this.schema = schema;
            return this;
        }

        public ProviderConfigResponse build() {
            return new ProviderConfigResponse(this);
        }
    }
}

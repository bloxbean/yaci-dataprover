package com.bloxbean.cardano.dataprover.service.provider;

import java.util.Map;

/**
 * Metadata about a data provider, including configuration schema
 * for dynamic UI generation.
 */
public class ProviderMetadata {
    private String name;
    private String description;
    private String dataType;
    private ProviderStatus status;
    private String statusMessage;
    private ConfigSchema configSchema;
    private ConnectionConfigSchema connectionConfigSchema;
    private KeySerializationSchema keySerializationSchema;
    private Map<String, Object> currentConnectionConfig;
    private String configSource;

    public ProviderMetadata() {
    }

    private ProviderMetadata(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.dataType = builder.dataType;
        this.status = builder.status;
        this.statusMessage = builder.statusMessage;
        this.configSchema = builder.configSchema;
        this.connectionConfigSchema = builder.connectionConfigSchema;
        this.keySerializationSchema = builder.keySerializationSchema;
        this.currentConnectionConfig = builder.currentConnectionConfig;
        this.configSource = builder.configSource;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public ProviderStatus getStatus() {
        return status;
    }

    public void setStatus(ProviderStatus status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public ConfigSchema getConfigSchema() {
        return configSchema;
    }

    public void setConfigSchema(ConfigSchema configSchema) {
        this.configSchema = configSchema;
    }

    public ConnectionConfigSchema getConnectionConfigSchema() {
        return connectionConfigSchema;
    }

    public void setConnectionConfigSchema(ConnectionConfigSchema connectionConfigSchema) {
        this.connectionConfigSchema = connectionConfigSchema;
    }

    public KeySerializationSchema getKeySerializationSchema() {
        return keySerializationSchema;
    }

    public void setKeySerializationSchema(KeySerializationSchema keySerializationSchema) {
        this.keySerializationSchema = keySerializationSchema;
    }

    public Map<String, Object> getCurrentConnectionConfig() {
        return currentConnectionConfig;
    }

    public void setCurrentConnectionConfig(Map<String, Object> currentConnectionConfig) {
        this.currentConnectionConfig = currentConnectionConfig;
    }

    public String getConfigSource() {
        return configSource;
    }

    public void setConfigSource(String configSource) {
        this.configSource = configSource;
    }

    public static class Builder {
        private String name;
        private String description;
        private String dataType;
        private ProviderStatus status = ProviderStatus.AVAILABLE;
        private String statusMessage;
        private ConfigSchema configSchema;
        private ConnectionConfigSchema connectionConfigSchema;
        private KeySerializationSchema keySerializationSchema;
        private Map<String, Object> currentConnectionConfig;
        private String configSource;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder dataType(String dataType) {
            this.dataType = dataType;
            return this;
        }

        public Builder status(ProviderStatus status) {
            this.status = status;
            return this;
        }

        public Builder statusMessage(String statusMessage) {
            this.statusMessage = statusMessage;
            return this;
        }

        public Builder configSchema(ConfigSchema configSchema) {
            this.configSchema = configSchema;
            return this;
        }

        public Builder connectionConfigSchema(ConnectionConfigSchema connectionConfigSchema) {
            this.connectionConfigSchema = connectionConfigSchema;
            return this;
        }

        public Builder keySerializationSchema(KeySerializationSchema keySerializationSchema) {
            this.keySerializationSchema = keySerializationSchema;
            return this;
        }

        public Builder currentConnectionConfig(Map<String, Object> currentConnectionConfig) {
            this.currentConnectionConfig = currentConnectionConfig;
            return this;
        }

        public Builder configSource(String configSource) {
            this.configSource = configSource;
            return this;
        }

        public ProviderMetadata build() {
            return new ProviderMetadata(this);
        }
    }
}

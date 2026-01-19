package com.bloxbean.cardano.dataprover.service.provider;

import java.util.ArrayList;
import java.util.List;

/**
 * Schema defining the connection configuration fields required by a data provider.
 * Used by the UI to render dynamic forms for one-time provider setup (DB connections, API keys, etc.).
 *
 * This is separate from ConfigSchema which is used for runtime/ingestion parameters.
 */
public class ConnectionConfigSchema {
    private List<ConfigField> fields;

    public ConnectionConfigSchema() {
        this.fields = new ArrayList<>();
    }

    private ConnectionConfigSchema(Builder builder) {
        this.fields = builder.fields != null ? builder.fields : new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<ConfigField> getFields() {
        return fields;
    }

    public void setFields(List<ConfigField> fields) {
        this.fields = fields;
    }

    public static class Builder {
        private List<ConfigField> fields;

        public Builder fields(List<ConfigField> fields) {
            this.fields = fields;
            return this;
        }

        public Builder addField(ConfigField field) {
            if (this.fields == null) {
                this.fields = new ArrayList<>();
            }
            this.fields.add(field);
            return this;
        }

        public ConnectionConfigSchema build() {
            return new ConnectionConfigSchema(this);
        }
    }
}

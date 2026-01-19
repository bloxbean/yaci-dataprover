package com.bloxbean.cardano.dataprover.service.provider;

import java.util.ArrayList;
import java.util.List;

/**
 * Schema defining the configuration fields required by a data provider.
 * Used by the UI to render dynamic forms for provider configuration.
 */
public class ConfigSchema {
    private List<ConfigField> fields;

    public ConfigSchema() {
        this.fields = new ArrayList<>();
    }

    private ConfigSchema(Builder builder) {
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

        public ConfigSchema build() {
            return new ConfigSchema(this);
        }
    }
}

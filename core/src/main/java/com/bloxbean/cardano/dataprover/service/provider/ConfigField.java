package com.bloxbean.cardano.dataprover.service.provider;

import java.util.List;

/**
 * Definition of a configuration field for a data provider.
 * Used by the UI to render dynamic forms based on provider requirements.
 */
public class ConfigField {
    private String name;
    private String label;
    private FieldType type;
    private boolean required;
    private String description;
    private String placeholder;
    private Object defaultValue;
    private FieldValidation validation;
    private List<SelectOption> options;

    public ConfigField() {
    }

    private ConfigField(Builder builder) {
        this.name = builder.name;
        this.label = builder.label;
        this.type = builder.type;
        this.required = builder.required;
        this.description = builder.description;
        this.placeholder = builder.placeholder;
        this.defaultValue = builder.defaultValue;
        this.validation = builder.validation;
        this.options = builder.options;
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public FieldType getType() {
        return type;
    }

    public void setType(FieldType type) {
        this.type = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public FieldValidation getValidation() {
        return validation;
    }

    public void setValidation(FieldValidation validation) {
        this.validation = validation;
    }

    public List<SelectOption> getOptions() {
        return options;
    }

    public void setOptions(List<SelectOption> options) {
        this.options = options;
    }

    public static class Builder {
        private String name;
        private String label;
        private FieldType type = FieldType.STRING;
        private boolean required = false;
        private String description;
        private String placeholder;
        private Object defaultValue;
        private FieldValidation validation;
        private List<SelectOption> options;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder type(FieldType type) {
            this.type = type;
            return this;
        }

        public Builder required(boolean required) {
            this.required = required;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder placeholder(String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public Builder defaultValue(Object defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder validation(FieldValidation validation) {
            this.validation = validation;
            return this;
        }

        public Builder options(List<SelectOption> options) {
            this.options = options;
            return this;
        }

        public ConfigField build() {
            return new ConfigField(this);
        }
    }
}

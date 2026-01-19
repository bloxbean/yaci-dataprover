package com.bloxbean.cardano.dataprover.service.provider;

/**
 * Schema defining how to serialize keys for a data provider.
 * Used by the UI to render input fields for key serialization.
 */
public class KeySerializationSchema {
    private String keyFieldName;
    private String keyFieldLabel;
    private String keyFieldType;
    private String keyFieldPlaceholder;
    private String keyDescription;

    public KeySerializationSchema() {
    }

    private KeySerializationSchema(Builder builder) {
        this.keyFieldName = builder.keyFieldName;
        this.keyFieldLabel = builder.keyFieldLabel;
        this.keyFieldType = builder.keyFieldType;
        this.keyFieldPlaceholder = builder.keyFieldPlaceholder;
        this.keyDescription = builder.keyDescription;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getKeyFieldName() {
        return keyFieldName;
    }

    public void setKeyFieldName(String keyFieldName) {
        this.keyFieldName = keyFieldName;
    }

    public String getKeyFieldLabel() {
        return keyFieldLabel;
    }

    public void setKeyFieldLabel(String keyFieldLabel) {
        this.keyFieldLabel = keyFieldLabel;
    }

    public String getKeyFieldType() {
        return keyFieldType;
    }

    public void setKeyFieldType(String keyFieldType) {
        this.keyFieldType = keyFieldType;
    }

    public String getKeyFieldPlaceholder() {
        return keyFieldPlaceholder;
    }

    public void setKeyFieldPlaceholder(String keyFieldPlaceholder) {
        this.keyFieldPlaceholder = keyFieldPlaceholder;
    }

    public String getKeyDescription() {
        return keyDescription;
    }

    public void setKeyDescription(String keyDescription) {
        this.keyDescription = keyDescription;
    }

    public static class Builder {
        private String keyFieldName;
        private String keyFieldLabel;
        private String keyFieldType = "string";
        private String keyFieldPlaceholder;
        private String keyDescription;

        public Builder keyFieldName(String keyFieldName) {
            this.keyFieldName = keyFieldName;
            return this;
        }

        public Builder keyFieldLabel(String keyFieldLabel) {
            this.keyFieldLabel = keyFieldLabel;
            return this;
        }

        public Builder keyFieldType(String keyFieldType) {
            this.keyFieldType = keyFieldType;
            return this;
        }

        public Builder keyFieldPlaceholder(String keyFieldPlaceholder) {
            this.keyFieldPlaceholder = keyFieldPlaceholder;
            return this;
        }

        public Builder keyDescription(String keyDescription) {
            this.keyDescription = keyDescription;
            return this;
        }

        public KeySerializationSchema build() {
            return new KeySerializationSchema(this);
        }
    }
}

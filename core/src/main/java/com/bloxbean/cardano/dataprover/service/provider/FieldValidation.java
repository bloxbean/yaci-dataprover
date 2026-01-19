package com.bloxbean.cardano.dataprover.service.provider;

/**
 * Validation rules for a configuration field.
 */
public class FieldValidation {
    private Number min;
    private Number max;
    private String pattern;
    private Integer minLength;
    private Integer maxLength;

    public FieldValidation() {
    }

    private FieldValidation(Builder builder) {
        this.min = builder.min;
        this.max = builder.max;
        this.pattern = builder.pattern;
        this.minLength = builder.minLength;
        this.maxLength = builder.maxLength;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Number getMin() {
        return min;
    }

    public void setMin(Number min) {
        this.min = min;
    }

    public Number getMax() {
        return max;
    }

    public void setMax(Number max) {
        this.max = max;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public static class Builder {
        private Number min;
        private Number max;
        private String pattern;
        private Integer minLength;
        private Integer maxLength;

        public Builder min(Number min) {
            this.min = min;
            return this;
        }

        public Builder max(Number max) {
            this.max = max;
            return this;
        }

        public Builder pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Builder minLength(Integer minLength) {
            this.minLength = minLength;
            return this;
        }

        public Builder maxLength(Integer maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public FieldValidation build() {
            return new FieldValidation(this);
        }
    }
}

package com.bloxbean.cardano.dataprover.polyglot;

import java.util.Map;

/**
 * Configuration for scheduled provider execution.
 * Supports cron expressions and interval-based scheduling.
 */
public class ScheduleConfig {
    private boolean enabled = false;
    private String cron;
    private String interval;
    private String description;
    private String targetMerkle;
    private boolean autoCreateMerkle = false;
    private Map<String, Object> defaultConfig;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTargetMerkle() {
        return targetMerkle;
    }

    public void setTargetMerkle(String targetMerkle) {
        this.targetMerkle = targetMerkle;
    }

    public boolean isAutoCreateMerkle() {
        return autoCreateMerkle;
    }

    public void setAutoCreateMerkle(boolean autoCreateMerkle) {
        this.autoCreateMerkle = autoCreateMerkle;
    }

    public Map<String, Object> getDefaultConfig() {
        return defaultConfig;
    }

    public void setDefaultConfig(Map<String, Object> defaultConfig) {
        this.defaultConfig = defaultConfig;
    }
}

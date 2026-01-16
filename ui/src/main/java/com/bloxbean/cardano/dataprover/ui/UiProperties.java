package com.bloxbean.cardano.dataprover.ui;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the DataProver Admin UI.
 */
@ConfigurationProperties(prefix = "dataprover.ui")
public class UiProperties {

    /**
     * Enable or disable the Admin UI.
     * Default is false - UI must be explicitly enabled.
     */
    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

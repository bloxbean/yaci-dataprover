package com.bloxbean.cardano.proofserver.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Auto-configuration properties for Proof Server.
 */
@ConfigurationProperties(prefix = "proof-server.autoconfigure")
public class ProofServerAutoConfigProperties {

    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

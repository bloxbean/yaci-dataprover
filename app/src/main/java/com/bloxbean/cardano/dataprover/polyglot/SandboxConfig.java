package com.bloxbean.cardano.dataprover.polyglot;

import java.util.Map;

/**
 * Security and resource configuration for script sandbox.
 * Controls I/O, native access, and execution limits.
 */
public class SandboxConfig {
    private boolean allowIO = false;
    private boolean allowNetwork = true;
    private long maxStatements = 10000000L;

    public boolean isAllowIO() {
        return allowIO;
    }

    public void setAllowIO(boolean allowIO) {
        this.allowIO = allowIO;
    }

    public boolean isAllowNetwork() {
        return allowNetwork;
    }

    public void setAllowNetwork(boolean allowNetwork) {
        this.allowNetwork = allowNetwork;
    }

    public long getMaxStatements() {
        return maxStatements;
    }

    public void setMaxStatements(long maxStatements) {
        this.maxStatements = maxStatements;
    }

    public static SandboxConfig fromMap(Map<String, Object> map) {
        SandboxConfig config = new SandboxConfig();
        if (map == null) {
            return config;
        }
        if (map.containsKey("allowIO")) {
            config.setAllowIO((Boolean) map.get("allowIO"));
        }
        if (map.containsKey("allowNetwork")) {
            config.setAllowNetwork((Boolean) map.get("allowNetwork"));
        }
        if (map.containsKey("maxStatements")) {
            config.setMaxStatements(((Number) map.get("maxStatements")).longValue());
        }
        return config;
    }
}

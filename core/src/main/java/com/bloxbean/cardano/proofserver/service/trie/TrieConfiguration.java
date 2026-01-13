package com.bloxbean.cardano.proofserver.service.trie;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for creating a trie instance.
 */
public class TrieConfiguration {

    private String identifier;
    private String trieType;
    private String storagePath;
    private String hashFunction;
    private Map<String, Object> customConfig;

    public TrieConfiguration() {
        this.customConfig = new HashMap<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getTrieType() {
        return trieType;
    }

    public void setTrieType(String trieType) {
        this.trieType = trieType;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getHashFunction() {
        return hashFunction;
    }

    public void setHashFunction(String hashFunction) {
        this.hashFunction = hashFunction;
    }

    public Map<String, Object> getCustomConfig() {
        return customConfig;
    }

    public void setCustomConfig(Map<String, Object> customConfig) {
        this.customConfig = customConfig;
    }

    public static class Builder {
        private final TrieConfiguration config = new TrieConfiguration();

        public Builder identifier(String identifier) {
            config.setIdentifier(identifier);
            return this;
        }

        public Builder trieType(String trieType) {
            config.setTrieType(trieType);
            return this;
        }

        public Builder storagePath(String storagePath) {
            config.setStoragePath(storagePath);
            return this;
        }

        public Builder hashFunction(String hashFunction) {
            config.setHashFunction(hashFunction);
            return this;
        }

        public Builder customConfig(Map<String, Object> customConfig) {
            config.setCustomConfig(new HashMap<>(customConfig));
            return this;
        }

        public TrieConfiguration build() {
            return config;
        }
    }
}

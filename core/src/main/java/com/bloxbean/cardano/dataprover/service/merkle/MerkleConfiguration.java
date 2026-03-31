package com.bloxbean.cardano.dataprover.service.merkle;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for creating a merkle instance.
 */
public class MerkleConfiguration {

    private String identifier;
    private String scheme;
    private String storagePath;
    private String hashFunction;
    private String rootHash;
    private Map<String, Object> customConfig;
    private boolean storeOriginalKeys;

    public MerkleConfiguration() {
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

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
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

    public String getRootHash() {
        return rootHash;
    }

    public void setRootHash(String rootHash) {
        this.rootHash = rootHash;
    }

    public Map<String, Object> getCustomConfig() {
        return customConfig;
    }

    public void setCustomConfig(Map<String, Object> customConfig) {
        this.customConfig = customConfig;
    }

    public boolean isStoreOriginalKeys() {
        return storeOriginalKeys;
    }

    public void setStoreOriginalKeys(boolean storeOriginalKeys) {
        this.storeOriginalKeys = storeOriginalKeys;
    }

    public static class Builder {
        private final MerkleConfiguration config = new MerkleConfiguration();

        public Builder identifier(String identifier) {
            config.setIdentifier(identifier);
            return this;
        }

        public Builder scheme(String scheme) {
            config.setScheme(scheme);
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

        public Builder rootHash(String rootHash) {
            config.setRootHash(rootHash);
            return this;
        }

        public Builder customConfig(Map<String, Object> customConfig) {
            config.setCustomConfig(new HashMap<>(customConfig));
            return this;
        }

        public Builder storeOriginalKeys(boolean storeOriginalKeys) {
            config.setStoreOriginalKeys(storeOriginalKeys);
            return this;
        }

        public MerkleConfiguration build() {
            return config;
        }
    }
}

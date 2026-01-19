package com.bloxbean.cardano.dataprover.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing stored configuration for a data provider.
 * Stores connection settings configured via UI, separate from env-based config.
 */
@Entity
@Table(name = "provider_configuration")
public class ProviderConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_name", unique = true, nullable = false, length = 64)
    private String providerName;

    @Convert(converter = JsonMapConverter.class)
    @Column(name = "config_json", nullable = false, columnDefinition = "TEXT")
    private Map<String, Object> configJson;

    @Column(name = "encrypted_secrets", columnDefinition = "TEXT")
    private String encryptedSecrets;

    @Column(name = "source", nullable = false, length = 20)
    private String source = "UI";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    @Version
    private Long version;

    public ProviderConfiguration() {
        this.configJson = new HashMap<>();
        this.createdAt = Instant.now();
        this.source = "UI";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public Map<String, Object> getConfigJson() {
        return configJson;
    }

    public void setConfigJson(Map<String, Object> configJson) {
        this.configJson = configJson;
    }

    public String getEncryptedSecrets() {
        return encryptedSecrets;
    }

    public void setEncryptedSecrets(String encryptedSecrets) {
        this.encryptedSecrets = encryptedSecrets;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public void touch() {
        this.lastUpdated = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ProviderConfiguration config = new ProviderConfiguration();

        public Builder providerName(String providerName) {
            config.setProviderName(providerName);
            return this;
        }

        public Builder configJson(Map<String, Object> configJson) {
            config.setConfigJson(new HashMap<>(configJson));
            return this;
        }

        public Builder encryptedSecrets(String encryptedSecrets) {
            config.setEncryptedSecrets(encryptedSecrets);
            return this;
        }

        public Builder source(String source) {
            config.setSource(source);
            return this;
        }

        public ProviderConfiguration build() {
            return config;
        }
    }
}

package com.bloxbean.cardano.dataprover.service;

import com.bloxbean.cardano.dataprover.config.DataProverProperties;
import com.bloxbean.cardano.dataprover.model.ProviderConfiguration;
import com.bloxbean.cardano.dataprover.repository.ProviderConfigurationRepository;
import com.bloxbean.cardano.dataprover.service.provider.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for managing provider configuration.
 * Handles merging env config with UI-configured DB config,
 * and manages encryption of sensitive values.
 */
@Service
public class ProviderConfigurationService {

    private static final Logger log = LoggerFactory.getLogger(ProviderConfigurationService.class);
    private static final String MASKED_PASSWORD = "****";
    private static final TypeReference<Map<String, String>> MAP_TYPE_REF = new TypeReference<>() {};

    private final ProviderConfigurationRepository repository;
    private final DataProverProperties properties;
    private final EncryptionService encryptionService;
    private final DataProviderRegistry providerRegistry;
    private final ObjectMapper objectMapper;

    public ProviderConfigurationService(ProviderConfigurationRepository repository,
                                        DataProverProperties properties,
                                        EncryptionService encryptionService,
                                        DataProviderRegistry providerRegistry,
                                        @Autowired(required = false) ObjectMapper objectMapper) {
        this.repository = repository;
        this.properties = properties;
        this.encryptionService = encryptionService;
        this.providerRegistry = providerRegistry;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Get effective config by merging env config with DB config.
     * Precedence: defaults <- env vars <- application.yml <- DB config (UI)
     *
     * @param providerName the provider name
     * @return merged configuration map
     */
    public Map<String, Object> getEffectiveConfig(String providerName) {
        // Start with env/yml config
        Map<String, Object> config = new HashMap<>(getEnvConfig(providerName));

        // Override with DB config if present
        Optional<ProviderConfiguration> dbConfig = repository.findByProviderName(providerName);
        if (dbConfig.isPresent()) {
            ProviderConfiguration stored = dbConfig.get();

            // Add non-sensitive config
            if (stored.getConfigJson() != null) {
                config.putAll(stored.getConfigJson());
            }

            // Decrypt and add sensitive values
            if (stored.getEncryptedSecrets() != null) {
                Map<String, String> secrets = decryptSecrets(stored.getEncryptedSecrets());
                config.putAll(secrets);
            }
        }

        return config;
    }

    /**
     * Get the configuration source for a provider.
     *
     * @param providerName the provider name
     * @return "UI" if DB config exists, "ENV" otherwise
     */
    public String getConfigSource(String providerName) {
        return repository.existsByProviderName(providerName) ? "UI" : "ENV";
    }

    /**
     * Get env/yml configuration for a provider.
     *
     * @param providerName the provider name
     * @return configuration from env/yml
     */
    public Map<String, Object> getEnvConfig(String providerName) {
        Map<String, Map<String, Object>> providers = properties.getPlugins().getProviders();
        if (providers != null && providers.containsKey(providerName)) {
            return new HashMap<>(providers.get(providerName));
        }
        return new HashMap<>();
    }

    /**
     * Get masked configuration (passwords shown as "****").
     *
     * @param providerName the provider name
     * @return masked configuration map
     */
    public Map<String, Object> getMaskedConfig(String providerName) {
        Map<String, Object> effectiveConfig = getEffectiveConfig(providerName);

        // Find password fields from provider schema
        Set<String> passwordFields = getPasswordFieldNames(providerName);

        // Mask password values
        Map<String, Object> masked = new HashMap<>(effectiveConfig);
        for (String field : passwordFields) {
            if (masked.containsKey(field) && masked.get(field) != null) {
                masked.put(field, MASKED_PASSWORD);
            }
        }

        return masked;
    }

    /**
     * Save UI configuration, encrypting sensitive values.
     *
     * @param providerName the provider name
     * @param config the configuration to save
     */
    @Transactional
    public void saveConfiguration(String providerName, Map<String, Object> config) {
        log.info("Saving configuration for provider: {}", providerName);

        // Separate sensitive and non-sensitive values
        Set<String> passwordFields = getPasswordFieldNames(providerName);
        Map<String, Object> nonSensitive = new HashMap<>();
        Map<String, String> sensitive = new HashMap<>();

        for (Map.Entry<String, Object> entry : config.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (passwordFields.contains(key)) {
                // Don't save if it's the masked placeholder (unchanged from previous value)
                if (!MASKED_PASSWORD.equals(value)) {
                    sensitive.put(key, value != null ? value.toString() : null);
                } else {
                    // Keep existing encrypted value
                    preserveExistingSecret(providerName, key, sensitive);
                }
            } else {
                nonSensitive.put(key, value);
            }
        }

        // Encrypt sensitive values
        String encryptedSecrets = sensitive.isEmpty() ? null : encryptSecrets(sensitive);

        // Save to DB
        ProviderConfiguration entity = repository.findByProviderName(providerName)
                .orElse(ProviderConfiguration.builder()
                        .providerName(providerName)
                        .build());

        entity.setConfigJson(nonSensitive);
        entity.setEncryptedSecrets(encryptedSecrets);
        entity.setSource("UI");
        entity.touch();

        repository.save(entity);

        // Reconfigure the provider with new settings
        reconfigureProvider(providerName);
    }

    /**
     * Test configuration without saving.
     *
     * @param providerName the provider name
     * @param config the configuration to test
     * @return test result
     */
    public ConfigTestResult testConfiguration(String providerName, Map<String, Object> config) {
        log.info("Testing configuration for provider: {}", providerName);

        try {
            DataProvider<?> provider = providerRegistry.getProvider(providerName);

            // If password fields are masked, merge with existing values
            Map<String, Object> testConfig = resolveTestConfig(providerName, config);

            return provider.testConfiguration(testConfig);
        } catch (Exception e) {
            log.error("Configuration test failed for provider: {}", providerName, e);
            return ConfigTestResult.failure("Test failed: " + e.getMessage());
        }
    }

    /**
     * Reset provider to env-only configuration (delete DB config).
     *
     * @param providerName the provider name
     */
    @Transactional
    public void resetToEnvConfig(String providerName) {
        log.info("Resetting configuration to env for provider: {}", providerName);

        repository.deleteByProviderName(providerName);

        // Reconfigure with env-only config
        reconfigureProvider(providerName);
    }

    /**
     * Check if a provider has UI configuration stored.
     *
     * @param providerName the provider name
     * @return true if UI config exists
     */
    public boolean hasUiConfig(String providerName) {
        return repository.existsByProviderName(providerName);
    }

    private void reconfigureProvider(String providerName) {
        try {
            DataProvider<?> provider = providerRegistry.getProvider(providerName);
            Map<String, Object> config = getEffectiveConfig(providerName);

            boolean success = provider.reconfigure(config);
            if (success) {
                log.info("Provider {} reconfigured successfully", providerName);
            } else {
                log.warn("Provider {} reconfiguration returned false", providerName);
            }
        } catch (Exception e) {
            log.error("Failed to reconfigure provider: {}", providerName, e);
        }
    }

    private Set<String> getPasswordFieldNames(String providerName) {
        Set<String> passwordFields = new HashSet<>();

        try {
            DataProvider<?> provider = providerRegistry.getProvider(providerName);
            ConnectionConfigSchema schema = provider.getConnectionConfigSchema();

            if (schema != null && schema.getFields() != null) {
                for (ConfigField field : schema.getFields()) {
                    if (field.getType() == FieldType.PASSWORD) {
                        passwordFields.add(field.getName());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not get password fields for provider: {}", providerName, e);
        }

        return passwordFields;
    }

    private void preserveExistingSecret(String providerName, String key, Map<String, String> sensitive) {
        repository.findByProviderName(providerName).ifPresent(existing -> {
            if (existing.getEncryptedSecrets() != null) {
                Map<String, String> existingSecrets = decryptSecrets(existing.getEncryptedSecrets());
                if (existingSecrets.containsKey(key)) {
                    sensitive.put(key, existingSecrets.get(key));
                }
            }
        });
    }

    private Map<String, Object> resolveTestConfig(String providerName, Map<String, Object> config) {
        Map<String, Object> resolved = new HashMap<>(config);
        Set<String> passwordFields = getPasswordFieldNames(providerName);

        // For masked password fields, use existing values
        for (String field : passwordFields) {
            Object value = resolved.get(field);
            if (MASKED_PASSWORD.equals(value)) {
                // Get existing value
                Map<String, Object> effective = getEffectiveConfig(providerName);
                if (effective.containsKey(field)) {
                    resolved.put(field, effective.get(field));
                }
            }
        }

        return resolved;
    }

    private String encryptSecrets(Map<String, String> secrets) {
        try {
            String json = objectMapper.writeValueAsString(secrets);
            return encryptionService.encrypt(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize secrets", e);
        }
    }

    private Map<String, String> decryptSecrets(String encrypted) {
        try {
            String json = encryptionService.decrypt(encrypted);
            return objectMapper.readValue(json, MAP_TYPE_REF);
        } catch (Exception e) {
            log.error("Failed to decrypt secrets", e);
            return new HashMap<>();
        }
    }
}

package com.bloxbean.cardano.dataprover.controller;

import com.bloxbean.cardano.dataprover.dto.*;
import com.bloxbean.cardano.dataprover.service.IngestionService;
import com.bloxbean.cardano.dataprover.service.ProviderConfigurationService;
import com.bloxbean.cardano.dataprover.service.provider.ConfigTestResult;
import com.bloxbean.cardano.dataprover.service.provider.DataProvider;
import com.bloxbean.cardano.dataprover.service.provider.DataProviderRegistry;
import com.bloxbean.cardano.dataprover.service.provider.ProviderMetadata;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HexFormat;
import java.util.Map;

/**
 * REST controller for data provider operations.
 */
@RestController
@RequestMapping("/api/v1/providers")
public class ProviderController {

    private static final Logger log = LoggerFactory.getLogger(ProviderController.class);
    private static final HexFormat HEX = HexFormat.of();

    private final DataProviderRegistry providerRegistry;
    private final IngestionService ingestionService;
    private final ProviderConfigurationService configurationService;

    public ProviderController(DataProviderRegistry providerRegistry,
                              IngestionService ingestionService,
                              ProviderConfigurationService configurationService) {
        this.providerRegistry = providerRegistry;
        this.ingestionService = ingestionService;
        this.configurationService = configurationService;
    }

    /**
     * List all available data providers.
     */
    @GetMapping
    public ResponseEntity<ProviderListResponse> listProviders() {
        log.debug("Listing all data providers");

        var providers = providerRegistry.getAllProviderMetadata();

        return ResponseEntity.ok(new ProviderListResponse(providers));
    }

    /**
     * Get detailed information about a specific provider.
     */
    @GetMapping("/{name}")
    public ResponseEntity<ProviderMetadata> getProvider(@PathVariable String name) {
        log.debug("Getting provider details: {}", name);

        ProviderMetadata metadata = providerRegistry.getProviderMetadata(name);

        // Enrich with current config info
        metadata.setCurrentConnectionConfig(configurationService.getMaskedConfig(name));
        metadata.setConfigSource(configurationService.getConfigSource(name));

        return ResponseEntity.ok(metadata);
    }

    /**
     * Serialize a domain key to hex format using the provider's serialization logic.
     */
    @PostMapping("/{name}/serialize-key")
    public ResponseEntity<SerializeKeyResponse> serializeKey(
            @PathVariable String name,
            @Valid @RequestBody SerializeKeyRequest request) {

        log.debug("Serializing key for provider {}: {}", name, request.getKey());

        DataProvider<?> provider = providerRegistry.getProvider(name);

        byte[] serializedKey = provider.serializeKeyFromInput(request.getKey());

        String hex = "0x" + HEX.formatHex(serializedKey);

        SerializeKeyResponse response = SerializeKeyResponse.builder()
                .originalKey(request.getKey())
                .serializedKeyHex(hex)
                .keyLength(serializedKey.length)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Ingest data using a provider with optional auto-create merkle.
     */
    @PostMapping("/ingest")
    public ResponseEntity<ProviderIngestResponse> ingest(
            @Valid @RequestBody ProviderIngestRequest request) {

        log.info("Provider ingest request: merkle={}, provider={}, createIfNotExists={}",
                request.getMerkleName(), request.getProvider(), request.isCreateIfNotExists());

        ProviderIngestResponse response = ingestionService.ingestWithProvider(request);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Get current configuration for a provider (masked passwords).
     */
    @GetMapping("/{name}/config")
    public ResponseEntity<ProviderConfigResponse> getConfig(@PathVariable String name) {
        log.debug("Getting configuration for provider: {}", name);

        DataProvider<?> provider = providerRegistry.getProvider(name);
        Map<String, Object> config = configurationService.getMaskedConfig(name);
        String source = configurationService.getConfigSource(name);

        ProviderConfigResponse response = ProviderConfigResponse.builder()
                .providerName(name)
                .config(config)
                .source(source)
                .schema(provider.getConnectionConfigSchema())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Save configuration for a provider.
     */
    @PutMapping("/{name}/config")
    public ResponseEntity<ProviderConfigResponse> saveConfig(
            @PathVariable String name,
            @Valid @RequestBody ProviderConfigRequest request) {

        log.info("Saving configuration for provider: {}", name);

        configurationService.saveConfiguration(name, request.getConfig());

        // Return updated config
        DataProvider<?> provider = providerRegistry.getProvider(name);
        Map<String, Object> config = configurationService.getMaskedConfig(name);

        ProviderConfigResponse response = ProviderConfigResponse.builder()
                .providerName(name)
                .config(config)
                .source("UI")
                .schema(provider.getConnectionConfigSchema())
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Test configuration without saving.
     */
    @PostMapping("/{name}/config/test")
    public ResponseEntity<ConfigTestResponse> testConfig(
            @PathVariable String name,
            @Valid @RequestBody ConfigTestRequest request) {

        log.info("Testing configuration for provider: {}", name);

        ConfigTestResult result = configurationService.testConfiguration(name, request.getConfig());

        return ResponseEntity.ok(ConfigTestResponse.fromTestResult(result));
    }

    /**
     * Reset provider configuration to environment-only settings.
     */
    @DeleteMapping("/{name}/config")
    public ResponseEntity<Void> resetConfig(@PathVariable String name) {
        log.info("Resetting configuration for provider: {}", name);

        configurationService.resetToEnvConfig(name);

        return ResponseEntity.noContent().build();
    }
}

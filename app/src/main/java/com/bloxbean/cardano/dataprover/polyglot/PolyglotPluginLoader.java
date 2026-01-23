package com.bloxbean.cardano.dataprover.polyglot;

import com.bloxbean.cardano.dataprover.config.DataProverProperties;
import com.bloxbean.cardano.dataprover.service.ProviderConfigurationService;
import com.bloxbean.cardano.dataprover.service.provider.DataProviderRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Discovers and loads polyglot (JavaScript/Python) provider plugins from the plugins directory.
 * Looks for directories containing manifest.json and provider.js/py files.
 */
@Component
public class PolyglotPluginLoader {
    private static final Logger log = LoggerFactory.getLogger(PolyglotPluginLoader.class);

    private final DataProverProperties properties;
    private final DataProviderRegistry registry;
    private final ProviderConfigurationService configurationService;
    private final Map<String, PolyglotProviderAdapter> loadedProviders = new ConcurrentHashMap<>();

    public PolyglotPluginLoader(DataProverProperties properties,
                                 DataProviderRegistry registry,
                                 ProviderConfigurationService configurationService) {
        this.properties = properties;
        this.registry = registry;
        this.configurationService = configurationService;
    }

    @PostConstruct
    public void loadPlugins() {
        if (!properties.getPlugins().getPolyglot().isEnabled()) {
            log.info("Polyglot plugin support is disabled");
            return;
        }

        // Check if GraalVM polyglot is available and compatible
        if (!isPolyglotAvailable()) {
            log.warn("GraalVM Polyglot is not available or incompatible with current JVM. " +
                    "Polyglot providers will not be loaded. " +
                    "Use a compatible GraalVM version or standard JDK with polyglot dependencies.");
            return;
        }

        String pluginsPath = properties.getPlugins().getPath();
        Path pluginDir = Paths.get(pluginsPath);

        if (!Files.exists(pluginDir)) {
            log.info("Plugins directory does not exist: {}. Skipping polyglot plugin loading.", pluginsPath);
            return;
        }

        log.info("Loading polyglot plugins from: {}", pluginDir.toAbsolutePath());

        try (Stream<Path> directories = Files.list(pluginDir)
                .filter(Files::isDirectory)
                .filter(this::isPolyglotProvider)) {

            for (Path providerDir : directories.toList()) {
                loadProviderFromDirectory(providerDir);
            }
        } catch (IOException e) {
            log.error("Failed to list plugins directory: {}", pluginsPath, e);
        }

        log.info("Loaded {} polyglot providers", loadedProviders.size());
    }

    private boolean isPolyglotAvailable() {
        try {
            // Try to create a minimal context to verify polyglot is working
            try (org.graalvm.polyglot.Context context = org.graalvm.polyglot.Context.create("js")) {
                context.eval("js", "1 + 1");
            }
            return true;
        } catch (Throwable e) {
            log.debug("Polyglot availability check failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean isPolyglotProvider(Path directory) {
        // A polyglot provider must have manifest.json
        Path manifestPath = directory.resolve("manifest.json");
        if (!Files.exists(manifestPath)) {
            return false;
        }

        // And either provider.js or provider.py
        return Files.exists(directory.resolve("provider.js"))
                || Files.exists(directory.resolve("provider.py"));
    }

    private void loadProviderFromDirectory(Path providerDir) {
        Path manifestPath = providerDir.resolve("manifest.json");

        try {
            log.info("Loading polyglot provider from: {}", providerDir.getFileName());

            // Parse manifest
            ProviderManifest manifest = ProviderManifest.load(manifestPath);

            // Verify script file exists
            ScriptLanguage language = manifest.getScriptLanguage();
            Path scriptPath = providerDir.resolve(language.getDefaultFile());

            if (!Files.exists(scriptPath)) {
                log.warn("Script file not found for provider {}: {}",
                        manifest.getName(), scriptPath.getFileName());
                return;
            }

            // Create and register the provider adapter
            PolyglotProviderAdapter adapter = new PolyglotProviderAdapter(manifest, providerDir);
            loadedProviders.put(manifest.getName(), adapter);

            // Register with the provider registry
            registry.register(adapter);

            // Get configuration and initialize
            Map<String, Object> config = configurationService.getEffectiveConfig(manifest.getName());

            // Check if provider requires connection config
            boolean requiresConnectionConfig = adapter.getConnectionConfigSchema() != null
                    && !adapter.getConnectionConfigSchema().getFields().isEmpty();

            if (!config.isEmpty() || !requiresConnectionConfig) {
                // Initialize if we have config OR if the provider doesn't require connection config
                try {
                    adapter.initialize(config);
                    log.info("Initialized polyglot provider: {} (language: {})",
                            manifest.getName(), language.name());
                } catch (Exception e) {
                    log.warn("Failed to initialize provider {} with config: {}",
                            manifest.getName(), e.getMessage());
                }
            } else {
                log.info("Registered polyglot provider (not initialized - requires config): {} (language: {})",
                        manifest.getName(), language.name());
            }

        } catch (Exception e) {
            log.error("Failed to load polyglot provider from: {}", providerDir, e);
        }
    }

    /**
     * Reload a specific provider's script.
     * Used for hot-reload during development.
     */
    public void reloadProvider(String providerName) {
        PolyglotProviderAdapter adapter = loadedProviders.get(providerName);
        if (adapter == null) {
            log.warn("Provider not found for reload: {}", providerName);
            return;
        }

        try {
            adapter.reload();
            log.info("Reloaded polyglot provider: {}", providerName);
        } catch (IOException e) {
            log.error("Failed to reload provider: {}", providerName, e);
        }
    }

    /**
     * Get a loaded polyglot provider by name.
     */
    public PolyglotProviderAdapter getProvider(String name) {
        return loadedProviders.get(name);
    }

    /**
     * Get all loaded polyglot providers.
     */
    public Map<String, PolyglotProviderAdapter> getLoadedProviders() {
        return Map.copyOf(loadedProviders);
    }

    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up {} polyglot providers", loadedProviders.size());
        for (PolyglotProviderAdapter adapter : loadedProviders.values()) {
            try {
                adapter.close();
            } catch (Exception e) {
                log.warn("Error closing provider {}: {}", adapter.getName(), e.getMessage());
            }
        }
        loadedProviders.clear();
    }
}

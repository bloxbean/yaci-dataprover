package com.bloxbean.cardano.proofserver;

import com.bloxbean.cardano.proofserver.config.TrieServerProperties;
import com.bloxbean.cardano.proofserver.service.provider.DataProvider;
import com.bloxbean.cardano.proofserver.service.provider.DataProviderRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Stream;

/**
 * Loads DataProvider plugins from JAR files in the plugins directory.
 * Uses Java's ServiceLoader (SPI) mechanism to discover providers.
 */
@Component
public class PluginLoader {

    private static final Logger log = LoggerFactory.getLogger(PluginLoader.class);

    private final TrieServerProperties properties;
    private final DataProviderRegistry registry;

    public PluginLoader(TrieServerProperties properties, DataProviderRegistry registry) {
        this.properties = properties;
        this.registry = registry;
    }

    @PostConstruct
    public void loadPlugins() {
        String pluginsPath = properties.getPlugins().getPath();
        Path pluginDir = Paths.get(pluginsPath);

        if (!Files.exists(pluginDir)) {
            log.info("Plugins directory does not exist: {}. Skipping plugin loading.", pluginsPath);
            return;
        }

        log.info("Loading plugins from: {}", pluginDir.toAbsolutePath());

        try (Stream<Path> jars = Files.list(pluginDir)
                .filter(p -> p.toString().endsWith(".jar"))) {

            for (Path jar : jars.toList()) {
                loadProviderFromJar(jar);
            }
        } catch (IOException e) {
            log.error("Failed to list plugins directory: {}", pluginsPath, e);
        }
    }

    private void loadProviderFromJar(Path jarPath) {
        try {
            log.info("Loading plugin JAR: {}", jarPath.getFileName());

            URL[] urls = { jarPath.toUri().toURL() };
            URLClassLoader classLoader = new URLClassLoader(urls, getClass().getClassLoader());

            @SuppressWarnings("rawtypes")
            ServiceLoader<DataProvider> loader = ServiceLoader.load(DataProvider.class, classLoader);

            for (DataProvider<?> provider : loader) {
                initializeAndRegister(provider, jarPath);
            }

        } catch (Exception e) {
            log.error("Failed to load plugin from JAR: {}", jarPath, e);
        }
    }

    private void initializeAndRegister(DataProvider<?> provider, Path jarPath) {
        try {
            String providerName = provider.getName();

            // Get provider-specific configuration
            Map<String, Object> config = getProviderConfig(providerName);

            // Initialize the provider with its configuration
            provider.initialize(config);

            // Register with the registry
            registry.register(provider);

            log.info("Loaded provider: {} from {}", providerName, jarPath.getFileName());

        } catch (Exception e) {
            log.error("Failed to initialize provider from JAR: {}", jarPath, e);
        }
    }

    private Map<String, Object> getProviderConfig(String providerName) {
        Map<String, Map<String, Object>> providers = properties.getPlugins().getProviders();

        if (providers != null && providers.containsKey(providerName)) {
            return providers.get(providerName);
        }

        return Map.of();
    }
}

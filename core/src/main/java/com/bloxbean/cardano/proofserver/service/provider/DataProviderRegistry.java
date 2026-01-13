package com.bloxbean.cardano.proofserver.service.provider;

import com.bloxbean.cardano.proofserver.exception.ProviderNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for managing data providers.
 * Supports both Spring-managed beans and dynamically loaded plugins.
 */
@Component
public class DataProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(DataProviderRegistry.class);

    private final Map<String, DataProvider<?>> providers;

    public DataProviderRegistry(List<DataProvider<?>> providerBeans) {
        this.providers = new ConcurrentHashMap<>();

        // Auto-register all provider beans
        for (DataProvider<?> provider : providerBeans) {
            register(provider);
        }

        log.info("DataProviderRegistry initialized with {} providers: {}",
                providers.size(), providers.keySet());
    }

    /**
     * Registers a data provider.
     * Can be called for both Spring beans and plugin-loaded providers.
     *
     * @param provider the provider to register
     */
    public void register(DataProvider<?> provider) {
        String name = provider.getName();
        if (providers.containsKey(name)) {
            log.warn("Overriding existing provider: {}", name);
        }

        providers.put(name, provider);
        log.info("Registered data provider: {} - {}", name, provider.getDescription());
    }

    /**
     * Gets a provider by name.
     *
     * @param name the provider name
     * @return the provider
     * @throws ProviderNotFoundException if the provider doesn't exist
     */
    public DataProvider<?> getProvider(String name) {
        DataProvider<?> provider = providers.get(name);
        if (provider == null) {
            throw new ProviderNotFoundException(name);
        }
        return provider;
    }

    /**
     * Gets a provider by name with type safety.
     */
    @SuppressWarnings("unchecked")
    public <T> DataProvider<T> getProvider(String name, Class<T> dataType) {
        DataProvider<?> provider = getProvider(name);

        if (!dataType.equals(provider.getDataType())) {
            throw new ClassCastException(
                String.format("Provider '%s' handles type %s, not %s",
                    name, provider.getDataType().getSimpleName(), dataType.getSimpleName())
            );
        }

        return (DataProvider<T>) provider;
    }

    public boolean hasProvider(String name) {
        return providers.containsKey(name);
    }

    public Set<String> getAvailableProviders() {
        return new HashSet<>(providers.keySet());
    }

    public List<ProviderInfo> getProviderInfo() {
        return providers.values().stream()
            .map(p -> new ProviderInfo(
                p.getName(),
                p.getDescription(),
                p.getDataType().getSimpleName()
            ))
            .sorted(Comparator.comparing(ProviderInfo::name))
            .collect(Collectors.toList());
    }

    public record ProviderInfo(String name, String description, String dataType) {}
}

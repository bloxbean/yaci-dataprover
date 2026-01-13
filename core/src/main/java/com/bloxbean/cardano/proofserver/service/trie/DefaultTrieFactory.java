package com.bloxbean.cardano.proofserver.service.trie;

import com.bloxbean.cardano.proofserver.config.TrieServerProperties;
import com.bloxbean.cardano.proofserver.exception.UnsupportedTrieTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Default implementation of TrieFactory.
 * Auto-registers all TrieProvider beans and provides trie creation.
 */
@Component
public class DefaultTrieFactory implements TrieFactory {

    private static final Logger log = LoggerFactory.getLogger(DefaultTrieFactory.class);

    private final TrieServerProperties properties;
    private final Map<String, TrieProvider> providers;

    public DefaultTrieFactory(TrieServerProperties properties,
                             List<TrieProvider> providerBeans) {
        this.properties = properties;
        this.providers = new ConcurrentHashMap<>();

        for (TrieProvider provider : providerBeans) {
            registerProvider(provider);
        }

        log.info("TrieFactory initialized with {} providers: {}",
                providers.size(), providers.keySet());
    }

    public void registerProvider(TrieProvider provider) {
        String type = provider.getType();
        if (providers.containsKey(type)) {
            log.warn("Overriding existing provider for type: {}", type);
        }

        providers.put(type, provider);
        log.info("Registered trie provider: {} - {}", type, provider.getDescription());
    }

    @Override
    public TrieImplementation createTrie(String type, TrieConfiguration config)
            throws UnsupportedTrieTypeException {

        String actualType = (type != null && !type.isBlank())
            ? type
            : properties.getDefaultTrieType();

        TrieProvider provider = providers.get(actualType);
        if (provider == null) {
            throw new UnsupportedTrieTypeException(actualType);
        }

        config.setTrieType(actualType);

        log.debug("Creating trie with type: {} for identifier: {}",
                 actualType, config.getIdentifier());

        return provider.create(config);
    }

    @Override
    public Set<String> getSupportedTypes() {
        return new HashSet<>(providers.keySet());
    }

    public List<ProviderInfo> getProviderInfo() {
        return providers.values().stream()
            .map(p -> new ProviderInfo(p.getType(), p.getDescription()))
            .sorted(Comparator.comparing(ProviderInfo::type))
            .collect(Collectors.toList());
    }

    public record ProviderInfo(String type, String description) {}
}

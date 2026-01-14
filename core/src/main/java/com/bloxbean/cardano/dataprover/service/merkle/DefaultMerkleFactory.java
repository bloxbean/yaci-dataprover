package com.bloxbean.cardano.dataprover.service.merkle;

import com.bloxbean.cardano.dataprover.config.DataProverProperties;
import com.bloxbean.cardano.dataprover.exception.UnsupportedMerkleSchemeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Default implementation of MerkleFactory.
 * Auto-registers all MerkleProvider beans and provides merkle creation.
 */
@Component
public class DefaultMerkleFactory implements MerkleFactory {

    private static final Logger log = LoggerFactory.getLogger(DefaultMerkleFactory.class);

    private final DataProverProperties properties;
    private final Map<String, MerkleProvider> providers;

    public DefaultMerkleFactory(DataProverProperties properties,
                               List<MerkleProvider> providerBeans) {
        this.properties = properties;
        this.providers = new ConcurrentHashMap<>();

        for (MerkleProvider provider : providerBeans) {
            registerProvider(provider);
        }

        log.info("MerkleFactory initialized with {} providers: {}",
                providers.size(), providers.keySet());
    }

    public void registerProvider(MerkleProvider provider) {
        String scheme = provider.getScheme();
        if (providers.containsKey(scheme)) {
            log.warn("Overriding existing provider for scheme: {}", scheme);
        }

        providers.put(scheme, provider);
        log.info("Registered merkle provider: {} - {}", scheme, provider.getDescription());
    }

    @Override
    public MerkleImplementation createMerkle(String scheme, MerkleConfiguration config)
            throws UnsupportedMerkleSchemeException {

        String actualScheme = (scheme != null && !scheme.isBlank())
            ? scheme
            : properties.getDefaultScheme();

        MerkleProvider provider = providers.get(actualScheme);
        if (provider == null) {
            throw new UnsupportedMerkleSchemeException(actualScheme);
        }

        config.setScheme(actualScheme);

        log.debug("Creating merkle with scheme: {} for identifier: {}",
                 actualScheme, config.getIdentifier());

        return provider.create(config);
    }

    @Override
    public Set<String> getSupportedSchemes() {
        return new HashSet<>(providers.keySet());
    }

    public List<ProviderInfo> getProviderInfo() {
        return providers.values().stream()
            .map(p -> new ProviderInfo(p.getScheme(), p.getDescription()))
            .sorted(Comparator.comparing(ProviderInfo::scheme))
            .collect(Collectors.toList());
    }

    public record ProviderInfo(String scheme, String description) {}
}

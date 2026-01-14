package com.bloxbean.cardano.dataprover.service.merkle;

import com.bloxbean.cardano.dataprover.config.DataProverProperties;
import com.bloxbean.cardano.dataprover.exception.MerkleNotFoundException;
import com.bloxbean.cardano.dataprover.model.MerkleMetadata;
import com.bloxbean.cardano.dataprover.repository.MerkleMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Registry for managing active merkle instances with LRU cache eviction.
 */
@Component
public class MerkleRegistry {

    private static final Logger log = LoggerFactory.getLogger(MerkleRegistry.class);

    private final DataProverProperties properties;
    private final MerkleMetadataRepository metadataRepository;
    private final MerkleFactory merkleFactory;

    private final Map<String, CachedMerkle> merkleCache;
    private final LinkedHashMap<String, Long> accessOrder;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private long cacheHits = 0;
    private long cacheMisses = 0;

    public MerkleRegistry(DataProverProperties properties,
                         MerkleMetadataRepository metadataRepository,
                         MerkleFactory merkleFactory) {
        this.properties = properties;
        this.metadataRepository = metadataRepository;
        this.merkleFactory = merkleFactory;

        int maxSize = properties.getCache().getMaxActiveMerkle();
        this.merkleCache = new ConcurrentHashMap<>(maxSize);
        this.accessOrder = new LinkedHashMap<>(maxSize, 0.75f, true);

        log.info("MerkleRegistry initialized with max cache size: {}", maxSize);
    }

    public MerkleImplementation getOrLoadMerkle(String identifier) {
        lock.readLock().lock();
        try {
            CachedMerkle cached = merkleCache.get(identifier);
            if (cached != null && !cached.isExpired()) {
                cacheHits++;
                updateAccessOrder(identifier);
                log.debug("Cache hit for merkle: {}", identifier);
                return cached.getMerkle();
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            CachedMerkle cached = merkleCache.get(identifier);
            if (cached != null && !cached.isExpired()) {
                cacheHits++;
                updateAccessOrder(identifier);
                return cached.getMerkle();
            }

            cacheMisses++;
            log.debug("Cache miss for merkle: {}", identifier);

            MerkleImplementation merkle = loadMerkle(identifier);

            evictIfNecessary();

            merkleCache.put(identifier, new CachedMerkle(merkle, properties.getCache().getTtlMinutes()));
            accessOrder.put(identifier, System.currentTimeMillis());

            log.info("Loaded and cached merkle: {} (cache size: {})", identifier, merkleCache.size());
            return merkle;

        } finally {
            lock.writeLock().unlock();
        }
    }

    private MerkleImplementation loadMerkle(String identifier) {
        MerkleMetadata metadata = metadataRepository.findByIdentifier(identifier)
            .orElseThrow(() -> new MerkleNotFoundException(identifier));

        MerkleConfiguration config = MerkleConfiguration.builder()
            .identifier(identifier)
            .scheme(metadata.getScheme())
            .storagePath(properties.getStorage().getRocksdbPath())
            .hashFunction("blake2b-256")
            .customConfig(metadata.getMetadata() != null ? metadata.getMetadata() : Map.of())
            .build();

        return merkleFactory.createMerkle(metadata.getScheme(), config);
    }

    public void registerMerkle(String identifier, MerkleImplementation merkle) {
        lock.writeLock().lock();
        try {
            evictIfNecessary();
            merkleCache.put(identifier, new CachedMerkle(merkle, properties.getCache().getTtlMinutes()));
            accessOrder.put(identifier, System.currentTimeMillis());
            log.info("Registered new merkle: {}", identifier);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void unloadMerkle(String identifier) {
        lock.writeLock().lock();
        try {
            CachedMerkle cached = merkleCache.remove(identifier);
            accessOrder.remove(identifier);

            if (cached != null) {
                try {
                    cached.getMerkle().close();
                    log.info("Unloaded merkle: {}", identifier);
                } catch (Exception e) {
                    log.error("Error closing merkle: {}", identifier, e);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean isCached(String identifier) {
        lock.readLock().lock();
        try {
            CachedMerkle cached = merkleCache.get(identifier);
            return cached != null && !cached.isExpired();
        } finally {
            lock.readLock().unlock();
        }
    }

    public Set<String> getCachedIdentifiers() {
        lock.readLock().lock();
        try {
            return new HashSet<>(merkleCache.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    private void evictIfNecessary() {
        int maxSize = properties.getCache().getMaxActiveMerkle();

        while (merkleCache.size() >= maxSize) {
            evictLeastRecentlyUsed();
        }
    }

    public void evictLeastRecentlyUsed() {
        lock.writeLock().lock();
        try {
            if (accessOrder.isEmpty()) {
                return;
            }

            Iterator<String> iterator = accessOrder.keySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }

            String lruIdentifier = iterator.next();
            iterator.remove();

            CachedMerkle cached = merkleCache.remove(lruIdentifier);
            if (cached != null) {
                try {
                    cached.getMerkle().close();
                    log.info("Evicted LRU merkle: {}", lruIdentifier);
                } catch (Exception e) {
                    log.error("Error closing evicted merkle: {}", lruIdentifier, e);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void updateAccessOrder(String identifier) {
        accessOrder.remove(identifier);
        accessOrder.put(identifier, System.currentTimeMillis());
    }

    public void clearCache() {
        lock.writeLock().lock();
        try {
            for (Map.Entry<String, CachedMerkle> entry : merkleCache.entrySet()) {
                try {
                    entry.getValue().getMerkle().close();
                } catch (Exception e) {
                    log.error("Error closing merkle during cache clear: {}", entry.getKey(), e);
                }
            }

            merkleCache.clear();
            accessOrder.clear();
            log.info("Cleared merkle cache");
        } finally {
            lock.writeLock().unlock();
        }
    }

    public CacheStats getCacheStats() {
        lock.readLock().lock();
        try {
            long total = cacheHits + cacheMisses;
            double hitRate = total > 0 ? (double) cacheHits / total : 0.0;

            return new CacheStats(
                merkleCache.size(),
                properties.getCache().getMaxActiveMerkle(),
                cacheHits,
                cacheMisses,
                hitRate
            );
        } finally {
            lock.readLock().unlock();
        }
    }

    private static class CachedMerkle {
        private final MerkleImplementation merkle;
        private final Instant cachedAt;
        private final long ttlMinutes;

        CachedMerkle(MerkleImplementation merkle, long ttlMinutes) {
            this.merkle = merkle;
            this.cachedAt = Instant.now();
            this.ttlMinutes = ttlMinutes;
        }

        MerkleImplementation getMerkle() {
            return merkle;
        }

        boolean isExpired() {
            if (ttlMinutes <= 0) {
                return false;
            }

            Instant expiresAt = cachedAt.plusSeconds(ttlMinutes * 60);
            return Instant.now().isAfter(expiresAt);
        }
    }

    public record CacheStats(
        int currentSize,
        int maxSize,
        long hits,
        long misses,
        double hitRate
    ) {}
}

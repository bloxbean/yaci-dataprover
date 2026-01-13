package com.bloxbean.cardano.proofserver.service.trie;

import com.bloxbean.cardano.proofserver.config.TrieServerProperties;
import com.bloxbean.cardano.proofserver.exception.TrieNotFoundException;
import com.bloxbean.cardano.proofserver.model.TrieMetadata;
import com.bloxbean.cardano.proofserver.repository.TrieMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Registry for managing active trie instances with LRU cache eviction.
 */
@Component
public class TrieRegistry {

    private static final Logger log = LoggerFactory.getLogger(TrieRegistry.class);

    private final TrieServerProperties properties;
    private final TrieMetadataRepository metadataRepository;
    private final TrieFactory trieFactory;

    private final Map<String, CachedTrie> trieCache;
    private final LinkedHashMap<String, Long> accessOrder;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private long cacheHits = 0;
    private long cacheMisses = 0;

    public TrieRegistry(TrieServerProperties properties,
                       TrieMetadataRepository metadataRepository,
                       TrieFactory trieFactory) {
        this.properties = properties;
        this.metadataRepository = metadataRepository;
        this.trieFactory = trieFactory;

        int maxSize = properties.getCache().getMaxActiveTries();
        this.trieCache = new ConcurrentHashMap<>(maxSize);
        this.accessOrder = new LinkedHashMap<>(maxSize, 0.75f, true);

        log.info("TrieRegistry initialized with max cache size: {}", maxSize);
    }

    public TrieImplementation getOrLoadTrie(String identifier) {
        lock.readLock().lock();
        try {
            CachedTrie cached = trieCache.get(identifier);
            if (cached != null && !cached.isExpired()) {
                cacheHits++;
                updateAccessOrder(identifier);
                log.debug("Cache hit for trie: {}", identifier);
                return cached.getTrie();
            }
        } finally {
            lock.readLock().unlock();
        }

        lock.writeLock().lock();
        try {
            CachedTrie cached = trieCache.get(identifier);
            if (cached != null && !cached.isExpired()) {
                cacheHits++;
                updateAccessOrder(identifier);
                return cached.getTrie();
            }

            cacheMisses++;
            log.debug("Cache miss for trie: {}", identifier);

            TrieImplementation trie = loadTrie(identifier);

            evictIfNecessary();

            trieCache.put(identifier, new CachedTrie(trie, properties.getCache().getTtlMinutes()));
            accessOrder.put(identifier, System.currentTimeMillis());

            log.info("Loaded and cached trie: {} (cache size: {})", identifier, trieCache.size());
            return trie;

        } finally {
            lock.writeLock().unlock();
        }
    }

    private TrieImplementation loadTrie(String identifier) {
        TrieMetadata metadata = metadataRepository.findByIdentifier(identifier)
            .orElseThrow(() -> new TrieNotFoundException(identifier));

        TrieConfiguration config = TrieConfiguration.builder()
            .identifier(identifier)
            .trieType(metadata.getTrieType())
            .storagePath(properties.getStorage().getRocksdbPath())
            .hashFunction("blake2b-256")
            .customConfig(metadata.getMetadata() != null ? metadata.getMetadata() : Map.of())
            .build();

        return trieFactory.createTrie(metadata.getTrieType(), config);
    }

    public void registerTrie(String identifier, TrieImplementation trie) {
        lock.writeLock().lock();
        try {
            evictIfNecessary();
            trieCache.put(identifier, new CachedTrie(trie, properties.getCache().getTtlMinutes()));
            accessOrder.put(identifier, System.currentTimeMillis());
            log.info("Registered new trie: {}", identifier);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void unloadTrie(String identifier) {
        lock.writeLock().lock();
        try {
            CachedTrie cached = trieCache.remove(identifier);
            accessOrder.remove(identifier);

            if (cached != null) {
                try {
                    cached.getTrie().close();
                    log.info("Unloaded trie: {}", identifier);
                } catch (Exception e) {
                    log.error("Error closing trie: {}", identifier, e);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean isCached(String identifier) {
        lock.readLock().lock();
        try {
            CachedTrie cached = trieCache.get(identifier);
            return cached != null && !cached.isExpired();
        } finally {
            lock.readLock().unlock();
        }
    }

    public Set<String> getCachedIdentifiers() {
        lock.readLock().lock();
        try {
            return new HashSet<>(trieCache.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    private void evictIfNecessary() {
        int maxSize = properties.getCache().getMaxActiveTries();

        while (trieCache.size() >= maxSize) {
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

            CachedTrie cached = trieCache.remove(lruIdentifier);
            if (cached != null) {
                try {
                    cached.getTrie().close();
                    log.info("Evicted LRU trie: {}", lruIdentifier);
                } catch (Exception e) {
                    log.error("Error closing evicted trie: {}", lruIdentifier, e);
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
            for (Map.Entry<String, CachedTrie> entry : trieCache.entrySet()) {
                try {
                    entry.getValue().getTrie().close();
                } catch (Exception e) {
                    log.error("Error closing trie during cache clear: {}", entry.getKey(), e);
                }
            }

            trieCache.clear();
            accessOrder.clear();
            log.info("Cleared trie cache");
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
                trieCache.size(),
                properties.getCache().getMaxActiveTries(),
                cacheHits,
                cacheMisses,
                hitRate
            );
        } finally {
            lock.readLock().unlock();
        }
    }

    private static class CachedTrie {
        private final TrieImplementation trie;
        private final Instant cachedAt;
        private final long ttlMinutes;

        CachedTrie(TrieImplementation trie, long ttlMinutes) {
            this.trie = trie;
            this.cachedAt = Instant.now();
            this.ttlMinutes = ttlMinutes;
        }

        TrieImplementation getTrie() {
            return trie;
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

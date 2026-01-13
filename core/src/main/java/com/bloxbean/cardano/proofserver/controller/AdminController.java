package com.bloxbean.cardano.proofserver.controller;

import com.bloxbean.cardano.proofserver.service.storage.RocksDbManager;
import com.bloxbean.cardano.proofserver.service.trie.TrieRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for administrative operations.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final TrieRegistry trieRegistry;
    private final RocksDbManager rocksDbManager;

    public AdminController(TrieRegistry trieRegistry, RocksDbManager rocksDbManager) {
        this.trieRegistry = trieRegistry;
        this.rocksDbManager = rocksDbManager;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        log.debug("Health check requested");

        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("rocksdb", rocksDbManager.getDb() != null ? "AVAILABLE" : "UNAVAILABLE");

        return ResponseEntity.ok(health);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        log.debug("System statistics requested");

        Map<String, Object> stats = new HashMap<>();

        var cacheStats = trieRegistry.getCacheStats();
        Map<String, Object> cacheMap = new HashMap<>();
        cacheMap.put("currentSize", cacheStats.currentSize());
        cacheMap.put("maxSize", cacheStats.maxSize());
        cacheMap.put("cacheHits", cacheStats.hits());
        cacheMap.put("cacheMisses", cacheStats.misses());
        cacheMap.put("hitRate", String.format("%.2f%%", cacheStats.hitRate() * 100));

        stats.put("cache", cacheMap);

        Map<String, Object> rocksStats = new HashMap<>();
        rocksStats.put("isOpen", rocksDbManager.getDb() != null);
        rocksStats.put("columnFamilyCount", rocksDbManager.getActiveColumnFamilies().size());

        stats.put("rocksdb", rocksStats);

        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/cache/evict/{trieId}")
    public ResponseEntity<Map<String, String>> evictTrie(@PathVariable String trieId) {
        log.info("Evicting trie from cache: {}", trieId);

        trieRegistry.unloadTrie(trieId);

        return ResponseEntity.ok(Map.of(
                "message", "Trie evicted from cache",
                "trieId", trieId
        ));
    }

    @DeleteMapping("/cache/clear")
    public ResponseEntity<Map<String, Object>> clearCache() {
        log.info("Clearing all tries from cache");

        var cachedIdentifiers = trieRegistry.getCachedIdentifiers();
        int activeCount = cachedIdentifiers.size();

        for (String identifier : cachedIdentifiers) {
            trieRegistry.unloadTrie(identifier);
        }

        return ResponseEntity.ok(Map.of(
                "message", "Cache cleared",
                "evictedTries", activeCount
        ));
    }

    @GetMapping("/cache")
    public ResponseEntity<Map<String, Object>> getCacheInfo() {
        log.debug("Cache info requested");

        var cacheStats = trieRegistry.getCacheStats();
        var cachedIdentifiers = trieRegistry.getCachedIdentifiers();

        Map<String, Object> info = new HashMap<>();
        info.put("cachedTries", cachedIdentifiers);
        info.put("currentSize", cacheStats.currentSize());
        info.put("maxSize", cacheStats.maxSize());
        info.put("cacheHits", cacheStats.hits());
        info.put("cacheMisses", cacheStats.misses());
        info.put("hitRate", String.format("%.2f%%", cacheStats.hitRate() * 100));

        return ResponseEntity.ok(info);
    }

    @GetMapping("/storage")
    public ResponseEntity<Map<String, Object>> getStorageInfo() {
        log.debug("Storage info requested");

        var columnFamilies = rocksDbManager.getActiveColumnFamilies();

        Map<String, Object> info = new HashMap<>();
        info.put("isOpen", rocksDbManager.getDb() != null);
        info.put("columnFamilies", columnFamilies);
        info.put("columnFamilyCount", columnFamilies.size());

        return ResponseEntity.ok(info);
    }
}

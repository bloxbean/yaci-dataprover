package com.bloxbean.cardano.dataprover.controller;

import com.bloxbean.cardano.dataprover.service.storage.RocksDbManager;
import com.bloxbean.cardano.dataprover.service.merkle.MerkleRegistry;
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

    private final MerkleRegistry merkleRegistry;
    private final RocksDbManager rocksDbManager;

    public AdminController(MerkleRegistry merkleRegistry, RocksDbManager rocksDbManager) {
        this.merkleRegistry = merkleRegistry;
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

        var cacheStats = merkleRegistry.getCacheStats();
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

    @DeleteMapping("/cache/evict/{merkleId}")
    public ResponseEntity<Map<String, String>> evictMerkle(@PathVariable String merkleId) {
        log.info("Evicting merkle from cache: {}", merkleId);

        merkleRegistry.unloadMerkle(merkleId);

        return ResponseEntity.ok(Map.of(
                "message", "Merkle evicted from cache",
                "merkleId", merkleId
        ));
    }

    @DeleteMapping("/cache/clear")
    public ResponseEntity<Map<String, Object>> clearCache() {
        log.info("Clearing all merkle from cache");

        var cachedIdentifiers = merkleRegistry.getCachedIdentifiers();
        int activeCount = cachedIdentifiers.size();

        for (String identifier : cachedIdentifiers) {
            merkleRegistry.unloadMerkle(identifier);
        }

        return ResponseEntity.ok(Map.of(
                "message", "Cache cleared",
                "evictedMerkle", activeCount
        ));
    }

    @GetMapping("/cache")
    public ResponseEntity<Map<String, Object>> getCacheInfo() {
        log.debug("Cache info requested");

        var cacheStats = merkleRegistry.getCacheStats();
        var cachedIdentifiers = merkleRegistry.getCachedIdentifiers();

        Map<String, Object> info = new HashMap<>();
        info.put("cachedMerkle", cachedIdentifiers);
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

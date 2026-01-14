package com.bloxbean.cardano.dataprover.service.storage;

import com.bloxbean.cardano.dataprover.config.DataProverProperties;
import com.bloxbean.cardano.dataprover.exception.MerkleOperationException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.rocksdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages RocksDB lifecycle and column family operations.
 */
@Component
public class RocksDbManager {

    private static final Logger log = LoggerFactory.getLogger(RocksDbManager.class);

    private static final String ROOTS_CF_NAME = "roots";
    private static final String DEFAULT_CF_NAME = "default";

    private final DataProverProperties properties;
    private final Map<String, ColumnFamilyHandle> columnFamilyHandles;

    private RocksDB db;
    private ColumnFamilyHandle rootsHandle;
    private ColumnFamilyHandle defaultHandle;
    private DBOptions dbOptions;
    private ColumnFamilyOptions cfOptions;

    public RocksDbManager(DataProverProperties properties) {
        this.properties = properties;
        this.columnFamilyHandles = new ConcurrentHashMap<>();
    }

    @PostConstruct
    public void initialize() {
        try {
            log.info("Initializing RocksDB at path: {}", properties.getStorage().getRocksdbPath());
            RocksDB.loadLibrary();

            Path dbPath = Paths.get(properties.getStorage().getRocksdbPath());
            if (!Files.exists(dbPath)) {
                Files.createDirectories(dbPath);
                log.info("Created RocksDB directory: {}", dbPath);
            }

            this.db = openDatabase();
            this.rootsHandle = getOrCreateColumnFamily(ROOTS_CF_NAME);

            log.info("RocksDB initialized successfully with {} existing column families",
                     columnFamilyHandles.size());
        } catch (Exception e) {
            log.error("Failed to initialize RocksDB", e);
            throw new MerkleOperationException("Failed to initialize RocksDB", e);
        }
    }

    private RocksDB openDatabase() throws RocksDBException {
        String dbPath = properties.getStorage().getRocksdbPath();

        this.dbOptions = createDbOptions();
        this.cfOptions = createColumnFamilyOptions();

        List<byte[]> existingCfNames;
        try {
            existingCfNames = RocksDB.listColumnFamilies(new Options(), dbPath);
            if (existingCfNames.isEmpty()) {
                existingCfNames = List.of(DEFAULT_CF_NAME.getBytes());
            }
        } catch (RocksDBException e) {
            existingCfNames = List.of(DEFAULT_CF_NAME.getBytes());
        }

        List<ColumnFamilyDescriptor> cfDescriptors = new ArrayList<>();
        for (byte[] cfName : existingCfNames) {
            cfDescriptors.add(new ColumnFamilyDescriptor(cfName, cfOptions));
        }

        List<ColumnFamilyHandle> handles = new ArrayList<>();
        RocksDB db = RocksDB.open(dbOptions, dbPath, cfDescriptors, handles);

        for (int i = 0; i < cfDescriptors.size(); i++) {
            String cfName = new String(cfDescriptors.get(i).getName());
            ColumnFamilyHandle handle = handles.get(i);

            if (DEFAULT_CF_NAME.equals(cfName)) {
                this.defaultHandle = handle;
            } else {
                columnFamilyHandles.put(cfName, handle);
            }

            log.debug("Loaded column family: {}", cfName);
        }

        return db;
    }

    private DBOptions createDbOptions() {
        return new DBOptions()
            .setCreateIfMissing(properties.getStorage().getCreateIfMissing())
            .setCreateMissingColumnFamilies(true)
            .setMaxOpenFiles(properties.getStorage().getMaxOpenFiles())
            .setStatsDumpPeriodSec(300)
            .setKeepLogFileNum(10);
    }

    private ColumnFamilyOptions createColumnFamilyOptions() {
        DataProverProperties.StorageProperties storage = properties.getStorage();

        BlockBasedTableConfig tableConfig = new BlockBasedTableConfig()
            .setBlockSize(16 * 1024)
            .setBlockCache(new LRUCache(storage.getCacheSizeMb() * 1024L * 1024L))
            .setFilterPolicy(new BloomFilter(10, false))
            .setCacheIndexAndFilterBlocks(true)
            .setPinL0FilterAndIndexBlocksInCache(true);

        CompressionType compression = parseCompressionType(storage.getCompression());

        return new ColumnFamilyOptions()
            .setTableFormatConfig(tableConfig)
            .setWriteBufferSize(storage.getWriteBufferSizeMb() * 1024L * 1024L)
            .setMaxWriteBufferNumber(3)
            .setCompressionType(compression)
            .setLevel0FileNumCompactionTrigger(4)
            .setTargetFileSizeBase(64 * 1024 * 1024);
    }

    private CompressionType parseCompressionType(String compression) {
        if (compression == null) {
            return CompressionType.LZ4_COMPRESSION;
        }

        return switch (compression.toUpperCase()) {
            case "NONE" -> CompressionType.NO_COMPRESSION;
            case "SNAPPY" -> CompressionType.SNAPPY_COMPRESSION;
            case "ZLIB" -> CompressionType.ZLIB_COMPRESSION;
            case "LZ4" -> CompressionType.LZ4_COMPRESSION;
            case "LZ4HC" -> CompressionType.LZ4HC_COMPRESSION;
            case "ZSTD" -> CompressionType.ZSTD_COMPRESSION;
            default -> {
                log.warn("Unknown compression type: {}, defaulting to LZ4", compression);
                yield CompressionType.LZ4_COMPRESSION;
            }
        };
    }

    public synchronized ColumnFamilyHandle getOrCreateColumnFamily(String identifier) {
        ColumnFamilyHandle existing = columnFamilyHandles.get(identifier);
        if (existing != null) {
            return existing;
        }

        try {
            ColumnFamilyDescriptor cfDescriptor =
                new ColumnFamilyDescriptor(identifier.getBytes(), cfOptions);

            ColumnFamilyHandle handle = db.createColumnFamily(cfDescriptor);
            columnFamilyHandles.put(identifier, handle);

            log.info("Created column family for trie: {}", identifier);
            return handle;

        } catch (RocksDBException e) {
            log.error("Failed to create column family for trie: {}", identifier, e);
            throw new MerkleOperationException(
                "Failed to create column family for trie: " + identifier, e);
        }
    }

    public synchronized void deleteColumnFamily(String identifier) {
        ColumnFamilyHandle handle = columnFamilyHandles.remove(identifier);
        if (handle == null) {
            log.warn("Attempted to delete non-existent column family: {}", identifier);
            return;
        }

        try {
            db.dropColumnFamily(handle);
            handle.close();
            log.info("Deleted column family for trie: {}", identifier);
        } catch (RocksDBException e) {
            log.error("Failed to delete column family for trie: {}", identifier, e);
            throw new MerkleOperationException(
                "Failed to delete column family for trie: " + identifier, e);
        }
    }

    public void persistRootHash(String identifier, byte[] rootHash) {
        try {
            db.put(rootsHandle, identifier.getBytes(), rootHash);
            log.debug("Persisted root hash for trie: {}", identifier);
        } catch (RocksDBException e) {
            log.error("Failed to persist root hash for trie: {}", identifier, e);
            throw new MerkleOperationException(
                "Failed to persist root hash for trie: " + identifier, e);
        }
    }

    public Optional<byte[]> loadRootHash(String identifier) {
        try {
            byte[] rootHash = db.get(rootsHandle, identifier.getBytes());
            return Optional.ofNullable(rootHash);
        } catch (RocksDBException e) {
            log.error("Failed to load root hash for trie: {}", identifier, e);
            throw new MerkleOperationException(
                "Failed to load root hash for trie: " + identifier, e);
        }
    }

    public RocksDB getDb() {
        return db;
    }

    public ColumnFamilyHandle getRootsHandle() {
        return rootsHandle;
    }

    public Set<String> getActiveColumnFamilies() {
        return new HashSet<>(columnFamilyHandles.keySet());
    }

    public void flush() {
        try {
            FlushOptions flushOptions = new FlushOptions().setWaitForFlush(true);
            db.flush(flushOptions);
            log.debug("Flushed RocksDB to disk");
        } catch (RocksDBException e) {
            log.error("Failed to flush RocksDB", e);
            throw new MerkleOperationException("Failed to flush RocksDB", e);
        }
    }

    @PreDestroy
    public void close() {
        log.info("Closing RocksDB...");

        for (Map.Entry<String, ColumnFamilyHandle> entry : columnFamilyHandles.entrySet()) {
            try {
                entry.getValue().close();
                log.debug("Closed column family: {}", entry.getKey());
            } catch (Exception e) {
                log.error("Error closing column family: {}", entry.getKey(), e);
            }
        }

        if (rootsHandle != null) {
            rootsHandle.close();
        }
        if (defaultHandle != null) {
            defaultHandle.close();
        }

        if (cfOptions != null) {
            cfOptions.close();
        }
        if (dbOptions != null) {
            dbOptions.close();
        }

        if (db != null) {
            db.close();
        }

        log.info("RocksDB closed successfully");
    }
}

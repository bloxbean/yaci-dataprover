package com.bloxbean.cardano.dataprover.service.merkle;

import com.bloxbean.cardano.dataprover.service.storage.RocksDbManager;
import com.bloxbean.cardano.vds.mpt.rocksdb.RocksDbNodeStore;
import org.rocksdb.ColumnFamilyHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Provider for creating MPF (Merkle Patricia Forestry) merkle instances.
 * Uses Cardano Client Library's SecureTrie with Blake2b-256 hashing.
 */
@Component
public class MpfMerkleProvider implements MerkleProvider {

    private static final Logger log = LoggerFactory.getLogger(MpfMerkleProvider.class);
    private static final String SCHEME = "mpf";

    private final RocksDbManager rocksDbManager;

    public MpfMerkleProvider(RocksDbManager rocksDbManager) {
        this.rocksDbManager = rocksDbManager;
        log.info("MPF merkle provider initialized");
    }

    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public MerkleImplementation create(MerkleConfiguration config) {
        String identifier = config.getIdentifier();
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Merkle identifier is required");
        }

        ColumnFamilyHandle columnFamilyHandle =
            rocksDbManager.getOrCreateColumnFamily(identifier);

        RocksDbNodeStore nodeStore = new RocksDbNodeStore(
            rocksDbManager.getDb(),
            columnFamilyHandle
        );

        String rootHash = config.getRootHash();
        boolean storeOriginalKeys = config.isStoreOriginalKeys();
        MpfMerkleImplementation merkle = new MpfMerkleImplementation(identifier, nodeStore, rootHash, storeOriginalKeys);

        log.info("Created MPF merkle: {} (rootHash: {}, storeOriginalKeys: {})",
            identifier, rootHash != null ? "present" : "null", storeOriginalKeys);
        return merkle;
    }

    @Override
    public String getDescription() {
        return "Merkle Patricia Forestry (MPF) with Blake2b-256 hashing - Cardano compatible";
    }
}

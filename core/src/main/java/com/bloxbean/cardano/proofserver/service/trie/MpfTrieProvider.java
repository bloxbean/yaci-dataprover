package com.bloxbean.cardano.proofserver.service.trie;

import com.bloxbean.cardano.proofserver.service.storage.RocksDbManager;
import com.bloxbean.cardano.vds.mpt.rocksdb.RocksDbNodeStore;
import org.rocksdb.ColumnFamilyHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Provider for creating MPF (Merkle Patricia Forestry) trie instances.
 * Uses Cardano Client Library's SecureTrie with Blake2b-256 hashing.
 */
@Component
public class MpfTrieProvider implements TrieProvider {

    private static final Logger log = LoggerFactory.getLogger(MpfTrieProvider.class);
    private static final String TYPE = "mpf";

    private final RocksDbManager rocksDbManager;

    public MpfTrieProvider(RocksDbManager rocksDbManager) {
        this.rocksDbManager = rocksDbManager;
        log.info("MPF trie provider initialized");
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public TrieImplementation create(TrieConfiguration config) {
        String identifier = config.getIdentifier();
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Trie identifier is required");
        }

        ColumnFamilyHandle columnFamilyHandle =
            rocksDbManager.getOrCreateColumnFamily(identifier);

        RocksDbNodeStore nodeStore = new RocksDbNodeStore(
            rocksDbManager.getDb(),
            columnFamilyHandle
        );

        MpfTrieImplementation trie = new MpfTrieImplementation(identifier, nodeStore);

        log.info("Created MPF trie: {}", identifier);
        return trie;
    }

    @Override
    public String getDescription() {
        return "Merkle Patricia Forestry (MPF) with Blake2b-256 hashing - Cardano compatible";
    }
}

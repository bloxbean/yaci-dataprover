package com.bloxbean.cardano.dataprover.service.merkle;

import com.bloxbean.cardano.dataprover.exception.MerkleOperationException;
import com.bloxbean.cardano.vds.mpf.MpfTrie;
import com.bloxbean.cardano.vds.mpf.rocksdb.RocksDbNodeStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * MPF (Merkle Patricia Forestry) merkle implementation.
 * Wraps Cardano Client Library's SecureTrie with Blake2b-256 hashing.
 */
public class MpfMerkleImplementation implements MerkleImplementation {

    private static final Logger log = LoggerFactory.getLogger(MpfMerkleImplementation.class);
    private static final String SCHEME = "mpf";
    private static final HexFormat HEX = HexFormat.of();

    private final String identifier;
    private final MpfTrie trie;
    private final RocksDbNodeStore nodeStore;
    private final boolean storeOriginalKeys;

    private long operationCount = 0;

    public MpfMerkleImplementation(String identifier, RocksDbNodeStore nodeStore, String rootHashHex) {
        this(identifier, nodeStore, rootHashHex, false);
    }

    public MpfMerkleImplementation(String identifier, RocksDbNodeStore nodeStore, String rootHashHex, boolean storeOriginalKeys) {
        this.identifier = identifier;
        this.nodeStore = nodeStore;
        this.storeOriginalKeys = storeOriginalKeys;

        if (rootHashHex != null && !rootHashHex.isBlank()) {
            byte[] rootHash = HEX.parseHex(rootHashHex);
            this.trie = new MpfTrie(nodeStore, rootHash);
            log.debug("Created MPF merkle implementation for: {} with existing root hash (storeOriginalKeys: {})", identifier, storeOriginalKeys);
        } else {
            this.trie = new MpfTrie(nodeStore, null);
            log.debug("Created MPF merkle implementation for: {} (new trie, storeOriginalKeys: {})", identifier, storeOriginalKeys);
        }
    }

    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public void put(byte[] key, byte[] value) throws MerkleOperationException {
        try {
            trie.put(key, value);
            operationCount++;

            if (operationCount % 1000 == 0) {
                log.debug("MPF merkle {} has processed {} operations", identifier, operationCount);
            }
        } catch (Exception e) {
            log.error("Failed to put entry in MPF merkle: {}", identifier, e);
            throw new MerkleOperationException("Failed to put entry in MPF merkle", e);
        }
    }

    @Override
    public Optional<byte[]> get(byte[] key) throws MerkleOperationException {
        try {
            byte[] value = trie.get(key);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            log.error("Failed to get entry from MPF merkle: {}", identifier, e);
            throw new MerkleOperationException("Failed to get entry from MPF merkle", e);
        }
    }

    @Override
    public Optional<byte[]> getProofWire(byte[] key) throws MerkleOperationException {
        try {
            return trie.getProofWire(key);
        } catch (Exception e) {
            log.error("Failed to generate proof for MPF merkle: {}", identifier, e);
            throw new MerkleOperationException("Failed to generate proof for MPF merkle", e);
        }
    }

    @Override
    public byte[] getRootHash() {
        try {
            return trie.getRootHash();
        } catch (Exception e) {
            log.error("Failed to get root hash from MPF merkle: {}", identifier, e);
            throw new MerkleOperationException("Failed to get root hash from MPF merkle", e);
        }
    }

    @Override
    public boolean verifyProofWire(byte[] rootHash, byte[] key, byte[] value,
                                   boolean expectedPresence, Optional<byte[]> proof) {
        try {
            if (proof.isEmpty()) {
                return false;
            }
            return trie.verifyProofWire(rootHash, key, value, expectedPresence, proof.get());
        } catch (Exception e) {
            log.error("Failed to verify proof for MPF merkle: {}", identifier, e);
            return false;
        }
    }

    @Override
    public long size() {
        try {
            return trie.computeSize();
        } catch (Exception e) {
            log.error("Failed to compute size for MPF merkle: {}", identifier, e);
            throw new MerkleOperationException("Failed to compute size", e);
        }
    }

    @Override
    public void commit() throws MerkleOperationException {
        try {
            log.debug("Committed MPF merkle: {} ({} operations)", identifier, operationCount);
        } catch (Exception e) {
            log.error("Failed to commit MPF merkle: {}", identifier, e);
            throw new MerkleOperationException("Failed to commit MPF merkle", e);
        }
    }

    @Override
    public List<Entry> getEntries(int maxEntries) throws MerkleOperationException {
        try {
            return trie.getAllEntries().stream()
                .limit(maxEntries)
                .map(e -> new Entry(e.getKey(), e.getPath(), e.getValue()))
                .toList();
        } catch (Exception e) {
            log.error("Failed to get entries for MPF merkle: {}", identifier, e);
            throw new MerkleOperationException("Failed to get entries", e);
        }
    }

    @Override
    public void close() {
        try {
            commit();
            log.debug("Closed MPF merkle: {}", identifier);
        } catch (Exception e) {
            log.error("Error closing MPF merkle: {}", identifier, e);
        }
    }

    public MpfTrie getTrie() {
        return trie;
    }

    public RocksDbNodeStore getNodeStore() {
        return nodeStore;
    }

    public String getIdentifier() {
        return identifier;
    }

    public long getOperationCount() {
        return operationCount;
    }

    public boolean isStoreOriginalKeys() {
        return storeOriginalKeys;
    }
}

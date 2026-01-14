package com.bloxbean.cardano.dataprover.service.merkle;

import com.bloxbean.cardano.dataprover.exception.MerkleOperationException;
import com.bloxbean.cardano.vds.core.hash.Blake2b256;
import com.bloxbean.cardano.vds.mpt.SecureTrie;
import com.bloxbean.cardano.vds.mpt.rocksdb.RocksDbNodeStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * MPF (Merkle Patricia Forestry) merkle implementation.
 * Wraps Cardano Client Library's SecureTrie with Blake2b-256 hashing.
 */
public class MpfMerkleImplementation implements MerkleImplementation {

    private static final Logger log = LoggerFactory.getLogger(MpfMerkleImplementation.class);
    private static final String SCHEME = "mpf";

    private final String identifier;
    private final SecureTrie trie;
    private final RocksDbNodeStore nodeStore;

    private long operationCount = 0;

    public MpfMerkleImplementation(String identifier, RocksDbNodeStore nodeStore) {
        this.identifier = identifier;
        this.nodeStore = nodeStore;
        this.trie = new SecureTrie(nodeStore, Blake2b256::digest);

        log.debug("Created MPF merkle implementation for: {}", identifier);
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
        return 0;
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
    public void close() {
        try {
            commit();
            log.debug("Closed MPF merkle: {}", identifier);
        } catch (Exception e) {
            log.error("Error closing MPF merkle: {}", identifier, e);
        }
    }

    public SecureTrie getSecureTrie() {
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
}

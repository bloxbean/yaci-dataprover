package com.bloxbean.cardano.proofserver.service.trie;

import com.bloxbean.cardano.proofserver.exception.TrieOperationException;
import com.bloxbean.cardano.vds.core.hash.Blake2b256;
import com.bloxbean.cardano.vds.mpt.SecureTrie;
import com.bloxbean.cardano.vds.mpt.rocksdb.RocksDbNodeStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * MPF (Merkle Patricia Forestry) trie implementation.
 * Wraps Cardano Client Library's SecureTrie with Blake2b-256 hashing.
 */
public class MpfTrieImplementation implements TrieImplementation {

    private static final Logger log = LoggerFactory.getLogger(MpfTrieImplementation.class);
    private static final String TYPE = "mpf";

    private final String identifier;
    private final SecureTrie trie;
    private final RocksDbNodeStore nodeStore;

    private long operationCount = 0;

    public MpfTrieImplementation(String identifier, RocksDbNodeStore nodeStore) {
        this.identifier = identifier;
        this.nodeStore = nodeStore;
        this.trie = new SecureTrie(nodeStore, Blake2b256::digest);

        log.debug("Created MPF trie implementation for: {}", identifier);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void put(byte[] key, byte[] value) throws TrieOperationException {
        try {
            trie.put(key, value);
            operationCount++;

            if (operationCount % 1000 == 0) {
                log.debug("MPF trie {} has processed {} operations", identifier, operationCount);
            }
        } catch (Exception e) {
            log.error("Failed to put entry in MPF trie: {}", identifier, e);
            throw new TrieOperationException("Failed to put entry in MPF trie", e);
        }
    }

    @Override
    public Optional<byte[]> get(byte[] key) throws TrieOperationException {
        try {
            byte[] value = trie.get(key);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            log.error("Failed to get entry from MPF trie: {}", identifier, e);
            throw new TrieOperationException("Failed to get entry from MPF trie", e);
        }
    }

    @Override
    public Optional<byte[]> getProofWire(byte[] key) throws TrieOperationException {
        try {
            return trie.getProofWire(key);
        } catch (Exception e) {
            log.error("Failed to generate proof for MPF trie: {}", identifier, e);
            throw new TrieOperationException("Failed to generate proof for MPF trie", e);
        }
    }

    @Override
    public byte[] getRootHash() {
        try {
            return trie.getRootHash();
        } catch (Exception e) {
            log.error("Failed to get root hash from MPF trie: {}", identifier, e);
            throw new TrieOperationException("Failed to get root hash from MPF trie", e);
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
            log.error("Failed to verify proof for MPF trie: {}", identifier, e);
            return false;
        }
    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public void commit() throws TrieOperationException {
        try {
            log.debug("Committed MPF trie: {} ({} operations)", identifier, operationCount);
        } catch (Exception e) {
            log.error("Failed to commit MPF trie: {}", identifier, e);
            throw new TrieOperationException("Failed to commit MPF trie", e);
        }
    }

    @Override
    public void close() {
        try {
            commit();
            log.debug("Closed MPF trie: {}", identifier);
        } catch (Exception e) {
            log.error("Error closing MPF trie: {}", identifier, e);
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

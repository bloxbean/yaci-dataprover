package com.bloxbean.cardano.proofserver.service.trie;

import com.bloxbean.cardano.proofserver.exception.TrieOperationException;

import java.util.Optional;

/**
 * Core abstraction for trie implementations.
 * Supports different trie types (MPF, JMT, etc.) through a common interface.
 */
public interface TrieImplementation extends AutoCloseable {

    String getType();

    void put(byte[] key, byte[] value) throws TrieOperationException;

    Optional<byte[]> get(byte[] key) throws TrieOperationException;

    Optional<byte[]> getProofWire(byte[] key) throws TrieOperationException;

    byte[] getRootHash();

    boolean verifyProofWire(byte[] rootHash, byte[] key, byte[] value,
                            boolean expectedPresence, Optional<byte[]> proof);

    long size();

    void commit() throws TrieOperationException;

    @Override
    void close();
}

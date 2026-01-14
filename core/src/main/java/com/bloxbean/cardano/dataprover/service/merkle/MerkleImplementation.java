package com.bloxbean.cardano.dataprover.service.merkle;

import com.bloxbean.cardano.dataprover.exception.MerkleOperationException;

import java.util.Optional;

/**
 * Core abstraction for merkle implementations.
 * Supports different merkle schemes (MPF, JMT, etc.) through a common interface.
 */
public interface MerkleImplementation extends AutoCloseable {

    String getScheme();

    void put(byte[] key, byte[] value) throws MerkleOperationException;

    Optional<byte[]> get(byte[] key) throws MerkleOperationException;

    Optional<byte[]> getProofWire(byte[] key) throws MerkleOperationException;

    byte[] getRootHash();

    boolean verifyProofWire(byte[] rootHash, byte[] key, byte[] value,
                            boolean expectedPresence, Optional<byte[]> proof);

    long size();

    void commit() throws MerkleOperationException;

    @Override
    void close();
}

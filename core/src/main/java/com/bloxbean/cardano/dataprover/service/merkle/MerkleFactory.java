package com.bloxbean.cardano.dataprover.service.merkle;

import com.bloxbean.cardano.dataprover.exception.UnsupportedMerkleSchemeException;

import java.util.Set;

/**
 * Factory for creating merkle instances.
 */
public interface MerkleFactory {

    MerkleImplementation createMerkle(String scheme, MerkleConfiguration config)
            throws UnsupportedMerkleSchemeException;

    Set<String> getSupportedSchemes();
}

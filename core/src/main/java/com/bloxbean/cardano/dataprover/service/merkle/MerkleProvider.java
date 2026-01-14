package com.bloxbean.cardano.dataprover.service.merkle;

/**
 * Provider interface for creating merkle instances.
 */
public interface MerkleProvider {

    String getScheme();

    MerkleImplementation create(MerkleConfiguration config);

    String getDescription();
}

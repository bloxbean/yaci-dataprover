package com.bloxbean.cardano.proofserver.service.trie;

/**
 * Provider interface for creating trie instances.
 */
public interface TrieProvider {

    String getType();

    TrieImplementation create(TrieConfiguration config);

    String getDescription();
}

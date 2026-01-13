package com.bloxbean.cardano.proofserver.service.trie;

import com.bloxbean.cardano.proofserver.exception.UnsupportedTrieTypeException;

import java.util.Set;

/**
 * Factory for creating trie instances.
 */
public interface TrieFactory {

    TrieImplementation createTrie(String type, TrieConfiguration config)
            throws UnsupportedTrieTypeException;

    Set<String> getSupportedTypes();
}

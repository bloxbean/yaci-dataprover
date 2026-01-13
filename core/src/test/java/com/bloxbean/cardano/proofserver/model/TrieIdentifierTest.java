package com.bloxbean.cardano.proofserver.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrieIdentifierTest {

    @Test
    void testValidTrieIdentifier() {
        TrieIdentifier id = TrieIdentifier.of("epoch-stake-500");

        assertNotNull(id);
        assertEquals("epoch-stake-500", id.getValue());
        assertEquals("epoch-stake-500", id.toString());
    }

    @Test
    void testValidTrieIdentifierNormalized() {
        TrieIdentifier id = TrieIdentifier.of("EPOCH-STAKE");

        assertEquals("epoch-stake", id.getValue());
    }

    @Test
    void testValidTrieIdentifierShort() {
        TrieIdentifier id = TrieIdentifier.of("abc");

        assertEquals("abc", id.getValue());
    }

    @Test
    void testTrieIdentifierEquality() {
        TrieIdentifier id1 = TrieIdentifier.of("test-trie");
        TrieIdentifier id2 = TrieIdentifier.of("TEST-TRIE");
        TrieIdentifier id3 = TrieIdentifier.of("other-trie");

        assertEquals(id1, id2);
        assertNotEquals(id1, id3);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void testInvalidTrieIdentifierBlank() {
        assertThrows(IllegalArgumentException.class, () -> TrieIdentifier.of(null));
        assertThrows(IllegalArgumentException.class, () -> TrieIdentifier.of(""));
        assertThrows(IllegalArgumentException.class, () -> TrieIdentifier.of("   "));
    }

    @Test
    void testInvalidTrieIdentifierSpecialChars() {
        assertThrows(IllegalArgumentException.class, () -> TrieIdentifier.of("test_trie"));
        assertThrows(IllegalArgumentException.class, () -> TrieIdentifier.of("test.trie"));
        assertThrows(IllegalArgumentException.class, () -> TrieIdentifier.of("test@trie"));
    }

    @Test
    void testInvalidTrieIdentifierStartsOrEndsWithHyphen() {
        assertThrows(IllegalArgumentException.class, () -> TrieIdentifier.of("-test"));
        assertThrows(IllegalArgumentException.class, () -> TrieIdentifier.of("test-"));
    }

    @Test
    void testSingleCharIdentifierValid() {
        // Single character matches the required first part with optional group skipped
        TrieIdentifier id1 = TrieIdentifier.of("a");
        assertEquals("a", id1.getValue());
    }

    @Test
    void testTwoCharIdentifierInvalid() {
        // Two chars fails: first char matches, but second char can't match the optional group
        // (which requires 2+ chars when present)
        assertThrows(IllegalArgumentException.class, () -> TrieIdentifier.of("ab"));
    }
}

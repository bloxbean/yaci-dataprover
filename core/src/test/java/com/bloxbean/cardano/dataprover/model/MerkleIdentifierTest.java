package com.bloxbean.cardano.dataprover.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MerkleIdentifierTest {

    @Test
    void testValidMerkleIdentifier() {
        MerkleIdentifier id = MerkleIdentifier.of("epoch-stake-500");

        assertNotNull(id);
        assertEquals("epoch-stake-500", id.getValue());
        assertEquals("epoch-stake-500", id.toString());
    }

    @Test
    void testValidMerkleIdentifierNormalized() {
        MerkleIdentifier id = MerkleIdentifier.of("EPOCH-STAKE");

        assertEquals("epoch-stake", id.getValue());
    }

    @Test
    void testValidMerkleIdentifierShort() {
        MerkleIdentifier id = MerkleIdentifier.of("abc");

        assertEquals("abc", id.getValue());
    }

    @Test
    void testMerkleIdentifierEquality() {
        MerkleIdentifier id1 = MerkleIdentifier.of("test-merkle");
        MerkleIdentifier id2 = MerkleIdentifier.of("TEST-MERKLE");
        MerkleIdentifier id3 = MerkleIdentifier.of("other-merkle");

        assertEquals(id1, id2);
        assertNotEquals(id1, id3);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void testInvalidMerkleIdentifierBlank() {
        assertThrows(IllegalArgumentException.class, () -> MerkleIdentifier.of(null));
        assertThrows(IllegalArgumentException.class, () -> MerkleIdentifier.of(""));
        assertThrows(IllegalArgumentException.class, () -> MerkleIdentifier.of("   "));
    }

    @Test
    void testInvalidMerkleIdentifierSpecialChars() {
        assertThrows(IllegalArgumentException.class, () -> MerkleIdentifier.of("test_merkle"));
        assertThrows(IllegalArgumentException.class, () -> MerkleIdentifier.of("test.merkle"));
        assertThrows(IllegalArgumentException.class, () -> MerkleIdentifier.of("test@merkle"));
    }

    @Test
    void testInvalidMerkleIdentifierStartsOrEndsWithHyphen() {
        assertThrows(IllegalArgumentException.class, () -> MerkleIdentifier.of("-test"));
        assertThrows(IllegalArgumentException.class, () -> MerkleIdentifier.of("test-"));
    }

    @Test
    void testSingleCharIdentifierValid() {
        // Single character matches the required first part with optional group skipped
        MerkleIdentifier id1 = MerkleIdentifier.of("a");
        assertEquals("a", id1.getValue());
    }

    @Test
    void testTwoCharIdentifierInvalid() {
        // Two chars fails: first char matches, but second char can't match the optional group
        // (which requires 2+ chars when present)
        assertThrows(IllegalArgumentException.class, () -> MerkleIdentifier.of("ab"));
    }
}

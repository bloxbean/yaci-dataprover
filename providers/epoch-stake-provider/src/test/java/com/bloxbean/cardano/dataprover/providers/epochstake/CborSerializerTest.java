package com.bloxbean.cardano.dataprover.providers.epochstake;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CborSerializer using CCL's PlutusData API.
 * <p>
 * The encoding uses standard Plutus Constr encoding:
 * - Tag 121 (0xD8 0x79) for Constr 0 (alternatives 0-6 use tags 121-127)
 * - Variable-length integer encoding (minimal bytes)
 * - ByteString encoding with length prefix
 */
class CborSerializerTest {

    @Test
    void testSerializeStakeInfo() {
        long amount = 1000000000L; // 1000 ADA (fits in uint32)
        String poolIdHex = "abc123def456789012345678901234567890abcdef12345678901234";

        byte[] cbor = CborSerializer.serializeStakeInfo(amount, poolIdHex);

        assertNotNull(cbor);
        // CCL Plutus encoding: tag 121 (D8 79) + array (variable) + uint32 (variable) + bytestring 28 (variable)
        // CCL may use indefinite-length arrays (0x9F...0xFF) or fixed-length (0x82)
        assertTrue(cbor.length >= 35 && cbor.length <= 50, "Expected ~38-42 bytes, got " + cbor.length);

        // Check CBOR tag 121 (Constr 0 in standard Plutus encoding)
        assertEquals((byte) 0xD8, cbor[0]);
        assertEquals((byte) 0x79, cbor[1]); // Tag 121

        // Check it's an array (either fixed-length 0x82 or indefinite-length 0x9F)
        byte arrayMarker = cbor[2];
        assertTrue(arrayMarker == (byte) 0x82 || arrayMarker == (byte) 0x9F,
                "Expected array marker 0x82 or 0x9F, got " + String.format("0x%02X", arrayMarker & 0xFF));
    }

    @Test
    void testSerializeStakeInfoWithZeroAmount() {
        long amount = 0L;
        String poolIdHex = "0000000000000000000000000000000000000000000000000000000000000000".substring(0, 56);

        byte[] cbor = CborSerializer.serializeStakeInfo(amount, poolIdHex);

        assertNotNull(cbor);
        // With zero amount, integer encoding is minimal (1 byte: 0x00)
        // tag 121 (2) + array 2 (1) + uint 0 (1) + bytestring 28 (2 + 28) = 34 bytes
        assertTrue(cbor.length >= 34 && cbor.length <= 36, "Expected ~34-35 bytes, got " + cbor.length);
    }

    @Test
    void testSerializeStakeInfoWithMaxAmount() {
        long amount = Long.MAX_VALUE;
        String poolIdHex = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffff";

        byte[] cbor = CborSerializer.serializeStakeInfo(amount, poolIdHex);

        assertNotNull(cbor);
        // With max long, integer encoding uses 8 bytes (1B prefix + 8 bytes)
        // tag 121 (2) + array 2 (1) + uint64 (9) + bytestring 28 (2 + 28) = 42 bytes
        // But CCL may use BigInteger encoding which could be 1 byte larger
        assertTrue(cbor.length >= 42 && cbor.length <= 44, "Expected ~42-43 bytes, got " + cbor.length);
    }

    @Test
    void testSerializeStakeInfoInvalidPoolId() {
        long amount = 1000000L;

        // Pool ID too short
        assertThrows(IllegalArgumentException.class, () ->
            CborSerializer.serializeStakeInfo(amount, "abc123"));

        // Pool ID too long
        assertThrows(IllegalArgumentException.class, () ->
            CborSerializer.serializeStakeInfo(amount, "abc123def456789012345678901234567890abcdef1234567890123456"));

        // Pool ID with invalid characters
        assertThrows(IllegalArgumentException.class, () ->
            CborSerializer.serializeStakeInfo(amount, "gggggggggggggggggggggggggggggggggggggggggggggggggggggggg"));
    }

    @Test
    void testConsistentEncodingForSameInput() {
        long amount = 1000000000L;
        String poolIdHex = "abc123def456789012345678901234567890abcdef12345678901234";

        byte[] cbor1 = CborSerializer.serializeStakeInfo(amount, poolIdHex);
        byte[] cbor2 = CborSerializer.serializeStakeInfo(amount, poolIdHex);

        // Same input should produce identical CBOR bytes
        assertArrayEquals(cbor1, cbor2, "Same input should produce identical CBOR encoding");
    }
}

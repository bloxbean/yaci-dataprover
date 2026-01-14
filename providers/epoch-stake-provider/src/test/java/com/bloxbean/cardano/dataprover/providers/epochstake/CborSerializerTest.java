package com.bloxbean.cardano.dataprover.providers.epochstake;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CborSerializerTest {

    @Test
    void testSerializeStakeInfo() {
        long amount = 1000000000L; // 1000 ADA
        String poolIdHex = "abc123def456789012345678901234567890abcdef12345678901234";

        byte[] cbor = CborSerializer.serializeStakeInfo(amount, poolIdHex);

        assertNotNull(cbor);
        // CBOR encoding: tag 102 (D8 66) + array 2 (82) + uint64 (1B + 8 bytes) + bytestring 28 (58 1C + 28 bytes)
        // Total: 2 + 1 + 9 + 2 + 28 = 42 bytes
        assertEquals(42, cbor.length);

        // Check CBOR tag 102 (Constr 0)
        assertEquals((byte) 0xD8, cbor[0]);
        assertEquals((byte) 0x66, cbor[1]);

        // Check array of 2 elements
        assertEquals((byte) 0x82, cbor[2]);

        // Check uint64 marker
        assertEquals((byte) 0x1B, cbor[3]);
    }

    @Test
    void testSerializeStakeInfoWithZeroAmount() {
        long amount = 0L;
        String poolIdHex = "0000000000000000000000000000000000000000000000000000000000000000".substring(0, 56);

        byte[] cbor = CborSerializer.serializeStakeInfo(amount, poolIdHex);

        assertNotNull(cbor);
        assertEquals(42, cbor.length);
    }

    @Test
    void testSerializeStakeInfoWithMaxAmount() {
        long amount = Long.MAX_VALUE;
        String poolIdHex = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffff";

        byte[] cbor = CborSerializer.serializeStakeInfo(amount, poolIdHex);

        assertNotNull(cbor);
        assertEquals(42, cbor.length);
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
}

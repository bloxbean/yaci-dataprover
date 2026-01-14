package com.bloxbean.cardano.dataprover.providers.epochstake;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * CBOR serialization utilities for stake information.
 * Format: Constr 0 [amount: Integer, pool_key_hash: ByteArray (28 bytes)]
 */
public class CborSerializer {

    private static final Logger log = LoggerFactory.getLogger(CborSerializer.class);

    private CborSerializer() {
    }

    /**
     * Serializes stake information to CBOR bytes.
     */
    public static byte[] serializeStakeInfo(long amount, String poolIdHex) {
        byte[] poolKeyHash = AddressConverter.poolIdToKeyHash(poolIdHex);
        return serializeStakeInfo(amount, poolKeyHash);
    }

    /**
     * Serializes stake information to CBOR bytes.
     */
    public static byte[] serializeStakeInfo(long amount, byte[] poolKeyHash) {
        if (poolKeyHash == null || poolKeyHash.length != 28) {
            throw new IllegalArgumentException(
                "Pool key hash must be 28 bytes, got: " +
                (poolKeyHash == null ? "null" : poolKeyHash.length));
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // Tag 102 (Constr 0): 0xD8 0x66
            baos.write(0xD8);
            baos.write(0x66);

            // Array of 2 elements: 0x82
            baos.write(0x82);

            // Element 1: Unsigned integer (8 bytes): 0x1B + 8 bytes big-endian
            baos.write(0x1B);
            ByteBuffer buffer = ByteBuffer.allocate(8);
            buffer.putLong(amount);
            baos.write(buffer.array());

            // Element 2: Byte string (28 bytes): 0x58 0x1C + data
            baos.write(0x58);
            baos.write(0x1C);
            baos.write(poolKeyHash);

            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Failed to serialize StakeInfo: amount={}", amount, e);
            throw new RuntimeException("Failed to serialize StakeInfo", e);
        }
    }
}

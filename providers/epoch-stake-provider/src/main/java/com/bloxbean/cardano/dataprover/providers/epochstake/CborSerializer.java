package com.bloxbean.cardano.dataprover.providers.epochstake;

import co.nstant.in.cbor.CborException;
import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.plutus.spec.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CBOR serialization utilities for stake information using CCL's PlutusData API.
 * Format: Constr 0 [amount: Integer, pool_key_hash: ByteArray (28 bytes)]
 * <p>
 * Uses CCL's standard Plutus encoding (Tag 121 for Constr 0) for consistency
 * with polyglot providers.
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
     * Serializes stake information to CBOR bytes using CCL's PlutusData API.
     * <p>
     * Format: Constr 0 [amount, pool_key_hash]
     * Uses Tag 121 encoding (standard Plutus Constr 0).
     */
    public static byte[] serializeStakeInfo(long amount, byte[] poolKeyHash) {
        if (poolKeyHash == null || poolKeyHash.length != 28) {
            throw new IllegalArgumentException(
                "Pool key hash must be 28 bytes, got: " +
                (poolKeyHash == null ? "null" : poolKeyHash.length));
        }

        // Use CCL's PlutusData for consistent encoding with polyglot providers
        ConstrPlutusData stakeInfo = ConstrPlutusData.builder()
                .alternative(0)
                .data(ListPlutusData.of(
                        BigIntPlutusData.of(amount),
                        BytesPlutusData.of(poolKeyHash)
                ))
                .build();

        try {
            return CborSerializationUtil.serialize(stakeInfo.serialize());
        } catch (CborSerializationException | CborException e) {
            log.error("Failed to serialize StakeInfo: amount={}", amount, e);
            throw new RuntimeException("Failed to serialize StakeInfo", e);
        }
    }
}

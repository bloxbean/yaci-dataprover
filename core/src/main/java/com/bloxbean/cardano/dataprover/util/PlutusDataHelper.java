package com.bloxbean.cardano.dataprover.util;

import co.nstant.in.cbor.CborException;
import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.plutus.spec.*;

import java.math.BigInteger;
import java.util.List;

/**
 * Utility class for Plutus data serialization using CCL's PlutusData API.
 * <p>
 * This ensures consistent CBOR encoding across Java and polyglot providers,
 * using the standard Plutus Constr encoding (Tag 121-127 for alternatives 0-6).
 */
public class PlutusDataHelper {

    private PlutusDataHelper() {
        // Utility class
    }

    /**
     * Create a Plutus Constr with the given alternative and fields.
     * Uses CCL's ConstrPlutusData for consistent encoding.
     *
     * @param alternative the constructor alternative (0-6 uses tags 121-127, 7+ uses tag 1280+n)
     * @param fields      the data fields
     * @return the ConstrPlutusData
     */
    public static ConstrPlutusData constr(int alternative, PlutusData... fields) {
        return ConstrPlutusData.builder()
                .alternative(alternative)
                .data(ListPlutusData.of(fields))
                .build();
    }

    /**
     * Create a Plutus Constr with the given alternative and fields list.
     *
     * @param alternative the constructor alternative
     * @param fields      the data fields as a list
     * @return the ConstrPlutusData
     */
    public static ConstrPlutusData constr(int alternative, List<PlutusData> fields) {
        return ConstrPlutusData.builder()
                .alternative(alternative)
                .data(ListPlutusData.of(fields.toArray(new PlutusData[0])))
                .build();
    }

    /**
     * Serialize PlutusData to CBOR bytes.
     *
     * @param data the PlutusData to serialize
     * @return CBOR-encoded bytes
     * @throws RuntimeException if serialization fails
     */
    public static byte[] serialize(PlutusData data) {
        try {
            return CborSerializationUtil.serialize(data.serialize());
        } catch (CborSerializationException | CborException e) {
            throw new RuntimeException("Failed to serialize PlutusData", e);
        }
    }

    /**
     * Create BigIntPlutusData from a long value.
     *
     * @param value the long value
     * @return BigIntPlutusData
     */
    public static BigIntPlutusData bigInt(long value) {
        return BigIntPlutusData.of(value);
    }

    /**
     * Create BigIntPlutusData from a BigInteger value.
     *
     * @param value the BigInteger value
     * @return BigIntPlutusData
     */
    public static BigIntPlutusData bigInt(BigInteger value) {
        return BigIntPlutusData.of(value);
    }

    /**
     * Create BytesPlutusData from a byte array.
     *
     * @param data the byte array
     * @return BytesPlutusData
     */
    public static BytesPlutusData bytes(byte[] data) {
        return BytesPlutusData.of(data);
    }

    /**
     * Create a ListPlutusData from the given elements.
     *
     * @param elements the PlutusData elements
     * @return ListPlutusData
     */
    public static ListPlutusData list(PlutusData... elements) {
        return ListPlutusData.of(elements);
    }

    /**
     * Create a MapPlutusData from key-value pairs.
     * Pairs should be provided as alternating key, value, key, value, etc.
     *
     * @param keyValuePairs alternating key and value PlutusData elements
     * @return MapPlutusData
     */
    public static MapPlutusData map(PlutusData... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Map requires even number of arguments (key-value pairs)");
        }
        MapPlutusData mapData = new MapPlutusData();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            mapData.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return mapData;
    }

    /**
     * Serialize a Constr directly to CBOR bytes.
     * Convenience method combining constr() and serialize().
     *
     * @param alternative the constructor alternative
     * @param fields      the data fields
     * @return CBOR-encoded bytes
     */
    public static byte[] serializeConstr(int alternative, PlutusData... fields) {
        return serialize(constr(alternative, fields));
    }
}

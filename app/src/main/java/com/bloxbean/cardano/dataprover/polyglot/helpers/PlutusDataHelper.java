package com.bloxbean.cardano.dataprover.polyglot.helpers;

import co.nstant.in.cbor.CborException;
import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.plutus.spec.*;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Plutus data helper exposed to polyglot scripts.
 * <p>
 * Uses CCL's PlutusData API for consistent CBOR encoding across Java and polyglot providers.
 * This ensures both Java-based providers and JavaScript/Python providers produce identical
 * CBOR bytes for the same data structures.
 */
public class PlutusDataHelper {
    private static final Logger log = LoggerFactory.getLogger(PlutusDataHelper.class);

    /**
     * Create and serialize a Plutus Constr to CBOR bytes.
     * <p>
     * Example usage in JavaScript:
     * <pre>
     * var bytes = plutus.constr(0, [amount, poolIdBytes]);
     * </pre>
     *
     * @param alternative the constructor alternative (0-6 uses tags 121-127)
     * @param fields      the fields as an array or list
     * @return CBOR-encoded bytes
     */
    @HostAccess.Export
    public byte[] constr(int alternative, Object fields) {
        log.debug("plutus.constr called: alternative={}, fields type={}",
                alternative, fields != null ? fields.getClass().getName() : "null");

        List<PlutusData> plutusFields = convertToPlutusDataList(fields);

        ConstrPlutusData constr = ConstrPlutusData.builder()
                .alternative(alternative)
                .data(ListPlutusData.of(plutusFields.toArray(new PlutusData[0])))
                .build();

        return serializePlutusData(constr);
    }

    /**
     * Create and serialize a Plutus list to CBOR bytes.
     *
     * @param elements the list elements
     * @return CBOR-encoded bytes
     */
    @HostAccess.Export
    public byte[] list(Object elements) {
        List<PlutusData> plutusElements = convertToPlutusDataList(elements);
        ListPlutusData listData = ListPlutusData.of(plutusElements.toArray(new PlutusData[0]));
        return serializePlutusData(listData);
    }

    /**
     * Create and serialize a BigInt PlutusData to CBOR bytes.
     *
     * @param value the integer value (Number, BigInteger, or BigDecimal)
     * @return CBOR-encoded bytes
     */
    @HostAccess.Export
    public byte[] bigInt(Object value) {
        PlutusData data = toPlutusData(value);
        if (!(data instanceof BigIntPlutusData)) {
            throw new IllegalArgumentException("Expected numeric value, got: " + value);
        }
        return serializePlutusData(data);
    }

    /**
     * Create and serialize a Bytes PlutusData to CBOR bytes.
     *
     * @param value the byte array
     * @return CBOR-encoded bytes
     */
    @HostAccess.Export
    public byte[] bytes(Object value) {
        byte[] byteArray = convertToByteArray(value);
        BytesPlutusData data = BytesPlutusData.of(byteArray);
        return serializePlutusData(data);
    }

    /**
     * Convert fields to a list of PlutusData.
     */
    private List<PlutusData> convertToPlutusDataList(Object fields) {
        List<PlutusData> plutusFields = new ArrayList<>();

        if (fields instanceof Value polyglotValue && polyglotValue.hasArrayElements()) {
            log.debug("Processing as polyglot Value array, size={}", polyglotValue.getArraySize());
            for (int i = 0; i < polyglotValue.getArraySize(); i++) {
                Value element = polyglotValue.getArrayElement(i);
                Object converted = convertValue(element);
                log.debug("Field {}: converted to {}", i, converted != null ? converted.getClass().getName() : "null");
                plutusFields.add(toPlutusData(converted));
            }
        } else if (fields instanceof Object[] fieldsArray) {
            log.debug("Processing as Object[], length={}", fieldsArray.length);
            for (Object field : fieldsArray) {
                plutusFields.add(toPlutusData(field));
            }
        } else if (fields instanceof List<?> listFields) {
            log.debug("Processing as List, size={}", listFields.size());
            for (Object field : listFields) {
                if (field instanceof Value v) {
                    field = convertValue(v);
                }
                plutusFields.add(toPlutusData(field));
            }
        } else {
            throw new IllegalArgumentException("Fields must be an array or list, got: " +
                    (fields != null ? fields.getClass().getName() : "null"));
        }

        return plutusFields;
    }

    /**
     * Convert a Java/polyglot value to the appropriate PlutusData type.
     */
    private PlutusData toPlutusData(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Null values are not supported in PlutusData");
        }

        // Handle polyglot Value wrapper
        if (value instanceof Value polyglotValue) {
            value = convertValue(polyglotValue);
        }

        // Handle byte arrays -> BytesPlutusData
        if (value instanceof byte[] bytes) {
            return BytesPlutusData.of(bytes);
        }

        // Handle numbers -> BigIntPlutusData
        if (value instanceof BigInteger bi) {
            return BigIntPlutusData.of(bi);
        }
        if (value instanceof BigDecimal bd) {
            return BigIntPlutusData.of(bd.toBigInteger());
        }
        if (value instanceof Number n) {
            return BigIntPlutusData.of(n.longValue());
        }

        // Handle nested arrays -> ListPlutusData
        if (value instanceof Object[] arr) {
            PlutusData[] elements = new PlutusData[arr.length];
            for (int i = 0; i < arr.length; i++) {
                elements[i] = toPlutusData(arr[i]);
            }
            return ListPlutusData.of(elements);
        }
        if (value instanceof List<?> list) {
            PlutusData[] elements = new PlutusData[list.size()];
            for (int i = 0; i < list.size(); i++) {
                elements[i] = toPlutusData(list.get(i));
            }
            return ListPlutusData.of(elements);
        }

        throw new IllegalArgumentException("Unsupported type for PlutusData: " + value.getClass().getName() +
                ", value=" + value);
    }

    /**
     * Convert a polyglot Value to a Java object.
     */
    private Object convertValue(Value value) {
        if (value.isNull()) {
            return null;
        }

        // Handle host objects (Java objects passed through polyglot)
        if (value.isHostObject()) {
            Object hostObj = value.asHostObject();
            log.debug("convertValue: isHostObject, type={}", hostObj != null ? hostObj.getClass().getName() : "null");
            // Convert BigDecimal to BigInteger (common for DB results)
            if (hostObj instanceof BigDecimal bd) {
                return bd.toBigInteger();
            }
            return hostObj;
        }

        // Handle numbers
        if (value.isNumber()) {
            if (value.fitsInInt()) {
                return value.asInt();
            }
            if (value.fitsInLong()) {
                return value.asLong();
            }
            return value.as(BigInteger.class);
        }

        // Handle arrays
        if (value.hasArrayElements()) {
            int size = (int) value.getArraySize();
            Object[] array = new Object[size];
            for (int i = 0; i < size; i++) {
                array[i] = convertValue(value.getArrayElement(i));
            }
            return array;
        }

        // Handle typed arrays (Uint8Array, etc.)
        if (value.hasMembers() && value.hasMember("length")) {
            try {
                long length = value.getMember("length").asLong();
                byte[] bytes = new byte[(int) length];
                for (int i = 0; i < length; i++) {
                    bytes[i] = (byte) value.getArrayElement(i).asInt();
                }
                return bytes;
            } catch (Exception e) {
                // Not a typed array, fall through
                log.debug("Failed to convert as typed array: {}", e.getMessage());
            }
        }

        throw new IllegalArgumentException("Cannot convert polyglot value to PlutusData-compatible type: " + value);
    }

    /**
     * Convert various input types to byte array.
     */
    private byte[] convertToByteArray(Object value) {
        if (value instanceof byte[] bytes) {
            return bytes;
        }
        if (value instanceof Value polyglotValue) {
            Object converted = convertValue(polyglotValue);
            if (converted instanceof byte[] bytes) {
                return bytes;
            }
        }
        throw new IllegalArgumentException("Expected byte array, got: " +
                (value != null ? value.getClass().getName() : "null"));
    }

    /**
     * Serialize PlutusData to CBOR bytes, handling checked exceptions.
     */
    private byte[] serializePlutusData(PlutusData data) {
        try {
            return CborSerializationUtil.serialize(data.serialize());
        } catch (CborSerializationException | CborException e) {
            throw new RuntimeException("Failed to serialize PlutusData", e);
        }
    }
}

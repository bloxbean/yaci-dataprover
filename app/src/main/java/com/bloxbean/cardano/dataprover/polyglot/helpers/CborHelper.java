package com.bloxbean.cardano.dataprover.polyglot.helpers;

import co.nstant.in.cbor.CborBuilder;
import co.nstant.in.cbor.CborEncoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.*;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * CBOR encoding helper exposed to polyglot scripts.
 * Supports Plutus-compatible Constr encoding.
 */
public class CborHelper {
    private static final Logger log = LoggerFactory.getLogger(CborHelper.class);

    @HostAccess.Export
    public byte[] encode(Object value) {
        try {
            DataItem item = toDataItem(value);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new CborEncoder(baos).encode(item);
            return baos.toByteArray();
        } catch (CborException e) {
            throw new RuntimeException("CBOR encoding failed: " + e.getMessage(), e);
        }
    }

    @HostAccess.Export
    public byte[] encodeConstr(int tag, Object fields) {
        try {
            log.debug("encodeConstr called: tag={}, fields type={}", tag,
                    fields != null ? fields.getClass().getName() : "null");

            // Plutus Constr encoding: tag 121 + tag for alternative
            // Alternative 0-6: tag 121-127
            // Alternative 7+: tag 1280 + n
            int cborTag = tag <= 6 ? 121 + tag : 1280 + (tag - 7);

            Array fieldArray = new Array();

            // Handle polyglot Value (JavaScript array)
            if (fields instanceof Value polyglotValue && polyglotValue.hasArrayElements()) {
                log.debug("Processing as polyglot Value array, size={}", polyglotValue.getArraySize());
                for (int i = 0; i < polyglotValue.getArraySize(); i++) {
                    Value element = polyglotValue.getArrayElement(i);
                    log.debug("Element {}: isNull={}, isNumber={}, isHostObject={}, hasArrayElements={}",
                            i, element.isNull(), element.isNumber(), element.isHostObject(), element.hasArrayElements());
                    Object converted = convertValue(element);
                    log.debug("Converted element {} to: {}", i, converted != null ? converted.getClass().getName() : "null");
                    fieldArray.add(toDataItem(converted));
                }
            } else if (fields instanceof Object[] fieldsArray) {
                // Handle Java Object array
                log.debug("Processing as Object[], length={}", fieldsArray.length);
                for (Object field : fieldsArray) {
                    fieldArray.add(toDataItem(field));
                }
            } else if (fields instanceof List<?> listFields) {
                // Handle List (including PolyglotList from GraalVM)
                log.debug("Processing as List, size={}", listFields.size());
                for (Object field : listFields) {
                    log.debug("List element type: {}", field != null ? field.getClass().getName() : "null");
                    // Check if this is a polyglot Value that needs conversion
                    if (field instanceof Value v) {
                        field = convertValue(v);
                        log.debug("Converted Value to: {}", field != null ? field.getClass().getName() : "null");
                    }
                    fieldArray.add(toDataItem(field));
                }
            } else {
                throw new IllegalArgumentException("encodeConstr fields must be an array, got: " +
                        (fields != null ? fields.getClass().getName() : "null"));
            }

            DataItem tagged = new Tag(cborTag);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CborEncoder encoder = new CborEncoder(baos);
            encoder.encode(tagged);
            encoder.encode(fieldArray);
            return baos.toByteArray();
        } catch (CborException e) {
            log.error("CBOR Constr encoding failed", e);
            throw new RuntimeException("CBOR Constr encoding failed: " + e.getMessage(), e);
        }
    }

    @HostAccess.Export
    public byte[] encodeList(Object[] items) {
        try {
            Array array = new Array();
            for (Object item : items) {
                array.add(toDataItem(item));
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new CborEncoder(baos).encode(array);
            return baos.toByteArray();
        } catch (CborException e) {
            throw new RuntimeException("CBOR list encoding failed: " + e.getMessage(), e);
        }
    }

    @HostAccess.Export
    public byte[] encodeMap(Object mapValue) {
        try {
            co.nstant.in.cbor.model.Map cborMap = new co.nstant.in.cbor.model.Map();

            if (mapValue instanceof Value polyglotValue && polyglotValue.hasMembers()) {
                for (String key : polyglotValue.getMemberKeys()) {
                    Value val = polyglotValue.getMember(key);
                    cborMap.put(new UnicodeString(key), toDataItem(convertValue(val)));
                }
            } else if (mapValue instanceof Map<?, ?> javaMap) {
                for (Map.Entry<?, ?> entry : javaMap.entrySet()) {
                    cborMap.put(toDataItem(entry.getKey()), toDataItem(entry.getValue()));
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new CborEncoder(baos).encode(cborMap);
            return baos.toByteArray();
        } catch (CborException e) {
            throw new RuntimeException("CBOR map encoding failed: " + e.getMessage(), e);
        }
    }

    private DataItem toDataItem(Object value) {
        if (value == null) {
            return SimpleValue.NULL;
        }
        if (value instanceof Value polyglotValue) {
            value = convertValue(polyglotValue);
        }
        if (value instanceof Integer i) {
            return new UnsignedInteger(i);
        }
        if (value instanceof Long l) {
            if (l >= 0) {
                return new UnsignedInteger(l);
            } else {
                return new NegativeInteger(l);
            }
        }
        if (value instanceof BigInteger bi) {
            if (bi.signum() >= 0) {
                return new UnsignedInteger(bi);
            } else {
                return new NegativeInteger(bi);
            }
        }
        if (value instanceof BigDecimal bd) {
            // Convert BigDecimal to BigInteger for CBOR encoding
            BigInteger bi = bd.toBigInteger();
            if (bi.signum() >= 0) {
                return new UnsignedInteger(bi);
            } else {
                return new NegativeInteger(bi);
            }
        }
        if (value instanceof java.lang.Number n) {
            // Handle any other Number types (Double, Float, etc.)
            long l = n.longValue();
            if (l >= 0) {
                return new UnsignedInteger(l);
            } else {
                return new NegativeInteger(l);
            }
        }
        if (value instanceof String s) {
            return new UnicodeString(s);
        }
        if (value instanceof byte[] bytes) {
            return new ByteString(bytes);
        }
        if (value instanceof Boolean b) {
            return b ? SimpleValue.TRUE : SimpleValue.FALSE;
        }
        if (value instanceof List<?> list) {
            Array array = new Array();
            for (Object item : list) {
                array.add(toDataItem(item));
            }
            return array;
        }
        if (value instanceof Object[] array) {
            Array cborArray = new Array();
            for (Object item : array) {
                cborArray.add(toDataItem(item));
            }
            return cborArray;
        }
        // Handle Map - log details for debugging
        if (value instanceof Map<?, ?> map) {
            log.warn("toDataItem received Map with {} entries, keys: {}. Maps should be encoded explicitly via encodeMap",
                    map.size(), map.keySet());
            // If it's an empty map, it might be a polyglot conversion artifact
            if (map.isEmpty()) {
                log.warn("Empty map received - this might indicate a polyglot conversion issue");
            }
        }
        throw new IllegalArgumentException("Unsupported type for CBOR encoding: " + value.getClass() +
                ", data=" + value);
    }

    private Object convertValue(Value value) {
        if (value.isNull()) {
            return null;
        }
        // Handle host objects (Java objects passed through polyglot, like byte[] from hex.decode or BigDecimal from DB)
        if (value.isHostObject()) {
            Object hostObj = value.asHostObject();
            log.debug("convertValue: isHostObject, hostObj type={}", hostObj != null ? hostObj.getClass().getName() : "null");
            // Handle BigDecimal from database
            if (hostObj instanceof BigDecimal bd) {
                return bd.toBigInteger();
            }
            return hostObj;
        }
        if (value.isNumber()) {
            if (value.fitsInInt()) {
                return value.asInt();
            }
            if (value.fitsInLong()) {
                return value.asLong();
            }
            return value.as(BigInteger.class);
        }
        if (value.isString()) {
            return value.asString();
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.hasArrayElements()) {
            int size = (int) value.getArraySize();
            Object[] array = new Object[size];
            for (int i = 0; i < size; i++) {
                array[i] = convertValue(value.getArrayElement(i));
            }
            return array;
        }
        // Check if it's a Uint8Array or similar typed array
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
            }
        }
        return value.toString();
    }
}

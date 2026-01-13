package com.bloxbean.cardano.proofserver.model;

import java.util.Arrays;

/**
 * Represents a key-value pair for trie operations.
 */
public record KeyValuePair(byte[] key, byte[] value) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyValuePair that = (KeyValuePair) o;
        return Arrays.equals(key, that.key) && Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(key);
        result = 31 * result + Arrays.hashCode(value);
        return result;
    }
}

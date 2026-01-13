package com.bloxbean.cardano.proofserver.test;

import java.util.Objects;

/**
 * Simple POJO for test data items used in integration tests.
 */
public class TestDataItem {

    private final byte[] key;
    private final byte[] value;

    public TestDataItem(byte[] key, byte[] value) {
        this.key = key;
        this.value = value;
    }

    public static TestDataItem of(byte[] key, byte[] value) {
        return new TestDataItem(key, value);
    }

    public static TestDataItem of(String keyHex, String valueHex) {
        return new TestDataItem(
                hexToBytes(keyHex),
                hexToBytes(valueHex)
        );
    }

    public byte[] getKey() {
        return key;
    }

    public byte[] getValue() {
        return value;
    }

    public String getKeyHex() {
        return bytesToHex(key);
    }

    public String getValueHex() {
        return bytesToHex(value);
    }

    private static byte[] hexToBytes(String hex) {
        if (hex == null || hex.isEmpty()) {
            return new byte[0];
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestDataItem that = (TestDataItem) o;
        return java.util.Arrays.equals(key, that.key) &&
                java.util.Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        int result = java.util.Arrays.hashCode(key);
        result = 31 * result + java.util.Arrays.hashCode(value);
        return result;
    }

    @Override
    public String toString() {
        return "TestDataItem{key=" + getKeyHex() + ", value=" + getValueHex() + "}";
    }
}

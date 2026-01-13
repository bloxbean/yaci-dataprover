package com.bloxbean.cardano.proofserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO representing a single key-value entry for direct trie insertion.
 * Keys and values are hex-encoded byte arrays.
 */
public class EntryItem {

    @NotBlank(message = "Key is required")
    @Pattern(regexp = "^(0x)?[0-9a-fA-F]+$", message = "Key must be hex-encoded")
    private String key;

    @NotBlank(message = "Value is required")
    @Pattern(regexp = "^(0x)?[0-9a-fA-F]+$", message = "Value must be hex-encoded")
    private String value;

    public EntryItem() {
    }

    public EntryItem(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Decodes the hex-encoded key to bytes.
     * Handles optional "0x" prefix.
     */
    public byte[] getKeyBytes() {
        return hexToBytes(key);
    }

    /**
     * Decodes the hex-encoded value to bytes.
     * Handles optional "0x" prefix.
     */
    public byte[] getValueBytes() {
        return hexToBytes(value);
    }

    private byte[] hexToBytes(String hex) {
        if (hex == null) {
            return new byte[0];
        }
        String cleanHex = hex.startsWith("0x") || hex.startsWith("0X")
                ? hex.substring(2)
                : hex;
        int len = cleanHex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(cleanHex.charAt(i), 16) << 4)
                    + Character.digit(cleanHex.charAt(i + 1), 16));
        }
        return data;
    }
}

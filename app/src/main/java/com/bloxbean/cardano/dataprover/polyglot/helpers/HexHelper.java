package com.bloxbean.cardano.dataprover.polyglot.helpers;

import org.graalvm.polyglot.HostAccess;

/**
 * Hex encoding/decoding helper exposed to polyglot scripts.
 */
public class HexHelper {
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    @HostAccess.Export
    public byte[] decode(String hex) {
        if (hex == null) {
            return new byte[0];
        }
        String cleanHex = hex.startsWith("0x") ? hex.substring(2) : hex;
        int len = cleanHex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(cleanHex.charAt(i), 16) << 4)
                    + Character.digit(cleanHex.charAt(i + 1), 16));
        }
        return data;
    }

    @HostAccess.Export
    public String encode(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_CHARS[v >>> 4];
            hexChars[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(hexChars);
    }

    @HostAccess.Export
    public boolean isValidHex(String hex) {
        if (hex == null || hex.isEmpty()) {
            return false;
        }
        String cleanHex = hex.startsWith("0x") ? hex.substring(2) : hex;
        if (cleanHex.length() % 2 != 0) {
            return false;
        }
        for (char c : cleanHex.toCharArray()) {
            if (Character.digit(c, 16) == -1) {
                return false;
            }
        }
        return true;
    }
}

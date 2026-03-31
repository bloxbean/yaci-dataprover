package com.bloxbean.cardano.dataprover.providers.epochstake;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.util.HexUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for converting between Bech32 addresses and binary hashes.
 */
public class AddressConverter {

    private static final Logger log = LoggerFactory.getLogger(AddressConverter.class);

    private AddressConverter() {
    }

    /**
     * Converts bech32 stake address to 28-byte credential hash.
     * Uses getDelegationCredentialHash() to return only the hash bytes,
     * matching the behavior of CardanoHelper for polyglot providers.
     */
    public static byte[] stakeAddressToCredentialHash(String stakeAddressBech32) {
        try {
            Address address = new Address(stakeAddressBech32);
            // Use getDelegationCredentialHash() to get only the 28-byte hash
            // This matches CardanoHelper.stakeAddressToCredentialHash() behavior
            return address.getDelegationCredentialHash()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Invalid stake address: " + stakeAddressBech32));
        } catch (Exception e) {
            log.error("Failed to convert stake address to credential hash: {}",
                     stakeAddressBech32, e);
            throw new IllegalArgumentException(
                "Invalid stake address: " + stakeAddressBech32, e);
        }
    }

    /**
     * Converts hex pool ID to 28-byte key hash.
     */
    public static byte[] poolIdToKeyHash(String poolIdHex) {
        if (poolIdHex == null || poolIdHex.isBlank()) {
            throw new IllegalArgumentException("Pool ID cannot be null or empty");
        }

        if (poolIdHex.length() != 56) {
            throw new IllegalArgumentException(
                "Pool ID must be 56 hex characters (28 bytes), got: " + poolIdHex.length());
        }

        if (!poolIdHex.matches("[0-9a-fA-F]{56}")) {
            throw new IllegalArgumentException(
                "Pool ID must be valid hexadecimal: " + poolIdHex);
        }

        try {
            return HexUtil.decodeHexString(poolIdHex);
        } catch (Exception e) {
            log.error("Failed to decode pool ID hex: {}", poolIdHex, e);
            throw new IllegalArgumentException("Invalid pool ID hex: " + poolIdHex, e);
        }
    }
}

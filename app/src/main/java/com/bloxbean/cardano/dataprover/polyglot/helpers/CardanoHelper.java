package com.bloxbean.cardano.dataprover.polyglot.helpers;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.util.HexUtil;
import org.graalvm.polyglot.HostAccess;

/**
 * Cardano-specific helper exposed to polyglot scripts.
 * Provides address conversion and hash extraction utilities.
 */
public class CardanoHelper {

    @HostAccess.Export
    public byte[] stakeAddressToCredentialHash(String stakeAddress) {
        try {
            Address address = new Address(stakeAddress);
            // For stake/reward addresses, the stake credential is in the delegation position
            // (the cardano-client-lib stores stake address credentials in getDelegationCredential)
            byte[] credentialHash = address.getDelegationCredentialHash()
                    .orElseThrow(() -> new IllegalArgumentException("No credential in stake address: " + stakeAddress));
            return credentialHash;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract credential hash from stake address: " + stakeAddress, e);
        }
    }

    @HostAccess.Export
    public String stakeAddressToCredentialHashHex(String stakeAddress) {
        return HexUtil.encodeHexString(stakeAddressToCredentialHash(stakeAddress));
    }

    @HostAccess.Export
    public byte[] poolIdToKeyHash(String poolId) {
        try {
            // Pool ID is bech32-encoded key hash
            byte[] decoded = Bech32.decode(poolId).data;
            return decoded;
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode pool ID: " + poolId, e);
        }
    }

    @HostAccess.Export
    public String poolIdToKeyHashHex(String poolId) {
        return HexUtil.encodeHexString(poolIdToKeyHash(poolId));
    }

    @HostAccess.Export
    public byte[] addressToBytes(String address) {
        try {
            Address addr = new Address(address);
            return addr.getBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert address to bytes: " + address, e);
        }
    }

    @HostAccess.Export
    public String addressToHex(String address) {
        return HexUtil.encodeHexString(addressToBytes(address));
    }

    @HostAccess.Export
    public byte[] paymentCredentialHash(String address) {
        try {
            Address addr = new Address(address);
            return addr.getPaymentCredentialHash()
                    .orElseThrow(() -> new IllegalArgumentException("No payment credential in address: " + address));
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract payment credential from address: " + address, e);
        }
    }

    @HostAccess.Export
    public String paymentCredentialHashHex(String address) {
        return HexUtil.encodeHexString(paymentCredentialHash(address));
    }

    @HostAccess.Export
    public String bytesToBech32(byte[] bytes, String prefix) {
        return Bech32.encode(bytes, prefix);
    }

    @HostAccess.Export
    public byte[] bech32ToBytes(String bech32) {
        return Bech32.decode(bech32).data;
    }
}

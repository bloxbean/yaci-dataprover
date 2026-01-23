package com.bloxbean.cardano.dataprover.polyglot.helpers;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.util.HexUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CardanoHelperTest {

    @Test
    void testGenerateValidStakeAddress() {
        // Create a valid stake address for testnet
        // Stake address format: header byte (0xe0 for testnet key hash) + 28 byte credential hash
        byte[] headerByte = new byte[]{(byte) 0xe0}; // Testnet stake key hash
        byte[] credentialHash = HexUtil.decodeHexString("bab3a20a61f7c3e30b58c604c904a92daa91437f9720b8b8a62b3e23"); // 28 bytes

        byte[] stakeAddrBytes = new byte[29];
        stakeAddrBytes[0] = headerByte[0];
        System.arraycopy(credentialHash, 0, stakeAddrBytes, 1, 28);

        String stakeAddress = Bech32.encode(stakeAddrBytes, "stake_test");
        System.out.println("Generated stake address: " + stakeAddress);

        // Now test that we can parse it back
        Address address = new Address(stakeAddress);
        System.out.println("Address bytes: " + HexUtil.encodeHexString(address.getBytes()));
        System.out.println("Address type: " + address.getAddressType());

        // Check if it has a payment credential (for stake addresses, this is where the stake credential is)
        var paymentCred = address.getPaymentCredentialHash();
        System.out.println("Payment credential present: " + paymentCred.isPresent());
        if (paymentCred.isPresent()) {
            System.out.println("Payment credential hash: " + HexUtil.encodeHexString(paymentCred.get()));
        }

        // Check delegation credential
        var delegationCred = address.getDelegationCredentialHash();
        System.out.println("Delegation credential present: " + delegationCred.isPresent());
        if (delegationCred.isPresent()) {
            System.out.println("Delegation credential hash: " + HexUtil.encodeHexString(delegationCred.get()));
        }
    }

    @Test
    void testCreateStakeAddressFromKeyHash() {
        // Create a stake address using AddressProvider
        byte[] stakeKeyHash = HexUtil.decodeHexString("bab3a20a61f7c3e30b58c604c904a92daa91437f9720b8b8a62b3e23");

        // Create reward address (which is the same as stake address)
        Address rewardAddress = AddressProvider.getRewardAddress(
                com.bloxbean.cardano.client.address.Credential.fromKey(stakeKeyHash),
                Networks.testnet()
        );

        System.out.println("Reward address: " + rewardAddress.toBech32());
        System.out.println("Address bytes hex: " + HexUtil.encodeHexString(rewardAddress.getBytes()));
        System.out.println("Address type: " + rewardAddress.getAddressType());

        // Check all available credential methods
        System.out.println("getPaymentCredential: " + rewardAddress.getPaymentCredential());
        System.out.println("getPaymentCredentialHash: " + rewardAddress.getPaymentCredentialHash());
        System.out.println("getDelegationCredential: " + rewardAddress.getDelegationCredential());
        System.out.println("getDelegationCredentialHash: " + rewardAddress.getDelegationCredentialHash());

        // Try getting the bytes directly - for reward addresses, bytes[1:29] should be the credential hash
        byte[] addrBytes = rewardAddress.getBytes();
        System.out.println("Address bytes length: " + addrBytes.length);
        if (addrBytes.length == 29) {
            byte[] extractedHash = new byte[28];
            System.arraycopy(addrBytes, 1, extractedHash, 0, 28);
            System.out.println("Extracted from bytes[1:29]: " + HexUtil.encodeHexString(extractedHash));
            assertArrayEquals(stakeKeyHash, extractedHash);
        }

        // Test the CardanoHelper
        CardanoHelper helper = new CardanoHelper();
        byte[] helperExtractedHash = helper.stakeAddressToCredentialHash(rewardAddress.toBech32());
        System.out.println("CardanoHelper extracted: " + HexUtil.encodeHexString(helperExtractedHash));
        assertArrayEquals(stakeKeyHash, helperExtractedHash);
    }

    @Test
    void testPoolIdDecoding() {
        // Test pool ID decoding
        byte[] poolKeyHash = HexUtil.decodeHexString("0f292fcaa19b1eefc96cfd8dd4fac6c6e382514a0c3f6889a5c12f0c");
        String poolId = Bech32.encode(poolKeyHash, "pool");
        System.out.println("Generated pool ID: " + poolId);

        // Test decoding
        CardanoHelper helper = new CardanoHelper();
        byte[] decoded = helper.poolIdToKeyHash(poolId);
        System.out.println("Decoded pool hash: " + HexUtil.encodeHexString(decoded));

        assertArrayEquals(poolKeyHash, decoded);
    }

    @Test
    void generateTestSql() {
        // Generate SQL for test data
        String[] credentialHashes = {
            "bab3a20a61f7c3e30b58c604c904a92daa91437f9720b8b8a62b3e23",
            "1ab3a20a61f7c3e30b58c604c904a92daa91437f9720b8b8a62b3e24",
            "2ab3a20a61f7c3e30b58c604c904a92daa91437f9720b8b8a62b3e25"
        };
        String[] poolHashes = {
            "0f292fcaa19b1eefc96cfd8dd4fac6c6e382514a0c3f6889a5c12f0c",
            "1f292fcaa19b1eefc96cfd8dd4fac6c6e382514a0c3f6889a5c12f0d",
            "2f292fcaa19b1eefc96cfd8dd4fac6c6e382514a0c3f6889a5c12f0e"
        };
        long[] amounts = {1000000000L, 2000000000L, 3000000000L};

        System.out.println("\n-- SQL to update test data");
        System.out.println("DELETE FROM preprod.epoch_stake WHERE epoch = 232;");

        for (int i = 0; i < credentialHashes.length; i++) {
            byte[] keyHash = HexUtil.decodeHexString(credentialHashes[i]);
            Address stakeAddr = AddressProvider.getRewardAddress(
                com.bloxbean.cardano.client.address.Credential.fromKey(keyHash),
                Networks.testnet()
            );
            System.out.printf("INSERT INTO preprod.epoch_stake (address, amount, pool_id, epoch) VALUES ('%s', %d, '%s', 232);%n",
                stakeAddr.toBech32(), amounts[i], poolHashes[i]);
        }
    }
}

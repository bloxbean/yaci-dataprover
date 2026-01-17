package com.bloxbean.cardano.dataprover.util;

import com.bloxbean.cardano.vds.mpt.MpfTrie;
import com.bloxbean.cardano.vds.mpt.proof.ProofFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Utility service for MPF proof operations.
 */
@Component
public class ProofUtilsService {

    private static final Logger log = LoggerFactory.getLogger(ProofUtilsService.class);

    public String toAikenFormat(byte[] proofWire) {
        try {
            return ProofFormatter.toAiken(proofWire);
        } catch (Exception e) {
            log.error("Failed to convert proof to Aiken format", e);
            throw new IllegalArgumentException("Failed to convert proof to Aiken format", e);
        }
    }

    public boolean verifyProof(byte[] rootHash, byte[] key, byte[] value,
                              boolean expectedPresence, byte[] proofWire) {
        try {
            MpfTrie trie = new MpfTrie(null, rootHash);
            return trie.verifyProofWire(rootHash, key, value, expectedPresence, proofWire);
        } catch (Exception e) {
            log.error("Failed to verify proof", e);
            return false;
        }
    }

    public boolean isWellFormed(byte[] proofWire) {
        if (proofWire == null || proofWire.length == 0) {
            return false;
        }

        try {
            ProofFormatter.toAiken(proofWire);
            return true;
        } catch (Exception e) {
            log.debug("Proof is not well-formed: {}", e.getMessage());
            return false;
        }
    }
}

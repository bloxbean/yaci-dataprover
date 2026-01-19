package com.bloxbean.cardano.dataprover.service;

import com.bloxbean.cardano.dataprover.dto.ProofGenerationRequest;
import com.bloxbean.cardano.dataprover.dto.ProofGenerationResponse;
import com.bloxbean.cardano.dataprover.dto.ProofVerificationRequest;
import com.bloxbean.cardano.dataprover.dto.ProofVerificationResponse;
import com.bloxbean.cardano.dataprover.dto.ValueLookupResponse;
import com.bloxbean.cardano.dataprover.exception.ProofGenerationException;
import com.bloxbean.cardano.dataprover.exception.MerkleNotFoundException;
import com.bloxbean.cardano.dataprover.service.merkle.MerkleImplementation;
import com.bloxbean.cardano.dataprover.service.merkle.MerkleRegistry;
import com.bloxbean.cardano.dataprover.util.ProofUtilsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Service for proof generation and verification operations.
 */
@Service
public class ProofService {

    private static final Logger log = LoggerFactory.getLogger(ProofService.class);
    private static final HexFormat HEX = HexFormat.of();

    private final MerkleRegistry merkleRegistry;
    private final ProofUtilsService proofUtils;

    public ProofService(MerkleRegistry merkleRegistry, ProofUtilsService proofUtils) {
        this.merkleRegistry = merkleRegistry;
        this.proofUtils = proofUtils;
    }

    /**
     * Strip 0x prefix from hex string if present.
     */
    private String stripHexPrefix(String hex) {
        if (hex != null && hex.startsWith("0x")) {
            return hex.substring(2);
        }
        return hex;
    }

    public ProofGenerationResponse generateProof(String merkleIdentifier, ProofGenerationRequest request) {
        log.debug("Generating proof for key {} in merkle {}", request.getKey(), merkleIdentifier);

        MerkleImplementation merkle = merkleRegistry.getOrLoadMerkle(merkleIdentifier);
        if (merkle == null) {
            throw new MerkleNotFoundException(merkleIdentifier);
        }

        try {
            byte[] keyBytes = HEX.parseHex(stripHexPrefix(request.getKey()));

            Optional<byte[]> proofOpt = merkle.getProofWire(keyBytes);
            if (proofOpt.isEmpty()) {
                throw new ProofGenerationException("Failed to generate proof for key: " + request.getKey());
            }

            byte[] proof = proofOpt.get();

            Optional<byte[]> valueOpt = merkle.get(keyBytes);
            String valueHex = valueOpt.map(HEX::formatHex).orElse(null);

            byte[] rootHash = merkle.getRootHash();
            String rootHashHex = HEX.formatHex(rootHash);

            String proofHex;
            String format = request.getFormat();

            if ("aiken".equalsIgnoreCase(format)) {
                String aikenProof = proofUtils.toAikenFormat(proof);
                proofHex = aikenProof;
                format = "aiken";
            } else {
                proofHex = HEX.formatHex(proof);
                format = "wire";
            }

            log.debug("Generated proof for key {} in merkle {} (format: {})",
                    request.getKey(), merkleIdentifier, format);

            return ProofGenerationResponse.builder()
                    .key(request.getKey())
                    .value(valueHex)
                    .proof(proofHex)
                    .rootHash(rootHashHex)
                    .proofFormat(format)
                    .build();

        } catch (IllegalArgumentException e) {
            throw new ProofGenerationException("Invalid hex key: " + request.getKey(), e);
        } catch (Exception e) {
            log.error("Error generating proof for key {} in merkle {}", request.getKey(), merkleIdentifier, e);
            throw new ProofGenerationException("Failed to generate proof: " + e.getMessage(), e);
        }
    }

    public List<ProofGenerationResponse> generateBatchProofs(String merkleIdentifier, List<ProofGenerationRequest> requests) {
        log.info("Generating batch proofs for {} keys in merkle {}", requests.size(), merkleIdentifier);

        List<ProofGenerationResponse> responses = new ArrayList<>();

        for (ProofGenerationRequest request : requests) {
            try {
                ProofGenerationResponse response = generateProof(merkleIdentifier, request);
                responses.add(response);
            } catch (Exception e) {
                log.warn("Failed to generate proof for key {} in merkle {}: {}",
                        request.getKey(), merkleIdentifier, e.getMessage());

                ProofGenerationResponse errorResponse = ProofGenerationResponse.builder()
                        .key(request.getKey())
                        .build();
                responses.add(errorResponse);
            }
        }

        log.info("Generated {} proofs out of {} requests for merkle {}",
                responses.size(), requests.size(), merkleIdentifier);

        return responses;
    }

    public ProofVerificationResponse verifyProof(String merkleIdentifier, ProofVerificationRequest request) {
        log.debug("Verifying proof for key {} in merkle {}", request.getKey(), merkleIdentifier);

        MerkleImplementation merkle = merkleRegistry.getOrLoadMerkle(merkleIdentifier);
        if (merkle == null) {
            throw new MerkleNotFoundException(merkleIdentifier);
        }

        try {
            byte[] keyBytes = HEX.parseHex(stripHexPrefix(request.getKey()));
            byte[] proofBytes = HEX.parseHex(stripHexPrefix(request.getProof()));
            byte[] valueBytes = request.getValue() != null ? HEX.parseHex(stripHexPrefix(request.getValue())) : null;
            byte[] rootHashBytes = HEX.parseHex(stripHexPrefix(request.getRootHash()));

            boolean expectedPresence = valueBytes != null;
            Optional<byte[]> proofOpt = Optional.of(proofBytes);

            boolean verified = merkle.verifyProofWire(
                    rootHashBytes,
                    keyBytes,
                    valueBytes,
                    expectedPresence,
                    proofOpt
            );

            log.debug("Proof verification for key {} in merkle {}: {}",
                    request.getKey(), merkleIdentifier, verified ? "VALID" : "INVALID");

            return ProofVerificationResponse.builder()
                    .key(request.getKey())
                    .value(request.getValue())
                    .rootHash(request.getRootHash())
                    .verified(verified)
                    .build();

        } catch (IllegalArgumentException e) {
            throw new ProofGenerationException("Invalid hex input: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error verifying proof for key {} in merkle {}", request.getKey(), merkleIdentifier, e);
            throw new ProofGenerationException("Failed to verify proof: " + e.getMessage(), e);
        }
    }

    public String getRootHash(String merkleIdentifier) {
        log.debug("Getting root hash for merkle {}", merkleIdentifier);

        MerkleImplementation merkle = merkleRegistry.getOrLoadMerkle(merkleIdentifier);
        if (merkle == null) {
            throw new MerkleNotFoundException(merkleIdentifier);
        }

        byte[] rootHash = merkle.getRootHash();
        if (rootHash == null) {
            log.debug("Root hash for merkle {} is null (empty merkle tree)", merkleIdentifier);
            return null;
        }

        String rootHashHex = HEX.formatHex(rootHash);

        log.debug("Root hash for merkle {}: {}", merkleIdentifier, rootHashHex);

        return rootHashHex;
    }

    public ValueLookupResponse getValue(String merkleIdentifier, String hexKey) {
        log.debug("Looking up value for key {} in merkle {}", hexKey, merkleIdentifier);

        MerkleImplementation merkle = merkleRegistry.getOrLoadMerkle(merkleIdentifier);
        if (merkle == null) {
            throw new MerkleNotFoundException(merkleIdentifier);
        }

        try {
            String normalizedKey = normalizeHexKey(hexKey);
            byte[] keyBytes = HEX.parseHex(stripHexPrefix(normalizedKey));

            Optional<byte[]> valueOpt = merkle.get(keyBytes);

            if (valueOpt.isPresent()) {
                String valueHex = HEX.formatHex(valueOpt.get());
                log.debug("Found value for key {} in merkle {}", hexKey, merkleIdentifier);

                return ValueLookupResponse.builder()
                        .key(hexKey)
                        .value(valueHex)
                        .found(true)
                        .build();
            } else {
                log.debug("Key {} not found in merkle {}", hexKey, merkleIdentifier);

                return ValueLookupResponse.builder()
                        .key(hexKey)
                        .value(null)
                        .found(false)
                        .build();
            }

        } catch (IllegalArgumentException e) {
            throw new ProofGenerationException("Invalid hex key: " + hexKey, e);
        }
    }

    public List<ValueLookupResponse> getValues(String merkleIdentifier, List<String> hexKeys) {
        log.info("Looking up {} keys in merkle {}", hexKeys.size(), merkleIdentifier);

        List<ValueLookupResponse> responses = new ArrayList<>();

        for (String hexKey : hexKeys) {
            try {
                ValueLookupResponse response = getValue(merkleIdentifier, hexKey);
                responses.add(response);
            } catch (Exception e) {
                log.warn("Failed to lookup value for key {} in merkle {}: {}",
                        hexKey, merkleIdentifier, e.getMessage());

                responses.add(ValueLookupResponse.builder()
                        .key(hexKey)
                        .value(null)
                        .found(false)
                        .build());
            }
        }

        log.info("Looked up {} keys in merkle {}", responses.size(), merkleIdentifier);

        return responses;
    }

    private String normalizeHexKey(String hexKey) {
        if (hexKey != null && hexKey.startsWith("0x")) {
            return hexKey.substring(2);
        }
        return hexKey;
    }
}

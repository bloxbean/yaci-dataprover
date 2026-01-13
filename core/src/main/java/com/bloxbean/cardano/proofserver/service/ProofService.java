package com.bloxbean.cardano.proofserver.service;

import com.bloxbean.cardano.proofserver.dto.ProofGenerationRequest;
import com.bloxbean.cardano.proofserver.dto.ProofGenerationResponse;
import com.bloxbean.cardano.proofserver.dto.ProofVerificationRequest;
import com.bloxbean.cardano.proofserver.dto.ProofVerificationResponse;
import com.bloxbean.cardano.proofserver.exception.ProofGenerationException;
import com.bloxbean.cardano.proofserver.exception.TrieNotFoundException;
import com.bloxbean.cardano.proofserver.service.trie.TrieImplementation;
import com.bloxbean.cardano.proofserver.service.trie.TrieRegistry;
import com.bloxbean.cardano.proofserver.util.ProofUtilsService;
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

    private final TrieRegistry trieRegistry;
    private final ProofUtilsService proofUtils;

    public ProofService(TrieRegistry trieRegistry, ProofUtilsService proofUtils) {
        this.trieRegistry = trieRegistry;
        this.proofUtils = proofUtils;
    }

    public ProofGenerationResponse generateProof(String trieIdentifier, ProofGenerationRequest request) {
        log.debug("Generating proof for key {} in trie {}", request.getKey(), trieIdentifier);

        TrieImplementation trie = trieRegistry.getOrLoadTrie(trieIdentifier);
        if (trie == null) {
            throw new TrieNotFoundException(trieIdentifier);
        }

        try {
            byte[] keyBytes = HEX.parseHex(request.getKey());

            Optional<byte[]> proofOpt = trie.getProofWire(keyBytes);
            if (proofOpt.isEmpty()) {
                throw new ProofGenerationException("Failed to generate proof for key: " + request.getKey());
            }

            byte[] proof = proofOpt.get();

            Optional<byte[]> valueOpt = trie.get(keyBytes);
            String valueHex = valueOpt.map(HEX::formatHex).orElse(null);

            byte[] rootHash = trie.getRootHash();
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

            log.debug("Generated proof for key {} in trie {} (format: {})",
                    request.getKey(), trieIdentifier, format);

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
            log.error("Error generating proof for key {} in trie {}", request.getKey(), trieIdentifier, e);
            throw new ProofGenerationException("Failed to generate proof: " + e.getMessage(), e);
        }
    }

    public List<ProofGenerationResponse> generateBatchProofs(String trieIdentifier, List<ProofGenerationRequest> requests) {
        log.info("Generating batch proofs for {} keys in trie {}", requests.size(), trieIdentifier);

        List<ProofGenerationResponse> responses = new ArrayList<>();

        for (ProofGenerationRequest request : requests) {
            try {
                ProofGenerationResponse response = generateProof(trieIdentifier, request);
                responses.add(response);
            } catch (Exception e) {
                log.warn("Failed to generate proof for key {} in trie {}: {}",
                        request.getKey(), trieIdentifier, e.getMessage());

                ProofGenerationResponse errorResponse = ProofGenerationResponse.builder()
                        .key(request.getKey())
                        .build();
                responses.add(errorResponse);
            }
        }

        log.info("Generated {} proofs out of {} requests for trie {}",
                responses.size(), requests.size(), trieIdentifier);

        return responses;
    }

    public ProofVerificationResponse verifyProof(String trieIdentifier, ProofVerificationRequest request) {
        log.debug("Verifying proof for key {} in trie {}", request.getKey(), trieIdentifier);

        TrieImplementation trie = trieRegistry.getOrLoadTrie(trieIdentifier);
        if (trie == null) {
            throw new TrieNotFoundException(trieIdentifier);
        }

        try {
            byte[] keyBytes = HEX.parseHex(request.getKey());
            byte[] proofBytes = HEX.parseHex(request.getProof());
            byte[] valueBytes = request.getValue() != null ? HEX.parseHex(request.getValue()) : null;
            byte[] rootHashBytes = HEX.parseHex(request.getRootHash());

            boolean expectedPresence = valueBytes != null;
            Optional<byte[]> proofOpt = Optional.of(proofBytes);

            boolean verified = trie.verifyProofWire(
                    rootHashBytes,
                    keyBytes,
                    valueBytes,
                    expectedPresence,
                    proofOpt
            );

            log.debug("Proof verification for key {} in trie {}: {}",
                    request.getKey(), trieIdentifier, verified ? "VALID" : "INVALID");

            return ProofVerificationResponse.builder()
                    .key(request.getKey())
                    .value(request.getValue())
                    .rootHash(request.getRootHash())
                    .verified(verified)
                    .build();

        } catch (IllegalArgumentException e) {
            throw new ProofGenerationException("Invalid hex input: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error verifying proof for key {} in trie {}", request.getKey(), trieIdentifier, e);
            throw new ProofGenerationException("Failed to verify proof: " + e.getMessage(), e);
        }
    }

    public String getRootHash(String trieIdentifier) {
        log.debug("Getting root hash for trie {}", trieIdentifier);

        TrieImplementation trie = trieRegistry.getOrLoadTrie(trieIdentifier);
        if (trie == null) {
            throw new TrieNotFoundException(trieIdentifier);
        }

        byte[] rootHash = trie.getRootHash();
        String rootHashHex = HEX.formatHex(rootHash);

        log.debug("Root hash for trie {}: {}", trieIdentifier, rootHashHex);

        return rootHashHex;
    }
}

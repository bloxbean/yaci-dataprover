package com.bloxbean.cardano.proofserver.controller;

import com.bloxbean.cardano.proofserver.dto.ProofGenerationRequest;
import com.bloxbean.cardano.proofserver.dto.ProofGenerationResponse;
import com.bloxbean.cardano.proofserver.dto.ProofVerificationRequest;
import com.bloxbean.cardano.proofserver.dto.ProofVerificationResponse;
import com.bloxbean.cardano.proofserver.service.ProofService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for proof generation and verification operations.
 */
@RestController
@RequestMapping("/api/v1/tries/{trieId}")
public class ProofController {

    private static final Logger log = LoggerFactory.getLogger(ProofController.class);

    private final ProofService proofService;

    public ProofController(ProofService proofService) {
        this.proofService = proofService;
    }

    @PostMapping("/proofs")
    public ResponseEntity<ProofGenerationResponse> generateProof(
            @PathVariable String trieId,
            @Valid @RequestBody ProofGenerationRequest request) {

        log.info("Generating proof for key {} in trie {}", request.getKey(), trieId);

        ProofGenerationResponse response = proofService.generateProof(trieId, request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/proofs/batch")
    public ResponseEntity<List<ProofGenerationResponse>> generateBatchProofs(
            @PathVariable String trieId,
            @Valid @RequestBody List<ProofGenerationRequest> requests) {

        log.info("Generating batch proofs for {} keys in trie {}", requests.size(), trieId);

        List<ProofGenerationResponse> responses = proofService.generateBatchProofs(trieId, requests);

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/proofs/verify")
    public ResponseEntity<ProofVerificationResponse> verifyProof(
            @PathVariable String trieId,
            @Valid @RequestBody ProofVerificationRequest request) {

        log.info("Verifying proof for key {} in trie {}", request.getKey(), trieId);

        ProofVerificationResponse response = proofService.verifyProof(trieId, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/root")
    public ResponseEntity<Map<String, String>> getRootHash(@PathVariable String trieId) {

        log.debug("Getting root hash for trie {}", trieId);

        String rootHash = proofService.getRootHash(trieId);

        return ResponseEntity.ok(Map.of(
                "trieIdentifier", trieId,
                "rootHash", rootHash
        ));
    }
}

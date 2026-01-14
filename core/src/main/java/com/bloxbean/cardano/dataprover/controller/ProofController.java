package com.bloxbean.cardano.dataprover.controller;

import com.bloxbean.cardano.dataprover.dto.ProofGenerationRequest;
import com.bloxbean.cardano.dataprover.dto.ProofGenerationResponse;
import com.bloxbean.cardano.dataprover.dto.ProofVerificationRequest;
import com.bloxbean.cardano.dataprover.dto.ProofVerificationResponse;
import com.bloxbean.cardano.dataprover.service.ProofService;
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
@RequestMapping("/api/v1/merkle/{merkleId}")
public class ProofController {

    private static final Logger log = LoggerFactory.getLogger(ProofController.class);

    private final ProofService proofService;

    public ProofController(ProofService proofService) {
        this.proofService = proofService;
    }

    @PostMapping("/proofs")
    public ResponseEntity<ProofGenerationResponse> generateProof(
            @PathVariable String merkleId,
            @Valid @RequestBody ProofGenerationRequest request) {

        log.info("Generating proof for key {} in merkle {}", request.getKey(), merkleId);

        ProofGenerationResponse response = proofService.generateProof(merkleId, request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/proofs/batch")
    public ResponseEntity<List<ProofGenerationResponse>> generateBatchProofs(
            @PathVariable String merkleId,
            @Valid @RequestBody List<ProofGenerationRequest> requests) {

        log.info("Generating batch proofs for {} keys in merkle {}", requests.size(), merkleId);

        List<ProofGenerationResponse> responses = proofService.generateBatchProofs(merkleId, requests);

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/proofs/verify")
    public ResponseEntity<ProofVerificationResponse> verifyProof(
            @PathVariable String merkleId,
            @Valid @RequestBody ProofVerificationRequest request) {

        log.info("Verifying proof for key {} in merkle {}", request.getKey(), merkleId);

        ProofVerificationResponse response = proofService.verifyProof(merkleId, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/root")
    public ResponseEntity<Map<String, String>> getRootHash(@PathVariable String merkleId) {

        log.debug("Getting root hash for merkle {}", merkleId);

        String rootHash = proofService.getRootHash(merkleId);

        return ResponseEntity.ok(Map.of(
                "merkleIdentifier", merkleId,
                "rootHash", rootHash
        ));
    }
}

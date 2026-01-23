package com.bloxbean.cardano.dataprover.controller;

import com.bloxbean.cardano.dataprover.dto.CreateMerkleRequest;
import com.bloxbean.cardano.dataprover.dto.MerkleEntriesResponse;
import com.bloxbean.cardano.dataprover.dto.MerkleResponse;
import com.bloxbean.cardano.dataprover.dto.MerkleSizeResponse;
import com.bloxbean.cardano.dataprover.model.MerkleStatus;
import com.bloxbean.cardano.dataprover.service.MerkleManagementService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for merkle management operations.
 */
@RestController
@RequestMapping("/api/v1/merkle")
public class MerkleController {

    private static final Logger log = LoggerFactory.getLogger(MerkleController.class);

    private final MerkleManagementService merkleService;

    public MerkleController(MerkleManagementService merkleService) {
        this.merkleService = merkleService;
    }

    @PostMapping
    public ResponseEntity<MerkleResponse> createMerkle(@Valid @RequestBody CreateMerkleRequest request) {
        log.info("Creating merkle: {}", request.getIdentifier());
        MerkleResponse response = merkleService.createMerkle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<MerkleResponse> getMerkle(@PathVariable String identifier) {
        log.debug("Getting merkle: {}", identifier);
        MerkleResponse response = merkleService.getMerkle(identifier);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<MerkleResponse>> listMerkle(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) MerkleStatus status) {

        log.debug("Listing merkle: page={}, size={}, status={}", page, size, status);

        PageRequest pageable = PageRequest.of(page, size);
        Page<MerkleResponse> response = merkleService.listMerkle(pageable, status);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteMerkle(@PathVariable String identifier) {
        log.info("Deleting merkle: {}", identifier);
        merkleService.deleteMerkle(identifier);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{identifier}/size")
    public ResponseEntity<MerkleSizeResponse> computeSize(@PathVariable String identifier) {
        log.info("Computing size for merkle: {}", identifier);
        MerkleSizeResponse response = merkleService.computeSize(identifier);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{identifier}/entries")
    public ResponseEntity<MerkleEntriesResponse> getEntries(
            @PathVariable String identifier,
            @RequestParam(defaultValue = "100") int limit) {
        log.info("Getting entries for merkle: {} (limit: {})", identifier, limit);
        MerkleEntriesResponse response = merkleService.getEntries(identifier, limit);
        return ResponseEntity.ok(response);
    }
}

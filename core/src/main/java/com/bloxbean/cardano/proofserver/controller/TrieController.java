package com.bloxbean.cardano.proofserver.controller;

import com.bloxbean.cardano.proofserver.dto.CreateTrieRequest;
import com.bloxbean.cardano.proofserver.dto.TrieResponse;
import com.bloxbean.cardano.proofserver.model.TrieStatus;
import com.bloxbean.cardano.proofserver.service.TrieManagementService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for trie management operations.
 */
@RestController
@RequestMapping("/api/v1/tries")
public class TrieController {

    private static final Logger log = LoggerFactory.getLogger(TrieController.class);

    private final TrieManagementService trieService;

    public TrieController(TrieManagementService trieService) {
        this.trieService = trieService;
    }

    @PostMapping
    public ResponseEntity<TrieResponse> createTrie(@Valid @RequestBody CreateTrieRequest request) {
        log.info("Creating trie: {}", request.getIdentifier());
        TrieResponse response = trieService.createTrie(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<TrieResponse> getTrie(@PathVariable String identifier) {
        log.debug("Getting trie: {}", identifier);
        TrieResponse response = trieService.getTrie(identifier);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<TrieResponse>> listTries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TrieStatus status) {

        log.debug("Listing tries: page={}, size={}, status={}", page, size, status);

        PageRequest pageable = PageRequest.of(page, size);
        Page<TrieResponse> response = trieService.listTries(pageable, status);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteTrie(@PathVariable String identifier) {
        log.info("Deleting trie: {}", identifier);
        trieService.deleteTrie(identifier);
        return ResponseEntity.noContent().build();
    }
}

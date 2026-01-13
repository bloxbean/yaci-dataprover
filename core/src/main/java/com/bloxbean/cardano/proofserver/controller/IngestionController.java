package com.bloxbean.cardano.proofserver.controller;

import com.bloxbean.cardano.proofserver.dto.AddEntriesRequest;
import com.bloxbean.cardano.proofserver.dto.AddEntriesResponse;
import com.bloxbean.cardano.proofserver.dto.IngestRequest;
import com.bloxbean.cardano.proofserver.dto.IngestResponse;
import com.bloxbean.cardano.proofserver.service.IngestionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for data ingestion operations.
 */
@RestController
@RequestMapping("/api/v1/tries/{trieId}")
public class IngestionController {

    private static final Logger log = LoggerFactory.getLogger(IngestionController.class);

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<IngestResponse> ingestData(
            @PathVariable String trieId,
            @Valid @RequestBody IngestRequest request) {

        log.info("Ingesting data into trie {} using provider {}",
                trieId, request.getProvider());

        IngestResponse response = ingestionService.ingestData(trieId, request);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/entries")
    public ResponseEntity<AddEntriesResponse> addEntries(
            @PathVariable String trieId,
            @Valid @RequestBody AddEntriesRequest request) {

        log.info("Adding {} entries to trie {}", request.getEntries().size(), trieId);

        AddEntriesResponse response = ingestionService.addEntries(trieId, request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}

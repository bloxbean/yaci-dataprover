package com.bloxbean.cardano.dataprover.controller;

import com.bloxbean.cardano.dataprover.dto.AddEntriesRequest;
import com.bloxbean.cardano.dataprover.dto.AddEntriesResponse;
import com.bloxbean.cardano.dataprover.dto.IngestRequest;
import com.bloxbean.cardano.dataprover.dto.IngestResponse;
import com.bloxbean.cardano.dataprover.service.IngestionService;
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
@RequestMapping("/api/v1/merkle/{merkleId}")
public class IngestionController {

    private static final Logger log = LoggerFactory.getLogger(IngestionController.class);

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<IngestResponse> ingestData(
            @PathVariable String merkleId,
            @Valid @RequestBody IngestRequest request) {

        log.info("Ingesting data into merkle {} using provider {}",
                merkleId, request.getProvider());

        IngestResponse response = ingestionService.ingestData(merkleId, request);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/entries")
    public ResponseEntity<AddEntriesResponse> addEntries(
            @PathVariable String merkleId,
            @Valid @RequestBody AddEntriesRequest request) {

        log.info("Adding {} entries to merkle {}", request.getEntries().size(), merkleId);

        AddEntriesResponse response = ingestionService.addEntries(merkleId, request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}

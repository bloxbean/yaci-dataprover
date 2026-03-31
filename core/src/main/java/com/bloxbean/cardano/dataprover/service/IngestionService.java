package com.bloxbean.cardano.dataprover.service;

import com.bloxbean.cardano.dataprover.dto.*;
import com.bloxbean.cardano.dataprover.exception.DataProviderException;
import com.bloxbean.cardano.dataprover.exception.MerkleNotFoundException;
import com.bloxbean.cardano.dataprover.model.MerkleMetadata;
import com.bloxbean.cardano.dataprover.model.MerkleStatus;
import com.bloxbean.cardano.dataprover.repository.MerkleMetadataRepository;
import com.bloxbean.cardano.dataprover.service.provider.DataProvider;
import com.bloxbean.cardano.dataprover.service.provider.DataProviderRegistry;
import com.bloxbean.cardano.dataprover.service.provider.ValidationResult;
import com.bloxbean.cardano.dataprover.service.merkle.MerkleConfiguration;
import com.bloxbean.cardano.dataprover.service.merkle.MerkleFactory;
import com.bloxbean.cardano.dataprover.service.merkle.MerkleImplementation;
import com.bloxbean.cardano.dataprover.service.merkle.MerkleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Service for data ingestion operations.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    private static final HexFormat HEX = HexFormat.of();

    private final MerkleRegistry merkleRegistry;
    private final MerkleFactory merkleFactory;
    private final DataProviderRegistry providerRegistry;
    private final MerkleMetadataRepository metadataRepository;

    public IngestionService(MerkleRegistry merkleRegistry,
                           MerkleFactory merkleFactory,
                           DataProviderRegistry providerRegistry,
                           MerkleMetadataRepository metadataRepository) {
        this.merkleRegistry = merkleRegistry;
        this.merkleFactory = merkleFactory;
        this.providerRegistry = providerRegistry;
        this.metadataRepository = metadataRepository;
    }

    @Transactional
    public IngestResponse ingestData(String merkleIdentifier, IngestRequest request) {
        long startTime = System.currentTimeMillis();

        log.info("Starting ingestion for merkle {} using provider {}",
                merkleIdentifier, request.getProvider());

        MerkleImplementation merkle = merkleRegistry.getOrLoadMerkle(merkleIdentifier);
        if (merkle == null) {
            throw new MerkleNotFoundException(merkleIdentifier);
        }

        DataProvider<?> provider = providerRegistry.getProvider(request.getProvider());

        IngestResponse response = processData(merkle, provider, request, merkleIdentifier);

        // Update the root hash in the metadata
        if (response.getRecordsProcessed() > 0) {
            MerkleMetadata metadata = metadataRepository.findByIdentifier(merkleIdentifier)
                    .orElseThrow(() -> new MerkleNotFoundException(merkleIdentifier));
            metadata.setRootHash(response.getRootHash());
            metadata.touch();
            metadataRepository.save(metadata);
        }

        long duration = System.currentTimeMillis() - startTime;
        response.setDurationMs(duration);

        log.info("Completed ingestion for merkle {} using provider {}: {} records processed, {} skipped in {}ms",
                merkleIdentifier, request.getProvider(),
                response.getRecordsProcessed(), response.getRecordsSkipped(), duration);

        return response;
    }

    @SuppressWarnings("unchecked")
    private <T> IngestResponse processData(
            MerkleImplementation merkle,
            DataProvider<?> provider,
            IngestRequest request,
            String merkleIdentifier) {

        DataProvider<T> typedProvider = (DataProvider<T>) provider;

        int recordsProcessed = 0;
        int recordsSkipped = 0;
        List<String> errors = new ArrayList<>();

        try {
            log.debug("Fetching data from provider: {}", provider.getName());
            List<T> dataList = typedProvider.fetchData(request.getConfig());

            log.info("Fetched {} records from provider {}", dataList.size(), provider.getName());

            for (T data : dataList) {
                try {
                    ValidationResult validation = typedProvider.validate(data);
                    if (!validation.isValid()) {
                        log.warn("Skipping invalid record: {}", validation.getErrorMessage());
                        recordsSkipped++;
                        errors.add(validation.getErrorMessage());
                        continue;
                    }

                    byte[] key = typedProvider.serializeKey(data);
                    byte[] value = typedProvider.serializeValue(data);

                    merkle.put(key, value);

                    recordsProcessed++;

                    if (recordsProcessed % 1000 == 0) {
                        log.debug("Processed {} records...", recordsProcessed);
                    }

                } catch (Exception e) {
                    log.warn("Error processing record: {}", e.getMessage());
                    recordsSkipped++;
                    errors.add("Error processing record: " + e.getMessage());
                }
            }

            byte[] rootHash = merkle.getRootHash();
            String rootHashHex = rootHash != null ? HEX.formatHex(rootHash) : null;

            log.info("Ingestion complete. Root hash: {}", rootHashHex);

            return IngestResponse.builder()
                    .merkleIdentifier(merkleIdentifier)
                    .provider(provider.getName())
                    .recordsProcessed(recordsProcessed)
                    .recordsSkipped(recordsSkipped)
                    .rootHash(rootHashHex)
                    .errors(errors.isEmpty() ? null : errors)
                    .build();

        } catch (Exception e) {
            log.error("Error during data ingestion", e);
            throw new DataProviderException(provider.getName(), "Failed to ingest data: " + e.getMessage(), e);
        }
    }

    /**
     * Adds entries directly to a merkle without using a DataProvider.
     * Keys and values are hex-encoded in the request.
     *
     * @param merkleIdentifier the merkle identifier
     * @param request the request containing entries to add
     * @return response with results
     */
    @Transactional
    public AddEntriesResponse addEntries(String merkleIdentifier, AddEntriesRequest request) {
        long startTime = System.currentTimeMillis();

        log.info("Adding {} entries directly to merkle {}",
                request.getEntries().size(), merkleIdentifier);

        MerkleImplementation merkle = merkleRegistry.getOrLoadMerkle(merkleIdentifier);
        if (merkle == null) {
            throw new MerkleNotFoundException(merkleIdentifier);
        }

        int entriesAdded = 0;
        int entriesSkipped = 0;
        List<String> errors = new ArrayList<>();

        for (EntryItem entry : request.getEntries()) {
            try {
                byte[] key = entry.getKeyBytes();
                byte[] value = entry.getValueBytes();

                merkle.put(key, value);
                entriesAdded++;

                if (entriesAdded % 1000 == 0) {
                    log.debug("Added {} entries...", entriesAdded);
                }

            } catch (Exception e) {
                log.warn("Error adding entry with key {}: {}", entry.getKey(), e.getMessage());
                entriesSkipped++;
                errors.add("Entry " + entry.getKey() + ": " + e.getMessage());
            }
        }

        byte[] rootHash = merkle.getRootHash();
        String rootHashHex = HEX.formatHex(rootHash);

        // Update the root hash in the metadata
        if (entriesAdded > 0) {
            MerkleMetadata metadata = metadataRepository.findByIdentifier(merkleIdentifier)
                    .orElseThrow(() -> new MerkleNotFoundException(merkleIdentifier));
            metadata.setRootHash(rootHashHex);
            metadata.touch();
            metadataRepository.save(metadata);
        }

        long duration = System.currentTimeMillis() - startTime;

        log.info("Completed adding entries to merkle {}: {} added, {} skipped, root hash: {} in {}ms",
                merkleIdentifier, entriesAdded, entriesSkipped, rootHashHex, duration);

        return AddEntriesResponse.builder()
                .merkleIdentifier(merkleIdentifier)
                .entriesAdded(entriesAdded)
                .entriesSkipped(entriesSkipped)
                .rootHash(rootHashHex)
                .durationMs(duration)
                .errors(errors.isEmpty() ? null : errors)
                .build();
    }

    /**
     * Ingests data using a provider with optional auto-create merkle support.
     *
     * @param request the provider ingest request
     * @return response with results
     */
    @Transactional
    public ProviderIngestResponse ingestWithProvider(ProviderIngestRequest request) {
        long startTime = System.currentTimeMillis();
        String merkleIdentifier = request.getMerkleName();
        boolean merkleCreated = false;

        log.info("Starting provider ingestion for merkle {} using provider {}",
                merkleIdentifier, request.getProvider());

        // Check if merkle exists
        boolean exists = metadataRepository.existsByIdentifier(merkleIdentifier);

        if (!exists && request.isCreateIfNotExists()) {
            // Create the merkle
            log.info("Creating new merkle: {}", merkleIdentifier);

            String scheme = request.getMerkleScheme() != null ? request.getMerkleScheme() : "mpf";
            boolean storeOriginalKeys = request.getStoreOriginalKeys() != null && request.getStoreOriginalKeys();

            MerkleConfiguration config = MerkleConfiguration.builder()
                    .identifier(merkleIdentifier)
                    .scheme(scheme)
                    .storeOriginalKeys(storeOriginalKeys)
                    .build();

            MerkleImplementation merkle = merkleFactory.createMerkle(scheme, config);
            merkleRegistry.registerMerkle(merkleIdentifier, merkle);

            Map<String, Object> metadata = new HashMap<>();
            if (request.getMerkleDescription() != null) {
                metadata.put("description", request.getMerkleDescription());
            }
            metadata.put("provider", request.getProvider());

            MerkleMetadata merkleMetadata = MerkleMetadata.builder()
                    .identifier(merkleIdentifier)
                    .scheme(scheme)
                    .status(MerkleStatus.ACTIVE)
                    .customMetadata(metadata)
                    .storeOriginalKeys(storeOriginalKeys)
                    .build();

            metadataRepository.save(merkleMetadata);
            merkleCreated = true;

            log.info("Created merkle: {} (scheme: {})", merkleIdentifier, scheme);
        } else if (!exists) {
            throw new MerkleNotFoundException(merkleIdentifier);
        }

        // Create IngestRequest and delegate to existing method
        IngestRequest ingestRequest = new IngestRequest();
        ingestRequest.setProvider(request.getProvider());
        ingestRequest.setConfig(request.getConfig());

        IngestResponse ingestResponse = ingestData(merkleIdentifier, ingestRequest);

        long duration = System.currentTimeMillis() - startTime;

        log.info("Completed provider ingestion for merkle {}: {} records processed, {} skipped in {}ms",
                merkleIdentifier, ingestResponse.getRecordsProcessed(),
                ingestResponse.getRecordsSkipped(), duration);

        return ProviderIngestResponse.builder()
                .merkleIdentifier(merkleIdentifier)
                .merkleCreated(merkleCreated)
                .provider(request.getProvider())
                .recordsProcessed(ingestResponse.getRecordsProcessed())
                .recordsSkipped(ingestResponse.getRecordsSkipped())
                .rootHash(ingestResponse.getRootHash())
                .durationMs(duration)
                .errors(ingestResponse.getErrors())
                .build();
    }
}

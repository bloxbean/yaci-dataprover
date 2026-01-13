package com.bloxbean.cardano.proofserver.service;

import com.bloxbean.cardano.proofserver.dto.AddEntriesRequest;
import com.bloxbean.cardano.proofserver.dto.AddEntriesResponse;
import com.bloxbean.cardano.proofserver.dto.EntryItem;
import com.bloxbean.cardano.proofserver.dto.IngestRequest;
import com.bloxbean.cardano.proofserver.dto.IngestResponse;
import com.bloxbean.cardano.proofserver.exception.DataProviderException;
import com.bloxbean.cardano.proofserver.exception.TrieNotFoundException;
import com.bloxbean.cardano.proofserver.service.provider.DataProvider;
import com.bloxbean.cardano.proofserver.service.provider.DataProviderRegistry;
import com.bloxbean.cardano.proofserver.service.provider.ValidationResult;
import com.bloxbean.cardano.proofserver.service.trie.TrieImplementation;
import com.bloxbean.cardano.proofserver.service.trie.TrieRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Service for data ingestion operations.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    private static final HexFormat HEX = HexFormat.of();

    private final TrieRegistry trieRegistry;
    private final DataProviderRegistry providerRegistry;

    public IngestionService(TrieRegistry trieRegistry, DataProviderRegistry providerRegistry) {
        this.trieRegistry = trieRegistry;
        this.providerRegistry = providerRegistry;
    }

    public IngestResponse ingestData(String trieIdentifier, IngestRequest request) {
        long startTime = System.currentTimeMillis();

        log.info("Starting ingestion for trie {} using provider {}",
                trieIdentifier, request.getProvider());

        TrieImplementation trie = trieRegistry.getOrLoadTrie(trieIdentifier);
        if (trie == null) {
            throw new TrieNotFoundException(trieIdentifier);
        }

        DataProvider<?> provider = providerRegistry.getProvider(request.getProvider());

        IngestResponse response = processData(trie, provider, request, trieIdentifier);

        long duration = System.currentTimeMillis() - startTime;
        response.setDurationMs(duration);

        log.info("Completed ingestion for trie {} using provider {}: {} records processed, {} skipped in {}ms",
                trieIdentifier, request.getProvider(),
                response.getRecordsProcessed(), response.getRecordsSkipped(), duration);

        return response;
    }

    @SuppressWarnings("unchecked")
    private <T> IngestResponse processData(
            TrieImplementation trie,
            DataProvider<?> provider,
            IngestRequest request,
            String trieIdentifier) {

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

                    trie.put(key, value);

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

            byte[] rootHash = trie.getRootHash();
            String rootHashHex = HEX.formatHex(rootHash);

            log.info("Ingestion complete. Root hash: {}", rootHashHex);

            return IngestResponse.builder()
                    .trieIdentifier(trieIdentifier)
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
     * Adds entries directly to a trie without using a DataProvider.
     * Keys and values are hex-encoded in the request.
     *
     * @param trieIdentifier the trie identifier
     * @param request the request containing entries to add
     * @return response with results
     */
    public AddEntriesResponse addEntries(String trieIdentifier, AddEntriesRequest request) {
        long startTime = System.currentTimeMillis();

        log.info("Adding {} entries directly to trie {}",
                request.getEntries().size(), trieIdentifier);

        TrieImplementation trie = trieRegistry.getOrLoadTrie(trieIdentifier);
        if (trie == null) {
            throw new TrieNotFoundException(trieIdentifier);
        }

        int entriesAdded = 0;
        int entriesSkipped = 0;
        List<String> errors = new ArrayList<>();

        for (EntryItem entry : request.getEntries()) {
            try {
                byte[] key = entry.getKeyBytes();
                byte[] value = entry.getValueBytes();

                trie.put(key, value);
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

        byte[] rootHash = trie.getRootHash();
        String rootHashHex = HEX.formatHex(rootHash);

        long duration = System.currentTimeMillis() - startTime;

        log.info("Completed adding entries to trie {}: {} added, {} skipped, root hash: {} in {}ms",
                trieIdentifier, entriesAdded, entriesSkipped, rootHashHex, duration);

        return AddEntriesResponse.builder()
                .trieIdentifier(trieIdentifier)
                .entriesAdded(entriesAdded)
                .entriesSkipped(entriesSkipped)
                .rootHash(rootHashHex)
                .durationMs(duration)
                .errors(errors.isEmpty() ? null : errors)
                .build();
    }
}

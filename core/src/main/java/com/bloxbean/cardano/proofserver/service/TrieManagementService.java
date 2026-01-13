package com.bloxbean.cardano.proofserver.service;

import com.bloxbean.cardano.proofserver.dto.CreateTrieRequest;
import com.bloxbean.cardano.proofserver.dto.TrieResponse;
import com.bloxbean.cardano.proofserver.exception.DuplicateTrieException;
import com.bloxbean.cardano.proofserver.exception.TrieNotFoundException;
import com.bloxbean.cardano.proofserver.model.TrieIdentifier;
import com.bloxbean.cardano.proofserver.model.TrieMetadata;
import com.bloxbean.cardano.proofserver.model.TrieStatus;
import com.bloxbean.cardano.proofserver.repository.TrieMetadataRepository;
import com.bloxbean.cardano.proofserver.service.trie.TrieConfiguration;
import com.bloxbean.cardano.proofserver.service.trie.TrieFactory;
import com.bloxbean.cardano.proofserver.service.trie.TrieImplementation;
import com.bloxbean.cardano.proofserver.service.trie.TrieRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing trie lifecycle operations.
 */
@Service
public class TrieManagementService {

    private static final Logger log = LoggerFactory.getLogger(TrieManagementService.class);

    private final TrieMetadataRepository metadataRepository;
    private final TrieFactory trieFactory;
    private final TrieRegistry trieRegistry;

    public TrieManagementService(TrieMetadataRepository metadataRepository,
                                TrieFactory trieFactory,
                                TrieRegistry trieRegistry) {
        this.metadataRepository = metadataRepository;
        this.trieFactory = trieFactory;
        this.trieRegistry = trieRegistry;
    }

    @Transactional
    public TrieResponse createTrie(CreateTrieRequest request) {
        TrieIdentifier identifier = TrieIdentifier.of(request.getIdentifier());

        if (metadataRepository.existsByIdentifier(identifier.getValue())) {
            throw new DuplicateTrieException(identifier.getValue());
        }

        log.info("Creating new trie: {}", identifier.getValue());

        TrieConfiguration config = TrieConfiguration.builder()
            .identifier(identifier.getValue())
            .trieType(request.getTrieType())
            .build();

        TrieImplementation trie = trieFactory.createTrie(request.getTrieType(), config);

        trieRegistry.registerTrie(identifier.getValue(), trie);

        Map<String, Object> metadata = new HashMap<>(request.getMetadata());
        if (request.getDescription() != null) {
            metadata.put("description", request.getDescription());
        }

        TrieMetadata trieMetadata = TrieMetadata.builder()
            .identifier(identifier.getValue())
            .trieType(request.getTrieType())
            .status(TrieStatus.ACTIVE)
            .customMetadata(metadata)
            .build();

        trieMetadata = metadataRepository.save(trieMetadata);

        log.info("Created trie: {} (type: {})", identifier.getValue(), request.getTrieType());

        return TrieResponse.from(trieMetadata);
    }

    public TrieResponse getTrie(String identifier) {
        TrieMetadata metadata = metadataRepository.findByIdentifier(identifier)
            .orElseThrow(() -> new TrieNotFoundException(identifier));

        // Treat deleted tries as not found
        if (metadata.getStatus() == TrieStatus.DELETED) {
            throw new TrieNotFoundException(identifier);
        }

        return TrieResponse.from(metadata);
    }

    public Page<TrieResponse> listTries(Pageable pageable, TrieStatus status) {
        Page<TrieMetadata> metadataPage;

        if (status != null) {
            metadataPage = metadataRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            metadataPage = metadataRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return metadataPage.map(TrieResponse::from);
    }

    @Transactional
    public void deleteTrie(String identifier) {
        if (!metadataRepository.existsByIdentifier(identifier)) {
            throw new TrieNotFoundException(identifier);
        }

        log.info("Deleting trie: {}", identifier);

        trieRegistry.unloadTrie(identifier);

        metadataRepository.softDeleteByIdentifier(identifier);

        log.info("Deleted trie: {}", identifier);
    }
}

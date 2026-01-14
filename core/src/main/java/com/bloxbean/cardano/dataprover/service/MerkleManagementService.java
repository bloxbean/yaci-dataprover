package com.bloxbean.cardano.dataprover.service;

import com.bloxbean.cardano.dataprover.dto.CreateMerkleRequest;
import com.bloxbean.cardano.dataprover.dto.MerkleResponse;
import com.bloxbean.cardano.dataprover.exception.DuplicateMerkleException;
import com.bloxbean.cardano.dataprover.exception.MerkleNotFoundException;
import com.bloxbean.cardano.dataprover.model.MerkleIdentifier;
import com.bloxbean.cardano.dataprover.model.MerkleMetadata;
import com.bloxbean.cardano.dataprover.model.MerkleStatus;
import com.bloxbean.cardano.dataprover.repository.MerkleMetadataRepository;
import com.bloxbean.cardano.dataprover.service.merkle.MerkleConfiguration;
import com.bloxbean.cardano.dataprover.service.merkle.MerkleFactory;
import com.bloxbean.cardano.dataprover.service.merkle.MerkleImplementation;
import com.bloxbean.cardano.dataprover.service.merkle.MerkleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing merkle lifecycle operations.
 */
@Service
public class MerkleManagementService {

    private static final Logger log = LoggerFactory.getLogger(MerkleManagementService.class);

    private final MerkleMetadataRepository metadataRepository;
    private final MerkleFactory merkleFactory;
    private final MerkleRegistry merkleRegistry;

    public MerkleManagementService(MerkleMetadataRepository metadataRepository,
                                  MerkleFactory merkleFactory,
                                  MerkleRegistry merkleRegistry) {
        this.metadataRepository = metadataRepository;
        this.merkleFactory = merkleFactory;
        this.merkleRegistry = merkleRegistry;
    }

    @Transactional
    public MerkleResponse createMerkle(CreateMerkleRequest request) {
        MerkleIdentifier identifier = MerkleIdentifier.of(request.getIdentifier());

        if (metadataRepository.existsByIdentifier(identifier.getValue())) {
            throw new DuplicateMerkleException(identifier.getValue());
        }

        log.info("Creating new merkle: {}", identifier.getValue());

        MerkleConfiguration config = MerkleConfiguration.builder()
            .identifier(identifier.getValue())
            .scheme(request.getScheme())
            .build();

        MerkleImplementation merkle = merkleFactory.createMerkle(request.getScheme(), config);

        merkleRegistry.registerMerkle(identifier.getValue(), merkle);

        Map<String, Object> metadata = new HashMap<>(request.getMetadata());
        if (request.getDescription() != null) {
            metadata.put("description", request.getDescription());
        }

        MerkleMetadata merkleMetadata = MerkleMetadata.builder()
            .identifier(identifier.getValue())
            .scheme(request.getScheme())
            .status(MerkleStatus.ACTIVE)
            .customMetadata(metadata)
            .build();

        merkleMetadata = metadataRepository.save(merkleMetadata);

        log.info("Created merkle: {} (scheme: {})", identifier.getValue(), request.getScheme());

        return MerkleResponse.from(merkleMetadata);
    }

    public MerkleResponse getMerkle(String identifier) {
        MerkleMetadata metadata = metadataRepository.findByIdentifier(identifier)
            .orElseThrow(() -> new MerkleNotFoundException(identifier));

        // Treat deleted merkles as not found
        if (metadata.getStatus() == MerkleStatus.DELETED) {
            throw new MerkleNotFoundException(identifier);
        }

        return MerkleResponse.from(metadata);
    }

    public Page<MerkleResponse> listMerkle(Pageable pageable, MerkleStatus status) {
        Page<MerkleMetadata> metadataPage;

        if (status != null) {
            metadataPage = metadataRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            metadataPage = metadataRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return metadataPage.map(MerkleResponse::from);
    }

    @Transactional
    public void deleteMerkle(String identifier) {
        if (!metadataRepository.existsByIdentifier(identifier)) {
            throw new MerkleNotFoundException(identifier);
        }

        log.info("Deleting merkle: {}", identifier);

        merkleRegistry.unloadMerkle(identifier);

        metadataRepository.softDeleteByIdentifier(identifier);

        log.info("Deleted merkle: {}", identifier);
    }
}

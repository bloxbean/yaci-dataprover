package com.bloxbean.cardano.dataprover.repository;

import com.bloxbean.cardano.dataprover.model.MerkleMetadata;
import com.bloxbean.cardano.dataprover.model.MerkleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for accessing merkle metadata.
 * Uses Spring Data JPA for database operations.
 */
@Repository
public interface MerkleMetadataRepository extends JpaRepository<MerkleMetadata, Long> {

    Optional<MerkleMetadata> findByIdentifier(String identifier);

    List<MerkleMetadata> findByStatus(MerkleStatus status);

    Page<MerkleMetadata> findByStatusOrderByCreatedAtDesc(MerkleStatus status, Pageable pageable);

    Page<MerkleMetadata> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByIdentifier(String identifier);

    List<MerkleMetadata> findByScheme(String scheme);

    long countByStatus(MerkleStatus status);

    @Query("SELECT m FROM MerkleMetadata m WHERE m.recordCount > :minRecordCount ORDER BY m.recordCount DESC")
    List<MerkleMetadata> findMerkleWithMinRecordCount(@Param("minRecordCount") int minRecordCount);

    @Modifying
    @Query("UPDATE MerkleMetadata m SET m.status = 'DELETED', m.lastUpdated = :timestamp WHERE m.identifier = :identifier")
    void softDeleteByIdentifier(@Param("identifier") String identifier, @Param("timestamp") Instant timestamp);

    default void softDeleteByIdentifier(String identifier) {
        softDeleteByIdentifier(identifier, Instant.now());
    }
}

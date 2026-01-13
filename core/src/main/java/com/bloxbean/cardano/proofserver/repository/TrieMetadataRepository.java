package com.bloxbean.cardano.proofserver.repository;

import com.bloxbean.cardano.proofserver.model.TrieMetadata;
import com.bloxbean.cardano.proofserver.model.TrieStatus;
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
 * Repository for accessing trie metadata.
 * Uses Spring Data JPA for database operations.
 */
@Repository
public interface TrieMetadataRepository extends JpaRepository<TrieMetadata, Long> {

    Optional<TrieMetadata> findByIdentifier(String identifier);

    List<TrieMetadata> findByStatus(TrieStatus status);

    Page<TrieMetadata> findByStatusOrderByCreatedAtDesc(TrieStatus status, Pageable pageable);

    Page<TrieMetadata> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByIdentifier(String identifier);

    List<TrieMetadata> findByTrieType(String trieType);

    long countByStatus(TrieStatus status);

    @Query("SELECT t FROM TrieMetadata t WHERE t.recordCount > :minRecordCount ORDER BY t.recordCount DESC")
    List<TrieMetadata> findTriesWithMinRecordCount(@Param("minRecordCount") int minRecordCount);

    @Modifying
    @Query("UPDATE TrieMetadata t SET t.status = 'DELETED', t.lastUpdated = :timestamp WHERE t.identifier = :identifier")
    void softDeleteByIdentifier(@Param("identifier") String identifier, @Param("timestamp") Instant timestamp);

    default void softDeleteByIdentifier(String identifier) {
        softDeleteByIdentifier(identifier, Instant.now());
    }
}

package com.bloxbean.cardano.dataprover.repository;

import com.bloxbean.cardano.dataprover.model.ProviderConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for accessing provider configuration data.
 */
@Repository
public interface ProviderConfigurationRepository extends JpaRepository<ProviderConfiguration, Long> {

    Optional<ProviderConfiguration> findByProviderName(String providerName);

    boolean existsByProviderName(String providerName);

    void deleteByProviderName(String providerName);
}

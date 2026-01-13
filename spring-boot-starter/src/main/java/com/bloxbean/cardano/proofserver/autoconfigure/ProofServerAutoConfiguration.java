package com.bloxbean.cardano.proofserver.autoconfigure;

import com.bloxbean.cardano.proofserver.config.TrieServerProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Auto-configuration for Proof Server.
 */
@AutoConfiguration
@EnableConfigurationProperties(TrieServerProperties.class)
@ComponentScan(basePackages = "com.bloxbean.cardano.proofserver")
@EntityScan(basePackages = "com.bloxbean.cardano.proofserver.model")
@EnableJpaRepositories(basePackages = "com.bloxbean.cardano.proofserver.repository")
public class ProofServerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ProofServerAutoConfigProperties proofServerAutoConfigProperties() {
        return new ProofServerAutoConfigProperties();
    }
}

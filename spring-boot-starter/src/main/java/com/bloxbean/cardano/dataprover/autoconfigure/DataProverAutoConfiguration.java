package com.bloxbean.cardano.dataprover.autoconfigure;

import com.bloxbean.cardano.dataprover.config.DataProverProperties;
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
@EnableConfigurationProperties(DataProverProperties.class)
@ComponentScan(basePackages = "com.bloxbean.cardano.dataprover")
@EntityScan(basePackages = "com.bloxbean.cardano.dataprover.model")
@EnableJpaRepositories(basePackages = "com.bloxbean.cardano.dataprover.repository")
public class DataProverAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DataProverAutoConfigProperties proofServerAutoConfigProperties() {
        return new DataProverAutoConfigProperties();
    }
}

package com.bloxbean.cardano.proofserver;

import com.bloxbean.cardano.proofserver.config.TrieServerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test application for integration tests.
 * Bootstraps Spring Boot with all necessary configurations for testing.
 */
@SpringBootApplication
@EnableConfigurationProperties(TrieServerProperties.class)
@EntityScan(basePackages = "com.bloxbean.cardano.proofserver.model")
@EnableJpaRepositories(basePackages = "com.bloxbean.cardano.proofserver.repository")
@Import(com.bloxbean.cardano.proofserver.test.IntegrationTestConfig.class)
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}

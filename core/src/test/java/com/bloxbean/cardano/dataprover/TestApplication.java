package com.bloxbean.cardano.dataprover;

import com.bloxbean.cardano.dataprover.config.DataProverProperties;
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
@EnableConfigurationProperties(DataProverProperties.class)
@EntityScan(basePackages = "com.bloxbean.cardano.dataprover.model")
@EnableJpaRepositories(basePackages = "com.bloxbean.cardano.dataprover.repository")
@Import(com.bloxbean.cardano.dataprover.test.IntegrationTestConfig.class)
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}

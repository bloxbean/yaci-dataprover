package com.bloxbean.cardano.dataprover.test;

import com.bloxbean.cardano.dataprover.service.provider.DataProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Test configuration that provides test-specific beans.
 */
@TestConfiguration
public class IntegrationTestConfig {

    /**
     * Creates a TestDataProvider bean that will be auto-registered
     * with the DataProviderRegistry.
     */
    @Bean
    public DataProvider<TestDataItem> testDataProvider() {
        return new TestDataProvider();
    }
}

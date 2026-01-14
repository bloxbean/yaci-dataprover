package com.bloxbean.cardano.dataprover.test;

import com.bloxbean.cardano.dataprover.service.provider.DataProvider;
import com.bloxbean.cardano.dataprover.service.provider.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A simple test data provider for integration tests.
 * Allows programmatic control over test data.
 */
public class TestDataProvider implements DataProvider<TestDataItem> {

    public static final String PROVIDER_NAME = "test-provider";

    private List<TestDataItem> testData = new ArrayList<>();

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    @Override
    public String getDescription() {
        return "Test data provider for integration tests";
    }

    @Override
    public void initialize(Map<String, Object> config) {
        // No initialization needed for test provider
    }

    @Override
    public List<TestDataItem> fetchData(Map<String, Object> config) {
        return new ArrayList<>(testData);
    }

    @Override
    public byte[] serializeKey(TestDataItem data) {
        return data.getKey();
    }

    @Override
    public byte[] serializeValue(TestDataItem data) {
        return data.getValue();
    }

    @Override
    public ValidationResult validate(TestDataItem data) {
        if (data == null) {
            return ValidationResult.failure("Data item is null");
        }
        if (data.getKey() == null || data.getKey().length == 0) {
            return ValidationResult.failure("Key is empty");
        }
        if (data.getValue() == null || data.getValue().length == 0) {
            return ValidationResult.failure("Value is empty");
        }
        return ValidationResult.success();
    }

    @Override
    public Class<TestDataItem> getDataType() {
        return TestDataItem.class;
    }

    /**
     * Sets the test data to be returned by fetchData.
     */
    public void setTestData(List<TestDataItem> data) {
        this.testData = data != null ? new ArrayList<>(data) : new ArrayList<>();
    }

    /**
     * Adds a single test data item.
     */
    public void addTestData(TestDataItem item) {
        this.testData.add(item);
    }

    /**
     * Clears all test data.
     */
    public void clearTestData() {
        this.testData.clear();
    }

    /**
     * Returns the current count of test data items.
     */
    public int getTestDataCount() {
        return testData.size();
    }
}

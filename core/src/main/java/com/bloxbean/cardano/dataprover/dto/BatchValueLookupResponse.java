package com.bloxbean.cardano.dataprover.dto;

import java.util.List;

/**
 * Response DTO for batch value lookup.
 */
public class BatchValueLookupResponse {

    private List<ValueLookupResponse> results;

    public BatchValueLookupResponse() {
    }

    public BatchValueLookupResponse(List<ValueLookupResponse> results) {
        this.results = results;
    }

    public List<ValueLookupResponse> getResults() {
        return results;
    }

    public void setResults(List<ValueLookupResponse> results) {
        this.results = results;
    }
}

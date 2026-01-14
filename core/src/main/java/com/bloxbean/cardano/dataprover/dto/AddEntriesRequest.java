package com.bloxbean.cardano.dataprover.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for adding entries directly to a trie.
 */
public class AddEntriesRequest {

    @NotEmpty(message = "Entries list is required")
    @Valid
    private List<EntryItem> entries = new ArrayList<>();

    public AddEntriesRequest() {
    }

    public AddEntriesRequest(List<EntryItem> entries) {
        this.entries = entries;
    }

    public List<EntryItem> getEntries() {
        return entries;
    }

    public void setEntries(List<EntryItem> entries) {
        this.entries = entries;
    }
}

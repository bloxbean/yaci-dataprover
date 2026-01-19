package com.bloxbean.cardano.dataprover.dto;

import com.bloxbean.cardano.dataprover.service.provider.ProviderMetadata;

import java.util.List;

/**
 * Response containing list of available data providers.
 */
public class ProviderListResponse {
    private List<ProviderMetadata> providers;

    public ProviderListResponse() {
    }

    public ProviderListResponse(List<ProviderMetadata> providers) {
        this.providers = providers;
    }

    public List<ProviderMetadata> getProviders() {
        return providers;
    }

    public void setProviders(List<ProviderMetadata> providers) {
        this.providers = providers;
    }
}

import { apiGet, apiPost, apiPut, apiDelete } from './client';
import type {
	ProviderListResponse,
	ProviderInfo,
	SerializeKeyRequest,
	SerializeKeyResponse,
	ProviderIngestRequest,
	ProviderIngestResponse,
	ProviderConfigRequest,
	ProviderConfigResponse,
	ConfigTestRequest,
	ConfigTestResponse
} from './types';

export const providerApi = {
	/**
	 * List all available data providers
	 */
	list: () => apiGet<ProviderListResponse>('/providers'),

	/**
	 * Get detailed provider info with config schema
	 */
	get: (name: string) => apiGet<ProviderInfo>(`/providers/${encodeURIComponent(name)}`),

	/**
	 * Serialize a domain key to hex format using provider's serialization logic
	 */
	serializeKey: (providerName: string, request: SerializeKeyRequest) =>
		apiPost<SerializeKeyResponse>(
			`/providers/${encodeURIComponent(providerName)}/serialize-key`,
			request
		),

	/**
	 * Ingest data with optional auto-create merkle
	 */
	ingest: (request: ProviderIngestRequest) =>
		apiPost<ProviderIngestResponse>('/providers/ingest', request),

	/**
	 * Get current configuration for a provider (passwords masked)
	 */
	getConfig: (name: string) =>
		apiGet<ProviderConfigResponse>(`/providers/${encodeURIComponent(name)}/config`),

	/**
	 * Save configuration for a provider
	 */
	saveConfig: (name: string, request: ProviderConfigRequest) =>
		apiPut<ProviderConfigResponse>(`/providers/${encodeURIComponent(name)}/config`, request),

	/**
	 * Test configuration without saving
	 */
	testConfig: (name: string, request: ConfigTestRequest) =>
		apiPost<ConfigTestResponse>(`/providers/${encodeURIComponent(name)}/config/test`, request),

	/**
	 * Reset configuration to environment-only settings
	 */
	resetConfig: (name: string) => apiDelete(`/providers/${encodeURIComponent(name)}/config`)
};

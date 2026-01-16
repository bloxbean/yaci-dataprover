import { apiGet, apiPost, apiDelete } from './client';
import type {
	MerkleResponse,
	CreateMerkleRequest,
	PageResponse,
	AddEntriesRequest,
	AddEntriesResponse,
	RootHashResponse,
	ValueLookupResponse,
	BatchValueLookupRequest,
	BatchValueLookupResponse,
	MerkleStatus
} from './types';

export const merkleApi = {
	/**
	 * List all merkles with pagination
	 */
	list: (page = 0, size = 20, status?: MerkleStatus) => {
		let url = `/merkle?page=${page}&size=${size}`;
		if (status) {
			url += `&status=${status}`;
		}
		return apiGet<PageResponse<MerkleResponse>>(url);
	},

	/**
	 * Get a single merkle by identifier
	 */
	get: (id: string) => apiGet<MerkleResponse>(`/merkle/${encodeURIComponent(id)}`),

	/**
	 * Create a new merkle
	 */
	create: (request: CreateMerkleRequest) => apiPost<MerkleResponse>('/merkle', request),

	/**
	 * Delete a merkle
	 */
	delete: (id: string) => apiDelete(`/merkle/${encodeURIComponent(id)}`),

	/**
	 * Add entries to a merkle
	 */
	addEntries: (id: string, request: AddEntriesRequest) =>
		apiPost<AddEntriesResponse>(`/merkle/${encodeURIComponent(id)}/entries`, request),

	/**
	 * Get the root hash of a merkle
	 */
	getRoot: (id: string) => apiGet<RootHashResponse>(`/merkle/${encodeURIComponent(id)}/root`),

	/**
	 * Get a single value by key
	 */
	getValue: (id: string, key: string) =>
		apiGet<ValueLookupResponse>(
			`/merkle/${encodeURIComponent(id)}/values?key=${encodeURIComponent(key)}`
		),

	/**
	 * Get multiple values by keys
	 */
	getValuesBatch: (id: string, request: BatchValueLookupRequest) =>
		apiPost<BatchValueLookupResponse>(`/merkle/${encodeURIComponent(id)}/values/batch`, request)
};

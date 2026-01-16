import { apiGet, apiDelete } from './client';
import type { HealthResponse, SystemStats, CacheInfo, StorageInfo } from './types';

export const adminApi = {
	/**
	 * Get health status
	 */
	health: () => apiGet<HealthResponse>('/admin/health'),

	/**
	 * Get system statistics
	 */
	stats: () => apiGet<SystemStats>('/admin/stats'),

	/**
	 * Get cache information
	 */
	cache: () => apiGet<CacheInfo>('/admin/cache'),

	/**
	 * Get storage information
	 */
	storage: () => apiGet<StorageInfo>('/admin/storage'),

	/**
	 * Evict a specific merkle from cache
	 */
	evictCache: (merkleId: string) =>
		apiDelete<{ message: string; merkleId: string }>(
			`/admin/cache/evict/${encodeURIComponent(merkleId)}`
		),

	/**
	 * Clear all cache
	 */
	clearCache: () =>
		apiDelete<{ message: string; evictedMerkle: number }>('/admin/cache/clear')
};

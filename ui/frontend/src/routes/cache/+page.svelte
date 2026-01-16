<script lang="ts">
	import { onMount } from 'svelte';
	import { base } from '$app/paths';
	import { Header, Card, Button, Badge, Alert, Table } from '$lib/components';
	import { adminApi, type CacheInfo, type StorageInfo, ApiError } from '$lib/api';

	let cacheInfo: CacheInfo | null = $state(null);
	let storageInfo: StorageInfo | null = $state(null);
	let loading = $state(true);
	let error: string | null = $state(null);
	let success: string | null = $state(null);

	let evictLoading: Record<string, boolean> = $state({});
	let clearLoading = $state(false);

	onMount(async () => {
		await loadData();
	});

	async function loadData() {
		loading = true;
		error = null;
		try {
			const [cache, storage] = await Promise.all([
				adminApi.cache(),
				adminApi.storage()
			]);
			cacheInfo = cache;
			storageInfo = storage;
		} catch (e) {
			if (e instanceof ApiError) {
				error = e.message;
			} else {
				error = 'Failed to load cache information';
			}
		} finally {
			loading = false;
		}
	}

	async function handleEvict(merkleId: string) {
		evictLoading[merkleId] = true;
		error = null;
		success = null;
		try {
			await adminApi.evictCache(merkleId);
			success = `Evicted ${merkleId} from cache`;
			await loadData();
		} catch (e) {
			if (e instanceof ApiError) {
				error = e.message;
			} else {
				error = 'Failed to evict from cache';
			}
		} finally {
			evictLoading[merkleId] = false;
		}
	}

	async function handleClearAll() {
		if (!confirm('Are you sure you want to clear the entire cache?')) return;

		clearLoading = true;
		error = null;
		success = null;
		try {
			const result = await adminApi.clearCache();
			success = `Cleared ${result.evictedMerkle} merkle(s) from cache`;
			await loadData();
		} catch (e) {
			if (e instanceof ApiError) {
				error = e.message;
			} else {
				error = 'Failed to clear cache';
			}
		} finally {
			clearLoading = false;
		}
	}

	function formatPercent(rate: string): string {
		return rate;
	}
</script>

<Header title="Cache Management" description="Monitor and manage the merkle cache" />

{#if error}
	<Alert variant="error" title="Error" dismissible ondismiss={() => error = null}>
		{error}
	</Alert>
{/if}

{#if success}
	<Alert variant="success" dismissible ondismiss={() => success = null}>
		{success}
	</Alert>
{/if}

{#if loading}
	<div class="flex items-center justify-center h-64">
		<div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-500"></div>
	</div>
{:else if cacheInfo && storageInfo}
	<!-- Stats Grid -->
	<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
		<Card>
			<div class="text-center">
				<p class="text-3xl font-bold text-white">{cacheInfo.currentSize}</p>
				<p class="text-sm text-gray-400 mt-1">Cached Merkles</p>
				<p class="text-xs text-gray-500">Max: {cacheInfo.maxSize}</p>
			</div>
		</Card>

		<Card>
			<div class="text-center">
				<p class="text-3xl font-bold text-green-400">{formatPercent(cacheInfo.hitRate)}</p>
				<p class="text-sm text-gray-400 mt-1">Hit Rate</p>
			</div>
		</Card>

		<Card>
			<div class="text-center">
				<p class="text-3xl font-bold text-blue-400">{cacheInfo.cacheHits.toLocaleString()}</p>
				<p class="text-sm text-gray-400 mt-1">Cache Hits</p>
			</div>
		</Card>

		<Card>
			<div class="text-center">
				<p class="text-3xl font-bold text-yellow-400">{cacheInfo.cacheMisses.toLocaleString()}</p>
				<p class="text-sm text-gray-400 mt-1">Cache Misses</p>
			</div>
		</Card>
	</div>

	<div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
		<!-- Cached Merkles -->
		<Card title="Cached Merkle Trees" padding={false}>
			<div class="p-4 border-b border-gray-700 flex justify-between items-center">
				<p class="text-sm text-gray-400">
					{cacheInfo.cachedMerkle.length} merkle(s) in cache
				</p>
				<Button
					variant="danger"
					size="sm"
					onclick={handleClearAll}
					loading={clearLoading}
					disabled={cacheInfo.cachedMerkle.length === 0}
				>
					Clear All
				</Button>
			</div>

			{#if cacheInfo.cachedMerkle.length === 0}
				<div class="p-8 text-center text-gray-500">
					No merkle trees currently cached
				</div>
			{:else}
				<div class="divide-y divide-gray-700 max-h-96 overflow-auto">
					{#each cacheInfo.cachedMerkle as merkleId}
						<div class="flex items-center justify-between px-4 py-3 hover:bg-gray-700/30">
							<div class="flex items-center gap-3">
								<svg class="w-4 h-4 text-primary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
									<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4" />
								</svg>
								<a href="{base}/merkle/{merkleId}" class="text-primary-400 hover:text-primary-300">
									{merkleId}
								</a>
							</div>
							<Button
								variant="ghost"
								size="sm"
								onclick={() => handleEvict(merkleId)}
								loading={evictLoading[merkleId]}
							>
								Evict
							</Button>
						</div>
					{/each}
				</div>
			{/if}
		</Card>

		<!-- Storage Info -->
		<Card title="Storage Information">
			<div class="space-y-4">
				<div class="flex items-center justify-between">
					<span class="text-gray-400">RocksDB Status</span>
					<Badge
						variant={storageInfo.isOpen ? 'success' : 'error'}
						text={storageInfo.isOpen ? 'Running' : 'Stopped'}
					/>
				</div>

				<div>
					<div class="flex items-center justify-between mb-2">
						<span class="text-gray-400">Column Families</span>
						<span class="text-white font-medium">{storageInfo.columnFamilyCount}</span>
					</div>
					<div class="max-h-48 overflow-auto bg-gray-700/50 rounded-lg p-3">
						{#each storageInfo.columnFamilies as cf}
							<div class="text-sm text-gray-300 py-1">
								<code>{cf}</code>
							</div>
						{/each}
					</div>
				</div>
			</div>
		</Card>
	</div>

	<!-- Cache Usage Visual -->
	<Card title="Cache Usage">
		<div class="space-y-4">
			<div>
				<div class="flex justify-between text-sm mb-2">
					<span class="text-gray-400">Capacity</span>
					<span class="text-white">{cacheInfo.currentSize} / {cacheInfo.maxSize}</span>
				</div>
				<div class="w-full bg-gray-700 rounded-full h-4">
					<div
						class="bg-primary-500 h-4 rounded-full transition-all duration-300"
						style="width: {(cacheInfo.currentSize / cacheInfo.maxSize) * 100}%"
					></div>
				</div>
			</div>

			<div class="grid grid-cols-2 gap-4 pt-4 border-t border-gray-700">
				<div>
					<p class="text-sm text-gray-400">Total Requests</p>
					<p class="text-xl font-bold text-white">
						{(cacheInfo.cacheHits + cacheInfo.cacheMisses).toLocaleString()}
					</p>
				</div>
				<div>
					<p class="text-sm text-gray-400">Efficiency</p>
					<div class="flex items-center gap-2">
						<div class="flex-1 bg-gray-700 rounded-full h-2">
							<div
								class="bg-green-500 h-2 rounded-full"
								style="width: {(cacheInfo.cacheHits / (cacheInfo.cacheHits + cacheInfo.cacheMisses) || 0) * 100}%"
							></div>
						</div>
						<span class="text-sm text-gray-400">{formatPercent(cacheInfo.hitRate)}</span>
					</div>
				</div>
			</div>
		</div>
	</Card>
{/if}

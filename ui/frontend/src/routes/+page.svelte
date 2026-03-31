<script lang="ts">
	import { onMount } from 'svelte';
	import { base } from '$app/paths';
	import { Header, Card, Badge, Alert } from '$lib/components';
	import { adminApi, merkleApi, type SystemStats, type MerkleResponse, ApiError } from '$lib/api';

	let stats: SystemStats | null = $state(null);
	let recentMerkles: MerkleResponse[] = $state([]);
	let loading = $state(true);
	let error: string | null = $state(null);

	onMount(async () => {
		await loadData();
	});

	async function loadData() {
		loading = true;
		error = null;
		try {
			const [statsData, merklesData] = await Promise.all([
				adminApi.stats(),
				merkleApi.list(0, 5)
			]);
			stats = statsData;
			recentMerkles = merklesData.content;
		} catch (e) {
			if (e instanceof ApiError) {
				error = e.message;
			} else {
				error = 'Failed to load dashboard data';
			}
		} finally {
			loading = false;
		}
	}

	function formatNumber(n: number): string {
		return n.toLocaleString();
	}

	function formatPercent(rate: string): string {
		return rate;
	}
</script>

<Header title="Dashboard" description="Overview of your DataProver instance" />

{#if error}
	<Alert variant="error" title="Error" dismissible ondismiss={() => error = null}>
		{error}
	</Alert>
{/if}

{#if loading}
	<div class="flex items-center justify-center h-64">
		<div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-500"></div>
	</div>
{:else if stats}
	<!-- Stats Cards -->
	<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
		<Card>
			<div class="flex items-center gap-4">
				<div class="p-3 bg-primary-900/50 rounded-lg">
					<svg class="w-6 h-6 text-primary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4" />
					</svg>
				</div>
				<div>
					<p class="text-sm text-gray-400">Cached Merkles</p>
					<p class="text-2xl font-bold text-white">{stats.cache.currentSize} / {stats.cache.maxSize}</p>
				</div>
			</div>
		</Card>

		<Card>
			<div class="flex items-center gap-4">
				<div class="p-3 bg-green-900/50 rounded-lg">
					<svg class="w-6 h-6 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
					</svg>
				</div>
				<div>
					<p class="text-sm text-gray-400">Cache Hit Rate</p>
					<p class="text-2xl font-bold text-white">{formatPercent(stats.cache.hitRate)}</p>
				</div>
			</div>
		</Card>

		<Card>
			<div class="flex items-center gap-4">
				<div class="p-3 bg-blue-900/50 rounded-lg">
					<svg class="w-6 h-6 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
					</svg>
				</div>
				<div>
					<p class="text-sm text-gray-400">Cache Hits</p>
					<p class="text-2xl font-bold text-white">{formatNumber(stats.cache.cacheHits)}</p>
				</div>
			</div>
		</Card>

		<Card>
			<div class="flex items-center gap-4">
				<div class="p-3 bg-yellow-900/50 rounded-lg">
					<svg class="w-6 h-6 text-yellow-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
					</svg>
				</div>
				<div>
					<p class="text-sm text-gray-400">Cache Misses</p>
					<p class="text-2xl font-bold text-white">{formatNumber(stats.cache.cacheMisses)}</p>
				</div>
			</div>
		</Card>
	</div>

	<!-- Status Cards -->
	<div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
		<Card title="System Status">
			<div class="space-y-4">
				<div class="flex items-center justify-between">
					<span class="text-gray-400">RocksDB</span>
					<Badge
						variant={stats.rocksdb.isOpen ? 'success' : 'error'}
						text={stats.rocksdb.isOpen ? 'Running' : 'Stopped'}
					/>
				</div>
				<div class="flex items-center justify-between">
					<span class="text-gray-400">Column Families</span>
					<span class="text-white">{stats.rocksdb.columnFamilyCount}</span>
				</div>
			</div>
		</Card>

		<Card title="Quick Actions">
			<div class="grid grid-cols-2 gap-3">
				<a
					href="{base}/merkle"
					class="flex items-center gap-3 p-3 rounded-lg bg-gray-700/50 hover:bg-gray-700 transition-colors"
				>
					<svg class="w-5 h-5 text-primary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
					</svg>
					<span class="text-gray-300">New Merkle</span>
				</a>
				<a
					href="{base}/proofs"
					class="flex items-center gap-3 p-3 rounded-lg bg-gray-700/50 hover:bg-gray-700 transition-colors"
				>
					<svg class="w-5 h-5 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
					</svg>
					<span class="text-gray-300">Generate Proof</span>
				</a>
				<a
					href="{base}/cache"
					class="flex items-center gap-3 p-3 rounded-lg bg-gray-700/50 hover:bg-gray-700 transition-colors"
				>
					<svg class="w-5 h-5 text-yellow-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4" />
					</svg>
					<span class="text-gray-300">Manage Cache</span>
				</a>
				<button
					onclick={loadData}
					class="flex items-center gap-3 p-3 rounded-lg bg-gray-700/50 hover:bg-gray-700 transition-colors"
				>
					<svg class="w-5 h-5 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
					</svg>
					<span class="text-gray-300">Refresh</span>
				</button>
			</div>
		</Card>
	</div>

	<!-- Recent Merkles -->
	<Card title="Recent Merkle Trees">
		{#if recentMerkles.length === 0}
			<p class="text-gray-500 text-center py-8">No merkle trees found. Create one to get started.</p>
		{:else}
			<div class="overflow-x-auto">
				<table class="w-full text-left">
					<thead class="bg-gray-700/50 text-gray-300 text-sm">
						<tr>
							<th class="px-4 py-3 font-medium">Identifier</th>
							<th class="px-4 py-3 font-medium">Scheme</th>
							<th class="px-4 py-3 font-medium">Status</th>
							<th class="px-4 py-3 font-medium">Created</th>
						</tr>
					</thead>
					<tbody class="divide-y divide-gray-700">
						{#each recentMerkles as merkle}
							<tr class="hover:bg-gray-700/30">
								<td class="px-4 py-3">
									<a href="{base}/merkle/{merkle.identifier}" class="text-primary-400 hover:text-primary-300">
										{merkle.identifier}
									</a>
								</td>
								<td class="px-4 py-3 text-gray-400">{merkle.scheme}</td>
								<td class="px-4 py-3">
									<Badge
										variant={merkle.status === 'ACTIVE' ? 'success' : merkle.status === 'DELETED' ? 'error' : 'warning'}
										text={merkle.status}
										size="sm"
									/>
								</td>
								<td class="px-4 py-3 text-gray-400 text-sm">
									{new Date(merkle.createdAt).toLocaleDateString()}
								</td>
							</tr>
						{/each}
					</tbody>
				</table>
			</div>
			<div class="mt-4 text-center">
				<a href="{base}/merkle" class="text-primary-400 hover:text-primary-300 text-sm">
					View all merkle trees →
				</a>
			</div>
		{/if}
	</Card>
{/if}

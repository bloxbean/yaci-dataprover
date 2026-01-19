<script lang="ts">
	import { onMount } from 'svelte';
	import { base } from '$app/paths';
	import type { ProviderInfo } from '$lib/api/types';
	import { providerApi } from '$lib/api';
	import { Header, Alert, Input, ProviderCard } from '$lib/components';

	let providers = $state<ProviderInfo[]>([]);
	let filteredProviders = $state<ProviderInfo[]>([]);
	let searchQuery = $state('');
	let isLoading = $state(true);
	let error = $state<string | null>(null);

	// Filter providers based on search
	$effect(() => {
		if (!searchQuery.trim()) {
			filteredProviders = providers;
		} else {
			const query = searchQuery.toLowerCase();
			filteredProviders = providers.filter(
				(p) =>
					p.name.toLowerCase().includes(query) ||
					p.description.toLowerCase().includes(query) ||
					p.dataType.toLowerCase().includes(query)
			);
		}
	});

	onMount(async () => {
		await loadProviders();
	});

	async function loadProviders() {
		isLoading = true;
		error = null;

		try {
			const response = await providerApi.list();
			providers = response.providers;
		} catch (e) {
			error = e instanceof Error ? e.message : 'Failed to load providers';
		} finally {
			isLoading = false;
		}
	}
</script>

<Header title="Data Providers" description="Discover and manage data ingestion sources" />

<div class="space-y-6">
	{#if error}
		<Alert variant="error" dismissible onclose={() => (error = null)}>
			{error}
		</Alert>
	{/if}

	<!-- Search -->
	<div class="max-w-md">
		<Input placeholder="Search providers..." bind:value={searchQuery} />
	</div>

	<!-- Loading state -->
	{#if isLoading}
		<div class="flex justify-center py-12">
			<div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500"></div>
		</div>
	{:else if filteredProviders.length === 0}
		<!-- Empty state -->
		<div class="text-center py-12">
			<svg
				class="mx-auto h-12 w-12 text-gray-500"
				fill="none"
				viewBox="0 0 24 24"
				stroke="currentColor"
			>
				<path
					stroke-linecap="round"
					stroke-linejoin="round"
					stroke-width="2"
					d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4"
				/>
			</svg>
			{#if providers.length === 0}
				<h3 class="mt-4 text-lg font-medium text-white">No data providers found</h3>
				<p class="mt-1 text-sm text-gray-400">
					Data providers are loaded from plugin JARs in the plugins directory.
				</p>
			{:else}
				<h3 class="mt-4 text-lg font-medium text-white">No matching providers</h3>
				<p class="mt-1 text-sm text-gray-400">Try a different search term.</p>
			{/if}
		</div>
	{:else}
		<!-- Provider grid -->
		<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
			{#each filteredProviders as provider (provider.name)}
				<a
					href="{base}/providers/{provider.name}"
					class="block rounded-lg transition-all hover:ring-2 hover:ring-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
				>
					<ProviderCard {provider} />
				</a>
			{/each}
		</div>
	{/if}
</div>

<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/stores';
	import { base } from '$app/paths';
	import type { ProviderInfo } from '$lib/api/types';
	import { providerApi, ApiError } from '$lib/api';
	import {
		Header,
		Card,
		Alert,
		ProviderStatusBadge,
		ProviderKeySerializer,
		ProviderIngestionForm,
		ProviderConfigForm
	} from '$lib/components';

	const providerName = $derived($page.params.name);

	let provider = $state<ProviderInfo | null>(null);
	let loading = $state(true);
	let error = $state<string | null>(null);

	onMount(async () => {
		await loadProvider();
	});

	async function loadProvider() {
		loading = true;
		error = null;
		try {
			provider = await providerApi.get(providerName);
		} catch (e) {
			if (e instanceof ApiError) {
				if (e.status === 404) {
					error = `Provider "${providerName}" not found`;
				} else {
					error = e.message;
				}
			} else {
				error = 'Failed to load provider';
			}
		} finally {
			loading = false;
		}
	}
</script>

<div class="flex items-center gap-4 mb-6">
	<a
		href="{base}/providers"
		class="text-gray-400 hover:text-white transition-colors"
		aria-label="Back to providers"
	>
		<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
			<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
		</svg>
	</a>
	{#if provider}
		<div class="flex items-center gap-4 flex-1">
			<Header title={provider.name} description={provider.description} />
			<ProviderStatusBadge status={provider.status} />
		</div>
	{:else}
		<Header title={providerName} description="Loading provider details..." />
	{/if}
</div>

{#if error}
	<Alert variant="error" title="Error">
		{error}
		<div class="mt-4">
			<a
				href="{base}/providers"
				class="inline-flex items-center text-sm text-primary-400 hover:text-primary-300"
			>
				<svg class="w-4 h-4 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path
						stroke-linecap="round"
						stroke-linejoin="round"
						stroke-width="2"
						d="M15 19l-7-7 7-7"
					/>
				</svg>
				Back to Providers
			</a>
		</div>
	</Alert>
{/if}

{#if loading}
	<div class="flex items-center justify-center h-64">
		<div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-500"></div>
	</div>
{:else if provider}
	<div class="space-y-6">
		<!-- Provider Info Card -->
		<Card title="Provider Information" padding={true}>
			<div class="grid grid-cols-1 md:grid-cols-3 gap-6">
				<div>
					<span class="text-sm text-gray-400">Name</span>
					<p class="text-white font-medium">{provider.name}</p>
				</div>
				<div>
					<span class="text-sm text-gray-400">Data Type</span>
					<p class="text-white">{provider.dataType}</p>
				</div>
				<div>
					<span class="text-sm text-gray-400">Status</span>
					<div class="mt-1">
						<ProviderStatusBadge status={provider.status} />
					</div>
				</div>
			</div>
			{#if provider.description}
				<div class="mt-4 pt-4 border-t border-gray-700">
					<span class="text-sm text-gray-400">Description</span>
					<p class="text-white mt-1">{provider.description}</p>
				</div>
			{/if}
			{#if provider.statusMessage}
				<div class="mt-4 pt-4 border-t border-gray-700">
					<Alert variant="warning" dismissible={false}>
						{provider.statusMessage}
					</Alert>
				</div>
			{/if}
		</Card>

		<!-- Connection Configuration Card -->
		{#if provider.connectionConfigSchema?.fields?.length}
			<Card title="Connection Configuration" padding={true} collapsible={true} defaultExpanded={false}>
				<ProviderConfigForm {provider} onConfigured={loadProvider} />
			</Card>
		{/if}

		<!-- Key Serialization Card -->
		{#if provider.keySerializationSchema}
			<Card title="Key Serialization" padding={true}>
				<ProviderKeySerializer {provider} />
			</Card>
		{/if}

		<!-- Data Ingestion Card -->
		<Card title="Data Ingestion" padding={true}>
			{#if provider.status === 'AVAILABLE'}
				<ProviderIngestionForm {provider} />
			{:else}
				<Alert variant="warning" dismissible={false}>
					This provider is currently not available for ingestion.
					{#if provider.status === 'NOT_CONFIGURED'}
						Please configure the provider before running ingestion.
					{:else if provider.status === 'ERROR'}
						The provider encountered an error. Please check the logs.
					{/if}
				</Alert>
			{/if}
		</Card>
	</div>
{/if}

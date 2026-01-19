<script lang="ts">
	import { base } from '$app/paths';
	import { goto } from '$app/navigation';
	import type { ProviderInfo, ProviderIngestResponse } from '$lib/api/types';
	import { providerApi } from '$lib/api';
	import Modal from './Modal.svelte';
	import Button from './Button.svelte';
	import Input from './Input.svelte';
	import Alert from './Alert.svelte';
	import DynamicForm from './DynamicForm.svelte';

	interface Props {
		provider: ProviderInfo | null;
		onclose: () => void;
	}

	let { provider, onclose }: Props = $props();

	// Form state
	let merkleName = $state('');
	let createIfNotExists = $state(true);
	let config = $state<Record<string, unknown>>({});
	let errors = $state<Record<string, string>>({});

	// UI state
	let isLoading = $state(false);
	let result = $state<ProviderIngestResponse | null>(null);
	let error = $state<string | null>(null);

	// Reset state when provider changes
	$effect(() => {
		if (provider) {
			merkleName = '';
			createIfNotExists = true;
			config = {};
			errors = {};
			result = null;
			error = null;
		}
	});

	function validateForm(): boolean {
		const newErrors: Record<string, string> = {};

		if (!merkleName.trim()) {
			newErrors.merkleName = 'Merkle name is required';
		} else if (!/^[a-z0-9]([a-z0-9-]{1,62}[a-z0-9])?$/.test(merkleName)) {
			newErrors.merkleName = 'Must be 3-64 lowercase alphanumeric characters or hyphens';
		}

		// Validate required config fields
		if (provider?.configSchema?.fields) {
			for (const field of provider.configSchema.fields) {
				if (field.required && (config[field.name] === undefined || config[field.name] === '')) {
					newErrors[field.name] = `${field.label} is required`;
				}
			}
		}

		errors = newErrors;
		return Object.keys(newErrors).length === 0;
	}

	async function handleSubmit() {
		if (!provider || !validateForm()) return;

		isLoading = true;
		error = null;

		try {
			result = await providerApi.ingest({
				merkleName,
				createIfNotExists,
				merkleScheme: 'mpf',
				provider: provider.name,
				config
			});
		} catch (e) {
			error = e instanceof Error ? e.message : 'Failed to start ingestion';
		} finally {
			isLoading = false;
		}
	}

	function handleClose() {
		result = null;
		error = null;
		onclose();
	}

	function handleViewMerkle() {
		if (result) {
			goto(`${base}/merkle/${result.merkleIdentifier}`);
		}
	}

	function formatDuration(ms: number): string {
		if (ms < 1000) return `${ms}ms`;
		return `${(ms / 1000).toFixed(1)}s`;
	}

	function formatNumber(num: number): string {
		return num.toLocaleString();
	}
</script>

<Modal open={provider !== null} title="Ingest Data - {provider?.name ?? ''}" onclose={handleClose}>
	{#if result}
		<!-- Success state -->
		<div class="space-y-4">
			<Alert variant="success" dismissible={false}>
				Ingestion completed successfully!
			</Alert>

			<div class="bg-gray-800 rounded-lg p-4 space-y-3">
				<div class="flex justify-between">
					<span class="text-gray-400">Merkle</span>
					<span class="text-white font-mono">{result.merkleIdentifier}</span>
				</div>
				{#if result.merkleCreated}
					<div class="flex justify-between">
						<span class="text-gray-400">Status</span>
						<span class="text-green-400">New merkle created</span>
					</div>
				{/if}
				<div class="flex justify-between">
					<span class="text-gray-400">Records Processed</span>
					<span class="text-white">{formatNumber(result.recordsProcessed)}</span>
				</div>
				{#if result.recordsSkipped > 0}
					<div class="flex justify-between">
						<span class="text-gray-400">Records Skipped</span>
						<span class="text-yellow-400">{formatNumber(result.recordsSkipped)}</span>
					</div>
				{/if}
				<div class="flex justify-between">
					<span class="text-gray-400">Duration</span>
					<span class="text-white">{formatDuration(result.durationMs)}</span>
				</div>
				<div class="flex justify-between items-start">
					<span class="text-gray-400">Root Hash</span>
					<span class="text-white font-mono text-sm break-all max-w-[300px] text-right">
						{result.rootHash}
					</span>
				</div>
			</div>

			{#if result.errors && result.errors.length > 0}
				<Alert variant="warning" dismissible={false}>
					<p class="font-semibold mb-1">Some records had issues:</p>
					<ul class="text-sm list-disc list-inside">
						{#each result.errors.slice(0, 5) as err}
							<li>{err}</li>
						{/each}
						{#if result.errors.length > 5}
							<li>... and {result.errors.length - 5} more</li>
						{/if}
					</ul>
				</Alert>
			{/if}

			<div class="flex gap-3 justify-end pt-2">
				<Button variant="secondary" onclick={handleClose}>Close</Button>
				<Button variant="primary" onclick={handleViewMerkle}>View Merkle</Button>
			</div>
		</div>
	{:else}
		<!-- Form state -->
		<div class="space-y-6">
			{#if error}
				<Alert variant="error" dismissible onclose={() => error = null}>
					{error}
				</Alert>
			{/if}

			<!-- Merkle Settings -->
			<div class="space-y-4">
				<h3 class="text-sm font-medium text-gray-300 uppercase tracking-wider">Merkle Settings</h3>

				<Input
					label="Merkle Name"
					placeholder="e.g., epoch-stake-425"
					bind:value={merkleName}
					error={errors.merkleName}
					required
					disabled={isLoading}
				/>

				<label class="flex items-center gap-2">
					<input
						type="checkbox"
						bind:checked={createIfNotExists}
						disabled={isLoading}
						class="h-4 w-4 rounded border-gray-600 bg-gray-700 text-primary-600 focus:ring-primary-500"
					/>
					<span class="text-sm text-gray-300">Create if doesn't exist</span>
				</label>
			</div>

			<!-- Provider Configuration -->
			{#if provider?.configSchema?.fields && provider.configSchema.fields.length > 0}
				<div class="space-y-4">
					<h3 class="text-sm font-medium text-gray-300 uppercase tracking-wider">Provider Configuration</h3>

					<DynamicForm
						schema={provider.configSchema}
						bind:values={config}
						errors={errors}
						disabled={isLoading}
					/>
				</div>
			{/if}

			<div class="flex gap-3 justify-end pt-2">
				<Button variant="secondary" onclick={handleClose} disabled={isLoading}>
					Cancel
				</Button>
				<Button variant="primary" onclick={handleSubmit} loading={isLoading}>
					{isLoading ? 'Ingesting...' : 'Start Ingestion'}
				</Button>
			</div>
		</div>
	{/if}
</Modal>

<script lang="ts">
	import type { ProviderInfo, SerializeKeyResponse } from '$lib/api/types';
	import { providerApi } from '$lib/api';
	import Button from './Button.svelte';
	import Select from './Select.svelte';
	import Input from './Input.svelte';
	import Alert from './Alert.svelte';

	interface Props {
		providers: ProviderInfo[];
		onUseKey?: (hexKey: string) => void;
	}

	let { providers, onUseKey }: Props = $props();

	let expanded = $state(false);
	let selectedProvider = $state('');
	let keyInput = $state('');
	let isLoading = $state(false);
	let result = $state<SerializeKeyResponse | null>(null);
	let error = $state<string | null>(null);

	// Get the selected provider's schema
	const currentProvider = $derived(providers.find((p) => p.name === selectedProvider));
	const keySchema = $derived(currentProvider?.keySerializationSchema);

	// Filter to only providers with key serialization support
	const availableProviders = $derived(
		providers
			.filter((p) => p.keySerializationSchema && p.status === 'AVAILABLE')
			.map((p) => ({ value: p.name, label: p.name }))
	);

	function toggle() {
		expanded = !expanded;
	}

	async function handleSerialize() {
		if (!selectedProvider || !keyInput.trim()) return;

		isLoading = true;
		error = null;
		result = null;

		try {
			result = await providerApi.serializeKey(selectedProvider, { key: keyInput.trim() });
		} catch (e) {
			error = e instanceof Error ? e.message : 'Failed to serialize key';
		} finally {
			isLoading = false;
		}
	}

	async function copyToClipboard() {
		if (result?.serializedKeyHex) {
			await navigator.clipboard.writeText(result.serializedKeyHex);
		}
	}

	function handleUseKey() {
		if (result?.serializedKeyHex && onUseKey) {
			onUseKey(result.serializedKeyHex);
		}
	}
</script>

<div class="card overflow-hidden">
	<button
		type="button"
		onclick={toggle}
		class="w-full px-4 py-3 flex items-center justify-between text-left bg-gray-800/50 hover:bg-gray-800 transition-colors"
	>
		<span class="text-sm font-medium text-gray-300 uppercase tracking-wider">
			Key Serialization Utility
		</span>
		<svg
			class="w-5 h-5 text-gray-400 transition-transform {expanded ? 'rotate-180' : ''}"
			fill="none"
			viewBox="0 0 24 24"
			stroke="currentColor"
		>
			<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
		</svg>
	</button>

	{#if expanded}
		<div class="p-4 border-t border-gray-700 space-y-4">
			{#if availableProviders.length === 0}
				<p class="text-sm text-gray-400">No providers with key serialization support are available.</p>
			{:else}
				{#if error}
					<Alert variant="error" dismissible onclose={() => (error = null)}>
						{error}
					</Alert>
				{/if}

				<div class="grid grid-cols-1 md:grid-cols-2 gap-4">
					<Select
						label="Provider"
						bind:value={selectedProvider}
						options={availableProviders}
						placeholder="Select a provider"
					/>

					<Input
						label={keySchema?.keyFieldLabel ?? 'Key'}
						placeholder={keySchema?.keyFieldPlaceholder ?? 'Enter key value'}
						bind:value={keyInput}
					/>
				</div>

				{#if keySchema?.keyDescription}
					<p class="text-sm text-gray-400">{keySchema.keyDescription}</p>
				{/if}

				<div class="flex gap-2">
					<Button
						variant="primary"
						onclick={handleSerialize}
						loading={isLoading}
						disabled={!selectedProvider || !keyInput.trim()}
					>
						Convert to Hex
					</Button>
				</div>

				{#if result}
					<div class="bg-gray-800 rounded-lg p-4 space-y-3">
						<div>
							<span class="text-sm text-gray-400">Serialized Key (hex):</span>
							<div class="mt-1 font-mono text-sm text-white break-all bg-gray-900 rounded p-2">
								{result.serializedKeyHex}
							</div>
						</div>
						<div class="text-sm text-gray-400">
							Length: {result.keyLength} bytes
						</div>
						<div class="flex gap-2">
							<Button variant="secondary" size="sm" onclick={copyToClipboard}>
								Copy
							</Button>
							{#if onUseKey}
								<Button variant="primary" size="sm" onclick={handleUseKey}>
									Use in Generate Proof
								</Button>
							{/if}
						</div>
					</div>
				{/if}
			{/if}
		</div>
	{/if}
</div>

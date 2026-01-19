<script lang="ts">
	import { base } from '$app/paths';
	import { goto } from '$app/navigation';
	import type { ProviderInfo, SerializeKeyResponse } from '$lib/api/types';
	import { providerApi } from '$lib/api';
	import Button from './Button.svelte';
	import Input from './Input.svelte';
	import Alert from './Alert.svelte';

	interface Props {
		provider: ProviderInfo;
	}

	let { provider }: Props = $props();

	let keyInput = $state('');
	let isLoading = $state(false);
	let result = $state<SerializeKeyResponse | null>(null);
	let error = $state<string | null>(null);
	let copied = $state(false);

	const keySchema = $derived(provider?.keySerializationSchema);
	const hasKeySerializer = $derived(!!keySchema && provider?.status === 'AVAILABLE');

	async function handleSerialize() {
		if (!keyInput.trim()) return;

		isLoading = true;
		error = null;
		result = null;
		copied = false;

		try {
			result = await providerApi.serializeKey(provider.name, { key: keyInput.trim() });
		} catch (e) {
			error = e instanceof Error ? e.message : 'Failed to serialize key';
		} finally {
			isLoading = false;
		}
	}

	async function copyToClipboard() {
		if (result?.serializedKeyHex) {
			await navigator.clipboard.writeText(result.serializedKeyHex);
			copied = true;
			setTimeout(() => {
				copied = false;
			}, 2000);
		}
	}

	function handleGoToProofs() {
		goto(`${base}/proofs`);
	}

	function handleReset() {
		result = null;
		error = null;
		keyInput = '';
	}
</script>

{#if !hasKeySerializer}
	<p class="text-sm text-gray-400">
		This provider does not support key serialization.
	</p>
{:else}
	<div class="space-y-4">
		{#if keySchema?.keyDescription}
			<p class="text-sm text-gray-400">{keySchema.keyDescription}</p>
		{/if}

		{#if error}
			<Alert variant="error" dismissible onclose={() => (error = null)}>
				{error}
			</Alert>
		{/if}

		<Input
			label={keySchema?.keyFieldLabel ?? 'Key'}
			placeholder={keySchema?.keyFieldPlaceholder ?? 'Enter key value'}
			bind:value={keyInput}
			required
		/>

		<div class="flex gap-2">
			<Button
				variant="primary"
				onclick={handleSerialize}
				loading={isLoading}
				disabled={!keyInput.trim()}
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
				<div class="flex gap-2 flex-wrap">
					<Button variant="secondary" size="sm" onclick={copyToClipboard}>
						{copied ? 'Copied!' : 'Copy'}
					</Button>
					<Button variant="secondary" size="sm" onclick={handleGoToProofs}>
						Go to Proofs Page
					</Button>
					<Button variant="secondary" size="sm" onclick={handleReset}>
						Clear
					</Button>
				</div>
			</div>
		{/if}
	</div>
{/if}

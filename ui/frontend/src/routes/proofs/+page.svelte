<script lang="ts">
	import { onMount } from 'svelte';
	import { Header, Card, Button, Input, Textarea, Select, Alert, Badge } from '$lib/components';
	import { merkleApi, proofApi, type MerkleResponse, ApiError } from '$lib/api';

	let merkles: MerkleResponse[] = $state([]);
	let loading = $state(true);

	// Generate proof state
	let selectedMerkle = $state('');
	let proofKey = $state('');
	let proofFormat = $state('wire');
	let generateLoading = $state(false);
	let proofResult: {
		key: string;
		value: string | null;
		proof: string;
		rootHash: string;
		proofFormat: string;
	} | null = $state(null);
	let generateError: string | null = $state(null);

	// Verify proof state
	let verifyMerkle = $state('');
	let verifyKey = $state('');
	let verifyValue = $state('');
	let verifyProof = $state('');
	let verifyRootHash = $state('');
	let verifyLoading = $state(false);
	let verifyResult: { verified: boolean; key: string } | null = $state(null);
	let verifyError: string | null = $state(null);

	onMount(async () => {
		await loadMerkles();
	});

	async function loadMerkles() {
		loading = true;
		try {
			const result = await merkleApi.list(0, 100, 'ACTIVE');
			merkles = result.content;
		} catch (e) {
			console.error('Failed to load merkles', e);
		} finally {
			loading = false;
		}
	}

	async function handleGenerateProof() {
		if (!selectedMerkle || !proofKey.trim()) {
			generateError = 'Please select a merkle tree and enter a key';
			return;
		}

		generateLoading = true;
		generateError = null;
		proofResult = null;

		try {
			proofResult = await proofApi.generate(selectedMerkle, {
				key: proofKey.trim(),
				format: proofFormat
			});
		} catch (e) {
			if (e instanceof ApiError) {
				generateError = e.message;
			} else {
				generateError = 'Failed to generate proof';
			}
		} finally {
			generateLoading = false;
		}
	}

	async function handleVerifyProof() {
		if (!verifyMerkle || !verifyKey.trim() || !verifyProof.trim() || !verifyRootHash.trim()) {
			verifyError = 'Please fill in all required fields';
			return;
		}

		verifyLoading = true;
		verifyError = null;
		verifyResult = null;

		try {
			verifyResult = await proofApi.verify(verifyMerkle, {
				key: verifyKey.trim(),
				value: verifyValue.trim() || null,
				proof: verifyProof.trim(),
				rootHash: verifyRootHash.trim()
			});
		} catch (e) {
			if (e instanceof ApiError) {
				verifyError = e.message;
			} else {
				verifyError = 'Failed to verify proof';
			}
		} finally {
			verifyLoading = false;
		}
	}

	function copyToClipboard(text: string) {
		navigator.clipboard.writeText(text);
	}

	function useProofForVerification() {
		if (proofResult) {
			verifyMerkle = selectedMerkle;
			verifyKey = proofResult.key;
			verifyValue = proofResult.value || '';
			verifyProof = proofResult.proof;
			verifyRootHash = proofResult.rootHash;
		}
	}

	const merkleOptions = $derived(merkles.map(m => ({ value: m.identifier, label: m.identifier })));
</script>

<Header title="Proofs" description="Generate and verify merkle proofs" />

{#if loading}
	<div class="flex items-center justify-center h-64">
		<div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-500"></div>
	</div>
{:else}
	<div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
		<!-- Generate Proof -->
		<Card title="Generate Proof">
			{#if generateError}
				<Alert variant="error" dismissible ondismiss={() => generateError = null}>
					{generateError}
				</Alert>
			{/if}

			<div class="space-y-4 mt-4">
				<Select
					label="Merkle Tree"
					placeholder="Select a merkle tree"
					options={merkleOptions}
					bind:value={selectedMerkle}
					required
				/>

				<Input
					label="Key"
					placeholder="0x1234abcd..."
					bind:value={proofKey}
					required
				/>

				<div class="space-y-1">
					<label class="label">Proof Format</label>
					<select bind:value={proofFormat} class="input w-full px-3 py-2">
						<option value="wire">Wire (raw bytes)</option>
						<option value="aiken">Aiken (Plutus-compatible)</option>
					</select>
				</div>

				<Button onclick={handleGenerateProof} loading={generateLoading}>
					<svg class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
					</svg>
					Generate Proof
				</Button>
			</div>

			{#if proofResult}
				<div class="mt-6 p-4 bg-gray-700/50 rounded-lg space-y-3">
					<div class="flex items-center justify-between">
						<h4 class="font-medium text-white">Proof Generated</h4>
						<Badge variant={proofResult.value ? 'success' : 'warning'} text={proofResult.value ? 'Inclusion' : 'Exclusion'} size="sm" />
					</div>

					<div>
						<span class="text-sm text-gray-400">Key:</span>
						<code class="block mt-1 text-primary-300 text-sm break-all">{proofResult.key}</code>
					</div>

					<div>
						<span class="text-sm text-gray-400">Value:</span>
						{#if proofResult.value}
							<code class="block mt-1 text-green-300 text-sm break-all">{proofResult.value}</code>
						{:else}
							<span class="block mt-1 text-yellow-400 text-sm">Not found (exclusion proof)</span>
						{/if}
					</div>

					<div>
						<span class="text-sm text-gray-400">Root Hash:</span>
						<code class="block mt-1 text-primary-300 text-xs break-all">{proofResult.rootHash}</code>
					</div>

					<div>
						<span class="text-sm text-gray-400">Proof ({proofResult.proofFormat}):</span>
						<code class="block mt-1 text-gray-300 text-xs break-all max-h-24 overflow-auto bg-gray-800 p-2 rounded">{proofResult.proof}</code>
					</div>

					<div class="flex gap-2">
						<Button variant="secondary" size="sm" onclick={() => copyToClipboard(JSON.stringify(proofResult, null, 2))}>
							Copy JSON
						</Button>
						<Button variant="secondary" size="sm" onclick={useProofForVerification}>
							Use for Verification
						</Button>
					</div>
				</div>
			{/if}
		</Card>

		<!-- Verify Proof -->
		<Card title="Verify Proof">
			{#if verifyError}
				<Alert variant="error" dismissible ondismiss={() => verifyError = null}>
					{verifyError}
				</Alert>
			{/if}

			<div class="space-y-4 mt-4">
				<Select
					label="Merkle Tree"
					placeholder="Select a merkle tree"
					options={merkleOptions}
					bind:value={verifyMerkle}
					required
				/>

				<Input
					label="Key"
					placeholder="0x1234abcd..."
					bind:value={verifyKey}
					required
				/>

				<Input
					label="Value (optional for exclusion proofs)"
					placeholder="0xabcd1234..."
					bind:value={verifyValue}
				/>

				<Input
					label="Root Hash"
					placeholder="0x..."
					bind:value={verifyRootHash}
					required
				/>

				<Textarea
					label="Proof"
					placeholder="0x..."
					bind:value={verifyProof}
					rows={4}
					monospace
					required
				/>

				<Button onclick={handleVerifyProof} loading={verifyLoading}>
					<svg class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
					</svg>
					Verify Proof
				</Button>
			</div>

			{#if verifyResult}
				<div class="mt-6 p-4 rounded-lg {verifyResult.verified ? 'bg-green-900/30 border border-green-700' : 'bg-red-900/30 border border-red-700'}">
					<div class="flex items-center gap-3">
						{#if verifyResult.verified}
							<svg class="w-8 h-8 text-green-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
								<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
							</svg>
							<div>
								<h4 class="font-medium text-green-400">Proof Verified</h4>
								<p class="text-sm text-green-300/80">The proof is valid for key: {verifyResult.key}</p>
							</div>
						{:else}
							<svg class="w-8 h-8 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
								<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
							</svg>
							<div>
								<h4 class="font-medium text-red-400">Verification Failed</h4>
								<p class="text-sm text-red-300/80">The proof is invalid or does not match the provided data</p>
							</div>
						{/if}
					</div>
				</div>
			{/if}
		</Card>
	</div>
{/if}

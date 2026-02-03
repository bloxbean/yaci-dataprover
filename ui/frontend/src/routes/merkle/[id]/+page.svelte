<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { base } from '$app/paths';
	import { Header, Card, Button, Badge, Input, Alert, Modal, TreeVisualization } from '$lib/components';
	import { merkleApi, proofApi, type MerkleResponse, type RootHashResponse, type MerkleEntriesResponse, type TreeStructureResponse, type TreeNode, ApiError } from '$lib/api';

	const merkleId = $derived($page.params.id);

	let merkle: MerkleResponse | null = $state(null);
	let rootHash: RootHashResponse | null = $state(null);
	let loading = $state(true);
	let error: string | null = $state(null);

	// Value lookup
	let lookupKey = $state('');
	let lookupResult: { key: string; value: string | null; found: boolean } | null = $state(null);
	let lookupLoading = $state(false);

	// Add entries
	let showAddModal = $state(false);
	let addEntries = $state('');
	let addLoading = $state(false);
	let addError: string | null = $state(null);
	let addResult: { entriesAdded: number; entriesSkipped: number } | null = $state(null);

	// Random data
	let showRandomModal = $state(false);
	let randomCount = $state(10);
	let randomKeyLength = $state(8);
	let randomValueLength = $state(16);
	let randomLoading = $state(false);

	// Proof generation
	let showProofModal = $state(false);
	let proofKey = $state('');
	let proofLoading = $state(false);
	let proofResult: { key: string; value: string | null; proof: string; rootHash: string } | null = $state(null);

	// Compute size
	let computedSize: { size: number; computationTimeMs: number } | null = $state(null);
	let computeSizeLoading = $state(false);
	let computeSizeError: string | null = $state(null);

	// Entries
	let entriesData: MerkleEntriesResponse | null = $state(null);
	let entriesLoading = $state(false);
	let entriesError: string | null = $state(null);
	let entriesLimit = $state(100);

	// Tree structure
	let treeData: TreeStructureResponse | null = $state(null);
	let treeLoading = $state(false);
	let treeError: string | null = $state(null);
	let treeMaxNodes = $state(500);
	let treeCollapsed = $state(true);

	onMount(async () => {
		await loadMerkle();
	});

	async function loadMerkle() {
		loading = true;
		error = null;
		try {
			const [merkleData, rootData] = await Promise.all([
				merkleApi.get(merkleId),
				merkleApi.getRoot(merkleId)
			]);
			merkle = merkleData;
			rootHash = rootData;
		} catch (e) {
			if (e instanceof ApiError) {
				if (e.status === 404) {
					error = 'Merkle tree not found';
				} else {
					error = e.message;
				}
			} else {
				error = 'Failed to load merkle tree';
			}
		} finally {
			loading = false;
		}
	}

	async function handleLookup() {
		if (!lookupKey.trim()) return;

		lookupLoading = true;
		lookupResult = null;
		try {
			lookupResult = await merkleApi.getValue(merkleId, lookupKey.trim());
		} catch (e) {
			if (e instanceof ApiError) {
				error = e.message;
			}
		} finally {
			lookupLoading = false;
		}
	}

	async function handleAddEntries() {
		const lines = addEntries.trim().split('\n').filter(l => l.trim());
		if (lines.length === 0) {
			addError = 'Please enter at least one entry';
			return;
		}

		const entries: Array<{ key: string; value: string }> = [];
		for (const line of lines) {
			const parts = line.split(/[,\s]+/).filter(p => p.trim());
			if (parts.length >= 2) {
				entries.push({ key: parts[0].trim(), value: parts[1].trim() });
			}
		}

		if (entries.length === 0) {
			addError = 'Invalid format. Use: key,value or key value';
			return;
		}

		addLoading = true;
		addError = null;
		addResult = null;
		try {
			const result = await merkleApi.addEntries(merkleId, { entries });
			addResult = { entriesAdded: result.entriesAdded, entriesSkipped: result.entriesSkipped };
			addEntries = '';
			await loadMerkle();
		} catch (e) {
			if (e instanceof ApiError) {
				addError = e.message;
			} else {
				addError = 'Failed to add entries';
			}
		} finally {
			addLoading = false;
		}
	}

	function generateRandomHex(length: number): string {
		const bytes = new Uint8Array(length);
		crypto.getRandomValues(bytes);
		return '0x' + Array.from(bytes).map(b => b.toString(16).padStart(2, '0')).join('');
	}

	async function handleGenerateRandom() {
		const entries: Array<{ key: string; value: string }> = [];
		for (let i = 0; i < randomCount; i++) {
			entries.push({
				key: generateRandomHex(randomKeyLength),
				value: generateRandomHex(randomValueLength)
			});
		}

		randomLoading = true;
		try {
			await merkleApi.addEntries(merkleId, { entries });
			showRandomModal = false;
			await loadMerkle();
		} catch (e) {
			if (e instanceof ApiError) {
				error = e.message;
			}
		} finally {
			randomLoading = false;
		}
	}

	async function handleGenerateProof() {
		if (!proofKey.trim()) return;

		proofLoading = true;
		proofResult = null;
		try {
			proofResult = await proofApi.generate(merkleId, { key: proofKey.trim() });
		} catch (e) {
			if (e instanceof ApiError) {
				error = e.message;
			}
		} finally {
			proofLoading = false;
		}
	}

	async function handleDelete() {
		if (!confirm('Are you sure you want to delete this merkle tree?')) return;

		try {
			await merkleApi.delete(merkleId);
			goto(`${base}/merkle`);
		} catch (e) {
			if (e instanceof ApiError) {
				error = e.message;
			}
		}
	}

	async function handleComputeSize() {
		computeSizeLoading = true;
		computeSizeError = null;
		try {
			const result = await merkleApi.computeSize(merkleId);
			computedSize = { size: result.size, computationTimeMs: result.computationTimeMs };
		} catch (e) {
			if (e instanceof ApiError) {
				computeSizeError = e.message;
			} else {
				computeSizeError = 'Failed to compute size';
			}
		} finally {
			computeSizeLoading = false;
		}
	}

	function copyToClipboard(text: string) {
		navigator.clipboard.writeText(text);
	}

	async function handleLoadEntries() {
		entriesLoading = true;
		entriesError = null;
		try {
			entriesData = await merkleApi.getEntries(merkleId, entriesLimit);
		} catch (e) {
			if (e instanceof ApiError) {
				entriesError = e.message;
			} else {
				entriesError = 'Failed to load entries';
			}
		} finally {
			entriesLoading = false;
		}
	}

	function truncateHex(hex: string, maxLength: number = 20): string {
		if (!hex || hex.length <= maxLength) return hex;
		const start = hex.slice(0, maxLength / 2 + 2);
		const end = hex.slice(-maxLength / 2);
		return `${start}...${end}`;
	}

	async function handleLoadTree() {
		treeLoading = true;
		treeError = null;
		try {
			treeData = await merkleApi.getTreeStructure(merkleId, undefined, treeMaxNodes);
			treeCollapsed = false;
		} catch (e) {
			if (e instanceof ApiError) {
				treeError = e.message;
			} else {
				treeError = 'Failed to load tree structure';
			}
		} finally {
			treeLoading = false;
		}
	}

	async function handleExpandTreeNode(prefix: string) {
		if (!prefix) return;

		try {
			const expandedData = await merkleApi.getTreeStructure(merkleId, prefix, treeMaxNodes);
			if (expandedData.root && treeData?.root) {
				// Merge the expanded subtree into the existing tree
				mergeSubtree(treeData.root, prefix, expandedData.root);
				// Trigger reactivity by creating a new reference
				treeData = { ...treeData, totalNodes: treeData.totalNodes + expandedData.totalNodes };
			}
		} catch (e) {
			if (e instanceof ApiError) {
				treeError = e.message;
			}
		}
	}

	function mergeSubtree(node: TreeNode, targetPrefix: string, newSubtree: TreeNode): boolean {
		// Build path to target prefix and replace the truncated node
		return mergeSubtreeRecursive(node, targetPrefix, '', newSubtree);
	}

	function mergeSubtreeRecursive(node: TreeNode, targetPrefix: string, currentPrefix: string, newSubtree: TreeNode): boolean {
		if (node.type === 'truncated' && node.prefix === targetPrefix) {
			// Found the target - but we can't directly modify it, need to replace in parent
			return true;
		}

		if (node.type === 'branch') {
			for (const [nibble, child] of Object.entries(node.children)) {
				const childPrefix = currentPrefix + nibble;
				if (child.type === 'truncated' && child.prefix === targetPrefix) {
					// Replace the truncated node with the new subtree
					node.children[nibble] = newSubtree;
					return true;
				}
				if (mergeSubtreeRecursive(child, targetPrefix, childPrefix, newSubtree)) {
					return true;
				}
			}
		} else if (node.type === 'extension' && node.child) {
			const childPrefix = currentPrefix + node.path;
			if (node.child.type === 'truncated' && node.child.prefix === targetPrefix) {
				node.child = newSubtree;
				return true;
			}
			return mergeSubtreeRecursive(node.child, targetPrefix, childPrefix, newSubtree);
		}

		return false;
	}
</script>

<div class="flex items-center gap-4 mb-6">
	<a href="{base}/merkle" class="text-gray-400 hover:text-white">
		<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
			<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
		</svg>
	</a>
	<Header title={merkleId} description="Merkle tree details and operations" />
</div>

{#if error}
	<Alert variant="error" title="Error" dismissible ondismiss={() => error = null}>
		{error}
	</Alert>
{/if}

{#if loading}
	<div class="flex items-center justify-center h-64">
		<div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-500"></div>
	</div>
{:else if merkle}
	<div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
		<!-- Info Card -->
		<Card title="Information" padding={true}>
			<div class="space-y-4">
				<div>
					<span class="text-sm text-gray-400">Status</span>
					<div class="mt-1">
						<Badge
							variant={merkle.status === 'ACTIVE' ? 'success' : merkle.status === 'DELETED' ? 'error' : 'warning'}
							text={merkle.status}
						/>
					</div>
				</div>
				<div>
					<span class="text-sm text-gray-400">Scheme</span>
					<p class="text-white">{merkle.scheme}</p>
				</div>
				<div>
					<span class="text-sm text-gray-400">Tree Size</span>
					{#if computedSize}
						<p class="text-white text-xl font-bold">{computedSize.size.toLocaleString()}</p>
						<p class="text-xs text-gray-500">Computed in {computedSize.computationTimeMs}ms</p>
					{:else if computeSizeLoading}
						<div class="flex items-center gap-2 mt-1">
							<div class="animate-spin rounded-full h-4 w-4 border-b-2 border-primary-500"></div>
							<span class="text-gray-400 text-sm">Computing...</span>
						</div>
					{:else if computeSizeError}
						<p class="text-red-400 text-sm">{computeSizeError}</p>
						<Button variant="secondary" size="sm" onclick={handleComputeSize}>
							Retry
						</Button>
					{:else}
						<Button variant="secondary" size="sm" onclick={handleComputeSize}>
							Compute Size
						</Button>
						<p class="text-xs text-gray-500 mt-1">Traverses tree to count unique entries</p>
					{/if}
				</div>
				<div>
					<span class="text-sm text-gray-400">Created</span>
					<p class="text-white">{new Date(merkle.createdAt).toLocaleString()}</p>
				</div>
				{#if merkle.description}
					<div>
						<span class="text-sm text-gray-400">Description</span>
						<p class="text-white">{merkle.description}</p>
					</div>
				{/if}
			</div>
		</Card>

		<!-- Root Hash Card -->
		<Card title="Root Hash" padding={true}>
			{#if rootHash?.rootHash}
				<div class="space-y-3">
					<div class="bg-gray-700/50 rounded-lg p-3 break-all">
						<code class="text-sm text-primary-300">{rootHash.rootHash}</code>
					</div>
					<Button variant="secondary" size="sm" onclick={() => copyToClipboard(rootHash?.rootHash || '')}>
						<svg class="w-4 h-4 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
							<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
						</svg>
						Copy
					</Button>
				</div>
			{:else}
				<p class="text-gray-500">No root hash (empty merkle tree)</p>
			{/if}
		</Card>

		<!-- Actions Card -->
		<Card title="Actions" padding={true}>
			<div class="space-y-3">
				<Button variant="primary" onclick={() => showAddModal = true} disabled={merkle.status === 'DELETED'}>
					<svg class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
					</svg>
					Add Entries
				</Button>
				<Button variant="secondary" onclick={() => showRandomModal = true} disabled={merkle.status === 'DELETED'}>
					<svg class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z" />
					</svg>
					Generate Random Data
				</Button>
				<Button variant="secondary" onclick={() => showProofModal = true}>
					<svg class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
					</svg>
					Generate Proof
				</Button>
				{#if merkle.status !== 'DELETED'}
					<Button variant="danger" onclick={handleDelete}>
						<svg class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
							<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
						</svg>
						Delete
					</Button>
				{/if}
			</div>
		</Card>
	</div>

	<!-- Value Lookup -->
	<Card title="Lookup Value">
		<div class="flex gap-4">
			<div class="flex-1">
				<Input
					placeholder="Enter key (hex format, e.g., 0x1234...)"
					bind:value={lookupKey}
				/>
			</div>
			<Button onclick={handleLookup} loading={lookupLoading}>
				Lookup
			</Button>
		</div>

		{#if lookupResult}
			<div class="mt-4 p-4 bg-gray-700/50 rounded-lg">
				{#if lookupResult.found}
					<div class="space-y-2">
						<div>
							<span class="text-sm text-gray-400">Key:</span>
							<code class="ml-2 text-primary-300">{lookupResult.key}</code>
						</div>
						<div>
							<span class="text-sm text-gray-400">Value:</span>
							<code class="ml-2 text-green-300 break-all">{lookupResult.value}</code>
						</div>
					</div>
				{:else}
					<p class="text-yellow-400">Key not found in merkle tree</p>
				{/if}
			</div>
		{/if}
	</Card>

	<!-- Entries Section -->
	<Card title="Tree Entries" class="mt-6">
		{#if entriesError}
			<Alert variant="error" dismissible ondismiss={() => entriesError = null}>
				{entriesError}
			</Alert>
		{/if}

		{#if !entriesData && !entriesLoading}
			<div class="space-y-4">
				<p class="text-gray-400 text-sm">
					Load entries from the merkle tree. Limited to a maximum of 1000 entries for performance reasons.
				</p>
				<div class="flex items-center gap-4">
					<div class="flex items-center gap-2">
						<label class="text-sm text-gray-400">Limit:</label>
						<select bind:value={entriesLimit} class="input px-3 py-1.5 text-sm">
							<option value={10}>10</option>
							<option value={50}>50</option>
							<option value={100}>100</option>
							<option value={500}>500</option>
							<option value={1000}>1000</option>
						</select>
					</div>
					<Button onclick={handleLoadEntries}>
						<svg class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
							<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
						</svg>
						Load Entries
					</Button>
				</div>
			</div>
		{:else if entriesLoading}
			<div class="flex items-center justify-center h-32">
				<div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500"></div>
				<span class="ml-3 text-gray-400">Loading entries...</span>
			</div>
		{:else if entriesData}
			<div class="space-y-4">
				<div class="flex items-center justify-between">
					<div class="text-sm text-gray-400">
						Showing {entriesData.totalReturned} entries
						{#if entriesData.hasMore}
							<span class="text-yellow-400 ml-1">(tree has more entries)</span>
						{/if}
						<span class="text-gray-500 ml-2">- Computed in {entriesData.computationTimeMs}ms</span>
					</div>
					<div class="flex items-center gap-2">
						<select bind:value={entriesLimit} class="input px-3 py-1.5 text-sm">
							<option value={10}>10</option>
							<option value={50}>50</option>
							<option value={100}>100</option>
							<option value={500}>500</option>
							<option value={1000}>1000</option>
						</select>
						<Button variant="secondary" size="sm" onclick={handleLoadEntries}>
							Reload
						</Button>
					</div>
				</div>

				{#if entriesData.entries.length === 0}
					<p class="text-gray-500 text-center py-8">No entries in merkle tree</p>
				{:else}
					<div class="overflow-x-auto">
						<table class="w-full text-sm">
							<thead>
								<tr class="border-b border-gray-700">
									<th class="text-left py-2 px-3 text-gray-400 font-medium">Original Key</th>
									<th class="text-left py-2 px-3 text-gray-400 font-medium">Hashed Key</th>
									<th class="text-left py-2 px-3 text-gray-400 font-medium">Value</th>
								</tr>
							</thead>
							<tbody>
								{#each entriesData.entries as entry}
									<tr class="border-b border-gray-700/50 hover:bg-gray-700/30">
										<td class="py-2 px-3">
											{#if entry.originalKey}
												<div class="flex items-center gap-1">
													<code class="text-primary-300 text-xs" title={entry.originalKey}>
														{truncateHex(entry.originalKey, 24)}
													</code>
													<button class="text-gray-500 hover:text-white p-0.5" title="Copy" onclick={() => copyToClipboard(entry.originalKey)}>
														<svg class="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
															<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
														</svg>
													</button>
												</div>
											{:else}
												<span class="text-gray-500">-</span>
											{/if}
										</td>
										<td class="py-2 px-3">
											<div class="flex items-center gap-1">
												<code class="text-blue-300 text-xs" title={entry.hashedKey}>
													{truncateHex(entry.hashedKey, 24)}
												</code>
												<button class="text-gray-500 hover:text-white p-0.5" title="Copy" onclick={() => copyToClipboard(entry.hashedKey)}>
													<svg class="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
														<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
													</svg>
												</button>
											</div>
										</td>
										<td class="py-2 px-3">
											<div class="flex items-center gap-1">
												<code class="text-green-300 text-xs" title={entry.value}>
													{truncateHex(entry.value, 32)}
												</code>
												<button class="text-gray-500 hover:text-white p-0.5" title="Copy" onclick={() => copyToClipboard(entry.value)}>
													<svg class="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
														<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
													</svg>
												</button>
											</div>
										</td>
									</tr>
								{/each}
							</tbody>
						</table>
					</div>
				{/if}
			</div>
		{/if}
	</Card>

	<!-- Tree Structure Section -->
	<Card title="Tree Structure" class="mt-6">
		<div class="flex items-center justify-between mb-4">
			<div>
				<p class="text-gray-400 text-sm">
					Visual representation of the Merkle Patricia Trie structure.
					{#if merkle?.scheme !== 'mpf'}
						<span class="text-yellow-400">(Only available for MPF scheme)</span>
					{/if}
				</p>
			</div>
			<div class="flex items-center gap-2">
				{#if merkle?.scheme === 'mpf'}
					<a
						href="{base}/merkle/{encodeURIComponent(merkleId)}/tree"
						class="flex items-center gap-1 text-sm text-primary-400 hover:text-primary-300"
						title="Open full-page tree visualization"
					>
						<svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
							<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
						</svg>
						Full Tree View
					</a>
				{/if}
				<button
					class="text-gray-400 hover:text-white p-1"
					onclick={() => treeCollapsed = !treeCollapsed}
					title={treeCollapsed ? 'Expand' : 'Collapse'}
				>
					<svg class="w-5 h-5 transition-transform" class:rotate-180={!treeCollapsed} fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
					</svg>
				</button>
			</div>
		</div>

		{#if !treeCollapsed}
			{#if treeError}
				<Alert variant="error" dismissible ondismiss={() => treeError = null}>
					{treeError}
				</Alert>
			{/if}

			{#if !treeData && !treeLoading}
				<div class="space-y-4">
					<div class="flex items-center gap-4">
						<div class="flex items-center gap-2">
							<label class="text-sm text-gray-400">Max Nodes:</label>
							<select bind:value={treeMaxNodes} class="input px-3 py-1.5 text-sm">
								<option value={100}>100</option>
								<option value={250}>250</option>
								<option value={500}>500</option>
								<option value={1000}>1000</option>
								<option value={2000}>2000</option>
							</select>
						</div>
						<Button onclick={handleLoadTree} disabled={merkle?.scheme !== 'mpf'}>
							<svg class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
								<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
							</svg>
							Load Tree
						</Button>
					</div>
					<p class="text-xs text-gray-500">
						Loading large trees may take time. The tree will be truncated if it exceeds the max nodes limit.
					</p>
				</div>
			{:else if treeLoading}
				<div class="flex items-center justify-center h-32">
					<div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500"></div>
					<span class="ml-3 text-gray-400">Loading tree structure...</span>
				</div>
			{:else if treeData}
				<div class="space-y-4">
					<div class="flex items-center justify-between">
						<div class="text-sm text-gray-400">
							<span>{treeData.totalNodes} nodes</span>
							{#if treeData.truncated}
								<span class="text-amber-400 ml-2">(truncated - click amber nodes to expand)</span>
							{/if}
							<span class="text-gray-500 ml-2">- Loaded in {treeData.computationTimeMs}ms</span>
						</div>
						<div class="flex items-center gap-2">
							<select bind:value={treeMaxNodes} class="input px-3 py-1.5 text-sm">
								<option value={100}>100</option>
								<option value={250}>250</option>
								<option value={500}>500</option>
								<option value={1000}>1000</option>
								<option value={2000}>2000</option>
							</select>
							<Button variant="secondary" size="sm" onclick={handleLoadTree}>
								Reload
							</Button>
							<a href="{base}/merkle/{encodeURIComponent(merkleId)}/tree">
								<Button variant="secondary" size="sm">
									<svg class="w-4 h-4 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor">
										<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
									</svg>
									Full View
								</Button>
							</a>
						</div>
					</div>

					{#if !treeData.root}
						<p class="text-gray-500 text-center py-8">Empty tree (no root node)</p>
					{:else}
						<div class="border border-gray-700 rounded-lg overflow-hidden" style="height: 500px;">
							<TreeVisualization data={treeData.root} onExpandNode={handleExpandTreeNode} orientation="horizontal" />
						</div>
					{/if}
				</div>
			{/if}
		{/if}
	</Card>
{/if}

<!-- Add Entries Modal -->
<Modal
	open={showAddModal}
	title="Add Entries"
	onclose={() => { showAddModal = false; addError = null; addResult = null; }}
>
	{#if addError}
		<Alert variant="error" dismissible ondismiss={() => addError = null}>
			{addError}
		</Alert>
	{/if}

	{#if addResult}
		<Alert variant="success">
			Added {addResult.entriesAdded} entries. Skipped {addResult.entriesSkipped}.
		</Alert>
	{/if}

	<div class="mt-4">
		<label class="label">Entries (one per line: key,value or key value)</label>
		<textarea
			bind:value={addEntries}
			rows={8}
			placeholder="0x1234,0xabcd
0x5678 0xefgh"
			class="input w-full px-3 py-2 font-mono text-sm"
		></textarea>
	</div>

	{#snippet footer()}
		<Button variant="secondary" onclick={() => showAddModal = false}>Close</Button>
		<Button loading={addLoading} onclick={handleAddEntries}>Add Entries</Button>
	{/snippet}
</Modal>

<!-- Random Data Modal -->
<Modal
	open={showRandomModal}
	title="Generate Random Test Data"
	onclose={() => showRandomModal = false}
>
	<div class="space-y-4">
		<div>
			<label class="label">Number of Entries</label>
			<input type="number" bind:value={randomCount} min={1} max={1000} class="input w-full px-3 py-2" />
		</div>
		<div class="grid grid-cols-2 gap-4">
			<div>
				<label class="label">Key Length (bytes)</label>
				<input type="number" bind:value={randomKeyLength} min={1} max={64} class="input w-full px-3 py-2" />
			</div>
			<div>
				<label class="label">Value Length (bytes)</label>
				<input type="number" bind:value={randomValueLength} min={1} max={256} class="input w-full px-3 py-2" />
			</div>
		</div>
		<p class="text-sm text-gray-400">
			This will generate {randomCount} random key-value pairs with {randomKeyLength}-byte keys and {randomValueLength}-byte values.
		</p>
	</div>

	{#snippet footer()}
		<Button variant="secondary" onclick={() => showRandomModal = false}>Cancel</Button>
		<Button loading={randomLoading} onclick={handleGenerateRandom}>Generate</Button>
	{/snippet}
</Modal>

<!-- Proof Modal -->
<Modal
	open={showProofModal}
	title="Generate Proof"
	onclose={() => { showProofModal = false; proofResult = null; }}
>
	<div class="space-y-4">
		<Input
			label="Key"
			placeholder="0x1234..."
			bind:value={proofKey}
		/>

		<Button loading={proofLoading} onclick={handleGenerateProof}>
			Generate Proof
		</Button>

		{#if proofResult}
			<div class="space-y-3 p-4 bg-gray-700/50 rounded-lg">
				<div>
					<span class="text-sm text-gray-400">Key:</span>
					<code class="ml-2 text-primary-300 text-sm">{proofResult.key}</code>
				</div>
				<div>
					<span class="text-sm text-gray-400">Value:</span>
					{#if proofResult.value}
						<code class="ml-2 text-green-300 text-sm">{proofResult.value}</code>
					{:else}
						<span class="ml-2 text-yellow-400">Not found (exclusion proof)</span>
					{/if}
				</div>
				<div>
					<span class="text-sm text-gray-400">Root Hash:</span>
					<code class="block mt-1 text-primary-300 text-xs break-all">{proofResult.rootHash}</code>
				</div>
				<div>
					<span class="text-sm text-gray-400">Proof:</span>
					<code class="block mt-1 text-gray-300 text-xs break-all max-h-32 overflow-auto">{proofResult.proof}</code>
				</div>
				<Button variant="secondary" size="sm" onclick={() => copyToClipboard(JSON.stringify(proofResult, null, 2))}>
					Copy Proof Data
				</Button>
			</div>
		{/if}
	</div>

	{#snippet footer()}
		<Button variant="secondary" onclick={() => showProofModal = false}>Close</Button>
	{/snippet}
</Modal>

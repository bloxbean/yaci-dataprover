<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/stores';
	import { base } from '$app/paths';
	import { Header, Button, Badge, Alert, TreeVisualization } from '$lib/components';
	import { merkleApi, type MerkleResponse, type TreeStructureResponse, type TreeNode, ApiError } from '$lib/api';

	const merkleId = $derived($page.params.id);

	let merkle: MerkleResponse | null = $state(null);
	let loading = $state(true);
	let error: string | null = $state(null);

	// Tree structure
	let treeData: TreeStructureResponse | null = $state(null);
	let treeLoading = $state(false);
	let treeError: string | null = $state(null);
	let treeMaxNodes = $state(500);

	onMount(async () => {
		await loadMerkle();
	});

	async function loadMerkle() {
		loading = true;
		error = null;
		try {
			merkle = await merkleApi.get(merkleId);
			// Auto-load tree for MPF scheme
			if (merkle.scheme === 'mpf') {
				await handleLoadTree();
			}
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

	async function handleLoadTree() {
		treeLoading = true;
		treeError = null;
		try {
			treeData = await merkleApi.getTreeStructure(merkleId, undefined, treeMaxNodes);
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
		return mergeSubtreeRecursive(node, targetPrefix, '', newSubtree);
	}

	function mergeSubtreeRecursive(node: TreeNode, targetPrefix: string, currentPrefix: string, newSubtree: TreeNode): boolean {
		if (node.type === 'truncated' && node.prefix === targetPrefix) {
			return true;
		}

		if (node.type === 'branch') {
			for (const [nibble, child] of Object.entries(node.children)) {
				const childPrefix = currentPrefix + nibble;
				if (child.type === 'truncated' && child.prefix === targetPrefix) {
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

<svelte:head>
	<title>Tree Visualization - {merkleId}</title>
</svelte:head>

<div class="flex flex-col h-full">
	<!-- Header -->
	<div class="flex-shrink-0 border-b border-gray-700 bg-gray-800/50 px-6 py-4">
		<div class="flex items-center justify-between">
			<div class="flex items-center gap-4">
				<a href="{base}/merkle/{encodeURIComponent(merkleId)}" class="text-gray-400 hover:text-white">
					<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
					</svg>
				</a>
				<div>
					<h1 class="text-xl font-semibold text-white">{merkleId}</h1>
					<p class="text-sm text-gray-400">Merkle Patricia Trie Visualization</p>
				</div>
			</div>

			{#if merkle}
				<div class="flex items-center gap-4">
					<Badge
						variant={merkle.status === 'ACTIVE' ? 'success' : merkle.status === 'DELETED' ? 'error' : 'warning'}
						text={merkle.status}
					/>
					<span class="text-sm text-gray-400">{merkle.scheme}</span>
				</div>
			{/if}
		</div>

		{#if treeData}
			<div class="flex items-center justify-between mt-4">
				<div class="text-sm text-gray-400">
					<span>{treeData.totalNodes} nodes</span>
					{#if treeData.truncated}
						<span class="text-amber-400 ml-2">(truncated - click amber nodes to expand)</span>
					{/if}
					<span class="text-gray-500 ml-2">- Loaded in {treeData.computationTimeMs}ms</span>
				</div>
				<div class="flex items-center gap-2">
					<select bind:value={treeMaxNodes} class="input px-3 py-1.5 text-sm">
						<option value={100}>100 nodes</option>
						<option value={250}>250 nodes</option>
						<option value={500}>500 nodes</option>
						<option value={1000}>1000 nodes</option>
						<option value={2000}>2000 nodes</option>
					</select>
					<Button variant="secondary" size="sm" onclick={handleLoadTree} loading={treeLoading}>
						Reload
					</Button>
				</div>
			</div>
		{/if}
	</div>

	<!-- Error/Loading States -->
	{#if error}
		<div class="p-6">
			<Alert variant="error" title="Error" dismissible ondismiss={() => error = null}>
				{error}
			</Alert>
		</div>
	{/if}

	{#if treeError}
		<div class="p-6">
			<Alert variant="error" title="Tree Error" dismissible ondismiss={() => treeError = null}>
				{treeError}
			</Alert>
		</div>
	{/if}

	<!-- Main Content -->
	{#if loading || treeLoading}
		<div class="flex-1 flex items-center justify-center">
			<div class="text-center">
				<div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-500 mx-auto"></div>
				<p class="mt-4 text-gray-400">
					{loading ? 'Loading merkle...' : 'Loading tree structure...'}
				</p>
			</div>
		</div>
	{:else if merkle?.scheme !== 'mpf'}
		<div class="flex-1 flex items-center justify-center">
			<div class="text-center">
				<svg class="w-16 h-16 text-gray-600 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
				</svg>
				<p class="text-gray-400">Tree visualization is only available for MPF scheme merkles.</p>
				<p class="text-sm text-gray-500 mt-1">This merkle uses the "{merkle?.scheme}" scheme.</p>
				<a href="{base}/merkle/{encodeURIComponent(merkleId)}" class="inline-block mt-4 text-primary-400 hover:text-primary-300">
					&larr; Back to merkle details
				</a>
			</div>
		</div>
	{:else if !treeData?.root}
		<div class="flex-1 flex items-center justify-center">
			<div class="text-center">
				<svg class="w-16 h-16 text-gray-600 mx-auto mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4" />
				</svg>
				<p class="text-gray-400">Empty tree (no root node)</p>
				<p class="text-sm text-gray-500 mt-1">Add entries to the merkle tree to visualize its structure.</p>
				<a href="{base}/merkle/{encodeURIComponent(merkleId)}" class="inline-block mt-4 text-primary-400 hover:text-primary-300">
					&larr; Back to merkle details
				</a>
			</div>
		</div>
	{:else}
		<div class="flex-1 min-h-0">
			<TreeVisualization
				data={treeData.root}
				onExpandNode={handleExpandTreeNode}
				orientation="vertical"
				fullHeight={true}
			/>
		</div>
	{/if}
</div>

<style>
	:global(.h-full) {
		height: 100%;
	}
</style>

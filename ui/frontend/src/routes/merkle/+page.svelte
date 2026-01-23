<script lang="ts">
	import { onMount } from 'svelte';
	import { base } from '$app/paths';
	import { Header, Card, Button, Badge, Table, Pagination, Modal, Input, Alert } from '$lib/components';
	import { merkleApi, type MerkleResponse, type PageResponse, type MerkleStatus, ApiError } from '$lib/api';

	let merkles: PageResponse<MerkleResponse> | null = $state(null);
	let loading = $state(true);
	let error: string | null = $state(null);
	let currentPage = $state(0);
	let pageSize = $state(10);
	let statusFilter: MerkleStatus | undefined = $state(undefined);

	// Create modal
	let showCreateModal = $state(false);
	let createForm = $state({ identifier: '', scheme: 'mpf', description: '', storeOriginalKeys: false });
	let createLoading = $state(false);
	let createError: string | null = $state(null);

	// Delete confirmation
	let deleteTarget: string | null = $state(null);
	let deleteLoading = $state(false);

	onMount(async () => {
		await loadMerkles();
	});

	async function loadMerkles() {
		loading = true;
		error = null;
		try {
			merkles = await merkleApi.list(currentPage, pageSize, statusFilter);
		} catch (e) {
			if (e instanceof ApiError) {
				error = e.message;
			} else {
				error = 'Failed to load merkle trees';
			}
		} finally {
			loading = false;
		}
	}

	async function handlePageChange(page: number) {
		currentPage = page;
		await loadMerkles();
	}

	async function handleCreate() {
		if (!createForm.identifier.trim()) {
			createError = 'Identifier is required';
			return;
		}

		createLoading = true;
		createError = null;
		try {
			await merkleApi.create({
				identifier: createForm.identifier.trim(),
				scheme: createForm.scheme,
				description: createForm.description.trim() || undefined,
				storeOriginalKeys: createForm.storeOriginalKeys
			});
			showCreateModal = false;
			createForm = { identifier: '', scheme: 'mpf', description: '', storeOriginalKeys: false };
			await loadMerkles();
		} catch (e) {
			if (e instanceof ApiError) {
				createError = e.message;
			} else {
				createError = 'Failed to create merkle tree';
			}
		} finally {
			createLoading = false;
		}
	}

	async function handleDelete() {
		if (!deleteTarget) return;

		deleteLoading = true;
		try {
			await merkleApi.delete(deleteTarget);
			deleteTarget = null;
			await loadMerkles();
		} catch (e) {
			if (e instanceof ApiError) {
				error = e.message;
			} else {
				error = 'Failed to delete merkle tree';
			}
		} finally {
			deleteLoading = false;
		}
	}

	function formatDate(date: string): string {
		return new Date(date).toLocaleString();
	}
</script>

<Header title="Merkle Trees" description="Manage your merkle tree structures" />

<div class="flex items-center justify-between mb-6">
	<div class="flex items-center gap-4">
		<select
			bind:value={statusFilter}
			onchange={() => { currentPage = 0; loadMerkles(); }}
			class="input px-3 py-2"
		>
			<option value={undefined}>All Status</option>
			<option value="ACTIVE">Active</option>
			<option value="BUILDING">Building</option>
			<option value="ARCHIVED">Archived</option>
			<option value="DELETED">Deleted</option>
		</select>
	</div>
	<Button onclick={() => showCreateModal = true}>
		<svg class="w-4 h-4 mr-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
			<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
		</svg>
		Create Merkle
	</Button>
</div>

{#if error}
	<Alert variant="error" title="Error" dismissible ondismiss={() => error = null}>
		{error}
	</Alert>
{/if}

<Card padding={false}>
	{#if loading}
		<div class="flex items-center justify-center h-64">
			<div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-500"></div>
		</div>
	{:else if merkles}
		<Table
			headers={['Identifier', 'Scheme', 'Root Hash', 'Status', 'Created', 'Actions']}
			isEmpty={merkles.content.length === 0}
			emptyMessage="No merkle trees found"
		>
			{#each merkles.content as merkle}
				<tr class="hover:bg-gray-700/30">
					<td class="px-4 py-3">
						<a href="{base}/merkle/{merkle.identifier}" class="text-primary-400 hover:text-primary-300 font-medium">
							{merkle.identifier}
						</a>
						{#if merkle.description}
							<p class="text-xs text-gray-500 mt-0.5">{merkle.description}</p>
						{/if}
					</td>
					<td class="px-4 py-3 text-gray-400">{merkle.scheme}</td>
					<td class="px-4 py-3">
						{#if merkle.rootHash}
							<code class="text-xs text-gray-400 bg-gray-700 px-2 py-1 rounded">
								{merkle.rootHash.substring(0, 16)}...
							</code>
						{:else}
							<span class="text-gray-500">-</span>
						{/if}
					</td>
					<td class="px-4 py-3">
						<Badge
							variant={merkle.status === 'ACTIVE' ? 'success' : merkle.status === 'DELETED' ? 'error' : 'warning'}
							text={merkle.status}
							size="sm"
						/>
					</td>
					<td class="px-4 py-3 text-gray-400 text-sm">
						{formatDate(merkle.createdAt)}
					</td>
					<td class="px-4 py-3">
						<div class="flex items-center gap-2">
							<a
								href="{base}/merkle/{merkle.identifier}"
								class="text-gray-400 hover:text-white"
								title="View"
							>
								<svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
									<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
									<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
								</svg>
							</a>
							{#if merkle.status !== 'DELETED'}
								<button
									onclick={() => deleteTarget = merkle.identifier}
									class="text-gray-400 hover:text-red-400"
									title="Delete"
								>
									<svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
										<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
									</svg>
								</button>
							{/if}
						</div>
					</td>
				</tr>
			{/each}
		</Table>

		{#if merkles.totalPages > 1}
			<div class="px-4 py-3 border-t border-gray-700">
				<Pagination
					currentPage={merkles.number}
					totalPages={merkles.totalPages}
					onPageChange={handlePageChange}
				/>
			</div>
		{/if}
	{/if}
</Card>

<!-- Create Modal -->
<Modal
	open={showCreateModal}
	title="Create Merkle Tree"
	onclose={() => { showCreateModal = false; createError = null; }}
>
	{#if createError}
		<Alert variant="error" dismissible ondismiss={() => createError = null}>
			{createError}
		</Alert>
	{/if}

	<div class="space-y-4 mt-4">
		<Input
			label="Identifier"
			placeholder="my-merkle-tree"
			required
			bind:value={createForm.identifier}
		/>

		<div class="space-y-1">
			<label class="label">Scheme</label>
			<select bind:value={createForm.scheme} class="input w-full px-3 py-2">
				<option value="mpf">MPF (Merkle Patricia Forestry)</option>
			</select>
		</div>

		<Input
			label="Description"
			placeholder="Optional description"
			bind:value={createForm.description}
		/>

		<div class="flex items-center gap-2">
			<input
				type="checkbox"
				id="storeOriginalKeys"
				bind:checked={createForm.storeOriginalKeys}
				class="w-4 h-4 rounded border-gray-600 bg-gray-700 text-primary-500 focus:ring-primary-500"
			/>
			<label for="storeOriginalKeys" class="text-sm text-gray-300">
				Store original keys
			</label>
		</div>
		<p class="text-xs text-gray-500 mt-1">
			Enable to store unhashed keys for debugging. Required to see original keys in entries list.
		</p>
	</div>

	{#snippet footer()}
		<Button variant="secondary" onclick={() => showCreateModal = false}>Cancel</Button>
		<Button loading={createLoading} onclick={handleCreate}>Create</Button>
	{/snippet}
</Modal>

<!-- Delete Confirmation Modal -->
<Modal
	open={deleteTarget !== null}
	title="Delete Merkle Tree"
	onclose={() => deleteTarget = null}
>
	<p class="text-gray-300">
		Are you sure you want to delete <span class="font-semibold text-white">{deleteTarget}</span>?
		This action cannot be undone.
	</p>

	{#snippet footer()}
		<Button variant="secondary" onclick={() => deleteTarget = null}>Cancel</Button>
		<Button variant="danger" loading={deleteLoading} onclick={handleDelete}>Delete</Button>
	{/snippet}
</Modal>

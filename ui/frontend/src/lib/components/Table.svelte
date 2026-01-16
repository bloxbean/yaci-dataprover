<script lang="ts">
	import type { Snippet } from 'svelte';

	interface Props {
		headers: string[];
		children: Snippet;
		emptyMessage?: string;
		isEmpty?: boolean;
	}

	let { headers, children, emptyMessage = 'No data available', isEmpty = false }: Props = $props();
</script>

<div class="overflow-x-auto">
	<table class="w-full text-left">
		<thead class="bg-gray-700/50 text-gray-300 text-sm">
			<tr>
				{#each headers as header}
					<th class="px-4 py-3 font-medium">{header}</th>
				{/each}
			</tr>
		</thead>
		<tbody class="divide-y divide-gray-700">
			{#if isEmpty}
				<tr>
					<td colspan={headers.length} class="px-4 py-8 text-center text-gray-500">
						{emptyMessage}
					</td>
				</tr>
			{:else}
				{@render children()}
			{/if}
		</tbody>
	</table>
</div>

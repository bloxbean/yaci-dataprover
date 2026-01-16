<script lang="ts">
	import type { Snippet } from 'svelte';

	interface Props {
		title?: string;
		padding?: boolean;
		children: Snippet;
	}

	let { title, padding = true, children }: Props = $props();
</script>

<div class="card">
	{#if title}
		<div class="px-4 py-3 border-b border-gray-700">
			<h3 class="text-lg font-medium text-white">{title}</h3>
		</div>
	{/if}
	<div class={padding ? 'p-4' : ''}>
		{@render children()}
	</div>
</div>

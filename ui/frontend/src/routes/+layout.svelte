<script lang="ts">
	import '../app.css';
	import { Sidebar } from '$lib/components';
	import type { Snippet } from 'svelte';

	interface Props {
		children: Snippet;
	}

	let { children }: Props = $props();
</script>

<div class="flex min-h-screen bg-gray-900">
	<Sidebar />
	<main class="flex-1 p-6 overflow-auto">
		{@render children()}
	</main>
</div>

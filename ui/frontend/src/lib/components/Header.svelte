<script lang="ts">
	interface Props {
		title: string;
		description?: string;
	}

	let { title, description }: Props = $props();
</script>

<header class="mb-6">
	<h1 class="text-2xl font-bold text-white">{title}</h1>
	{#if description}
		<p class="text-gray-400 mt-1">{description}</p>
	{/if}
</header>

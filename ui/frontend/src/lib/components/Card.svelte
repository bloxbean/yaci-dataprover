<script lang="ts">
	import type { Snippet } from 'svelte';

	interface Props {
		title?: string;
		padding?: boolean;
		collapsible?: boolean;
		defaultExpanded?: boolean;
		class?: string;
		children: Snippet;
	}

	let { title, padding = true, collapsible = false, defaultExpanded = true, class: className = '', children }: Props = $props();
	let expanded = $state(defaultExpanded);
</script>

<div class="card {className}">
	{#if title}
		<div
			class="px-4 py-3 border-b border-gray-700 {collapsible ? 'cursor-pointer select-none hover:bg-gray-800/50' : ''}"
			onclick={() => collapsible && (expanded = !expanded)}
			onkeydown={(e) => collapsible && (e.key === 'Enter' || e.key === ' ') && (expanded = !expanded)}
			role={collapsible ? 'button' : undefined}
			tabindex={collapsible ? 0 : undefined}
			aria-expanded={collapsible ? expanded : undefined}
		>
			<div class="flex items-center justify-between">
				<h3 class="text-lg font-medium text-white">{title}</h3>
				{#if collapsible}
					<svg
						class="w-5 h-5 text-gray-400 transition-transform {expanded ? 'rotate-180' : ''}"
						fill="none"
						viewBox="0 0 24 24"
						stroke="currentColor"
					>
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
					</svg>
				{/if}
			</div>
		</div>
	{/if}
	{#if !collapsible || expanded}
		<div class={padding ? 'p-4' : ''}>
			{@render children()}
		</div>
	{/if}
</div>

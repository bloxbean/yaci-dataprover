<script lang="ts">
	import type { ProviderInfo } from '$lib/api/types';
	import ProviderStatusBadge from './ProviderStatusBadge.svelte';

	interface Props {
		provider: ProviderInfo;
	}

	let { provider }: Props = $props();
</script>

<div class="card p-5 flex flex-col h-full cursor-pointer hover:bg-gray-800/80 transition-colors">
	<div class="flex items-start justify-between mb-3">
		<div class="flex items-center gap-3">
			<div
				class="w-10 h-10 rounded-lg bg-primary-900/50 border border-primary-700 flex items-center justify-center"
			>
				<svg class="w-5 h-5 text-primary-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path
						stroke-linecap="round"
						stroke-linejoin="round"
						stroke-width="2"
						d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4"
					/>
				</svg>
			</div>
			<div>
				<h3 class="text-lg font-semibold text-white">{provider.name}</h3>
				<span class="text-xs text-gray-500">{provider.dataType}</span>
			</div>
		</div>
		<ProviderStatusBadge status={provider.status} size="sm" />
	</div>

	<p class="text-sm text-gray-400 mb-4 flex-grow line-clamp-2">
		{provider.description}
	</p>

	<div class="mt-auto pt-3 border-t border-gray-700 flex items-center justify-between">
		<span class="text-xs text-gray-500">Click to view details</span>
		<svg class="w-4 h-4 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
			<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
		</svg>
	</div>
</div>

<script lang="ts">
	import type { Snippet } from 'svelte';

	interface Props {
		open: boolean;
		title: string;
		onclose: () => void;
		children: Snippet;
		footer?: Snippet;
	}

	let { open, title, onclose, children, footer }: Props = $props();

	function handleKeydown(e: KeyboardEvent) {
		if (e.key === 'Escape') {
			onclose();
		}
	}

	function handleBackdropClick(e: MouseEvent) {
		if (e.target === e.currentTarget) {
			onclose();
		}
	}
</script>

<svelte:window onkeydown={handleKeydown} />

{#if open}
	<!-- svelte-ignore a11y_click_events_have_key_events -->
	<!-- svelte-ignore a11y_no_static_element_interactions -->
	<div
		class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
		onclick={handleBackdropClick}
	>
		<div class="bg-gray-800 rounded-xl border border-gray-700 shadow-2xl w-full max-w-lg mx-4">
			<div class="flex items-center justify-between px-4 py-3 border-b border-gray-700">
				<h2 class="text-lg font-semibold text-white">{title}</h2>
				<button
					onclick={onclose}
					class="text-gray-400 hover:text-white transition-colors"
				>
					<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
					</svg>
				</button>
			</div>

			<div class="p-4">
				{@render children()}
			</div>

			{#if footer}
				<div class="px-4 py-3 border-t border-gray-700 flex justify-end gap-2">
					{@render footer()}
				</div>
			{/if}
		</div>
	</div>
{/if}

<script lang="ts">
	import type { Snippet } from 'svelte';

	interface Props {
		variant?: 'info' | 'success' | 'warning' | 'error';
		title?: string;
		dismissible?: boolean;
		ondismiss?: () => void;
		children: Snippet;
	}

	let { variant = 'info', title, dismissible = false, ondismiss, children }: Props = $props();

	const variantClasses = {
		info: 'bg-blue-900/30 border-blue-700 text-blue-300',
		success: 'bg-green-900/30 border-green-700 text-green-300',
		warning: 'bg-yellow-900/30 border-yellow-700 text-yellow-300',
		error: 'bg-red-900/30 border-red-700 text-red-300'
	};

	const iconPaths = {
		info: 'M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z',
		success: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z',
		warning: 'M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z',
		error: 'M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z'
	};
</script>

<div class="rounded-lg border p-4 {variantClasses[variant]}">
	<div class="flex">
		<svg class="w-5 h-5 flex-shrink-0 mr-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
			<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d={iconPaths[variant]} />
		</svg>
		<div class="flex-1">
			{#if title}
				<h4 class="font-medium mb-1">{title}</h4>
			{/if}
			<div class="text-sm">
				{@render children()}
			</div>
		</div>
		{#if dismissible}
			<button onclick={ondismiss} class="flex-shrink-0 ml-3 hover:opacity-75">
				<svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
					<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
				</svg>
			</button>
		{/if}
	</div>
</div>

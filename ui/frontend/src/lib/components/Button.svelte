<script lang="ts">
	import type { Snippet } from 'svelte';

	interface Props {
		variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
		size?: 'sm' | 'md' | 'lg';
		disabled?: boolean;
		loading?: boolean;
		type?: 'button' | 'submit' | 'reset';
		onclick?: () => void;
		children: Snippet;
	}

	let {
		variant = 'primary',
		size = 'md',
		disabled = false,
		loading = false,
		type = 'button',
		onclick,
		children
	}: Props = $props();

	const baseClasses = 'inline-flex items-center justify-center font-medium rounded-lg transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed';

	const variantClasses = {
		primary: 'bg-primary-600 hover:bg-primary-700 text-white',
		secondary: 'bg-gray-700 hover:bg-gray-600 text-white',
		danger: 'bg-red-600 hover:bg-red-700 text-white',
		ghost: 'bg-transparent hover:bg-gray-700 text-gray-300'
	};

	const sizeClasses = {
		sm: 'px-3 py-1.5 text-sm',
		md: 'px-4 py-2 text-sm',
		lg: 'px-6 py-3 text-base'
	};
</script>

<button
	{type}
	{disabled}
	class="{baseClasses} {variantClasses[variant]} {sizeClasses[size]}"
	onclick={onclick}
>
	{#if loading}
		<svg class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
			<circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
			<path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
		</svg>
	{/if}
	{@render children()}
</button>

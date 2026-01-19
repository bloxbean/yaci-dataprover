<script lang="ts">
	import type { ProviderStatus } from '$lib/api/types';

	interface Props {
		status: ProviderStatus;
		size?: 'sm' | 'default';
	}

	let { status, size = 'default' }: Props = $props();

	const statusConfig: Record<ProviderStatus, { label: string; classes: string }> = {
		AVAILABLE: {
			label: 'Available',
			classes: 'bg-green-900/50 text-green-400 border-green-700'
		},
		NOT_CONFIGURED: {
			label: 'Not Configured',
			classes: 'bg-yellow-900/50 text-yellow-400 border-yellow-700'
		},
		ERROR: {
			label: 'Error',
			classes: 'bg-red-900/50 text-red-400 border-red-700'
		}
	};

	const config = $derived(statusConfig[status] ?? statusConfig.ERROR);
	const sizeClasses = $derived(size === 'sm' ? 'px-2 py-0.5 text-xs' : 'px-2.5 py-1 text-xs');
</script>

<span class="inline-flex items-center rounded-full border font-medium {config.classes} {sizeClasses}">
	{config.label}
</span>

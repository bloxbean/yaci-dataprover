<script lang="ts">
	interface Props {
		variant?: 'default' | 'success' | 'warning' | 'error' | 'info';
		size?: 'sm' | 'md';
		text: string;
	}

	let { variant = 'default', size = 'md', text }: Props = $props();

	const variantClasses = {
		default: 'bg-gray-700 text-gray-300',
		success: 'bg-green-900/50 text-green-400 border border-green-700',
		warning: 'bg-yellow-900/50 text-yellow-400 border border-yellow-700',
		error: 'bg-red-900/50 text-red-400 border border-red-700',
		info: 'bg-blue-900/50 text-blue-400 border border-blue-700'
	};

	const sizeClasses = {
		sm: 'px-2 py-0.5 text-xs',
		md: 'px-2.5 py-1 text-sm'
	};
</script>

<span class="inline-flex items-center rounded-full font-medium {variantClasses[variant]} {sizeClasses[size]}">
	{text}
</span>

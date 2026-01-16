<script lang="ts">
	interface Props {
		type?: 'text' | 'number' | 'password' | 'email';
		value: string;
		placeholder?: string;
		label?: string;
		error?: string;
		disabled?: boolean;
		required?: boolean;
		id?: string;
		oninput?: (e: Event) => void;
	}

	let {
		type = 'text',
		value = $bindable(),
		placeholder = '',
		label,
		error,
		disabled = false,
		required = false,
		id,
		oninput
	}: Props = $props();
</script>

<div class="space-y-1">
	{#if label}
		<label for={id} class="label">
			{label}
			{#if required}
				<span class="text-red-400">*</span>
			{/if}
		</label>
	{/if}
	<input
		{id}
		{type}
		{placeholder}
		{disabled}
		{required}
		bind:value
		{oninput}
		class="input w-full px-3 py-2 {error ? 'border-red-500 focus:ring-red-500 focus:border-red-500' : ''}"
	/>
	{#if error}
		<p class="text-sm text-red-400">{error}</p>
	{/if}
</div>

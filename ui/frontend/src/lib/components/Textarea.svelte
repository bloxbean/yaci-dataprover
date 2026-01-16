<script lang="ts">
	interface Props {
		value: string;
		placeholder?: string;
		label?: string;
		error?: string;
		disabled?: boolean;
		required?: boolean;
		rows?: number;
		id?: string;
		readonly?: boolean;
		monospace?: boolean;
	}

	let {
		value = $bindable(),
		placeholder = '',
		label,
		error,
		disabled = false,
		required = false,
		rows = 4,
		id,
		readonly = false,
		monospace = false
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
	<textarea
		{id}
		{placeholder}
		{disabled}
		{required}
		{rows}
		{readonly}
		bind:value
		class="input w-full px-3 py-2 resize-none {monospace ? 'font-mono text-sm' : ''} {error ? 'border-red-500 focus:ring-red-500 focus:border-red-500' : ''}"
	></textarea>
	{#if error}
		<p class="text-sm text-red-400">{error}</p>
	{/if}
</div>

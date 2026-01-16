<script lang="ts">
	interface Option {
		value: string;
		label: string;
	}

	interface Props {
		value: string;
		options: Option[];
		placeholder?: string;
		label?: string;
		error?: string;
		disabled?: boolean;
		required?: boolean;
		id?: string;
	}

	let {
		value = $bindable(),
		options,
		placeholder = 'Select an option',
		label,
		error,
		disabled = false,
		required = false,
		id
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
	<select
		{id}
		{disabled}
		{required}
		bind:value
		class="input w-full px-3 py-2 {error ? 'border-red-500 focus:ring-red-500 focus:border-red-500' : ''}"
	>
		<option value="" disabled>{placeholder}</option>
		{#each options as option}
			<option value={option.value}>{option.label}</option>
		{/each}
	</select>
	{#if error}
		<p class="text-sm text-red-400">{error}</p>
	{/if}
</div>

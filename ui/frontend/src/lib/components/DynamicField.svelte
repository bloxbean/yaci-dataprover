<script lang="ts">
	import type { ConfigField, FieldType } from '$lib/api/types';

	interface Props {
		field: ConfigField;
		value: unknown;
		error?: string;
		disabled?: boolean;
	}

	let { field, value = $bindable(), error, disabled = false }: Props = $props();

	// State for password visibility toggle
	let showPassword = $state(false);

	// Get the string value for display
	let stringValue = $derived(value?.toString() ?? '');
	let numberValue = $derived(typeof value === 'number' ? value : (value ? Number(value) : 0));
	let booleanValue = $derived(typeof value === 'boolean' ? value : value === 'true');

	function handleInput(e: Event) {
		const target = e.target as HTMLInputElement;
		if (field.type === 'INTEGER') {
			value = parseInt(target.value, 10) || 0;
		} else if (field.type === 'NUMBER') {
			value = parseFloat(target.value) || 0;
		} else if (field.type === 'BOOLEAN') {
			value = target.checked;
		} else {
			value = target.value;
		}
	}

	function handleSelect(e: Event) {
		const target = e.target as HTMLSelectElement;
		value = target.value;
	}
</script>

<div class="space-y-1">
	<label class="label">
		{field.label}
		{#if field.required}
			<span class="text-red-400">*</span>
		{/if}
	</label>

	{#if field.type === 'STRING'}
		<input
			type="text"
			{disabled}
			required={field.required}
			placeholder={field.placeholder}
			value={stringValue}
			oninput={handleInput}
			class="input w-full px-3 py-2 {error ? 'border-red-500 focus:ring-red-500 focus:border-red-500' : ''}"
		/>
	{:else if field.type === 'PASSWORD'}
		<div class="relative">
			<input
				type={showPassword ? 'text' : 'password'}
				{disabled}
				required={field.required}
				placeholder={field.placeholder}
				value={stringValue}
				oninput={handleInput}
				class="input w-full px-3 py-2 pr-10 {error ? 'border-red-500 focus:ring-red-500 focus:border-red-500' : ''}"
			/>
			<button
				type="button"
				onclick={() => (showPassword = !showPassword)}
				class="absolute inset-y-0 right-0 flex items-center pr-3 text-gray-400 hover:text-gray-300"
				aria-label={showPassword ? 'Hide password' : 'Show password'}
			>
				{#if showPassword}
					<svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							stroke-width="2"
							d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"
						/>
					</svg>
				{:else}
					<svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							stroke-width="2"
							d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
						/>
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							stroke-width="2"
							d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"
						/>
					</svg>
				{/if}
			</button>
		</div>
	{:else if field.type === 'INTEGER'}
		<input
			type="number"
			step="1"
			{disabled}
			required={field.required}
			placeholder={field.placeholder}
			value={numberValue}
			min={field.validation?.min}
			max={field.validation?.max}
			oninput={handleInput}
			class="input w-full px-3 py-2 {error ? 'border-red-500 focus:ring-red-500 focus:border-red-500' : ''}"
		/>
	{:else if field.type === 'NUMBER'}
		<input
			type="number"
			step="any"
			{disabled}
			required={field.required}
			placeholder={field.placeholder}
			value={numberValue}
			min={field.validation?.min}
			max={field.validation?.max}
			oninput={handleInput}
			class="input w-full px-3 py-2 {error ? 'border-red-500 focus:ring-red-500 focus:border-red-500' : ''}"
		/>
	{:else if field.type === 'BOOLEAN'}
		<div class="flex items-center">
			<input
				type="checkbox"
				{disabled}
				checked={booleanValue}
				onchange={handleInput}
				class="h-4 w-4 rounded border-gray-600 bg-gray-700 text-primary-600 focus:ring-primary-500"
			/>
			{#if field.description}
				<span class="ml-2 text-sm text-gray-400">{field.description}</span>
			{/if}
		</div>
	{:else if field.type === 'SELECT'}
		<select
			{disabled}
			required={field.required}
			value={stringValue}
			onchange={handleSelect}
			class="input w-full px-3 py-2 {error ? 'border-red-500 focus:ring-red-500 focus:border-red-500' : ''}"
		>
			<option value="" disabled>Select an option</option>
			{#each field.options ?? [] as option}
				<option value={option.value}>{option.label}</option>
			{/each}
		</select>
	{/if}

	{#if field.description && field.type !== 'BOOLEAN'}
		<p class="text-sm text-gray-400">{field.description}</p>
	{/if}

	{#if error}
		<p class="text-sm text-red-400">{error}</p>
	{/if}
</div>

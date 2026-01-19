<script lang="ts">
	import type { ConfigSchema } from '$lib/api/types';
	import DynamicField from './DynamicField.svelte';

	interface Props {
		schema: ConfigSchema;
		values: Record<string, unknown>;
		errors?: Record<string, string>;
		disabled?: boolean;
	}

	let { schema, values = $bindable(), errors = {}, disabled = false }: Props = $props();

	// Initialize values with defaults
	$effect(() => {
		if (schema?.fields) {
			for (const field of schema.fields) {
				if (values[field.name] === undefined && field.defaultValue !== undefined) {
					values[field.name] = field.defaultValue;
				}
			}
		}
	});
</script>

{#if schema?.fields && schema.fields.length > 0}
	<div class="space-y-4">
		{#each schema.fields as field (field.name)}
			<DynamicField
				{field}
				bind:value={values[field.name]}
				error={errors[field.name]}
				{disabled}
			/>
		{/each}
	</div>
{:else}
	<p class="text-sm text-gray-400">No configuration required for this provider.</p>
{/if}

<script lang="ts">
	import type { ProviderInfo, ConfigTestResponse } from '$lib/api/types';
	import { providerApi, ApiError } from '$lib/api';
	import { DynamicField, Alert } from '$lib/components';

	interface Props {
		provider: ProviderInfo;
		onConfigured?: () => void;
	}

	let { provider, onConfigured }: Props = $props();

	// Form state
	let formValues = $state<Record<string, unknown>>({});
	let errors = $state<Record<string, string>>({});
	let testResult = $state<ConfigTestResponse | null>(null);
	let saving = $state(false);
	let testing = $state(false);
	let resetting = $state(false);
	let successMessage = $state<string | null>(null);
	let errorMessage = $state<string | null>(null);

	// Get fields from connection config schema
	let fields = $derived(provider.connectionConfigSchema?.fields ?? []);

	// Initialize form values from current config
	$effect(() => {
		if (provider.currentConnectionConfig) {
			formValues = { ...provider.currentConnectionConfig };
		}
	});

	function validateForm(): boolean {
		errors = {};
		let valid = true;

		for (const field of fields) {
			const value = formValues[field.name];
			if (field.required) {
				if (value === undefined || value === null || value === '') {
					errors[field.name] = `${field.label} is required`;
					valid = false;
				}
			}
		}

		return valid;
	}

	async function handleTestConnection() {
		if (!validateForm()) return;

		testing = true;
		testResult = null;
		errorMessage = null;

		try {
			testResult = await providerApi.testConfig(provider.name, { config: formValues });
		} catch (e) {
			if (e instanceof ApiError) {
				errorMessage = e.message;
			} else {
				errorMessage = 'Failed to test configuration';
			}
		} finally {
			testing = false;
		}
	}

	async function handleSave() {
		if (!validateForm()) return;

		saving = true;
		successMessage = null;
		errorMessage = null;

		try {
			const response = await providerApi.saveConfig(provider.name, { config: formValues });
			formValues = response.config;
			successMessage = 'Configuration saved successfully';
			testResult = null;
			onConfigured?.();
		} catch (e) {
			if (e instanceof ApiError) {
				errorMessage = e.message;
			} else {
				errorMessage = 'Failed to save configuration';
			}
		} finally {
			saving = false;
		}
	}

	async function handleReset() {
		if (!confirm('Are you sure you want to reset to environment configuration?')) {
			return;
		}

		resetting = true;
		successMessage = null;
		errorMessage = null;

		try {
			await providerApi.resetConfig(provider.name);
			successMessage = 'Configuration reset to environment settings';
			testResult = null;
			formValues = {};
			onConfigured?.();
		} catch (e) {
			if (e instanceof ApiError) {
				errorMessage = e.message;
			} else {
				errorMessage = 'Failed to reset configuration';
			}
		} finally {
			resetting = false;
		}
	}

	function dismissMessages() {
		successMessage = null;
		errorMessage = null;
		testResult = null;
	}
</script>

<div class="space-y-4">
	<!-- Config Source Badge -->
	<div class="flex items-center justify-between">
		<div class="flex items-center gap-2">
			<span class="text-sm text-gray-400">Configuration Source:</span>
			{#if provider.configSource === 'UI'}
				<span
					class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-primary-500/20 text-primary-400"
				>
					UI Configured
				</span>
			{:else}
				<span
					class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-500/20 text-gray-400"
				>
					Environment
				</span>
			{/if}
		</div>
		{#if provider.configSource === 'UI'}
			<button
				type="button"
				onclick={handleReset}
				disabled={resetting}
				class="text-sm text-red-400 hover:text-red-300 disabled:opacity-50"
			>
				{resetting ? 'Resetting...' : 'Reset to Env'}
			</button>
		{/if}
	</div>

	<!-- Messages -->
	{#if successMessage}
		<Alert variant="success" onDismiss={dismissMessages}>
			{successMessage}
		</Alert>
	{/if}

	{#if errorMessage}
		<Alert variant="error" onDismiss={dismissMessages}>
			{errorMessage}
		</Alert>
	{/if}

	{#if testResult}
		<Alert variant={testResult.success ? 'success' : 'error'} onDismiss={dismissMessages}>
			{testResult.message}
		</Alert>
	{/if}

	<!-- Form Fields -->
	{#if fields.length > 0}
		<form onsubmit={(e) => { e.preventDefault(); handleSave(); }} class="space-y-4">
			{#each fields as field (field.name)}
				<DynamicField
					{field}
					bind:value={formValues[field.name]}
					error={errors[field.name]}
					disabled={saving || testing}
				/>
			{/each}

			<!-- Actions -->
			<div class="flex gap-3 pt-2">
				<button
					type="button"
					onclick={handleTestConnection}
					disabled={testing || saving}
					class="btn btn-secondary flex-1"
				>
					{#if testing}
						<svg class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
							<circle
								class="opacity-25"
								cx="12"
								cy="12"
								r="10"
								stroke="currentColor"
								stroke-width="4"
							></circle>
							<path
								class="opacity-75"
								fill="currentColor"
								d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
							></path>
						</svg>
						Testing...
					{:else}
						Test Connection
					{/if}
				</button>
				<button
					type="submit"
					disabled={saving || testing}
					class="btn btn-primary flex-1"
				>
					{#if saving}
						<svg class="animate-spin -ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24">
							<circle
								class="opacity-25"
								cx="12"
								cy="12"
								r="10"
								stroke="currentColor"
								stroke-width="4"
							></circle>
							<path
								class="opacity-75"
								fill="currentColor"
								d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
							></path>
						</svg>
						Saving...
					{:else}
						Save Configuration
					{/if}
				</button>
			</div>
		</form>
	{:else}
		<p class="text-gray-400 text-sm">This provider does not have configurable connection settings.</p>
	{/if}
</div>

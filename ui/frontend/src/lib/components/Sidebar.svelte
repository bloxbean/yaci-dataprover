<script lang="ts">
	import { page } from '$app/stores';
	import { base } from '$app/paths';

	const navItems = $derived([
		{ href: `${base}`, label: 'Dashboard', icon: 'home' },
		{ href: `${base}/merkle`, label: 'Merkle Trees', icon: 'tree' },
		{ href: `${base}/proofs`, label: 'Proofs', icon: 'shield' },
		{ href: `${base}/cache`, label: 'Cache', icon: 'database' }
	]);

	function isActive(href: string, currentPath: string): boolean {
		if (href === base || href === `${base}/`) {
			return currentPath === base || currentPath === `${base}/`;
		}
		return currentPath.startsWith(href);
	}

	const icons: Record<string, string> = {
		home: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6',
		tree: 'M4 5a1 1 0 011-1h14a1 1 0 011 1v2a1 1 0 01-1 1H5a1 1 0 01-1-1V5zM4 13a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H5a1 1 0 01-1-1v-6zM16 13a1 1 0 011-1h2a1 1 0 011 1v6a1 1 0 01-1 1h-2a1 1 0 01-1-1v-6z',
		shield: 'M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z',
		database: 'M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4'
	};
</script>

<aside class="w-64 bg-gray-800 border-r border-gray-700 min-h-screen flex flex-col">
	<div class="p-4 border-b border-gray-700">
		<h1 class="text-xl font-bold text-white flex items-center gap-2">
			<svg class="w-8 h-8 text-primary-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
				<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z" />
			</svg>
			DataProver
		</h1>
		<p class="text-sm text-gray-400 mt-1">Admin Console</p>
	</div>

	<nav class="flex-1 p-4">
		<ul class="space-y-2">
			{#each navItems as item}
				{@const active = isActive(item.href, $page.url.pathname)}
				<li>
					<a
						href={item.href}
						class="flex items-center gap-3 px-3 py-2 rounded-lg transition-colors duration-200
							{active
								? 'bg-primary-600 text-white'
								: 'text-gray-300 hover:bg-gray-700 hover:text-white'}"
					>
						<svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
							<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d={icons[item.icon]} />
						</svg>
						{item.label}
					</a>
				</li>
			{/each}
		</ul>
	</nav>

	<div class="p-4 border-t border-gray-700">
		<div class="text-xs text-gray-500">
			<p>Yaci DataProver</p>
			<p>v0.1.0</p>
		</div>
	</div>
</aside>

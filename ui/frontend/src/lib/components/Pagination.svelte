<script lang="ts">
	interface Props {
		currentPage: number;
		totalPages: number;
		onPageChange: (page: number) => void;
	}

	let { currentPage, totalPages, onPageChange }: Props = $props();

	function getPageNumbers(): (number | '...')[] {
		const pages: (number | '...')[] = [];
		const showPages = 5;

		if (totalPages <= showPages) {
			for (let i = 0; i < totalPages; i++) {
				pages.push(i);
			}
		} else {
			pages.push(0);

			if (currentPage > 2) {
				pages.push('...');
			}

			const start = Math.max(1, currentPage - 1);
			const end = Math.min(totalPages - 2, currentPage + 1);

			for (let i = start; i <= end; i++) {
				pages.push(i);
			}

			if (currentPage < totalPages - 3) {
				pages.push('...');
			}

			pages.push(totalPages - 1);
		}

		return pages;
	}
</script>

{#if totalPages > 1}
	<nav class="flex items-center justify-center gap-1">
		<button
			onclick={() => onPageChange(currentPage - 1)}
			disabled={currentPage === 0}
			class="px-3 py-1.5 rounded-lg text-sm text-gray-400 hover:text-white hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
		>
			Previous
		</button>

		{#each getPageNumbers() as page}
			{#if page === '...'}
				<span class="px-2 text-gray-500">...</span>
			{:else}
				<button
					onclick={() => onPageChange(page)}
					class="px-3 py-1.5 rounded-lg text-sm {currentPage === page
						? 'bg-primary-600 text-white'
						: 'text-gray-400 hover:text-white hover:bg-gray-700'}"
				>
					{page + 1}
				</button>
			{/if}
		{/each}

		<button
			onclick={() => onPageChange(currentPage + 1)}
			disabled={currentPage >= totalPages - 1}
			class="px-3 py-1.5 rounded-lg text-sm text-gray-400 hover:text-white hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
		>
			Next
		</button>
	</nav>
{/if}

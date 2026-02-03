<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import * as d3Hierarchy from 'd3-hierarchy';
	import * as d3Selection from 'd3-selection';
	import * as d3Zoom from 'd3-zoom';
	import * as d3Drag from 'd3-drag';
	import type { TreeNode } from '$lib/api/types';

	interface Props {
		data: TreeNode | null;
		onExpandNode?: (prefix: string) => void;
		orientation?: 'horizontal' | 'vertical';
		fullHeight?: boolean;
	}

	let { data, onExpandNode, orientation = 'vertical', fullHeight = false }: Props = $props();

	let svgElement: SVGSVGElement;
	let containerElement: HTMLDivElement;
	let tooltip: HTMLDivElement;
	let tooltipVisible = $state(false);
	let tooltipContent = $state<{ type: string; hash: string; path?: string; value?: string; originalKey?: string; childCount?: number; prefix?: string } | null>(null);
	let tooltipPosition = $state({ x: 0, y: 0 });

	// Node colors
	const NODE_COLORS = {
		branch: { fill: '#3b82f6', stroke: '#1d4ed8' },    // Blue
		extension: { fill: '#8b5cf6', stroke: '#6d28d9' }, // Purple
		leaf: { fill: '#22c55e', stroke: '#16a34a' },      // Green
		truncated: { fill: '#f59e0b', stroke: '#d97706' }  // Amber
	};

	interface D3Node {
		id: string;
		name: string;
		type: string;
		hash: string;
		path?: string;
		value?: string;
		originalKey?: string;
		childCount?: number;
		prefix?: string;
		detailLabel?: string; // Secondary label with more info
		children?: D3Node[];
	}

	function transformToD3Hierarchy(node: TreeNode, id = 'root'): D3Node | null {
		if (!node) return null;

		const d3Node: D3Node = {
			id,
			name: getNodeLabel(node),
			type: node.type,
			hash: node.hash
		};

		if (node.type === 'branch') {
			d3Node.value = node.value ?? undefined;
			// Add detail label for branch with value
			if (node.value) {
				d3Node.detailLabel = `val:${truncate(node.value, 8)}`;
			}
			const children: D3Node[] = [];
			for (const [nibble, child] of Object.entries(node.children)) {
				const childNode = transformToD3Hierarchy(child, `${id}-${nibble}`);
				if (childNode) {
					childNode.name = `[${nibble}] ${childNode.name}`;
					children.push(childNode);
				}
			}
			if (children.length > 0) {
				d3Node.children = children;
			}
		} else if (node.type === 'extension') {
			d3Node.path = node.path;
			// Detail label shows full path
			d3Node.detailLabel = node.path;
			if (node.child) {
				const childNode = transformToD3Hierarchy(node.child, `${id}-ext`);
				if (childNode) {
					d3Node.children = [childNode];
				}
			}
		} else if (node.type === 'leaf') {
			d3Node.path = node.path;
			d3Node.value = node.value;
			d3Node.originalKey = node.originalKey ?? undefined;
			// Detail label shows value (and originalKey if available)
			if (node.originalKey) {
				d3Node.detailLabel = `key:${truncate(node.originalKey, 10)}`;
			} else if (node.value) {
				d3Node.detailLabel = `val:${truncate(node.value, 10)}`;
			}
		} else if (node.type === 'truncated') {
			d3Node.childCount = node.childCount;
			d3Node.prefix = node.prefix;
			d3Node.detailLabel = `${node.childCount} children`;
		}

		return d3Node;
	}

	function getNodeLabel(node: TreeNode): string {
		switch (node.type) {
			case 'branch':
				return 'B';
			case 'extension':
				return `E:${truncate(node.path, 6)}`;
			case 'leaf':
				return `L:${truncate(node.path, 6)}`;
			case 'truncated':
				return `...${node.childCount}`;
			default:
				return '?';
		}
	}

	function truncate(str: string | undefined | null, maxLen: number): string {
		if (!str || str.length <= maxLen) return str || '';
		return str.slice(0, maxLen) + '...';
	}

	// Store node positions for drag support
	type NodePosition = { x: number; y: number };
	let nodePositions = new Map<string, NodePosition>();

	function renderTree() {
		if (!data || !svgElement || !containerElement) return;

		const d3Data = transformToD3Hierarchy(data);
		if (!d3Data) return;

		// Clear previous content
		d3Selection.select(svgElement).selectAll('*').remove();
		nodePositions.clear();

		const width = containerElement.clientWidth;
		const height = fullHeight ? containerElement.clientHeight : Math.max(400, containerElement.clientHeight);

		// Adjust margins based on orientation
		const margin = orientation === 'vertical'
			? { top: 60, right: 40, bottom: 40, left: 40 }
			: { top: 40, right: 120, bottom: 40, left: 80 };

		// Create the tree layout
		const root = d3Hierarchy.hierarchy(d3Data);

		// For vertical: width is horizontal spread, height is vertical depth
		// For horizontal: height is vertical spread, width is horizontal depth
		const treeLayout = orientation === 'vertical'
			? d3Hierarchy.tree<D3Node>().size([width - margin.left - margin.right, height - margin.top - margin.bottom])
			: d3Hierarchy.tree<D3Node>().size([height - margin.top - margin.bottom, width - margin.left - margin.right]);

		const treeData = treeLayout(root);

		// Store initial positions for each node
		treeData.descendants().forEach((d) => {
			if (orientation === 'vertical') {
				nodePositions.set(d.data.id, { x: d.x, y: d.y });
			} else {
				nodePositions.set(d.data.id, { x: d.y, y: d.x });
			}
		});

		// Create SVG
		const svg = d3Selection.select(svgElement)
			.attr('width', width)
			.attr('height', height);

		// Create a group for zoom/pan
		const g = svg.append('g')
			.attr('transform', `translate(${margin.left},${margin.top})`);

		// Add zoom behavior
		const zoom = d3Zoom.zoom<SVGSVGElement, unknown>()
			.scaleExtent([0.1, 4])
			.filter((event) => {
				// Allow zoom on scroll, but only pan when not on a node
				return event.type === 'wheel' || (event.type === 'mousedown' && !event.target.closest('.node'));
			})
			.on('zoom', (event) => {
				g.attr('transform', `translate(${margin.left + event.transform.x},${margin.top + event.transform.y}) scale(${event.transform.k})`);
			});

		svg.call(zoom);

		// Draw links - different path based on orientation
		const links = g.selectAll('.link')
			.data(treeData.links())
			.enter()
			.append('path')
			.attr('class', 'link')
			.attr('fill', 'none')
			.attr('stroke', '#4b5563')
			.attr('stroke-width', 1.5)
			.attr('data-source', (d) => d.source.data.id)
			.attr('data-target', (d) => d.target.data.id)
			.attr('d', (d) => getLinkPath(d.source.data.id, d.target.data.id));

		// Draw edge labels
		const edgeLabels = g.selectAll('.edge-label')
			.data(treeData.links())
			.enter()
			.append('text')
			.attr('class', 'edge-label')
			.attr('fill', '#9ca3af')
			.attr('font-size', '10px')
			.attr('text-anchor', 'middle')
			.attr('data-source', (d) => d.source.data.id)
			.attr('data-target', (d) => d.target.data.id)
			.each(function(d) {
				const sourcePos = nodePositions.get(d.source.data.id)!;
				const targetPos = nodePositions.get(d.target.data.id)!;
				d3Selection.select(this)
					.attr('x', (sourcePos.x + targetPos.x) / 2)
					.attr('y', (sourcePos.y + targetPos.y) / 2 - 5);
			})
			.text((d) => {
				const match = d.target.data.name.match(/^\[(\w)\]/);
				return match ? match[1] : '';
			});

		// Create drag behavior
		const drag = d3Drag.drag<SVGGElement, d3Hierarchy.HierarchyPointNode<D3Node>>()
			.on('start', function(event, d) {
				d3Selection.select(this).raise().classed('dragging', true);
				hideTooltip();
			})
			.on('drag', function(event, d) {
				const nodeId = d.data.id;
				const pos = nodePositions.get(nodeId)!;
				pos.x += event.dx;
				pos.y += event.dy;

				// Update node position
				d3Selection.select(this).attr('transform', `translate(${pos.x},${pos.y})`);

				// Update all connected links
				g.selectAll<SVGPathElement, d3Hierarchy.HierarchyPointLink<D3Node>>('.link')
					.filter((link) => link.source.data.id === nodeId || link.target.data.id === nodeId)
					.attr('d', (link) => getLinkPath(link.source.data.id, link.target.data.id));

				// Update edge labels for connected links
				g.selectAll<SVGTextElement, d3Hierarchy.HierarchyPointLink<D3Node>>('.edge-label')
					.filter((link) => link.source.data.id === nodeId || link.target.data.id === nodeId)
					.each(function(link) {
						const sourcePos = nodePositions.get(link.source.data.id)!;
						const targetPos = nodePositions.get(link.target.data.id)!;
						d3Selection.select(this)
							.attr('x', (sourcePos.x + targetPos.x) / 2)
							.attr('y', (sourcePos.y + targetPos.y) / 2 - 5);
					});
			})
			.on('end', function(event, d) {
				d3Selection.select(this).classed('dragging', false);
			});

		// Draw nodes
		const nodes = g.selectAll<SVGGElement, d3Hierarchy.HierarchyPointNode<D3Node>>('.node')
			.data(treeData.descendants())
			.enter()
			.append('g')
			.attr('class', 'node')
			.attr('transform', (d) => {
				const pos = nodePositions.get(d.data.id)!;
				return `translate(${pos.x},${pos.y})`;
			})
			.call(drag);

		// Node circles
		nodes.append('circle')
			.attr('r', 16)
			.attr('fill', (d) => NODE_COLORS[d.data.type as keyof typeof NODE_COLORS]?.fill || '#6b7280')
			.attr('stroke', (d) => NODE_COLORS[d.data.type as keyof typeof NODE_COLORS]?.stroke || '#4b5563')
			.attr('stroke-width', 2)
			.attr('cursor', 'grab')
			.on('mouseenter', (event, d) => {
				showTooltip(event, d.data);
			})
			.on('mouseleave', () => {
				hideTooltip();
			})
			.on('click', (event, d) => {
				if (d.data.type === 'truncated' && d.data.prefix && onExpandNode) {
					onExpandNode(d.data.prefix);
				}
			});

		// Node labels (inside circle)
		nodes.append('text')
			.attr('dy', '0.35em')
			.attr('text-anchor', 'middle')
			.attr('fill', 'white')
			.attr('font-size', '10px')
			.attr('font-weight', 'bold')
			.attr('pointer-events', 'none')
			.text((d) => {
				const label = d.data.name;
				// Remove nibble prefix for display inside circle
				return label.replace(/^\[\w\]\s*/, '');
			});

		// Detail labels (below circle) - show value/key info
		nodes.filter((d) => !!d.data.detailLabel)
			.append('text')
			.attr('dy', '30px') // Position below the circle
			.attr('text-anchor', 'middle')
			.attr('fill', (d) => {
				// Color based on node type
				switch (d.data.type) {
					case 'leaf': return '#86efac'; // Light green
					case 'branch': return '#93c5fd'; // Light blue
					case 'extension': return '#c4b5fd'; // Light purple
					case 'truncated': return '#fcd34d'; // Light amber
					default: return '#9ca3af';
				}
			})
			.attr('font-size', '8px')
			.attr('pointer-events', 'none')
			.text((d) => d.data.detailLabel || '');
	}

	function getLinkPath(sourceId: string, targetId: string): string {
		const sourcePos = nodePositions.get(sourceId);
		const targetPos = nodePositions.get(targetId);
		if (!sourcePos || !targetPos) return '';

		// Draw curved path from source to target
		return `M${sourcePos.x},${sourcePos.y}
		        C${sourcePos.x},${(sourcePos.y + targetPos.y) / 2}
		         ${targetPos.x},${(sourcePos.y + targetPos.y) / 2}
		         ${targetPos.x},${targetPos.y}`;
	}

	function showTooltip(event: MouseEvent, data: D3Node) {
		const rect = containerElement.getBoundingClientRect();
		tooltipContent = {
			type: data.type,
			hash: data.hash,
			path: data.path,
			value: data.value,
			originalKey: data.originalKey,
			childCount: data.childCount,
			prefix: data.prefix
		};
		tooltipPosition = {
			x: event.clientX - rect.left + 10,
			y: event.clientY - rect.top - 10
		};
		tooltipVisible = true;
	}

	function hideTooltip() {
		tooltipVisible = false;
	}

	$effect(() => {
		if (data && svgElement && containerElement) {
			renderTree();
		}
	});

	onMount(() => {
		const resizeObserver = new ResizeObserver(() => {
			if (data) renderTree();
		});
		if (containerElement) {
			resizeObserver.observe(containerElement);
		}
		return () => resizeObserver.disconnect();
	});
</script>

<div class="relative w-full" class:h-full={fullHeight} bind:this={containerElement}>
	<!-- Legend -->
	<div class="absolute top-2 right-2 bg-gray-800/90 rounded-lg p-3 z-10 text-xs">
		<div class="font-medium text-gray-300 mb-2">Node Types</div>
		<div class="space-y-1.5">
			<div class="flex items-center gap-2">
				<div class="w-4 h-4 rounded-full" style="background-color: {NODE_COLORS.branch.fill}"></div>
				<span class="text-gray-400">Branch</span>
			</div>
			<div class="flex items-center gap-2">
				<div class="w-4 h-4 rounded-full" style="background-color: {NODE_COLORS.extension.fill}"></div>
				<span class="text-gray-400">Extension</span>
			</div>
			<div class="flex items-center gap-2">
				<div class="w-4 h-4 rounded-full" style="background-color: {NODE_COLORS.leaf.fill}"></div>
				<span class="text-gray-400">Leaf</span>
			</div>
			<div class="flex items-center gap-2">
				<div class="w-4 h-4 rounded-full" style="background-color: {NODE_COLORS.truncated.fill}"></div>
				<span class="text-gray-400">Truncated (click to expand)</span>
			</div>
		</div>
		<div class="mt-3 pt-2 border-t border-gray-700 text-gray-500">
			<div>Scroll to zoom, drag canvas to pan</div>
			<div class="mt-1 text-primary-400">Drag nodes to reposition</div>
		</div>
	</div>

	<!-- SVG Container -->
	<svg bind:this={svgElement} class="w-full bg-gray-900/50 rounded-lg" style="min-height: 400px;"></svg>

	<!-- Tooltip -->
	{#if tooltipVisible && tooltipContent}
		<div
			bind:this={tooltip}
			class="absolute z-20 bg-gray-800 border border-gray-700 rounded-lg shadow-lg p-3 text-sm max-w-xs pointer-events-none"
			style="left: {tooltipPosition.x}px; top: {tooltipPosition.y}px;"
		>
			<div class="flex items-center gap-2 mb-2">
				<div
					class="w-3 h-3 rounded-full"
					style="background-color: {NODE_COLORS[tooltipContent.type as keyof typeof NODE_COLORS]?.fill || '#6b7280'}"
				></div>
				<span class="font-medium text-white capitalize">{tooltipContent.type}</span>
			</div>

			<div class="space-y-1 text-xs">
				<div>
					<span class="text-gray-400">Hash: </span>
					<code class="text-primary-300 break-all">{truncate(tooltipContent.hash, 20)}</code>
				</div>
				{#if tooltipContent.path}
					<div>
						<span class="text-gray-400">Path: </span>
						<code class="text-purple-300">{tooltipContent.path}</code>
					</div>
				{/if}
				{#if tooltipContent.value}
					<div>
						<span class="text-gray-400">Value: </span>
						<code class="text-green-300 break-all">{truncate(tooltipContent.value, 30)}</code>
					</div>
				{/if}
				{#if tooltipContent.originalKey}
					<div>
						<span class="text-gray-400">Original Key: </span>
						<code class="text-cyan-300 break-all">{truncate(tooltipContent.originalKey, 30)}</code>
					</div>
				{/if}
				{#if tooltipContent.childCount !== undefined}
					<div>
						<span class="text-gray-400">Children: </span>
						<span class="text-amber-300">{tooltipContent.childCount}</span>
					</div>
				{/if}
				{#if tooltipContent.type === 'truncated' && tooltipContent.prefix}
					<div class="mt-2 text-amber-400">
						Click to expand subtree
					</div>
				{/if}
			</div>
		</div>
	{/if}
</div>

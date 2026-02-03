package com.bloxbean.cardano.dataprover.service;

import com.bloxbean.cardano.client.util.JsonUtil;
import com.bloxbean.cardano.dataprover.dto.CreateMerkleRequest;
import com.bloxbean.cardano.dataprover.dto.MerkleEntriesResponse;
import com.bloxbean.cardano.dataprover.dto.MerkleEntryResponse;
import com.bloxbean.cardano.dataprover.dto.MerkleResponse;
import com.bloxbean.cardano.dataprover.dto.MerkleSizeResponse;
import com.bloxbean.cardano.dataprover.dto.TreeStructureResponse;
import com.bloxbean.cardano.dataprover.exception.DuplicateMerkleException;
import com.bloxbean.cardano.dataprover.exception.MerkleNotFoundException;
import com.bloxbean.cardano.dataprover.model.MerkleIdentifier;
import com.bloxbean.cardano.dataprover.model.MerkleMetadata;
import com.bloxbean.cardano.dataprover.model.MerkleStatus;
import com.bloxbean.cardano.dataprover.repository.MerkleMetadataRepository;
import com.bloxbean.cardano.dataprover.service.merkle.MerkleConfiguration;
import com.bloxbean.cardano.dataprover.service.merkle.MerkleFactory;
import com.bloxbean.cardano.dataprover.service.merkle.MerkleImplementation;
import com.bloxbean.cardano.dataprover.service.merkle.MerkleRegistry;
import com.bloxbean.cardano.dataprover.service.merkle.MpfMerkleImplementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Service for managing merkle lifecycle operations.
 */
@Service
public class MerkleManagementService {

    private static final Logger log = LoggerFactory.getLogger(MerkleManagementService.class);
    private static final int MAX_ENTRIES_LIMIT = 1000;
    private static final int MAX_TREE_NODES = 2000;
    private static final HexFormat HEX = HexFormat.of();

    private final MerkleMetadataRepository metadataRepository;
    private final MerkleFactory merkleFactory;
    private final MerkleRegistry merkleRegistry;

    public MerkleManagementService(MerkleMetadataRepository metadataRepository,
                                  MerkleFactory merkleFactory,
                                  MerkleRegistry merkleRegistry) {
        this.metadataRepository = metadataRepository;
        this.merkleFactory = merkleFactory;
        this.merkleRegistry = merkleRegistry;
    }

    @Transactional
    public MerkleResponse createMerkle(CreateMerkleRequest request) {
        MerkleIdentifier identifier = MerkleIdentifier.of(request.getIdentifier());

        if (metadataRepository.existsByIdentifier(identifier.getValue())) {
            throw new DuplicateMerkleException(identifier.getValue());
        }

        log.info("Creating new merkle: {}", identifier.getValue());

        boolean storeOriginalKeys = request.getStoreOriginalKeys() != null && request.getStoreOriginalKeys();

        MerkleConfiguration config = MerkleConfiguration.builder()
            .identifier(identifier.getValue())
            .scheme(request.getScheme())
            .storeOriginalKeys(storeOriginalKeys)
            .build();

        MerkleImplementation merkle = merkleFactory.createMerkle(request.getScheme(), config);

        merkleRegistry.registerMerkle(identifier.getValue(), merkle);

        Map<String, Object> metadata = new HashMap<>(request.getMetadata());
        if (request.getDescription() != null) {
            metadata.put("description", request.getDescription());
        }

        MerkleMetadata merkleMetadata = MerkleMetadata.builder()
            .identifier(identifier.getValue())
            .scheme(request.getScheme())
            .status(MerkleStatus.ACTIVE)
            .customMetadata(metadata)
            .storeOriginalKeys(storeOriginalKeys)
            .build();

        merkleMetadata = metadataRepository.save(merkleMetadata);

        log.info("Created merkle: {} (scheme: {})", identifier.getValue(), request.getScheme());

        return MerkleResponse.from(merkleMetadata);
    }

    public MerkleResponse getMerkle(String identifier) {
        MerkleMetadata metadata = metadataRepository.findByIdentifier(identifier)
            .orElseThrow(() -> new MerkleNotFoundException(identifier));

        // Treat deleted merkles as not found
        if (metadata.getStatus() == MerkleStatus.DELETED) {
            throw new MerkleNotFoundException(identifier);
        }

        return MerkleResponse.from(metadata);
    }

    public Page<MerkleResponse> listMerkle(Pageable pageable, MerkleStatus status) {
        Page<MerkleMetadata> metadataPage;

        if (status != null) {
            metadataPage = metadataRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            metadataPage = metadataRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return metadataPage.map(MerkleResponse::from);
    }

    @Transactional
    public void deleteMerkle(String identifier) {
        if (!metadataRepository.existsByIdentifier(identifier)) {
            throw new MerkleNotFoundException(identifier);
        }

        log.info("Deleting merkle: {}", identifier);

        merkleRegistry.unloadMerkle(identifier);

        metadataRepository.softDeleteByIdentifier(identifier);

        log.info("Deleted merkle: {}", identifier);
    }

    /**
     * Computes the actual size (unique entry count) of a merkle tree by traversing it.
     * This may take time for large trees.
     *
     * @param identifier the merkle identifier
     * @return the computed size response with timing information
     */
    public MerkleSizeResponse computeSize(String identifier) {
        MerkleMetadata metadata = metadataRepository.findByIdentifier(identifier)
            .orElseThrow(() -> new MerkleNotFoundException(identifier));

        if (metadata.getStatus() == MerkleStatus.DELETED) {
            throw new MerkleNotFoundException(identifier);
        }

        log.info("Computing size for merkle: {}", identifier);

        MerkleImplementation merkle = merkleRegistry.getOrLoadMerkle(identifier);

        long startTime = System.currentTimeMillis();
        long size = merkle.size();
        long computationTimeMs = System.currentTimeMillis() - startTime;

        log.info("Computed size for merkle {}: {} entries in {} ms", identifier, size, computationTimeMs);

        return MerkleSizeResponse.of(identifier, size, computationTimeMs);
    }

    /**
     * Returns entries from a merkle tree, limited to maxEntries.
     *
     * @param identifier the merkle identifier
     * @param limit the maximum number of entries to return (capped at MAX_ENTRIES_LIMIT)
     * @return the entries response with timing information
     */
    public MerkleEntriesResponse getEntries(String identifier, int limit) {
        MerkleMetadata metadata = metadataRepository.findByIdentifier(identifier)
            .orElseThrow(() -> new MerkleNotFoundException(identifier));

        if (metadata.getStatus() == MerkleStatus.DELETED) {
            throw new MerkleNotFoundException(identifier);
        }

        int effectiveLimit = Math.min(Math.max(limit, 1), MAX_ENTRIES_LIMIT);

        log.info("Getting entries for merkle: {} (limit: {})", identifier, effectiveLimit);

        MerkleImplementation merkle = merkleRegistry.getOrLoadMerkle(identifier);

        long startTime = System.currentTimeMillis();
        List<MerkleImplementation.Entry> entries = merkle.getEntries(effectiveLimit + 1);
        long computationTimeMs = System.currentTimeMillis() - startTime;

        boolean hasMore = entries.size() > effectiveLimit;
        List<MerkleImplementation.Entry> limitedEntries = hasMore
            ? entries.subList(0, effectiveLimit)
            : entries;

        List<MerkleEntryResponse> entryResponses = limitedEntries.stream()
            .map(e -> MerkleEntryResponse.of(e.originalKey(), e.hashedKey(), e.value()))
            .toList();

        log.info("Retrieved {} entries for merkle {} in {} ms (hasMore: {})",
            entryResponses.size(), identifier, computationTimeMs, hasMore);

        return MerkleEntriesResponse.builder()
            .merkleIdentifier(identifier)
            .entries(entryResponses)
            .totalReturned(entryResponses.size())
            .hasMore(hasMore)
            .computationTimeMs(computationTimeMs)
            .build();
    }

    /**
     * Returns the tree structure of a merkle tree for visualization.
     *
     * @param identifier the merkle identifier
     * @param prefix optional nibble prefix to start traversal from (hex string)
     * @param maxNodes maximum number of nodes to return
     * @return the tree structure response
     */
    public TreeStructureResponse getTreeStructure(String identifier, String prefix, int maxNodes) {
        MerkleMetadata metadata = metadataRepository.findByIdentifier(identifier)
            .orElseThrow(() -> new MerkleNotFoundException(identifier));

        if (metadata.getStatus() == MerkleStatus.DELETED) {
            throw new MerkleNotFoundException(identifier);
        }

        // Validate maxNodes
        int effectiveMaxNodes = Math.min(Math.max(maxNodes, 1), MAX_TREE_NODES);

        log.info("Getting tree structure for merkle: {} (prefix: {}, maxNodes: {})", identifier, prefix, effectiveMaxNodes);

        MerkleImplementation merkle = merkleRegistry.getOrLoadMerkle(identifier);

        // Only MPF scheme is supported for tree visualization
        if (!(merkle instanceof MpfMerkleImplementation mpfMerkle)) {
            throw new UnsupportedOperationException("Tree visualization is only supported for MPF scheme merkles");
        }

        byte[] rootHash = mpfMerkle.getRootHash();
        if (rootHash == null || rootHash.length == 0) {
            return TreeStructureResponse.of(identifier, null, 0, false, 0);
        }

        long startTime = System.currentTimeMillis();

        // Use MpfTrie's built-in tree structure method
        com.bloxbean.cardano.vds.mpf.TreeNode libraryTreeNode;
        int[] nibblePrefix = parseNibblePrefix(prefix);

        if (nibblePrefix.length == 0) {
            libraryTreeNode = mpfMerkle.getTrie().getTreeStructure(new int[]{}, effectiveMaxNodes);
        } else {
            libraryTreeNode = mpfMerkle.getTrie().getTreeStructure(nibblePrefix, effectiveMaxNodes);
        }

        System.out.println("Library TreeNode: " + JsonUtil.getPrettyJson(libraryTreeNode));

        // Convert library TreeNode to our DTO
        TreeStructureResponse.TreeNodeResponse root = convertTreeNode(libraryTreeNode, "");
        int totalNodes = countNodes(root);
        boolean truncated = hasTruncatedNodes(root);

        long computationTimeMs = System.currentTimeMillis() - startTime;

        log.info("Retrieved tree structure for merkle {} with {} nodes in {} ms (truncated: {})",
            identifier, totalNodes, computationTimeMs, truncated);

        return TreeStructureResponse.of(identifier, root, totalNodes, truncated, computationTimeMs);
    }

    /**
     * Parse a hex string prefix into nibbles.
     */
    private int[] parseNibblePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return new int[0];
        }
        String cleanPrefix = prefix.toLowerCase().replaceAll("[^0-9a-f]", "");
        int[] nibbles = new int[cleanPrefix.length()];
        for (int i = 0; i < cleanPrefix.length(); i++) {
            nibbles[i] = Character.digit(cleanPrefix.charAt(i), 16);
        }
        return nibbles;
    }

    /**
     * Convert library TreeNode to our DTO.
     */
    private TreeStructureResponse.TreeNodeResponse convertTreeNode(com.bloxbean.cardano.vds.mpf.TreeNode node, String currentPrefix) {
        if (node == null) {
            return null;
        }

        if (node instanceof com.bloxbean.cardano.vds.mpf.TreeNode.BranchTreeNode branch) {
            java.util.Map<String, TreeStructureResponse.TreeNodeResponse> children = new java.util.LinkedHashMap<>();
            for (var entry : branch.getChildren().entrySet()) {
                String nibble = entry.getKey();
                TreeStructureResponse.TreeNodeResponse child = convertTreeNode(entry.getValue(), currentPrefix + nibble);
                if (child != null) {
                    children.put(nibble, child);
                }
            }
            return new TreeStructureResponse.BranchTreeNode(branch.getHash(), branch.getValue(), children);
        } else if (node instanceof com.bloxbean.cardano.vds.mpf.TreeNode.ExtensionTreeNode ext) {
            String path = nibblesToHex(ext.getPath());
            TreeStructureResponse.TreeNodeResponse child = convertTreeNode(ext.getChild(), currentPrefix + path);
            return new TreeStructureResponse.ExtensionTreeNode(ext.getHash(), path, child);
        } else if (node instanceof com.bloxbean.cardano.vds.mpf.TreeNode.LeafTreeNode leaf) {
            String path = nibblesToHex(leaf.getPath());
            return new TreeStructureResponse.LeafTreeNode("leaf-" + path, path, leaf.getValue(), leaf.getKey());
        } else if (node instanceof com.bloxbean.cardano.vds.mpf.TreeNode.TruncatedTreeNode trunc) {
            return new TreeStructureResponse.TruncatedTreeNode(trunc.getHash(), trunc.getNodeType(), trunc.getChildCount(), currentPrefix);
        }

        return null;
    }

    /**
     * Convert nibbles to hex string.
     */
    private String nibblesToHex(int[] nibbles) {
        if (nibbles == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int nibble : nibbles) {
            sb.append(Integer.toHexString(nibble));
        }
        return sb.toString();
    }

    /**
     * Count total nodes in the tree.
     */
    private int countNodes(TreeStructureResponse.TreeNodeResponse node) {
        if (node == null) return 0;

        int count = 1;
        if (node instanceof TreeStructureResponse.BranchTreeNode branch) {
            for (var child : branch.getChildren().values()) {
                count += countNodes(child);
            }
        } else if (node instanceof TreeStructureResponse.ExtensionTreeNode ext) {
            count += countNodes(ext.getChild());
        }
        return count;
    }

    /**
     * Check if tree has any truncated nodes.
     */
    private boolean hasTruncatedNodes(TreeStructureResponse.TreeNodeResponse node) {
        if (node == null) return false;

        if (node instanceof TreeStructureResponse.TruncatedTreeNode) {
            return true;
        } else if (node instanceof TreeStructureResponse.BranchTreeNode branch) {
            for (var child : branch.getChildren().values()) {
                if (hasTruncatedNodes(child)) return true;
            }
        } else if (node instanceof TreeStructureResponse.ExtensionTreeNode ext) {
            return hasTruncatedNodes(ext.getChild());
        }
        return false;
    }
}

package com.bloxbean.cardano.dataprover.service.merkle;

import com.bloxbean.cardano.dataprover.dto.TreeStructureResponse;
import com.bloxbean.cardano.dataprover.dto.TreeStructureResponse.*;
import com.bloxbean.cardano.vds.mpf.MpfTrie;
import com.bloxbean.cardano.vds.mpf.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service for traversing MPF trie structure and converting to visualization DTOs.
 * Uses the MpfTrie library's native getTreeStructure() method.
 */
@Service
public class TreeTraversalService {

    private static final Logger log = LoggerFactory.getLogger(TreeTraversalService.class);

    /**
     * Traverses the trie structure using the library's native getTreeStructure() method.
     *
     * @param trie      the MpfTrie to traverse
     * @param prefix    optional nibble prefix to start traversal from (hex string)
     * @param maxNodes  maximum number of nodes to return
     * @return the tree structure response
     */
    public TreeStructureResponse traverse(MpfTrie trie, String prefix, int maxNodes) {
        if (trie == null) {
            return TreeStructureResponse.of(null, null, 0, false, 0);
        }

        byte[] rootHash = trie.getRootHash();
        if (rootHash == null || rootHash.length == 0) {
            return TreeStructureResponse.of(null, null, 0, false, 0);
        }

        long startTime = System.currentTimeMillis();

        // Parse nibble prefix from hex string
        int[] nibblePrefix = parseNibblePrefix(prefix);

        // Use MpfTrie's built-in tree structure method
        TreeNode libraryTreeNode = trie.getTreeStructure(nibblePrefix, maxNodes);

        // Convert library TreeNode to our DTO
        TreeNodeResponse root = convertTreeNode(libraryTreeNode, "");
        int totalNodes = countNodes(root);
        boolean truncated = hasTruncatedNodes(root);

        long computationTimeMs = System.currentTimeMillis() - startTime;

        log.debug("Traversed trie with {} nodes in {} ms (truncated: {})",
            totalNodes, computationTimeMs, truncated);

        return TreeStructureResponse.of(null, root, totalNodes, truncated, computationTimeMs);
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
    private TreeNodeResponse convertTreeNode(TreeNode node, String currentPrefix) {
        if (node == null) {
            return null;
        }

        if (node instanceof TreeNode.BranchTreeNode branch) {
            Map<String, TreeNodeResponse> children = new LinkedHashMap<>();
            for (var entry : branch.getChildren().entrySet()) {
                String nibble = entry.getKey();
                TreeNodeResponse child = convertTreeNode(entry.getValue(), currentPrefix + nibble);
                if (child != null) {
                    children.put(nibble, child);
                }
            }
            return new BranchTreeNode(branch.getHash(), branch.getValue(), children);
        } else if (node instanceof TreeNode.ExtensionTreeNode ext) {
            String path = nibblesToHex(ext.getPath());
            TreeNodeResponse child = convertTreeNode(ext.getChild(), currentPrefix + path);
            return new ExtensionTreeNode(ext.getHash(), path, child);
        } else if (node instanceof TreeNode.LeafTreeNode leaf) {
            String path = nibblesToHex(leaf.getPath());
            return new LeafTreeNode("leaf-" + currentPrefix + path, path, leaf.getValue(), leaf.getKey());
        } else if (node instanceof TreeNode.TruncatedTreeNode trunc) {
            return new TruncatedTreeNode(trunc.getHash(), trunc.getNodeType(), trunc.getChildCount(), currentPrefix);
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
    private int countNodes(TreeNodeResponse node) {
        if (node == null) return 0;

        int count = 1;
        if (node instanceof BranchTreeNode branch) {
            for (var child : branch.getChildren().values()) {
                count += countNodes(child);
            }
        } else if (node instanceof ExtensionTreeNode ext) {
            count += countNodes(ext.getChild());
        }
        return count;
    }

    /**
     * Check if tree has any truncated nodes.
     */
    private boolean hasTruncatedNodes(TreeNodeResponse node) {
        if (node == null) return false;

        if (node instanceof TruncatedTreeNode) {
            return true;
        } else if (node instanceof BranchTreeNode branch) {
            for (var child : branch.getChildren().values()) {
                if (hasTruncatedNodes(child)) return true;
            }
        } else if (node instanceof ExtensionTreeNode ext) {
            return hasTruncatedNodes(ext.getChild());
        }
        return false;
    }
}

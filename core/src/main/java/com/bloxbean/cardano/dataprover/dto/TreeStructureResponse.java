package com.bloxbean.cardano.dataprover.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Map;

/**
 * Response containing the tree structure of a Merkle Patricia Trie.
 */
public class TreeStructureResponse {

    private String merkleIdentifier;
    private TreeNodeResponse root;
    private int totalNodes;
    private boolean truncated;
    private long computationTimeMs;

    public TreeStructureResponse() {
    }

    public TreeStructureResponse(String merkleIdentifier, TreeNodeResponse root, int totalNodes,
                                  boolean truncated, long computationTimeMs) {
        this.merkleIdentifier = merkleIdentifier;
        this.root = root;
        this.totalNodes = totalNodes;
        this.truncated = truncated;
        this.computationTimeMs = computationTimeMs;
    }

    public static TreeStructureResponse of(String merkleIdentifier, TreeNodeResponse root, int totalNodes,
                                            boolean truncated, long computationTimeMs) {
        return new TreeStructureResponse(merkleIdentifier, root, totalNodes, truncated, computationTimeMs);
    }

    public String getMerkleIdentifier() {
        return merkleIdentifier;
    }

    public void setMerkleIdentifier(String merkleIdentifier) {
        this.merkleIdentifier = merkleIdentifier;
    }

    public TreeNodeResponse getRoot() {
        return root;
    }

    public void setRoot(TreeNodeResponse root) {
        this.root = root;
    }

    public int getTotalNodes() {
        return totalNodes;
    }

    public void setTotalNodes(int totalNodes) {
        this.totalNodes = totalNodes;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    public long getComputationTimeMs() {
        return computationTimeMs;
    }

    public void setComputationTimeMs(long computationTimeMs) {
        this.computationTimeMs = computationTimeMs;
    }

    /**
     * Base interface for tree node responses with Jackson polymorphism.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = BranchTreeNode.class, name = "branch"),
        @JsonSubTypes.Type(value = ExtensionTreeNode.class, name = "extension"),
        @JsonSubTypes.Type(value = LeafTreeNode.class, name = "leaf"),
        @JsonSubTypes.Type(value = TruncatedTreeNode.class, name = "truncated")
    })
    public interface TreeNodeResponse {
        String getHash();
    }

    /**
     * Branch node with up to 16 children (one per nibble 0-F).
     */
    public static class BranchTreeNode implements TreeNodeResponse {
        private String hash;
        private String value;
        private Map<String, TreeNodeResponse> children;

        public BranchTreeNode() {
        }

        public BranchTreeNode(String hash, String value, Map<String, TreeNodeResponse> children) {
            this.hash = hash;
            this.value = value;
            this.children = children;
        }

        @Override
        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public Map<String, TreeNodeResponse> getChildren() {
            return children;
        }

        public void setChildren(Map<String, TreeNodeResponse> children) {
            this.children = children;
        }
    }

    /**
     * Extension node with a path prefix and single child.
     */
    public static class ExtensionTreeNode implements TreeNodeResponse {
        private String hash;
        private String path;
        private TreeNodeResponse child;

        public ExtensionTreeNode() {
        }

        public ExtensionTreeNode(String hash, String path, TreeNodeResponse child) {
            this.hash = hash;
            this.path = path;
            this.child = child;
        }

        @Override
        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public TreeNodeResponse getChild() {
            return child;
        }

        public void setChild(TreeNodeResponse child) {
            this.child = child;
        }
    }

    /**
     * Leaf node containing a key-value pair.
     */
    public static class LeafTreeNode implements TreeNodeResponse {
        private String hash;
        private String path;
        private String value;
        private String originalKey;

        public LeafTreeNode() {
        }

        public LeafTreeNode(String hash, String path, String value, String originalKey) {
            this.hash = hash;
            this.path = path;
            this.value = value;
            this.originalKey = originalKey;
        }

        @Override
        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getOriginalKey() {
            return originalKey;
        }

        public void setOriginalKey(String originalKey) {
            this.originalKey = originalKey;
        }
    }

    /**
     * Truncated node placeholder for lazy loading large subtrees.
     */
    public static class TruncatedTreeNode implements TreeNodeResponse {
        private String hash;
        private String nodeType;
        private int childCount;
        private String prefix;

        public TruncatedTreeNode() {
        }

        public TruncatedTreeNode(String hash, String nodeType, int childCount, String prefix) {
            this.hash = hash;
            this.nodeType = nodeType;
            this.childCount = childCount;
            this.prefix = prefix;
        }

        @Override
        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }

        public String getNodeType() {
            return nodeType;
        }

        public void setNodeType(String nodeType) {
            this.nodeType = nodeType;
        }

        public int getChildCount() {
            return childCount;
        }

        public void setChildCount(int childCount) {
            this.childCount = childCount;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }
    }
}

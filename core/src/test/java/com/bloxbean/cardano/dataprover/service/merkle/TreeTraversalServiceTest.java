package com.bloxbean.cardano.dataprover.service.merkle;

import com.bloxbean.cardano.dataprover.dto.TreeStructureResponse;
import com.bloxbean.cardano.vds.mpf.MpfTrie;
import com.bloxbean.cardano.vds.mpf.rocksdb.RocksDbNodeStore;
import org.junit.jupiter.api.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for TreeTraversalService using MpfTrie's native getTreeStructure() method.
 */
class TreeTraversalServiceTest {

    private static final HexFormat HEX = HexFormat.of();

    private Path tempDir;
    private RocksDbNodeStore nodeStore;
    private MpfTrie trie;
    private TreeTraversalService traversalService;

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("trie-test");
        nodeStore = new RocksDbNodeStore(tempDir.toString());
        trie = new MpfTrie(nodeStore, null);
        traversalService = new TreeTraversalService();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (nodeStore != null) {
            nodeStore.close();
        }
        if (tempDir != null) {
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    @Test
    @DisplayName("Should traverse trie with single entry and find leaf node")
    void testSingleEntry() throws Exception {
        // Add a single entry
        byte[] key = HEX.parseHex("aabbccdd");
        byte[] value = HEX.parseHex("11223344");
        trie.put(key, value);

        // Traverse using the library's native method
        TreeStructureResponse response = traversalService.traverse(trie, null, 100);

        System.out.println("Total nodes: " + response.getTotalNodes());
        System.out.println("Truncated: " + response.isTruncated());
        System.out.println("Root type: " + (response.getRoot() != null ? response.getRoot().getClass().getSimpleName() : "null"));

        assertThat(response.getRoot()).isNotNull();
        // With a single entry, the root should be a leaf node
        assertThat(response.getRoot()).isInstanceOf(TreeStructureResponse.LeafTreeNode.class);
    }

    @Test
    @DisplayName("Should traverse trie with multiple entries and find leaf nodes")
    void testMultipleEntries() throws Exception {
        // Add multiple entries
        trie.put(HEX.parseHex("aabbccdd"), HEX.parseHex("11111111"));
        trie.put(HEX.parseHex("aabbccee"), HEX.parseHex("22222222"));
        trie.put(HEX.parseHex("aabbddff"), HEX.parseHex("33333333"));

        System.out.println("\n=== Multiple entries test ===");
        System.out.println("Root hash: " + HEX.formatHex(trie.getRootHash()));

        // Traverse using the library's native method
        TreeStructureResponse response = traversalService.traverse(trie, null, 100);

        System.out.println("\nTraversal result:");
        System.out.println("Total nodes: " + response.getTotalNodes());
        System.out.println("Truncated: " + response.isTruncated());
        printTreeNode(response.getRoot(), 0);

        assertThat(response.getRoot()).isNotNull();
        assertThat(response.getTotalNodes()).isGreaterThanOrEqualTo(3); // At least 3 leaf nodes
    }

    @Test
    @DisplayName("Should find leaf nodes in a larger trie")
    void testLargerTrie() throws Exception {
        // Add 10 entries
        for (int i = 0; i < 10; i++) {
            String keyHex = String.format("%08x", i);
            String valueHex = String.format("%08x", i * 100);
            trie.put(HEX.parseHex(keyHex), HEX.parseHex(valueHex));
        }

        System.out.println("\n=== Larger trie test (10 entries) ===");
        System.out.println("Root hash: " + HEX.formatHex(trie.getRootHash()));

        // Traverse using the library's native method
        TreeStructureResponse response = traversalService.traverse(trie, null, 100);

        System.out.println("Total nodes: " + response.getTotalNodes());
        System.out.println("Truncated: " + response.isTruncated());

        // Count leaf nodes
        int leafCount = countLeafNodes(response.getRoot());
        System.out.println("Leaf nodes found: " + leafCount);

        assertThat(leafCount).isEqualTo(10); // Should have 10 leaf nodes
    }

    @Test
    @DisplayName("Should handle empty trie")
    void testEmptyTrie() {
        // Don't add any entries

        TreeStructureResponse response = traversalService.traverse(trie, null, 100);

        assertThat(response.getRoot()).isNull();
        assertThat(response.getTotalNodes()).isEqualTo(0);
        assertThat(response.isTruncated()).isFalse();
    }

    @Test
    @DisplayName("Should handle null trie")
    void testNullTrie() {
        TreeStructureResponse response = traversalService.traverse(null, null, 100);

        assertThat(response.getRoot()).isNull();
        assertThat(response.getTotalNodes()).isEqualTo(0);
        assertThat(response.isTruncated()).isFalse();
    }

    private void printTreeNode(TreeStructureResponse.TreeNodeResponse node, int indent) {
        if (node == null) return;

        String indentStr = "  ".repeat(indent);

        if (node instanceof TreeStructureResponse.BranchTreeNode branch) {
            System.out.println(indentStr + "Branch (hash: " + truncateHash(branch.getHash()) + ")");
            if (branch.getValue() != null) {
                System.out.println(indentStr + "  value: " + branch.getValue());
            }
            for (var entry : branch.getChildren().entrySet()) {
                System.out.println(indentStr + "  [" + entry.getKey() + "]:");
                printTreeNode(entry.getValue(), indent + 2);
            }
        } else if (node instanceof TreeStructureResponse.ExtensionTreeNode ext) {
            System.out.println(indentStr + "Extension (path: " + ext.getPath() + ", hash: " + truncateHash(ext.getHash()) + ")");
            printTreeNode(ext.getChild(), indent + 1);
        } else if (node instanceof TreeStructureResponse.LeafTreeNode leaf) {
            System.out.println(indentStr + "LEAF (path: " + leaf.getPath() + ", value: " + leaf.getValue() + ")");
        } else if (node instanceof TreeStructureResponse.TruncatedTreeNode trunc) {
            System.out.println(indentStr + "Truncated (" + trunc.getNodeType() + ", children: " + trunc.getChildCount() + ")");
        }
    }

    private String truncateHash(String hash) {
        if (hash == null || hash.length() <= 16) return hash;
        return hash.substring(0, 16) + "...";
    }

    private int countLeafNodes(TreeStructureResponse.TreeNodeResponse node) {
        if (node == null) return 0;

        if (node instanceof TreeStructureResponse.LeafTreeNode) {
            return 1;
        } else if (node instanceof TreeStructureResponse.BranchTreeNode branch) {
            int count = 0;
            for (var child : branch.getChildren().values()) {
                count += countLeafNodes(child);
            }
            return count;
        } else if (node instanceof TreeStructureResponse.ExtensionTreeNode ext) {
            return countLeafNodes(ext.getChild());
        }
        return 0;
    }
}

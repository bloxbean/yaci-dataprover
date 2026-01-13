package com.bloxbean.cardano.proofserver;

import com.bloxbean.cardano.proofserver.dto.*;
import com.bloxbean.cardano.proofserver.model.TrieStatus;
import com.bloxbean.cardano.proofserver.service.trie.TrieRegistry;
import com.bloxbean.cardano.proofserver.test.TestDataItem;
import com.bloxbean.cardano.proofserver.test.TestDataProvider;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration tests for the Proof Server.
 * Tests the complete workflow: create trie -> ingest data -> generate/verify proofs.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProofServerIntegrationTest {

    private static final String API_BASE = "/api/v1";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TestDataProvider testDataProvider;

    @Autowired
    private TrieRegistry trieRegistry;

    @BeforeEach
    void setUp() {
        testDataProvider.clearTestData();
    }

    @AfterEach
    void tearDown() {
        // Clear cache to release RocksDB handles
        trieRegistry.clearCache();
    }

    @Test
    @Order(1)
    @DisplayName("Should create a new trie")
    void testCreateTrie() {
        String trieId = generateTrieId();

        CreateTrieRequest request = new CreateTrieRequest();
        request.setIdentifier(trieId);
        request.setTrieType("mpf");
        request.setDescription("Test trie for integration tests");

        ResponseEntity<TrieResponse> response = restTemplate.postForEntity(
                API_BASE + "/tries",
                request,
                TrieResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIdentifier()).isEqualTo(trieId);
        assertThat(response.getBody().getTrieType()).isEqualTo("mpf");
        assertThat(response.getBody().getStatus()).isEqualTo(TrieStatus.ACTIVE);
        assertThat(response.getBody().getRecordCount()).isEqualTo(0);
    }

    @Test
    @Order(2)
    @DisplayName("Should retrieve a created trie")
    void testGetTrie() {
        String trieId = createTrie();

        ResponseEntity<TrieResponse> response = restTemplate.getForEntity(
                API_BASE + "/tries/" + trieId,
                TrieResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIdentifier()).isEqualTo(trieId);
    }

    @Test
    @Order(3)
    @DisplayName("Should ingest data using test provider")
    void testIngestData() {
        String trieId = createTrie();

        // Set up test data
        testDataProvider.addTestData(TestDataItem.of("0102030405", "aabbccddee"));
        testDataProvider.addTestData(TestDataItem.of("0506070809", "ffeeddccbb"));
        testDataProvider.addTestData(TestDataItem.of("0a0b0c0d0e", "1122334455"));

        IngestRequest ingestRequest = new IngestRequest();
        ingestRequest.setProvider(TestDataProvider.PROVIDER_NAME);

        ResponseEntity<IngestResponse> response = restTemplate.postForEntity(
                API_BASE + "/tries/" + trieId + "/ingest",
                ingestRequest,
                IngestResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTrieIdentifier()).isEqualTo(trieId);
        assertThat(response.getBody().getProvider()).isEqualTo(TestDataProvider.PROVIDER_NAME);
        assertThat(response.getBody().getRecordsProcessed()).isEqualTo(3);
        assertThat(response.getBody().getRecordsSkipped()).isEqualTo(0);
        assertThat(response.getBody().getRootHash()).isNotBlank();
    }

    @Test
    @Order(4)
    @DisplayName("Should generate inclusion proof for existing key")
    void testInclusionProof() {
        String trieId = createTrieWithData();

        // Generate proof for key that exists
        String existingKey = "0102030405";
        String expectedValue = "aabbccddee";

        ProofGenerationRequest proofRequest = new ProofGenerationRequest(existingKey);

        ResponseEntity<ProofGenerationResponse> response = restTemplate.postForEntity(
                API_BASE + "/tries/" + trieId + "/proofs",
                proofRequest,
                ProofGenerationResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getKey()).isEqualTo(existingKey);
        assertThat(response.getBody().getValue()).isEqualTo(expectedValue);
        assertThat(response.getBody().getProof()).isNotBlank();
        assertThat(response.getBody().getRootHash()).isNotBlank();
        assertThat(response.getBody().getProofFormat()).isEqualTo("wire");
    }

    @Test
    @Order(5)
    @DisplayName("Should verify inclusion proof successfully")
    void testVerifyInclusionProof() {
        String trieId = createTrieWithData();

        // First generate the proof
        String existingKey = "0102030405";
        String expectedValue = "aabbccddee";

        ProofGenerationRequest generateRequest = new ProofGenerationRequest(existingKey);

        ResponseEntity<ProofGenerationResponse> generateResponse = restTemplate.postForEntity(
                API_BASE + "/tries/" + trieId + "/proofs",
                generateRequest,
                ProofGenerationResponse.class
        );

        assertThat(generateResponse.getBody()).isNotNull();
        String proof = generateResponse.getBody().getProof();
        String rootHash = generateResponse.getBody().getRootHash();

        // Now verify the proof
        ProofVerificationRequest verifyRequest = new ProofVerificationRequest(
                existingKey, expectedValue, proof, rootHash);

        ResponseEntity<ProofVerificationResponse> verifyResponse = restTemplate.postForEntity(
                API_BASE + "/tries/" + trieId + "/proofs/verify",
                verifyRequest,
                ProofVerificationResponse.class
        );

        assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(verifyResponse.getBody()).isNotNull();
        assertThat(verifyResponse.getBody().getVerified()).isTrue();
    }

    @Test
    @Order(6)
    @DisplayName("Should generate exclusion proof for non-existing key")
    void testExclusionProof() {
        String trieId = createTrieWithData();

        // Generate proof for key that doesn't exist
        String nonExistingKey = "ffffffffffff";

        ProofGenerationRequest proofRequest = new ProofGenerationRequest(nonExistingKey);

        ResponseEntity<ProofGenerationResponse> response = restTemplate.postForEntity(
                API_BASE + "/tries/" + trieId + "/proofs",
                proofRequest,
                ProofGenerationResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getKey()).isEqualTo(nonExistingKey);
        assertThat(response.getBody().getValue()).isNull(); // Key doesn't exist
        assertThat(response.getBody().getProof()).isNotBlank();
        assertThat(response.getBody().getRootHash()).isNotBlank();
    }

    @Test
    @Order(7)
    @DisplayName("Should verify exclusion proof successfully")
    void testVerifyExclusionProof() {
        String trieId = createTrieWithData();

        // Generate exclusion proof
        String nonExistingKey = "ffffffffffff";

        ProofGenerationRequest generateRequest = new ProofGenerationRequest(nonExistingKey);

        ResponseEntity<ProofGenerationResponse> generateResponse = restTemplate.postForEntity(
                API_BASE + "/tries/" + trieId + "/proofs",
                generateRequest,
                ProofGenerationResponse.class
        );

        assertThat(generateResponse.getBody()).isNotNull();
        String proof = generateResponse.getBody().getProof();
        String rootHash = generateResponse.getBody().getRootHash();

        // Verify exclusion (null value means proving non-existence)
        ProofVerificationRequest verifyRequest = new ProofVerificationRequest();
        verifyRequest.setKey(nonExistingKey);
        verifyRequest.setProof(proof);
        verifyRequest.setRootHash(rootHash);
        // Note: value is null for exclusion proof

        ResponseEntity<ProofVerificationResponse> verifyResponse = restTemplate.postForEntity(
                API_BASE + "/tries/" + trieId + "/proofs/verify",
                verifyRequest,
                ProofVerificationResponse.class
        );

        assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(verifyResponse.getBody()).isNotNull();
        assertThat(verifyResponse.getBody().getVerified()).isTrue();
    }

    @Test
    @Order(8)
    @DisplayName("Should generate batch proofs")
    void testBatchProofs() {
        String trieId = createTrieWithData();

        List<ProofGenerationRequest> requests = List.of(
                new ProofGenerationRequest("0102030405"),
                new ProofGenerationRequest("0506070809"),
                new ProofGenerationRequest("ffffffffffff") // non-existing
        );

        ResponseEntity<List<ProofGenerationResponse>> response = restTemplate.exchange(
                API_BASE + "/tries/" + trieId + "/proofs/batch",
                HttpMethod.POST,
                new HttpEntity<>(requests),
                new ParameterizedTypeReference<List<ProofGenerationResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(3);

        // Verify first key exists
        assertThat(response.getBody().get(0).getValue()).isEqualTo("aabbccddee");
        // Verify second key exists
        assertThat(response.getBody().get(1).getValue()).isEqualTo("ffeeddccbb");
        // Verify third key doesn't exist
        assertThat(response.getBody().get(2).getValue()).isNull();
    }

    @Test
    @Order(9)
    @DisplayName("Should get root hash")
    void testGetRootHash() {
        String trieId = createTrieWithData();

        ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                API_BASE + "/tries/" + trieId + "/root",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, String>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("trieIdentifier");
        assertThat(response.getBody()).containsKey("rootHash");
        assertThat(response.getBody().get("trieIdentifier")).isEqualTo(trieId);
        assertThat(response.getBody().get("rootHash")).isNotBlank();
    }

    @Test
    @Order(10)
    @DisplayName("Should delete trie")
    void testDeleteTrie() {
        String trieId = createTrie();

        // Delete the trie
        restTemplate.delete(API_BASE + "/tries/" + trieId);

        // Verify it's gone (should return 404)
        ResponseEntity<TrieResponse> response = restTemplate.getForEntity(
                API_BASE + "/tries/" + trieId,
                TrieResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(11)
    @DisplayName("Should return 404 for non-existing trie")
    void testTrieNotFound() {
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                API_BASE + "/tries/non-existing-trie-12345",
                ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(12)
    @DisplayName("Should return 409 for duplicate trie")
    void testDuplicateTrie() {
        String trieId = createTrie();

        CreateTrieRequest request = new CreateTrieRequest();
        request.setIdentifier(trieId);

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                API_BASE + "/tries",
                request,
                ErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @Order(13)
    @DisplayName("Should add entries directly to trie")
    void testAddEntriesDirectly() {
        String trieId = createTrie();

        // Create entries request
        AddEntriesRequest request = new AddEntriesRequest();
        request.setEntries(List.of(
                new EntryItem("0x1234abcd", "0xdeadbeef"),
                new EntryItem("5678ef01", "cafebabe"),  // Without 0x prefix
                new EntryItem("0xAABBCCDD", "0x11223344")  // Uppercase hex
        ));

        ResponseEntity<AddEntriesResponse> response = restTemplate.postForEntity(
                API_BASE + "/tries/" + trieId + "/entries",
                request,
                AddEntriesResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTrieIdentifier()).isEqualTo(trieId);
        assertThat(response.getBody().getEntriesAdded()).isEqualTo(3);
        assertThat(response.getBody().getEntriesSkipped()).isEqualTo(0);
        assertThat(response.getBody().getRootHash()).isNotBlank();
        assertThat(response.getBody().getErrors()).isNull();
    }

    @Test
    @Order(14)
    @DisplayName("Should generate and verify proof for directly added entries")
    void testProofForDirectlyAddedEntries() {
        String trieId = createTrie();

        // Add entries directly
        AddEntriesRequest addRequest = new AddEntriesRequest();
        addRequest.setEntries(List.of(
                new EntryItem("0xaabbccdd", "0x11223344")
        ));

        restTemplate.postForEntity(
                API_BASE + "/tries/" + trieId + "/entries",
                addRequest,
                AddEntriesResponse.class
        );

        // Generate proof for the added key
        ProofGenerationRequest proofRequest = new ProofGenerationRequest("aabbccdd");

        ResponseEntity<ProofGenerationResponse> proofResponse = restTemplate.postForEntity(
                API_BASE + "/tries/" + trieId + "/proofs",
                proofRequest,
                ProofGenerationResponse.class
        );

        assertThat(proofResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(proofResponse.getBody()).isNotNull();
        assertThat(proofResponse.getBody().getKey()).isEqualTo("aabbccdd");
        assertThat(proofResponse.getBody().getValue()).isEqualTo("11223344");
        assertThat(proofResponse.getBody().getProof()).isNotBlank();

        // Verify the proof
        String proof = proofResponse.getBody().getProof();
        String rootHash = proofResponse.getBody().getRootHash();

        ProofVerificationRequest verifyRequest = new ProofVerificationRequest(
                "aabbccdd", "11223344", proof, rootHash);

        ResponseEntity<ProofVerificationResponse> verifyResponse = restTemplate.postForEntity(
                API_BASE + "/tries/" + trieId + "/proofs/verify",
                verifyRequest,
                ProofVerificationResponse.class
        );

        assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(verifyResponse.getBody()).isNotNull();
        assertThat(verifyResponse.getBody().getVerified()).isTrue();
    }

    @Test
    @Order(15)
    @DisplayName("Should verify proof fails with wrong value")
    void testVerifyProofFailsWithWrongValue() {
        String trieId = createTrieWithData();

        // Generate proof
        String existingKey = "0102030405";

        ProofGenerationRequest generateRequest = new ProofGenerationRequest(existingKey);

        ResponseEntity<ProofGenerationResponse> generateResponse = restTemplate.postForEntity(
                API_BASE + "/tries/" + trieId + "/proofs",
                generateRequest,
                ProofGenerationResponse.class
        );

        assertThat(generateResponse.getBody()).isNotNull();
        String proof = generateResponse.getBody().getProof();
        String rootHash = generateResponse.getBody().getRootHash();

        // Try to verify with wrong value
        ProofVerificationRequest verifyRequest = new ProofVerificationRequest(
                existingKey, "deadbeef", proof, rootHash); // Wrong value

        ResponseEntity<ProofVerificationResponse> verifyResponse = restTemplate.postForEntity(
                API_BASE + "/tries/" + trieId + "/proofs/verify",
                verifyRequest,
                ProofVerificationResponse.class
        );

        assertThat(verifyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(verifyResponse.getBody()).isNotNull();
        assertThat(verifyResponse.getBody().getVerified()).isFalse();
    }

    // Helper methods

    private String generateTrieId() {
        return "test-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String createTrie() {
        String trieId = generateTrieId();

        CreateTrieRequest request = new CreateTrieRequest();
        request.setIdentifier(trieId);
        request.setTrieType("mpf");

        ResponseEntity<TrieResponse> response = restTemplate.postForEntity(
                API_BASE + "/tries",
                request,
                TrieResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return trieId;
    }

    private String createTrieWithData() {
        String trieId = createTrie();

        // Set up test data
        testDataProvider.addTestData(TestDataItem.of("0102030405", "aabbccddee"));
        testDataProvider.addTestData(TestDataItem.of("0506070809", "ffeeddccbb"));
        testDataProvider.addTestData(TestDataItem.of("0a0b0c0d0e", "1122334455"));

        IngestRequest ingestRequest = new IngestRequest();
        ingestRequest.setProvider(TestDataProvider.PROVIDER_NAME);

        ResponseEntity<IngestResponse> response = restTemplate.postForEntity(
                API_BASE + "/tries/" + trieId + "/ingest",
                ingestRequest,
                IngestResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        return trieId;
    }
}

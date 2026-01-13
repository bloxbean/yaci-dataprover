# Yaci Proof Server - Design Document

## 1. Overview

Yaci Proof Server is a multi-purpose cryptographic proof server with pluggable trie implementations designed for Cardano and other blockchain use cases.

### Key Features

- **Multiple Independent Tries** - Support multiple trie instances in a single deployment
- **Pluggable Trie Implementations** - MPF (Merkle Patricia Forestry) default, extensible for other types
- **Data Provider Pattern** - Flexible data ingestion from various sources via plugins
- **RESTful API** - Complete API for trie and proof operations
- **CBOR Serialization** - On-chain Cardano compatibility
- **RocksDB Persistence** - High-performance storage with column family isolation

### Use Cases

- Epoch stake distribution proofs for Cardano
- Generic key-value merkle proofs
- On-chain verification of off-chain data

---

## 2. Architecture

### High-Level System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      REST API Layer                          │
│  ┌─────────────┐ ┌───────────────┐ ┌───────────────────────┐│
│  │TrieController│ │ProofController│ │IngestionController   ││
│  └─────────────┘ └───────────────┘ └───────────────────────┘│
│                    ┌────────────────┐                        │
│                    │AdminController │                        │
│                    └────────────────┘                        │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│                      Service Layer                           │
│  ┌───────────────────┐ ┌─────────────┐ ┌──────────────────┐ │
│  │TrieManagementSvc  │ │ ProofService│ │IngestionService  │ │
│  └───────────────────┘ └─────────────┘ └──────────────────┘ │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│              Trie Abstraction Layer                          │
│  ┌────────────┐  ┌───────────┐  ┌─────────────────────────┐ │
│  │TrieRegistry│  │TrieFactory│  │DataProviderRegistry     │ │
│  └────────────┘  └───────────┘  └─────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────┐│
│  │         TrieImplementation (MPF, future: JMT)           ││
│  └─────────────────────────────────────────────────────────┘│
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│                    Storage Layer                             │
│  ┌─────────────────────────┐  ┌───────────────────────────┐ │
│  │       RocksDB           │  │      PostgreSQL/H2        │ │
│  │   (Trie Nodes)          │  │      (Metadata)           │ │
│  └─────────────────────────┘  └───────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Plugin Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      proof-server-app                        │
│  ┌─────────────┐    ┌──────────────┐    ┌────────────────┐  │
│  │PluginLoader │───▶│ServiceLoader │───▶│DataProvider<T> │  │
│  └─────────────┘    └──────────────┘    └────────────────┘  │
│         │                                        ▲           │
│         ▼                                        │           │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ plugins/                                                 ││
│  │   ├── epoch-stake-provider.jar ──────────────────────┐  ││
│  │   ├── csv-provider.jar ──────────────────────────────┤  ││
│  │   └── custom-provider.jar ───────────────────────────┘  ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### Data Flow

1. **Trie Creation**: Client creates trie via REST → Service persists metadata → RocksDB column family created
2. **Data Ingestion**: Client triggers ingestion → Provider fetches data → Service serializes and inserts into trie
3. **Proof Generation**: Client requests proof → Service generates merkle proof from trie → Returns hex/CBOR encoded proof
4. **Proof Verification**: Client submits proof + root hash → Service verifies against trie → Returns verification result

---

## 3. Module Structure

```
yaci-proof-server/
├── core/                          # Core library
│   └── src/main/java/.../
│       ├── controller/            # REST controllers
│       ├── service/               # Business logic
│       │   ├── trie/              # Trie abstractions
│       │   ├── provider/          # Data provider abstractions
│       │   └── storage/           # RocksDB management
│       ├── dto/                   # Request/Response DTOs
│       ├── model/                 # Domain models & entities
│       ├── repository/            # JPA repositories
│       ├── config/                # Configuration classes
│       ├── exception/             # Custom exceptions
│       └── util/                  # Utilities
│
├── spring-boot-starter/           # Auto-configuration
│   └── src/main/java/.../
│       └── autoconfigure/         # Spring Boot auto-config
│
├── app/                           # Application entry point
│   └── src/main/java/.../
│       ├── ProofServerApplication.java
│       └── PluginLoader.java
│
└── providers/
    └── epoch-stake-provider/      # Example provider plugin
        └── src/main/java/.../
            ├── EpochStakeDataProvider.java
            ├── EpochStake.java
            ├── AddressConverter.java
            └── CborSerializer.java
```

### Module Responsibilities

| Module | Purpose |
|--------|---------|
| `core` | Core library with controllers, services, DTOs, and trie implementations |
| `spring-boot-starter` | Auto-configuration for embedding in other Spring Boot apps |
| `app` | Standalone application with plugin loading |
| `providers/*` | Data provider plugins (fat JARs) |

---

## 4. Core Interfaces

### DataProvider<T>

Abstraction for data ingestion from external sources.

```java
public interface DataProvider<T> {
    String getName();                                    // Unique identifier
    String getDescription();                             // Human-readable description
    void initialize(Map<String, Object> config);         // Initialize with config
    List<T> fetchData(Map<String, Object> fetchConfig);  // Fetch data from source
    byte[] serializeKey(T data);                         // Serialize to key bytes
    byte[] serializeValue(T data);                       // Serialize to value bytes
    ValidationResult validate(T data);                   // Validate data item
    Class<T> getDataType();                              // Data type metadata
}
```

**Location:** `core/src/main/java/.../service/provider/DataProvider.java`

### TrieImplementation

Interface for trie operations.

```java
public interface TrieImplementation {
    String getType();                                     // Trie type (e.g., "mpf")
    void put(byte[] key, byte[] value);                   // Add/update entry
    Optional<byte[]> get(byte[] key);                     // Retrieve value
    Optional<byte[]> getProofWire(byte[] key);            // Generate proof
    byte[] getRootHash();                                 // Get merkle root
    boolean verifyProofWire(byte[] key, byte[] value,
                           byte[] proof, byte[] rootHash); // Verify proof
    long size();                                          // Entry count
    void commit();                                        // Persist changes
    void close();                                         // Cleanup
}
```

**Location:** `core/src/main/java/.../service/trie/TrieImplementation.java`

### TrieFactory

Factory for creating trie instances.

```java
public interface TrieFactory {
    TrieImplementation createTrie(String type, TrieConfiguration config);
    Set<String> getSupportedTypes();
}
```

**Location:** `core/src/main/java/.../service/trie/TrieFactory.java`

### TrieRegistry

LRU cache for active trie instances.

- Configurable max cache size (default: 50)
- TTL-based expiration (default: 60 minutes)
- Thread-safe with ReadWriteLock
- Tracks cache statistics (hits, misses, hit rate)

**Location:** `core/src/main/java/.../service/trie/TrieRegistry.java`

---

## 5. REST API

**Base URL:** `/api/v1`

### Trie Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/tries` | Create new trie |
| GET | `/tries/{identifier}` | Get trie details |
| GET | `/tries` | List all tries (paginated) |
| DELETE | `/tries/{identifier}` | Delete trie |

**Create Trie Request:**
```json
{
  "identifier": "epoch-500-stakes",
  "trieType": "mpf",
  "description": "Epoch 500 stake distribution",
  "metadata": { "epoch": 500 }
}
```

### Data Ingestion

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/tries/{trieId}/ingest` | Ingest data via provider |
| POST | `/tries/{trieId}/entries` | Add entries directly |

**Ingest Request:**
```json
{
  "provider": "epoch-stake",
  "config": { "epoch": 500 }
}
```

**Add Entries Request:**
```json
{
  "entries": [
    { "key": "0x1234abcd", "value": "0xdeadbeef" },
    { "key": "0x5678ef01", "value": "0xcafebabe" }
  ]
}
```

### Proof Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/tries/{trieId}/proofs` | Generate single proof |
| POST | `/tries/{trieId}/proofs/batch` | Generate batch proofs |
| POST | `/tries/{trieId}/proofs/verify` | Verify proof |
| GET | `/tries/{trieId}/root` | Get root hash |

**Proof Generation Request:**
```json
{
  "key": "0x1234abcd",
  "format": "wire"
}
```

**Proof Generation Response:**
```json
{
  "key": "0x1234abcd",
  "value": "0xdeadbeef",
  "proof": "0x...",
  "rootHash": "0x7a8b9c...",
  "proofFormat": "wire"
}
```

### Admin

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/admin/health` | Health check |
| GET | `/admin/stats` | System statistics |
| GET | `/admin/cache` | Cache information |
| GET | `/admin/storage` | Storage info |
| DELETE | `/admin/cache/evict/{trieId}` | Evict from cache |
| DELETE | `/admin/cache/clear` | Clear all cache |

---

## 6. Data Models

### Request DTOs

| DTO | Purpose |
|-----|---------|
| `CreateTrieRequest` | Create new trie (identifier, type, description, metadata) |
| `IngestRequest` | Trigger provider ingestion (provider name, config) |
| `ProofGenerationRequest` | Request proof (key, format) |
| `ProofVerificationRequest` | Verify proof (key, proof, value, rootHash) |
| `AddEntriesRequest` | Add entries directly (list of EntryItem) |

### Response DTOs

| DTO | Purpose |
|-----|---------|
| `TrieResponse` | Trie metadata (identifier, type, status, rootHash, recordCount) |
| `IngestResponse` | Ingestion result (recordsProcessed, rootHash, durationMs) |
| `ProofGenerationResponse` | Generated proof (key, value, proof, rootHash) |
| `AddEntriesResponse` | Entry addition result (entriesAdded, rootHash) |

### Entity: TrieMetadata

| Field | Type | Description |
|-------|------|-------------|
| identifier | String | Primary key (3-64 chars) |
| trieType | String | Trie type (mpf, jmt) |
| rootHash | String | Current merkle root |
| recordCount | Integer | Number of entries |
| status | Enum | ACTIVE, ARCHIVED, DELETED |
| metadata | JSON | Custom metadata |
| createdAt | Instant | Creation timestamp |
| lastUpdated | Instant | Last update timestamp |
| version | Long | Optimistic locking |

**Location:** `core/src/main/java/.../model/TrieMetadata.java`

---

## 7. Storage

### RocksDB (Trie Nodes)

- **Purpose:** High-performance key-value storage for trie nodes
- **Organization:** One column family per trie for isolation
- **System Column Families:**
  - `default` - Default column family
  - `roots` - Stores root hashes by trie identifier

**Configuration:**
- Block cache with LRU eviction
- Bloom filters for key lookups
- LZ4 compression (configurable)
- Level-based compaction

**Location:** `core/src/main/java/.../service/storage/RocksDbManager.java`

### PostgreSQL/H2 (Metadata)

- **Purpose:** Trie metadata persistence
- **Tables:** `trie_metadata`
- **Migrations:** Flyway (`classpath:db/migration`)

---

## 8. Plugin System

### ServiceLoader (SPI) Mechanism

Providers are discovered using Java's standard ServiceLoader.

**Plugin JAR Structure:**
```
epoch-stake-provider.jar
├── com/bloxbean/.../
│   ├── EpochStakeDataProvider.class
│   └── ...
└── META-INF/
    └── services/
        └── com.bloxbean.cardano.proofserver.service.provider.DataProvider
```

**Service File Contents:**
```
com.bloxbean.cardano.proofserver.providers.epochstake.EpochStakeDataProvider
```

### Plugin Loading Flow

1. Application starts
2. `PluginLoader` scans `${proof-server.plugins.path}` (default: `./plugins`)
3. For each JAR:
   - Create `URLClassLoader`
   - Load `DataProvider` implementations via `ServiceLoader`
   - Call `provider.initialize(config)` with provider-specific config
   - Register with `DataProviderRegistry`
4. Providers available via `/api/v1/tries/{id}/ingest`

**Location:** `app/src/main/java/.../PluginLoader.java`

### Example: EpochStakeDataProvider

Fetches Cardano epoch stake distribution from Yaci Store database.

**Configuration:**
```yaml
proof-server:
  plugins:
    providers:
      epoch-stake:
        jdbc-url: jdbc:postgresql://localhost:5432/yaci
        username: yaci
        password: yaci
```

**Key Serialization:** Stake address → Credential hash (28 bytes)
**Value Serialization:** Stake amount + Pool ID → CBOR format

**Location:** `providers/epoch-stake-provider/src/main/java/.../EpochStakeDataProvider.java`

---

## 9. Configuration

### Application Configuration

```yaml
proof-server:
  # RocksDB Storage
  storage:
    rocksdb-path: ./data/rocksdb      # Storage location
    cache-size-mb: 512                # Block cache size
    write-buffer-size-mb: 128         # Write buffer size
    compression: LZ4                  # NONE, SNAPPY, ZLIB, LZ4, LZ4HC, ZSTD
    max-open-files: 1000              # Max file handles
    create-if-missing: true           # Auto-create DB

  # Trie Cache
  cache:
    max-active-tries: 50              # Max tries in memory
    eviction-policy: LRU              # Cache eviction strategy
    ttl-minutes: 60                   # Cache TTL

  # Retention
  retention:
    max-tries: 100                    # Maximum tries
    archive-policy: oldest-first      # Auto-archive policy
    auto-archive-threshold: 90        # Archive at X% capacity

  # Default trie type
  default-trie-type: mpf

  # Plugins
  plugins:
    path: ./plugins                   # Plugin directory
    providers:
      epoch-stake:                    # Provider-specific config
        jdbc-url: jdbc:postgresql://...
        username: ${EPOCH_STAKE_DB_USER}
        password: ${EPOCH_STAKE_DB_PASSWORD}

# Spring Configuration
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:5432/${DB_NAME:proofserver}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

### Configuration Class

**Location:** `core/src/main/java/.../config/TrieServerProperties.java`
**Prefix:** `proof-server`

---

## 10. Technology Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| Java | 21+ | Virtual threads support |
| Spring Boot | 3.4.x | Framework |
| Spring Data JPA | - | Data access |
| Cardano Client Library | 0.8.0-SNAPSHOT | MPF trie implementation |
| RocksDB | 9.8.4 | Key-value storage |
| PostgreSQL | 42.7.4 | Metadata storage |
| H2 | - | Development/testing |
| Flyway | 11.1.0 | Database migrations |
| CBOR | 0.9 | Binary serialization |
| Springdoc OpenAPI | 2.7.0 | API documentation |
| Gradle | 9.1.0 | Build system |

---

## 11. Building & Running

### Prerequisites

- Java 21+
- PostgreSQL 12+ (production) or H2 (development)

### Build

```bash
./gradlew clean build
```

### Run

**Development (H2):**
```bash
./gradlew :app:bootRun --args='--spring.profiles.active=h2'
```

**Production (PostgreSQL):**
```bash
./gradlew :app:bootRun
```

### With Plugins

```bash
# Build provider plugin
./gradlew :providers:epoch-stake-provider:shadowJar

# Copy to plugins directory
mkdir -p plugins
cp providers/epoch-stake-provider/build/libs/epoch-stake-provider.jar plugins/

# Run application
./gradlew :app:bootRun
```

### Endpoints

- **API:** `http://localhost:8080/api/v1`
- **Health:** `http://localhost:8080/actuator/health`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`

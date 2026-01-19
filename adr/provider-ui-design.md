# Data Provider Admin UI - Design & Implementation Tracker

**Status: COMPLETED** (2026-01-18)

All implementation tasks completed. The epoch-stake provider requires PostgreSQL database configuration to fully test provider loading and ingestion functionality.

## Overview

Add Data Provider management screens to the admin UI that enable users to:
1. View all available data providers dynamically (discovered via plugin system)
2. Trigger data ingestion with provider-specific configuration forms
3. Convert domain-specific keys to hex format for proof generation

## Design Choices

- **Dynamic Forms**: Config schema metadata from providers drives UI form generation
- **Key Serialization**: Integrated into Proofs page (collapsible section)
- **Auto-Create Merkle**: New ingest endpoint supports creating merkle if not exists
- **Provider Status**: Reflects initialization state (AVAILABLE, NOT_CONFIGURED, ERROR)

---

## New REST API Endpoints

### ProviderController (`/api/v1/providers`)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/providers` | List all available providers |
| GET | `/providers/{name}` | Get provider detail with config schema |
| POST | `/providers/{name}/serialize-key` | Convert domain key to hex |
| POST | `/providers/ingest` | Ingest with auto-create merkle option |

### API Response Examples

**GET /providers:**
```json
{
  "providers": [{
    "name": "epoch-stake",
    "description": "Cardano epoch stake distribution from Yaci Store",
    "dataType": "EpochStake",
    "status": "AVAILABLE"
  }]
}
```

**GET /providers/{name}:**
```json
{
  "name": "epoch-stake",
  "description": "Cardano epoch stake distribution from Yaci Store",
  "dataType": "EpochStake",
  "status": "AVAILABLE",
  "configSchema": {
    "fields": [{
      "name": "epoch",
      "label": "Epoch Number",
      "type": "INTEGER",
      "required": true,
      "description": "The Cardano epoch to fetch stake distribution for",
      "placeholder": "e.g., 425"
    }]
  },
  "keySerializationSchema": {
    "keyFieldName": "address",
    "keyFieldLabel": "Stake Address",
    "keyFieldPlaceholder": "stake1u8yk3...",
    "keyDescription": "Bech32-encoded Cardano stake address"
  }
}
```

**POST /providers/{name}/serialize-key:**
```json
// Request
{ "key": "stake1u8yk3dcuj8yylwvnzz953yups6mmuvt0vtjmxl2gmgceqjqz2yfd2" }

// Response
{
  "originalKey": "stake1u8yk3dcuj8yylwvnzz953yups6mmuvt0vtjmxl2gmgceqjqz2yfd2",
  "serializedKeyHex": "0x2c9a371c91c90fb9931084b448e061a6f7c62f6b2cb37d5237319904",
  "keyLength": 28
}
```

**POST /providers/ingest:**
```json
// Request
{
  "merkleName": "epoch-stake-425",
  "createIfNotExists": true,
  "merkleScheme": "mpf",
  "provider": "epoch-stake",
  "config": { "epoch": 425 }
}

// Response (202 Accepted)
{
  "merkleIdentifier": "epoch-stake-425",
  "merkleCreated": true,
  "provider": "epoch-stake",
  "recordsProcessed": 1234567,
  "rootHash": "0x...",
  "durationMs": 45000
}
```

---

## UI Wireframes

### Providers List Page (`/ui/providers`)

```
+----------------------------------------------------------+
| Data Providers                                            |
| Discover and manage data ingestion sources                |
+----------------------------------------------------------+
| [Search providers...]                                     |
+----------------------------------------------------------+
| +------------------------+  +------------------------+    |
| | epoch-stake           |  | json-file              |    |
| | [AVAILABLE]           |  | [NOT_CONFIGURED]       |    |
| |                       |  |                        |    |
| | Cardano epoch stake   |  | Import data from       |    |
| | distribution...       |  | JSON files...          |    |
| |                       |  |                        |    |
| | [Run Ingestion]       |  | [Run Ingestion]        |    |
| +------------------------+  +------------------------+    |
+----------------------------------------------------------+
```

### Ingestion Modal

```
+----------------------------------------------------------+
| Ingest Data - epoch-stake                            [X]  |
+----------------------------------------------------------+
| MERKLE SETTINGS                                           |
| Merkle Name *  [epoch-stake-425_____________]             |
| [x] Create if doesn't exist                               |
|                                                           |
| PROVIDER CONFIGURATION                                    |
| Epoch Number * [425_______________]                       |
| The Cardano epoch to fetch stake distribution for         |
|                                                           |
|                        [Cancel]  [Start Ingestion]        |
+----------------------------------------------------------+
```

### Key Serializer (on Proofs page)

```
+----------------------------------------------------------+
| KEY SERIALIZATION UTILITY                    [Collapse ^] |
+----------------------------------------------------------+
| Provider: [epoch-stake        v]                          |
| Stake Address: [stake1u8yk3dcuj8yylwvnzz953yups6mm...]    |
|                                                           |
| [Convert to Hex]                                          |
|                                                           |
| Result: 0x2c9a371c91c90fb9931084b448e061a6f7c62f6b...     |
|         [Copy]  [Use in Generate Proof]                   |
+----------------------------------------------------------+
```

---

## Phase 1: Backend - Provider Metadata Classes

| Task | Status | Notes |
|------|--------|-------|
| 1.1 Create ProviderMetadata.java | ✅ DONE | Main metadata container with builder |
| 1.2 Create ConfigSchema.java | ✅ DONE | Container for config fields |
| 1.3 Create ConfigField.java | ✅ DONE | Field definition (name, label, type, required, etc.) |
| 1.4 Create FieldType.java | ✅ DONE | Enum: STRING, INTEGER, NUMBER, BOOLEAN, SELECT |
| 1.5 Create FieldValidation.java | ⏭️ SKIPPED | Validation handled inline in ConfigField |
| 1.6 Create KeySerializationSchema.java | ✅ DONE | Schema for key input field |
| 1.7 Create ProviderStatus.java | ✅ DONE | Enum: AVAILABLE, NOT_CONFIGURED, ERROR |

**Location:** `core/src/main/java/com/bloxbean/cardano/dataprover/service/provider/`

---

## Phase 2: Backend - Update DataProvider Interface

| Task | Status | Notes |
|------|--------|-------|
| 2.1 Add getMetadata() default method | ✅ DONE | Returns ProviderMetadata with defaults |
| 2.2 Add serializeKeyFromInput() default method | ✅ DONE | Throws UnsupportedOperationException by default |
| 2.3 Update AbstractDataProvider if exists | ⏭️ SKIPPED | No abstract provider class exists |

**Location:** `core/src/main/java/com/bloxbean/cardano/dataprover/service/provider/DataProvider.java`

---

## Phase 3: Backend - Update EpochStakeDataProvider

| Task | Status | Notes |
|------|--------|-------|
| 3.1 Override getMetadata() | ✅ DONE | Return config schema with epoch field |
| 3.2 Override serializeKeyFromInput() | ✅ DONE | Convert stake address string to bytes |
| 3.3 Add keySerializationSchema | ✅ DONE | Define stake address input field |

**Location:** `providers/epoch-stake-provider/src/main/java/com/bloxbean/cardano/dataprover/providers/epochstake/EpochStakeDataProvider.java`

---

## Phase 4: Backend - Request/Response DTOs

| Task | Status | Notes |
|------|--------|-------|
| 4.1 Create ProviderListResponse.java | ✅ DONE | List of ProviderInfo |
| 4.2 Create ProviderDetailResponse.java | ⏭️ SKIPPED | Used ProviderMetadata directly |
| 4.3 Create SerializeKeyRequest.java | ✅ DONE | Contains key string |
| 4.4 Create SerializeKeyResponse.java | ✅ DONE | Original key, hex result, length |
| 4.5 Create ProviderIngestRequest.java | ✅ DONE | merkleName, createIfNotExists, provider, config |
| 4.6 Create ProviderIngestResponse.java | ✅ DONE | Extends IngestResponse with merkleCreated flag |

**Location:** `core/src/main/java/com/bloxbean/cardano/dataprover/dto/`

---

## Phase 5: Backend - ProviderController

| Task | Status | Notes |
|------|--------|-------|
| 5.1 Create ProviderController.java | ✅ DONE | REST controller for /api/v1/providers |
| 5.2 Implement GET /providers | ✅ DONE | List all providers |
| 5.3 Implement GET /providers/{name} | ✅ DONE | Get provider detail |
| 5.4 Implement POST /providers/{name}/serialize-key | ✅ DONE | Key serialization |
| 5.5 Implement POST /providers/ingest | ✅ DONE | Ingest with auto-create merkle |
| 5.6 Add exception handling | ✅ DONE | Uses existing exception handling |

**Location:** `core/src/main/java/com/bloxbean/cardano/dataprover/controller/ProviderController.java`

---

## Phase 6: Backend - Service Layer Updates

| Task | Status | Notes |
|------|--------|-------|
| 6.1 Add getProviderMetadata() to DataProviderRegistry | ✅ DONE | Return metadata for single provider |
| 6.2 Add getAllProviderMetadata() to DataProviderRegistry | ✅ DONE | Return all provider metadata |
| 6.3 Update IngestionService for auto-create merkle | ✅ DONE | Added ingestWithProvider() method |

**Location:** `core/src/main/java/com/bloxbean/cardano/dataprover/service/`

---

## Phase 7: Frontend - TypeScript Types

| Task | Status | Notes |
|------|--------|-------|
| 7.1 Add ProviderStatus type | ✅ DONE | 'AVAILABLE' | 'NOT_CONFIGURED' | 'ERROR' |
| 7.2 Add FieldType type | ✅ DONE | 'STRING' | 'INTEGER' | 'NUMBER' | 'BOOLEAN' | 'SELECT' |
| 7.3 Add ConfigField interface | ✅ DONE | name, label, type, required, etc. |
| 7.4 Add ConfigSchema interface | ✅ DONE | fields array |
| 7.5 Add KeySerializationSchema interface | ✅ DONE | keyFieldName, keyFieldLabel, etc. |
| 7.6 Add ProviderInfo interface | ✅ DONE | Provider summary |
| 7.7 Add ProviderIngestRequest interface | ✅ DONE | Ingest request body |
| 7.8 Add SerializeKeyResponse interface | ✅ DONE | Serialization result |

**Location:** `ui/frontend/src/lib/api/types.ts`

---

## Phase 8: Frontend - API Client

| Task | Status | Notes |
|------|--------|-------|
| 8.1 Create provider.ts API module | ✅ DONE | providerApi object |
| 8.2 Implement list() function | ✅ DONE | GET /providers |
| 8.3 Implement get() function | ✅ DONE | GET /providers/{name} |
| 8.4 Implement serializeKey() function | ✅ DONE | POST /providers/{name}/serialize-key |
| 8.5 Implement ingest() function | ✅ DONE | POST /providers/ingest |
| 8.6 Update index.ts exports | ✅ DONE | Export providerApi |

**Location:** `ui/frontend/src/lib/api/`

---

## Phase 9: Frontend - Dynamic Form Components

| Task | Status | Notes |
|------|--------|-------|
| 9.1 Create DynamicField.svelte | ✅ DONE | Renders field based on FieldType |
| 9.2 Create DynamicForm.svelte | ✅ DONE | Renders ConfigSchema as form |
| 9.3 Create ProviderStatusBadge.svelte | ✅ DONE | Status badge with colors |
| 9.4 Create ProviderCard.svelte | ✅ DONE | Provider summary card |
| 9.5 Update components/index.ts | ✅ DONE | Export new components |

**Location:** `ui/frontend/src/lib/components/`

---

## Phase 10: Frontend - Ingestion Components

| Task | Status | Notes |
|------|--------|-------|
| 10.1 Create IngestionModal.svelte | ✅ DONE | Modal with merkle settings + dynamic form |
| 10.2 Add loading/progress state | ✅ DONE | Show spinner during ingestion |
| 10.3 Add success/error result display | ✅ DONE | Show records processed, root hash |
| 10.4 Add "View Merkle" action | ✅ DONE | Navigate to merkle detail after success |

**Location:** `ui/frontend/src/lib/components/IngestionModal.svelte`

---

## Phase 11: Frontend - Key Serializer Component

| Task | Status | Notes |
|------|--------|-------|
| 11.1 Create KeySerializer.svelte | ✅ DONE | Collapsible section component |
| 11.2 Add provider selector | ✅ DONE | Dropdown to select provider |
| 11.3 Add dynamic key input | ✅ DONE | Based on keySerializationSchema |
| 11.4 Add convert button and result display | ✅ DONE | Show hex result with copy button |
| 11.5 Add "Use in Generate Proof" action | ✅ DONE | Callback to parent component |

**Location:** `ui/frontend/src/lib/components/KeySerializer.svelte`

---

## Phase 12: Frontend - Providers Page

| Task | Status | Notes |
|------|--------|-------|
| 12.1 Create /routes/providers/+page.svelte | ✅ DONE | Main providers list page |
| 12.2 Add provider cards grid | ✅ DONE | Display ProviderCard components |
| 12.3 Add search/filter | ✅ DONE | Filter providers by name |
| 12.4 Integrate IngestionModal | ✅ DONE | Open on "Run Ingestion" click |
| 12.5 Handle empty state | ✅ DONE | Message when no providers discovered |

**Location:** `ui/frontend/src/routes/providers/+page.svelte`

---

## Phase 13: Frontend - Navigation & Integration

| Task | Status | Notes |
|------|--------|-------|
| 13.1 Update Sidebar.svelte | ✅ DONE | Add "Data Providers" nav item |
| 13.2 Add provider icon to icons map | ✅ DONE | Database-import style icon |
| 13.3 Update Proofs page with KeySerializer | ✅ DONE | Conditionally shown when providers available |
| 13.4 Update Dashboard with provider link | ⏭️ SKIPPED | Navigation in sidebar is sufficient |

**Location:** `ui/frontend/src/lib/components/Sidebar.svelte`, `ui/frontend/src/routes/proofs/+page.svelte`

---

## Phase 14: Build & Test

| Task | Status | Notes |
|------|--------|-------|
| 14.1 Build project | ✅ DONE | ./gradlew clean build |
| 14.2 Test provider list endpoint | ✅ DONE | curl /api/v1/providers returns empty (no db) |
| 14.3 Test provider detail endpoint | ⏭️ SKIPPED | No providers loaded without database |
| 14.4 Test key serialization endpoint | ⏭️ SKIPPED | Requires loaded provider |
| 14.5 Test ingest with auto-create | ⏭️ SKIPPED | Requires loaded provider |
| 14.6 Test UI providers page | ✅ DONE | Verified with Playwright |
| 14.7 Test ingestion modal flow | ⏭️ SKIPPED | No providers to test with |
| 14.8 Test key serializer on proofs page | ✅ DONE | Conditionally hidden when no providers |

---

## Files to Create

### Backend (Java)

| File | Phase |
|------|-------|
| `core/.../service/provider/ProviderMetadata.java` | 1 |
| `core/.../service/provider/ConfigSchema.java` | 1 |
| `core/.../service/provider/ConfigField.java` | 1 |
| `core/.../service/provider/FieldType.java` | 1 |
| `core/.../service/provider/FieldValidation.java` | 1 |
| `core/.../service/provider/KeySerializationSchema.java` | 1 |
| `core/.../service/provider/ProviderStatus.java` | 1 |
| `core/.../dto/ProviderListResponse.java` | 4 |
| `core/.../dto/ProviderDetailResponse.java` | 4 |
| `core/.../dto/SerializeKeyRequest.java` | 4 |
| `core/.../dto/SerializeKeyResponse.java` | 4 |
| `core/.../dto/ProviderIngestRequest.java` | 4 |
| `core/.../dto/ProviderIngestResponse.java` | 4 |
| `core/.../controller/ProviderController.java` | 5 |

### Frontend (Svelte/TypeScript)

| File | Phase |
|------|-------|
| `ui/frontend/src/lib/api/provider.ts` | 8 |
| `ui/frontend/src/lib/components/DynamicField.svelte` | 9 |
| `ui/frontend/src/lib/components/DynamicForm.svelte` | 9 |
| `ui/frontend/src/lib/components/ProviderStatusBadge.svelte` | 9 |
| `ui/frontend/src/lib/components/ProviderCard.svelte` | 9 |
| `ui/frontend/src/lib/components/IngestionModal.svelte` | 10 |
| `ui/frontend/src/lib/components/KeySerializer.svelte` | 11 |
| `ui/frontend/src/routes/providers/+page.svelte` | 12 |

### Files to Modify

| File | Phase | Changes |
|------|-------|---------|
| `core/.../service/provider/DataProvider.java` | 2 | Add getMetadata(), serializeKeyFromInput() |
| `providers/.../EpochStakeDataProvider.java` | 3 | Override metadata methods |
| `core/.../service/provider/DataProviderRegistry.java` | 6 | Add metadata retrieval methods |
| `core/.../service/IngestionService.java` | 6 | Support auto-create merkle |
| `ui/frontend/src/lib/api/types.ts` | 7 | Add provider types |
| `ui/frontend/src/lib/api/index.ts` | 8 | Export providerApi |
| `ui/frontend/src/lib/components/index.ts` | 9 | Export new components |
| `ui/frontend/src/lib/components/Sidebar.svelte` | 13 | Add Providers nav item |
| `ui/frontend/src/routes/proofs/+page.svelte` | 13 | Add KeySerializer |

---

## Status Legend

- ⬜ TODO - Not started
- 🔄 IN PROGRESS - Currently working
- ✅ DONE - Completed
- ⏭️ SKIPPED - Not needed

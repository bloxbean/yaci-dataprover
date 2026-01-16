# Admin UI Module - Design & Implementation Tracker

## Overview

Create a professional admin UI for Yaci DataProver using SvelteKit + TailwindCSS, served as an optional module that can be conditionally enabled via `--ui` flag or `dataprover.ui.enabled=true`.

## Design Choices

- **Framework**: SvelteKit with adapter-static
- **Styling**: TailwindCSS with dark theme
- **Base Path**: `/ui`
- **Test Data Format**: Random hex strings (e.g., `0x1a2b3c...`)
- **Default State**: Disabled (must explicitly enable)

## Module Structure

```
yaci-dataprover/
├── ui/                                    # New module
│   ├── build.gradle
│   ├── frontend/                          # SvelteKit project
│   │   ├── package.json
│   │   ├── svelte.config.js
│   │   ├── vite.config.js
│   │   ├── tailwind.config.js
│   │   └── src/
│   │       ├── routes/                    # SvelteKit pages
│   │       └── lib/                       # Components, API, stores
│   └── src/main/
│       ├── java/.../ui/
│       │   ├── UiAutoConfiguration.java
│       │   ├── UiProperties.java
│       │   └── controller/SpaController.java
│       └── resources/
│           ├── META-INF/spring/...imports
│           └── static/                    # Svelte build output
```

---

## Phase 1: Module Setup

| Task | Status | Notes |
|------|--------|-------|
| 1.1 Update settings.gradle | ✅ DONE | Added `include 'ui'` |
| 1.2 Create ui/build.gradle | ✅ DONE | Gradle Node plugin 7.1.0 |
| 1.3 Update app/build.gradle | ✅ DONE | Added ui dependency |

---

## Phase 2: Spring Boot Auto-Configuration

| Task | Status | Notes |
|------|--------|-------|
| 2.1 Create UiProperties.java | ✅ DONE | Config properties |
| 2.2 Create UiAutoConfiguration.java | ✅ DONE | Conditional bean loading |
| 2.3 Create SpaController.java | ✅ DONE | SPA route forwarding |
| 2.4 Create AutoConfiguration.imports | ✅ DONE | Register auto-config |
| 2.5 Update DataProverApplication.java | ✅ DONE | Parse --ui argument |

---

## Phase 3: Frontend Foundation

| Task | Status | Notes |
|------|--------|-------|
| 3.1 Create package.json | ✅ DONE | SvelteKit 2.9 + Svelte 5 + TailwindCSS |
| 3.2 Create svelte.config.js | ✅ DONE | Static adapter, /ui base |
| 3.3 Create vite.config.js | ✅ DONE | API proxy config |
| 3.4 Create tailwind.config.js | ✅ DONE | Dark theme with primary colors |
| 3.5 Create postcss.config.js | ✅ DONE | PostCSS for Tailwind |
| 3.6 Create tsconfig.json | ✅ DONE | TypeScript config |
| 3.7 Create app.html | ✅ DONE | Dark mode HTML template |
| 3.8 Create app.css | ✅ DONE | Global styles with components |

---

## Phase 4: API Client Layer

| Task | Status | Notes |
|------|--------|-------|
| 4.1 Create client.ts | ✅ DONE | Base API functions with error handling |
| 4.2 Create merkle.ts | ✅ DONE | Merkle API calls |
| 4.3 Create proof.ts | ✅ DONE | Proof API calls |
| 4.4 Create admin.ts | ✅ DONE | Admin API calls |
| 4.5 Create types.ts | ✅ DONE | Full TypeScript interfaces |

---

## Phase 5: Layout Components

| Task | Status | Notes |
|------|--------|-------|
| 5.1 Create +layout.svelte | ✅ DONE | Root layout with sidebar |
| 5.2 Create Sidebar.svelte | ✅ DONE | Navigation sidebar with icons |
| 5.3 Create Header.svelte | ✅ DONE | Page header component |

---

## Phase 6: Common Components

| Task | Status | Notes |
|------|--------|-------|
| 6.1 Create Button.svelte | ✅ DONE | Button variants + loading state |
| 6.2 Create Card.svelte | ✅ DONE | Card container |
| 6.3 Create Modal.svelte | ✅ DONE | Modal dialog with backdrop |
| 6.4 Create Input.svelte | ✅ DONE | Form input with validation |
| 6.5 Create Badge.svelte | ✅ DONE | Status badges |
| 6.6 Create Alert.svelte | ✅ DONE | Alert messages |
| 6.7 Create Table.svelte | ✅ DONE | Data table |
| 6.8 Create Pagination.svelte | ✅ DONE | Page navigation |
| 6.9 Create Textarea.svelte | ✅ DONE | Multiline input |
| 6.10 Create Select.svelte | ✅ DONE | Dropdown select |

---

## Phase 7: Dashboard Page

| Task | Status | Notes |
|------|--------|-------|
| 7.1 Create dashboard +page.svelte | ✅ DONE | Stats, health, quick actions, recent merkles |

---

## Phase 8: Merkle Pages

| Task | Status | Notes |
|------|--------|-------|
| 8.1 Create merkle list +page.svelte | ✅ DONE | Paginated table with filters |
| 8.2 Create merkle detail +page.svelte | ✅ DONE | Info, root hash, value lookup |
| 8.3 Add Entries modal | ✅ DONE | Manual entry addition |
| 8.4 Random Data Generator | ✅ DONE | Configurable random hex generation |
| 8.5 Proof generation from detail | ✅ DONE | Integrated in detail page |

---

## Phase 9: Proofs Page

| Task | Status | Notes |
|------|--------|-------|
| 9.1 Create proofs +page.svelte | ✅ DONE | Split view: generate & verify |
| 9.2 Proof generation form | ✅ DONE | With format selection |
| 9.3 Proof verification form | ✅ DONE | With result display |
| 9.4 Use proof for verification | ✅ DONE | Copy generated proof to verify |

---

## Phase 10: Cache Page

| Task | Status | Notes |
|------|--------|-------|
| 10.1 Create cache +page.svelte | ✅ DONE | Stats, cached list, storage info |
| 10.2 Evict/Clear actions | ✅ DONE | Individual and bulk eviction |
| 10.3 Visual cache usage | ✅ DONE | Progress bar and stats |

---

## Phase 11: Build & Test

| Task | Status | Notes |
|------|--------|-------|
| 11.1 Build project | ✅ DONE | ./gradlew clean build -x test succeeded |
| 11.2 Test with --ui flag | ✅ DONE | UI loads at /ui |
| 11.3 Test without --ui flag | ⬜ TODO | Verify UI disabled |
| 11.4 Test all pages | ✅ DONE | All pages tested with Playwright |

---

## Bug Fixes

| Issue | Status | Fix |
|-------|--------|-----|
| Merkle detail page not loading | ✅ FIXED | Updated all links to use SvelteKit `base` import from `$app/paths` instead of hardcoded `/ui` paths |
| NullPointerException in ProofService.getRootHash | ✅ FIXED | Added null check for root hash when merkle tree is empty (`core/.../ProofService.java:171-175`) |
| NullPointerException in ProofController.getRootHash | ✅ FIXED | Changed `Map.of()` to `HashMap` to allow null values (`core/.../ProofController.java:78-83`) |
| Records count always shows 0 | ✅ FIXED | Updated `IngestionService.addEntries()` and `ingestData()` to update metadata record count and root hash (`core/.../IngestionService.java`) |

---

## Playwright Test Results (2026-01-16)

| Feature | Status | Notes |
|---------|--------|-------|
| Dashboard | ✅ PASS | Stats, cache info, recent merkles display correctly |
| Merkle List | ✅ PASS | List, filter, create modal work |
| Merkle Detail | ✅ PASS | Info, root hash, actions display correctly |
| Add Entries (Random Data) | ✅ PASS | Generates entries, root hash updates |
| Proof Generation | ✅ PASS | Generates proofs for any key |
| Proof Verification | ✅ PASS | Verifies proofs correctly |
| Cache Management | ✅ PASS | Shows cache stats, evict functionality |
| Create Merkle | ✅ PASS | Creates new merkle trees |

---

## Activation Methods

```bash
# Option 1: Program argument
./gradlew :app:bootRun --args='--ui'

# Option 2: Property
./gradlew :app:bootRun --args='--dataprover.ui.enabled=true'

# Option 3: application.yml
dataprover:
  ui:
    enabled: true
```

## UI Access

When enabled: `http://localhost:8080/ui`

---

## Status Legend

- ⬜ TODO - Not started
- 🔄 IN PROGRESS - Currently working
- ✅ DONE - Completed
- ⏭️ SKIPPED - Not needed

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Run Commands

```bash
# Build
./gradlew clean build

# Run tests
./gradlew test

# Run a single test class
./gradlew :core:test --tests "com.bloxbean.cardano.dataprover.DataProverIntegrationTest"

# Run a single test method
./gradlew :core:test --tests "com.bloxbean.cardano.dataprover.DataProverIntegrationTest.testMethodName"

# Run with H2 (development)
./gradlew :app:bootRun --args='--spring.profiles.active=h2'

# Run with PostgreSQL (production)
./gradlew :app:bootRun

# Build provider plugin as fat JAR
./gradlew :providers:epoch-stake-provider:shadowJar
```

## Architecture Overview

Yaci DataProver is a multi-purpose data prover with pluggable Merkle implementations for Cardano blockchain. It generates cryptographic proofs for data membership verification.

### Module Structure

- **core/** - Core library with controllers, services, DTOs, and merkle implementations
- **spring-boot-starter/** - Auto-configuration for embedding in other Spring Boot apps
- **app/** - Standalone application with plugin loading
- **providers/** - Data provider plugins (built as fat JARs using Shadow plugin)

### Key Layers

1. **REST API Layer** (`core/controller/`) - MerkleController, ProofController, IngestionController, AdminController
2. **Service Layer** (`core/service/`) - MerkleManagementService, ProofService, IngestionService
3. **Merkle Abstraction** (`core/service/merkle/`) - MerkleRegistry, MerkleFactory, MerkleImplementation interface with MpfMerkleImplementation (Merkle Patricia Forestry)
4. **Storage Layer** - RocksDB for merkle nodes, PostgreSQL/H2 for metadata

### Core Interfaces

- **DataProvider<T>** (`core/service/provider/DataProvider.java`) - Plugin interface for data ingestion. Implement this to create new data sources.
- **MerkleImplementation** (`core/service/merkle/MerkleImplementation.java`) - Interface for merkle operations (put, get, getProofWire, verify, getRootHash)
- **MerkleFactory** (`core/service/merkle/MerkleFactory.java`) - Creates merkle instances for different schemes

### Plugin System

Plugins are discovered via Java ServiceLoader (SPI). Provider JARs must include:
- `META-INF/services/com.bloxbean.cardano.dataprover.service.provider.DataProvider`
- Use `shadowJar` task to build fat JARs excluding core dependencies
- Place JARs in `./plugins` directory (configurable via `dataprover.plugins.path`)

### Technology Stack

- Java 21+ (required for virtual threads)
- Spring Boot 3.4.x
- RocksDB for merkle node storage (column family per merkle)
- PostgreSQL for metadata (H2 for development)
- Flyway for database migrations
- Cardano Client Library for MPF merkle implementation

### API Endpoints

Base URL: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`
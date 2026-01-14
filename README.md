# Yaci DataProver

A multi-purpose data prover with pluggable Merkle implementations for Cardano and other blockchain use cases. Generates cryptographic proofs for data membership/existence verification.

## Features

- **Multiple Independent Merkle Structures** - Support multiple merkle instances in a single deployment
- **Pluggable Merkle Schemes** - MPF (Merkle Patricia Forestry) default, extensible for other types (JMT, etc.)
- **Data Provider Pattern** - Flexible data ingestion from various sources via plugins
- **RESTful API** - Complete API for merkle and proof operations

## Prerequisites

- Java 21+
- PostgreSQL (for production) or H2 (for development)

## Build

```bash
./gradlew clean build
```

## Run

**Development (H2 in-memory database):**

```bash
./gradlew :app:bootRun --args='--spring.profiles.active=h2'
```

**Production (PostgreSQL):**

```bash
./gradlew :app:bootRun
```

## API

- Base URL: `http://localhost:8080/api/v1`
- Health check: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### Key Endpoints

| Endpoint | Description |
|----------|-------------|
| `POST /api/v1/merkle` | Create a new merkle structure |
| `GET /api/v1/merkle/{id}` | Get merkle details |
| `POST /api/v1/merkle/{id}/ingest` | Ingest data via provider |
| `POST /api/v1/merkle/{id}/proofs` | Generate proof |
| `POST /api/v1/merkle/{id}/proofs/verify` | Verify proof |

See [DESIGN.md](adr/DESIGN.md) for detailed documentation.

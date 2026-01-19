# Yaci DataProver

A multi-purpose data prover with pluggable Merkle implementations for Cardano and other blockchain use cases. Generates cryptographic proofs for data membership/existence verification.

## Features

- **Multiple Independent Merkle Structures** - Support multiple merkle instances in a single deployment
- **Pluggable Merkle Schemes** - MPF (Merkle Patricia Forestry) default, extensible for other types (JMT, etc.)
- **Data Provider Pattern** - Flexible data ingestion from various sources via plugins
- **RESTful API** - Complete API for merkle and proof operations
- **Admin UI** - Web interface for managing merkle trees and generating proofs

## Quick Start with Docker (Recommended)

The easiest way to run Yaci DataProver is with Docker. No Java installation required.

### Prerequisites

- Docker and Docker Compose

### Start the Application

```bash
docker compose up -d --build
```

This starts both PostgreSQL and the DataProver application with the Admin UI enabled.

### Access Points

| Service | URL |
|---------|-----|
| **Admin UI** | http://localhost:9090/ui |
| **Swagger API** | http://localhost:9090/swagger-ui.html |
| **Health Check** | http://localhost:9090/api/v1/admin/health |

### View Logs

```bash
docker compose logs -f dataprover
```

### Stop Services

```bash
docker compose down
```

### Start Fresh (Remove Database)

To remove all data and start with a clean database:

```bash
docker compose down -v
rm -rf ./data/rocksdb
docker compose up -d --build
```

## Running with Java

### Prerequisites

- Java 21+
- PostgreSQL (for production) or use H2 (for development)

### Build

```bash
./gradlew clean build
```

### Run with H2 (Development)

For quick development and testing with an in-memory database:

```bash
java -jar app/build/libs/yaci-dataprover-*.jar --spring.profiles.active=h2
```

### Run with H2 and Admin UI

```bash
java -jar app/build/libs/yaci-dataprover-*.jar --spring.profiles.active=h2 --ui
```

### Run with PostgreSQL (Production)

Set environment variables for database connection:

```bash
export DP_DB_HOST=localhost
export DP_DB_PORT=5432
export DP_DB_NAME=dataprover
export DP_DB_USER=postgres
export DP_DB_PASSWORD=postgres

java -jar app/build/libs/yaci-dataprover-*.jar
```

With Admin UI:

```bash
java -jar app/build/libs/yaci-dataprover-*.jar --ui
```

### Run with Gradle (Development)

```bash
# H2 database
./gradlew :app:bootRun --args='--spring.profiles.active=h2'

# H2 with Admin UI
./gradlew :app:bootRun --args='--spring.profiles.active=h2 --ui'

# PostgreSQL (default)
./gradlew :app:bootRun
```

## Admin UI

The Admin UI provides a web interface for managing merkle trees, generating proofs, and monitoring the system.

### Enabling the UI

**With Java:**

```bash
java -jar app/build/libs/yaci-dataprover-*.jar --ui
```

Or set environment variable:

```bash
export DATAPROVER_UI_ENABLED=true
java -jar app/build/libs/yaci-dataprover-*.jar
```

**With Docker:**

The UI is enabled by default in `docker-compose.yml`. To disable, remove or set `DATAPROVER_UI_ENABLED: "false"` in the environment section.

### Access

http://localhost:9090/ui

## Configuration

| Environment Variable         | Description                      | Default          |
|------------------------------|----------------------------------|------------------|
| `DP_DB_HOST`                 | PostgreSQL host                  | `localhost`      |
| `DP_DB_PORT`                 | PostgreSQL port                  | `5432`           |
| `DP_DB_NAME`                 | Database name                    | `dataprover`     |
| `DP_DB_USER`                 | Database username                | `postgres`       |
| `DP_DB_PASSWORD`             | Database password                | `postgres`       |
| `DP_DB_SCHEMA`               | Database schema                  | `public`         |
| `ROCKSDB_PATH`               | Path for RocksDB storage         | `./data/rocksdb` |
| `PLUGINS_PATH`               | Path for plugin JARs             | `./plugins`      |
| `DATAPROVER_UI_ENABLED`      | Enable Admin UI                  | `false`          |
| `DATAPROVER_ENCRYPTION_KEY`  | AES-256 key for sensitive config | (none)           |

### Encryption Key (Optional)

The encryption key is used to encrypt sensitive information in provider configurations, such as database passwords and API keys. This is **optional** - if not set, provider configurations with sensitive fields cannot be saved.

**When do you need it?**

- Only if you're using data providers that require sensitive configuration (e.g., database credentials, API keys)
- Not required for basic merkle operations or providers without sensitive config

**Generate a Key**

Use the built-in key generator to create a secure AES-256 key:

```bash
# With Java
java -jar app/build/libs/yaci-dataprover-*.jar --generate-key

# With Docker
docker run --rm yaci-dataprover:latest --generate-key
```

Output:
```
============================================
AES-256 Encryption Key (base64):
K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv72ol/pe/Unols=

Set this as an environment variable:
  export DATAPROVER_ENCRYPTION_KEY=K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv72ol/pe/Unols=
============================================
```

**Set the Key**

```bash
# Linux/macOS
export DATAPROVER_ENCRYPTION_KEY=<your-generated-key>

# Or in docker-compose.yml
environment:
  DATAPROVER_ENCRYPTION_KEY: "<your-generated-key>"
```

**Important:** Store this key securely. If the key is lost, encrypted provider configurations cannot be decrypted.

## API Reference

- Base URL: `http://localhost:9090/api/v1`
- Health check: `http://localhost:9090/api/v1/admin/health`
- Swagger UI: `http://localhost:9090/swagger-ui.html`

### Key Endpoints

| Endpoint | Description |
|----------|-------------|
| `POST /api/v1/merkle` | Create a new merkle structure |
| `GET /api/v1/merkle/{id}` | Get merkle details |
| `POST /api/v1/merkle/{id}/ingest` | Ingest data via provider |
| `POST /api/v1/merkle/{id}/proofs` | Generate proof |
| `POST /api/v1/merkle/{id}/proofs/verify` | Verify proof |

See [DESIGN.md](adr/DESIGN.md) for detailed documentation.

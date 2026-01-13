# Yaci Proof Server

A multi-purpose cryptographic proof server with pluggable trie implementations for Cardano and other blockchain use cases.

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

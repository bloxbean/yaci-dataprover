# Stage 1: Build the application
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build

# Copy Gradle wrapper and build files
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle settings.gradle gradle.properties ./

# Copy source code
COPY core ./core
COPY app ./app
COPY spring-boot-starter ./spring-boot-starter
COPY ui ./ui
COPY providers ./providers

# Build the application
RUN chmod +x gradlew && ./gradlew :app:bootJar --no-daemon -x test

# Stage 2: Runtime image
FROM eclipse-temurin:25-jre

WORKDIR /app

# Create non-root user for security (using 10001 to avoid conflicts with base image)
RUN groupadd --gid 10001 appuser && \
    useradd --uid 10001 --gid appuser --shell /bin/bash --create-home appuser

# Create directories for data and plugins
RUN mkdir -p /app/data/rocksdb /app/plugins && \
    chown -R appuser:appuser /app

# Copy the built JAR from builder stage
COPY --from=builder --chown=appuser:appuser /build/app/build/libs/yaci-dataprover-*.jar app.jar

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 9090

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:9090/api/v1/admin/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

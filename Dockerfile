# Multi-stage Dockerfile for Veccy Vector Database

# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy pom.xml first for better layer caching
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-jammy

# Install curl for health checks
RUN apt-get update && \
    apt-get install -y curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Create app user for security
RUN groupadd -r veccy && useradd -r -g veccy veccy

# Create directories
RUN mkdir -p /app /data /logs && \
    chown -R veccy:veccy /app /data /logs

WORKDIR /app

# Copy fat jar from builder stage
COPY --from=builder /build/target/veccy-*-fat.jar app.jar

# Copy scripts
COPY docker/entrypoint.sh /app/
RUN chmod +x /app/entrypoint.sh && \
    chown veccy:veccy /app/entrypoint.sh

# Switch to non-root user
USER veccy

# Expose ports
EXPOSE 8080 8081

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/health/live || exit 1

# Set environment variables
ENV JAVA_OPTS="-Xmx2g -Xms512m" \
    VECCY_DATA_DIR="/data" \
    VECCY_LOG_DIR="/logs" \
    VECCY_HEALTH_PORT="8080" \
    VECCY_METRICS_PORT="8081"

# Entry point
ENTRYPOINT ["/app/entrypoint.sh"]

# Default command (can be overridden)
CMD ["server"]

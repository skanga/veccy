# Docker Deployment Guide

This guide covers deploying Veccy vector database using Docker and Docker Compose.

## Table of Contents

- [Quick Start](#quick-start)
- [Docker Images](#docker-images)
- [Docker Compose](#docker-compose)
- [Environment Variables](#environment-variables)
- [Volumes and Persistence](#volumes-and-persistence)
- [Health Checks](#health-checks)
- [Monitoring](#monitoring)
- [Development Setup](#development-setup)
- [Production Deployment](#production-deployment)
- [Troubleshooting](#troubleshooting)

## Quick Start

### Using Docker

```bash
# Build the image
docker build -t veccy:latest .

# Run the container
docker run -d \
  --name veccy \
  -p 8080:8080 \
  -p 8081:8081 \
  -v veccy-data:/data \
  veccy:latest

# Check health
curl http://localhost:8080/health
```

### Using Docker Compose

```bash
# Start Veccy
docker-compose up -d

# View logs
docker-compose logs -f veccy

# Stop Veccy
docker-compose down
```

### With Monitoring (Prometheus + Grafana)

```bash
# Start with monitoring stack
docker-compose --profile monitoring up -d

# Access Grafana
open http://localhost:3000
# Login: admin/admin

# Access Prometheus
open http://localhost:9090
```

## Docker Images

### Production Image

The production Dockerfile uses a multi-stage build for optimization:

**Stage 1: Build**
- Base: `maven:3.9-eclipse-temurin-21`
- Builds the application fat JAR with Maven Shade plugin
- Downloads all dependencies and bundles them into single JAR
- Fat JAR includes all dependencies (~105MB)

**Stage 2: Runtime**
- Base: `eclipse-temurin:21-jre-jammy`
- Minimal runtime environment
- Copies only the fat JAR (no dependency copying needed)
- Runs as non-root user (`veccy`)
- Includes health check

**Image Size**: ~400MB (vs ~800MB single-stage build)
**Fat JAR Benefits**: Single executable, no classpath management, easier deployment

### Development Image

The development Dockerfile includes:
- Hot reload support
- Remote debugging on port 5005
- Maven cache volume
- Source code volume mounting

## Building Images

### Production Build

```bash
# Standard build
docker build -t veccy:latest .

# With build arguments
docker build \
  --build-arg MAVEN_VERSION=3.9 \
  --build-arg JAVA_VERSION=21 \
  -t veccy:v1.0.0 .

# Multi-platform build
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t veccy:latest .
```

### Development Build

```bash
# Build dev image
docker build -f Dockerfile.dev -t veccy:dev .

# Run with source mounting
docker run -it \
  -v $(pwd)/src:/app/src \
  -v $(pwd)/pom.xml:/app/pom.xml \
  -p 8080:8080 \
  -p 5005:5005 \
  veccy:dev
```

## Docker Compose

### Basic Configuration

The main `docker-compose.yml` includes:

- **veccy**: Main application service
- **prometheus**: Metrics collection (optional, with `monitoring` profile)
- **grafana**: Visualization dashboard (optional, with `monitoring` profile)

### Starting Services

```bash
# Start only Veccy
docker-compose up -d

# Start with monitoring
docker-compose --profile monitoring up -d

# Start specific service
docker-compose up -d veccy

# Scale replicas
docker-compose up -d --scale veccy=3
```

### Viewing Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f veccy

# Tail last 100 lines
docker-compose logs --tail=100 veccy
```

### Stopping Services

```bash
# Stop all services
docker-compose stop

# Stop and remove containers
docker-compose down

# Stop and remove containers + volumes (DATA LOSS!)
docker-compose down -v

# Stop and remove containers + images
docker-compose down --rmi all
```

## Environment Variables

### Core Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `JAVA_OPTS` | `-Xmx4g -Xms1g` | JVM options |
| `VECCY_DATA_DIR` | `/data` | Data storage directory |
| `VECCY_LOG_DIR` | `/logs` | Log output directory |
| `VECCY_HEALTH_PORT` | `8080` | Health check port |
| `VECCY_METRICS_PORT` | `8081` | Metrics endpoint port |

### Performance Tuning

```yaml
environment:
  # Memory settings
  - JAVA_OPTS=-Xmx8g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200

  # GC logging
  - JAVA_OPTS=-Xlog:gc*:file=/logs/gc.log:time,uptime:filecount=5,filesize=10M

  # JMX monitoring
  - JAVA_OPTS=-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9010
```

### Index Configuration

```yaml
environment:
  - VECCY_INDEX_TYPE=hnsw
  - VECCY_METRIC=cosine
  - HNSW_M=16
  - HNSW_EF_CONSTRUCTION=200
  - HNSW_EF_SEARCH=50
```

## Volumes and Persistence

### Named Volumes

```yaml
volumes:
  veccy-data:      # Persistent vector data
  veccy-logs:      # Application logs
  prometheus-data: # Prometheus TSDB
  grafana-data:    # Grafana dashboards/configs
```

### Volume Management

```bash
# List volumes
docker volume ls

# Inspect volume
docker volume inspect veccy-data

# Backup volume
docker run --rm \
  -v veccy-data:/data \
  -v $(pwd):/backup \
  ubuntu tar czf /backup/veccy-backup.tar.gz -C /data .

# Restore volume
docker run --rm \
  -v veccy-data:/data \
  -v $(pwd):/backup \
  ubuntu tar xzf /backup/veccy-backup.tar.gz -C /data

# Remove volume (DATA LOSS!)
docker volume rm veccy-data
```

### Bind Mounts

For development or custom data locations:

```yaml
services:
  veccy:
    volumes:
      - ./data:/data           # Local directory
      - ./logs:/logs           # Local logs
      - /mnt/ssd/veccy:/data   # Fast storage
```

## Health Checks

### Docker Health Check

Built into the Dockerfile:

```dockerfile
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/health/live || exit 1
```

### Checking Health Status

```bash
# View health status
docker ps

# Inspect health
docker inspect --format='{{json .State.Health}}' veccy | jq

# View health logs
docker inspect veccy | jq '.[0].State.Health.Log'
```

### Health Endpoints

- `GET /health` - Full health check with details
- `GET /health/live` - Liveness probe (for restarts)
- `GET /health/ready` - Readiness probe (for traffic)
- `GET /metrics` - Prometheus metrics

```bash
# Test health endpoints
curl http://localhost:8080/health
curl http://localhost:8080/health/live
curl http://localhost:8080/health/ready
curl http://localhost:8081/metrics
```

## Monitoring

### Prometheus Setup

Prometheus automatically scrapes Veccy metrics:

```yaml
# docker/prometheus.yml
scrape_configs:
  - job_name: 'veccy'
    static_configs:
      - targets: ['veccy:8080']
    metrics_path: '/metrics'
    scrape_interval: 15s
```

**Access Prometheus**: http://localhost:9090

### Grafana Setup

Grafana includes pre-configured dashboards:

**Access Grafana**: http://localhost:3000
- Username: `admin`
- Password: `admin`

**Pre-configured Dashboards**:
- Veccy Overview - Main application metrics
- JVM Metrics - Memory, GC, threads
- Request Metrics - Latency, throughput

### Custom Dashboards

Import additional dashboards:

1. Open Grafana → Dashboards → Import
2. Upload JSON from `docker/grafana/dashboards/`
3. Select Prometheus datasource

### Alerts

Configure alerts in `docker/prometheus.yml`:

```yaml
# Example alert rules
groups:
  - name: veccy
    rules:
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.9
        for: 5m
        annotations:
          summary: "High memory usage"
```

## Development Setup

### Using docker-compose.dev.yml

```bash
# Start development environment
docker-compose -f docker-compose.dev.yml up -d

# View logs with hot reload
docker-compose -f docker-compose.dev.yml logs -f
```

### Features

- **Hot Reload**: Source changes reflected without rebuild
- **Remote Debugging**: Attach debugger to port 5005
- **Maven Cache**: Faster dependency resolution
- **Interactive Shell**: Access container shell

### Remote Debugging

**IntelliJ IDEA**:
1. Run → Edit Configurations
2. Add New → Remote JVM Debug
3. Host: `localhost`, Port: `5005`
4. Start debugging

**VS Code** (launch.json):
```json
{
  "type": "java",
  "name": "Attach to Docker",
  "request": "attach",
  "hostName": "localhost",
  "port": 5005
}
```

### Rebuilding on Changes

```bash
# Rebuild and restart
docker-compose -f docker-compose.dev.yml up -d --build

# Restart without rebuild
docker-compose -f docker-compose.dev.yml restart veccy-dev
```

## Production Deployment

### Best Practices

1. **Resource Limits**: Set memory and CPU limits
2. **Restart Policy**: Use `unless-stopped` or `always`
3. **Health Checks**: Enable liveness and readiness probes
4. **Logging**: Configure log rotation
5. **Secrets**: Use Docker secrets or environment files
6. **Networks**: Use custom networks for isolation
7. **Monitoring**: Enable Prometheus and Grafana

### Production docker-compose.yml

```yaml
version: '3.8'

services:
  veccy:
    image: veccy:v1.0.0
    restart: unless-stopped

    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 6G
        reservations:
          cpus: '1'
          memory: 2G

    environment:
      - JAVA_OPTS=-Xmx4g -Xms2g -XX:+UseG1GC

    volumes:
      - type: volume
        source: veccy-data
        target: /data
      - type: volume
        source: veccy-logs
        target: /logs

    ports:
      - "8080:8080"
      - "8081:8081"

    networks:
      - veccy-network

    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health/live"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 30s

    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"

volumes:
  veccy-data:
    driver: local
  veccy-logs:
    driver: local

networks:
  veccy-network:
    driver: bridge
```

### Using Environment Files

```bash
# Create .env file
cat > .env <<EOF
JAVA_OPTS=-Xmx8g -Xms2g
VECCY_INDEX_TYPE=hnsw
VECCY_METRIC=cosine
EOF

# Use in docker-compose
docker-compose --env-file .env up -d
```

### Secrets Management

```bash
# Create secrets
echo "my-api-key" | docker secret create veccy-api-key -

# Use in Swarm mode
services:
  veccy:
    secrets:
      - veccy-api-key
    environment:
      - API_KEY_FILE=/run/secrets/veccy-api-key
```

### Log Rotation

```yaml
logging:
  driver: "json-file"
  options:
    max-size: "10m"
    max-file: "5"
    compress: "true"
```

Or use external log driver:

```yaml
logging:
  driver: "syslog"
  options:
    syslog-address: "tcp://logs.example.com:514"
    tag: "veccy"
```

## CLI Usage in Docker

### Interactive CLI

```bash
# Run CLI interactively
docker run -it --rm \
  -v veccy-data:/data \
  veccy:latest cli

# Or using docker-compose
docker-compose run --rm veccy cli
```

### Single Commands

```bash
# Initialize database
docker exec veccy /app/entrypoint.sh cli init /data/db --type hnsw

# Insert vector
docker exec veccy /app/entrypoint.sh cli insert \
  "[0.1, 0.2, 0.3]" \
  --metadata '{"id": 1}'

# Search
docker exec veccy /app/entrypoint.sh cli search \
  "[0.1, 0.2, 0.3]" \
  --top-k 10
```

## Troubleshooting

### Container Won't Start

```bash
# Check logs
docker logs veccy

# Check health
docker inspect veccy | jq '.[0].State.Health'

# Verify image
docker images | grep veccy

# Test manually
docker run -it --rm veccy:latest /bin/bash
```

### Port Already in Use

```bash
# Find process using port
lsof -i :8080  # Linux/Mac
netstat -ano | findstr :8080  # Windows

# Use different ports
docker run -p 9080:8080 -p 9081:8081 veccy:latest
```

### Volume Permission Issues

```bash
# Check volume permissions
docker exec veccy ls -la /data

# Fix permissions
docker exec veccy chown -R veccy:veccy /data

# Or in Dockerfile
RUN chown -R veccy:veccy /data
```

### Memory Issues

```bash
# Check memory usage
docker stats veccy

# Increase memory limit
docker run -m 8g veccy:latest

# Or in docker-compose
deploy:
  resources:
    limits:
      memory: 8G
```

### Network Issues

```bash
# Test connectivity
docker exec veccy ping prometheus
docker exec veccy curl http://prometheus:9090

# Check network
docker network ls
docker network inspect veccy-network

# Recreate network
docker network rm veccy-network
docker network create veccy-network
```

### Health Check Failing

```bash
# Test health endpoint manually
docker exec veccy curl -f http://localhost:8080/health/live

# Check application logs
docker logs veccy | grep ERROR

# Increase start period
HEALTHCHECK --start-period=60s ...
```

### Data Not Persisting

```bash
# Verify volume is mounted
docker inspect veccy | jq '.[0].Mounts'

# Check volume contents
docker run --rm -v veccy-data:/data ubuntu ls -la /data

# Ensure volume exists
docker volume ls | grep veccy-data
```

## Advanced Topics

### Multi-Stage Optimization

The Dockerfile uses multi-stage build:

```dockerfile
# Stage 1: Build with full Maven
FROM maven:3.9 AS builder
RUN mvn clean package

# Stage 2: Runtime with JRE only
FROM eclipse-temurin:21-jre
COPY --from=builder /build/target/*.jar app.jar
```

Benefits:
- Smaller image size (~50% reduction)
- Faster deployment
- Reduced attack surface

### Custom Base Images

Use your own base image:

```dockerfile
FROM your-registry.com/java:21-jre
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Swarm

Deploy to Swarm:

```bash
# Initialize swarm
docker swarm init

# Deploy stack
docker stack deploy -c docker-compose.yml veccy

# Scale service
docker service scale veccy_veccy=5

# View services
docker service ls
docker service ps veccy_veccy
```

### Kubernetes

See [k8s/README.md](../k8s/README.md) for Kubernetes deployment.

## Performance Tuning

### JVM Settings

```yaml
environment:
  # Heap size
  - JAVA_OPTS=-Xmx8g -Xms4g

  # GC tuning
  - JAVA_OPTS=-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:ParallelGCThreads=4

  # GC logging
  - JAVA_OPTS=-Xlog:gc*:file=/logs/gc.log
```

### Resource Limits

```yaml
deploy:
  resources:
    limits:
      cpus: '4'
      memory: 8G
      pids: 1000
    reservations:
      cpus: '2'
      memory: 4G
```

### Storage Performance

```yaml
volumes:
  veccy-data:
    driver_opts:
      type: tmpfs  # In-memory (testing only)
      device: tmpfs
      o: size=10g

  # Or use SSD mount
  - /mnt/ssd/veccy:/data
```

## Security

### Running as Non-Root

Already configured in Dockerfile:

```dockerfile
RUN groupadd -r veccy && useradd -r -g veccy veccy
USER veccy
```

### Network Security

```yaml
networks:
  veccy-network:
    driver: bridge
    driver_opts:
      com.docker.network.bridge.enable_icc: "false"
```

### Read-Only Root Filesystem

```yaml
services:
  veccy:
    read_only: true
    tmpfs:
      - /tmp
      - /logs
```

## Support

For issues and questions:
- GitHub Issues: https://github.com/skanga/veccy/issues
- Documentation: https://github.com/skanga/veccy/docs

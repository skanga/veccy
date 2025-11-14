# Veccy Health Checks Documentation

Comprehensive health monitoring system for production deployments.

## Table of Contents

- [Overview](#overview)
- [Quick Start](#quick-start)
- [Health Check Types](#health-check-types)
- [HTTP Endpoints](#http-endpoints)
- [Integration](#integration)
- [Configuration](#configuration)
- [Monitoring](#monitoring)
- [Best Practices](#best-practices)

## Overview

Veccy provides a robust health check system designed for production environments. The health check framework supports:

- ✅ **Component-level Health Checks** - Database, Storage, Index, Memory, Disk
- ✅ **HTTP Endpoints** - `/health`, `/health/live`, `/health/ready`
- ✅ **Kubernetes Integration** - Liveness and Readiness probes
- ✅ **Prometheus Metrics** - `/metrics` endpoint
- ✅ **Configurable Thresholds** - Memory, disk usage limits
- ✅ **Caching** - Efficient health check execution
- ✅ **Async Execution** - Non-blocking health checks
- ✅ **Extensible** - Custom health checks

## Quick Start

### Basic Setup

```java
import com.veccy.client.VectorDBClient;
import com.veccy.health.VeccyHealthManager;

// Create your database client
VectorDBClient client = ...;

// Create health manager with defaults
VeccyHealthManager healthManager = VeccyHealthManager.forClient(client);

// Check health programmatically
boolean healthy = healthManager.isHealthy();
HealthStatus status = healthManager.getHealthStatus();

// Health endpoint automatically starts on http://localhost:8080
// GET http://localhost:8080/health
// GET http://localhost:8080/health/live
// GET http://localhost:8080/health/ready
// GET http://localhost:8080/metrics
```

### Advanced Configuration

```java
VeccyHealthManager healthManager = VeccyHealthManager.builder()
        .client(client)
        .enableEndpoint(true)
        .endpointPort(9090)
        .enableMemoryCheck(true)
        .enableDiskCheck(true)
        .diskCheckPath("/var/lib/veccy")
        .healthCheckTimeout(5000)  // 5 seconds
        .cacheTtl(30000)  // 30 seconds
        .build();

// Get detailed results
AggregatedHealthCheckResult result = healthManager.checkHealth();
System.out.println("Status: " + result.getOverallStatus());
System.out.println("Duration: " + result.getDurationMs() + "ms");
result.getResults().forEach((name, checkResult) -> {
    System.out.println(name + ": " + checkResult.getStatus());
});

// Clean up when done
healthManager.close();
```

## Health Check Types

### 1. Database Health Check

**Purpose**: Verifies the overall database client is operational.

**Checks**:
- Client initialization status
- Vector count from storage statistics

**Status**:
- `UP`: Client is initialized and operational
- `DOWN`: Client not initialized or error occurred

**Example Response**:
```json
{
  "status": "UP",
  "message": "Database is operational",
  "details": {
    "initialized": true,
    "vector_count": 1000
  }
}
```

### 2. Storage Health Check

**Purpose**: Verifies storage backend is accessible and functional.

**Checks**:
- Storage stats availability
- Vector count presence

**Status**:
- `UP`: Storage is operational
- `DEGRADED`: Storage accessible but missing expected data
- `DOWN`: Storage inaccessible or error occurred

**Example Response**:
```json
{
  "status": "UP",
  "message": "Storage is operational",
  "details": {
    "type": "DiskStorage",
    "vector_count": 1000
  }
}
```

### 3. Index Health Check

**Purpose**: Verifies index is initialized and operational.

**Checks**:
- Index initialization status
- Index stats availability
- Vector count in index

**Status**:
- `UP`: Index is operational
- `DEGRADED`: Index initialized but stats unavailable
- `DOWN`: Index not initialized or error occurred

**Example Response**:
```json
{
  "status": "UP",
  "message": "Index is operational",
  "details": {
    "type": "HNSWIndex",
    "vector_count": 1000
  }
}
```

### 4. Memory Health Check

**Purpose**: Monitors JVM memory usage.

**Checks**:
- Total memory
- Free memory
- Used memory
- Max memory
- Usage percentage

**Thresholds**:
- `UP`: < 80% usage
- `DEGRADED`: 80-95% usage
- `DOWN`: > 95% usage

**Example Response**:
```json
{
  "status": "UP",
  "message": "Memory usage is healthy",
  "details": {
    "total_memory": 2147483648,
    "free_memory": 1073741824,
    "used_memory": 1073741824,
    "max_memory": 4294967296,
    "usage_percent": 25.0
  }
}
```

### 5. Disk Health Check

**Purpose**: Monitors disk space for data directory.

**Checks**:
- Total space
- Free space
- Usable space
- Usage percentage

**Thresholds**:
- `UP`: < 85% usage
- `DEGRADED`: 85-95% usage
- `DOWN`: > 95% usage or directory doesn't exist

**Example Response**:
```json
{
  "status": "UP",
  "message": "Disk usage is healthy",
  "details": {
    "directory": "/var/lib/veccy",
    "total_space": 1000000000000,
    "free_space": 500000000000,
    "usable_space": 450000000000,
    "usage_percent": 50.0
  }
}
```

## HTTP Endpoints

### GET /health

Full health check including all registered checks.

**Response**: 200 (if UP or DEGRADED), 503 (if DOWN)

```json
{
  "status": "UP",
  "duration_ms": 45,
  "timestamp": "2025-01-12T10:30:00Z",
  "checks": {
    "database": {
      "status": "UP",
      "message": "Database is operational",
      "details": {
        "initialized": true,
        "vector_count": 1000
      }
    },
    "storage": {
      "status": "UP",
      "message": "Storage is operational"
    },
    "index": {
      "status": "UP",
      "message": "Index is operational"
    },
    "memory": {
      "status": "UP",
      "message": "Memory usage is healthy",
      "details": {
        "usage_percent": 45.2
      }
    }
  }
}
```

### GET /health/live

Liveness probe for Kubernetes.

**Purpose**: Determines if the application process is alive and should be kept running.

**Response**: 200 (always, if the endpoint responds)

```json
{
  "status": "UP",
  "timestamp": "2025-01-12T10:30:00Z"
}
```

**Kubernetes Configuration**:
```yaml
livenessProbe:
  httpGet:
    path: /health/live
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3
```

### GET /health/ready

Readiness probe for Kubernetes.

**Purpose**: Determines if the application is ready to serve traffic.

**Response**: 200 (if ready), 503 (if not ready)

```json
{
  "status": "UP",
  "timestamp": "2025-01-12T10:30:00Z",
  "checks": {
    "database": {"status": "UP"},
    "storage": {"status": "UP"},
    "index": {"status": "UP"}
  }
}
```

**Kubernetes Configuration**:
```yaml
readinessProbe:
  httpGet:
    path: /health/ready
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  timeoutSeconds: 3
  successThreshold: 1
  failureThreshold: 3
```

### GET /metrics

Prometheus-compatible metrics endpoint.

**Response**: 200 (text/plain format)

```
# HELP veccy_health_status Health status (1=UP, 0.5=DEGRADED, 0=DOWN)
# TYPE veccy_health_status gauge
veccy_health_status{check="database"} 1
veccy_health_status{check="storage"} 1
veccy_health_status{check="index"} 1
veccy_health_status{check="memory"} 1
veccy_health_status{check="disk"} 0.5

# HELP veccy_health_check_duration_ms Health check duration in milliseconds
# TYPE veccy_health_check_duration_ms gauge
veccy_health_check_duration_ms 42
```

**Prometheus Configuration**:
```yaml
scrape_configs:
  - job_name: 'veccy'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/metrics'
    scrape_interval: 15s
```

## Integration

### Spring Boot Integration

```java
@Configuration
public class VeccyHealthConfig {

    @Bean
    public VeccyHealthManager healthManager(VectorDBClient client) {
        return VeccyHealthManager.builder()
                .client(client)
                .enableEndpoint(false)  // Use Spring's endpoint instead
                .enableMemoryCheck(true)
                .build();
    }

    @Bean
    public HealthIndicator veccyHealthIndicator(VeccyHealthManager healthManager) {
        return () -> {
            AggregatedHealthCheckResult result = healthManager.checkHealth();
            Health.Builder builder = result.isHealthy()
                ? Health.up()
                : Health.down();

            result.getResults().forEach((name, checkResult) -> {
                builder.withDetail(name, checkResult.getStatus());
            });

            return builder.build();
        };
    }
}
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: veccy-app
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: veccy
        image: veccy:1.0.0
        ports:
        - containerPort: 8080
          name: health

        livenessProbe:
          httpGet:
            path: /health/live
            port: health
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3

        readinessProbe:
          httpGet:
            path: /health/ready
            port: health
          initialDelaySeconds: 10
          periodSeconds: 5
          timeoutSeconds: 3
          successThreshold: 1
          failureThreshold: 3

        resources:
          requests:
            memory: "2Gi"
            cpu: "1000m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
```

### Docker Integration

```dockerfile
FROM openjdk:21-jdk-slim

WORKDIR /app
COPY target/veccy.jar .

# Health check configuration
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/health/live || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "veccy.jar"]
```

## Configuration

### Custom Health Check

```java
public class CustomHealthCheck implements HealthCheck {

    @Override
    public String getName() {
        return "custom";
    }

    @Override
    public String getCategory() {
        return "application";
    }

    @Override
    public HealthCheckResult check() {
        try {
            // Perform your custom check
            boolean isHealthy = performCheck();

            return isHealthy
                ? HealthCheckResult.up("Custom check passed")
                : HealthCheckResult.down("Custom check failed");
        } catch (Exception e) {
            return HealthCheckResult.down(e);
        }
    }

    @Override
    public boolean isCritical() {
        return false;  // Non-critical check
    }
}

// Register custom check
healthManager.getRegistry().register(new CustomHealthCheck());
```

### Environment-Specific Configuration

```java
// Development
VeccyHealthManager devHealth = VeccyHealthManager.builder()
        .client(client)
        .enableEndpoint(true)
        .endpointPort(8080)
        .enableMemoryCheck(true)
        .healthCheckTimeout(10000)  // More lenient
        .build();

// Production
VeccyHealthManager prodHealth = VeccyHealthManager.builder()
        .client(client)
        .enableEndpoint(true)
        .endpointPort(9090)
        .enableMemoryCheck(true)
        .enableDiskCheck(true)
        .diskCheckPath("/data/veccy")
        .healthCheckTimeout(5000)   // Stricter
        .cacheTtl(15000)            // Shorter cache
        .build();
```

## Monitoring

### Grafana Dashboard

Example Prometheus queries for Grafana:

```promql
# Overall health status
veccy_health_status

# Health by component
veccy_health_status{check="database"}
veccy_health_status{check="memory"}

# Health check duration
veccy_health_check_duration_ms

# Alert on unhealthy status
veccy_health_status < 1

# Alert on high memory usage
veccy_health_status{check="memory"} < 1
```

### Alerting Rules

```yaml
groups:
- name: veccy
  rules:
  - alert: VeccyDatabaseDown
    expr: veccy_health_status{check="database"} == 0
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "Veccy database is down"
      description: "Database health check failing for {{ $labels.instance }}"

  - alert: VeccyMemoryHigh
    expr: veccy_health_status{check="memory"} <= 0.5
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "Veccy memory usage is high"
      description: "Memory usage is at degraded level for {{ $labels.instance }}"

  - alert: VeccyDiskSpaceHigh
    expr: veccy_health_status{check="disk"} <= 0.5
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "Veccy disk space is running low"
```

## Best Practices

### 1. Always Enable Health Checks in Production

```java
// ✅ Good
VeccyHealthManager healthManager = VeccyHealthManager.forClient(client);

// ❌ Bad - no health monitoring
VectorDBClient client = ...;  // No health checks
```

### 2. Use Appropriate Timeouts

```java
// ✅ Good - reasonable timeout
.healthCheckTimeout(5000)  // 5 seconds

// ❌ Bad - too long or too short
.healthCheckTimeout(30000) // Too long - delays failure detection
.healthCheckTimeout(100)   // Too short - may cause false positives
```

### 3. Configure Kubernetes Probes Correctly

```yaml
# ✅ Good - appropriate delays and thresholds
livenessProbe:
  initialDelaySeconds: 30  # Allow time to start
  periodSeconds: 10        # Check every 10s
  failureThreshold: 3      # 3 failures before restart

readinessProbe:
  initialDelaySeconds: 10  # Start checking sooner
  periodSeconds: 5         # Check more frequently
  failureThreshold: 3      # 3 failures before removing from service
```

### 4. Monitor Health Check Duration

Long health check durations indicate problems:

```java
AggregatedHealthCheckResult result = healthManager.checkHealth();
if (result.getDurationMs() > 1000) {
    logger.warn("Health check took {}ms - investigate slow components",
                result.getDurationMs());
}
```

### 5. Differentiate Critical and Non-Critical Checks

```java
// Critical - affects service availability
public boolean isCritical() {
    return true;  // Database, storage, index
}

// Non-critical - informational only
public boolean isCritical() {
    return false;  // Memory, disk space warnings
}
```

### 6. Cache Results Appropriately

```java
// ✅ Good - reasonable cache for stable systems
.cacheTtl(30000)  // 30 seconds

// ❌ Bad - too long or no caching
.cacheTtl(300000) // 5 minutes - may miss issues
.cacheTtl(0)      // No cache - unnecessary load
```

### 7. Use Health Checks for Circuit Breaking

```java
public void performOperation() {
    if (!healthManager.isHealthy()) {
        throw new ServiceUnavailableException("System is not healthy");
    }
    // Proceed with operation
}
```

## Troubleshooting

### Health Endpoint Not Responding

**Problem**: Cannot connect to health endpoint

**Solutions**:
1. Check if endpoint is enabled: `healthManager.getEndpoint() != null`
2. Verify port is not blocked by firewall
3. Check if port is already in use
4. Review application logs for startup errors

### False Positives on Memory Checks

**Problem**: Memory health check failing intermittently

**Solutions**:
1. Increase memory thresholds (edit MemoryHealthCheck)
2. Tune JVM heap size: `-Xmx4g`
3. Investigate memory leaks
4. Increase cache TTL to reduce check frequency

### Slow Health Checks

**Problem**: Health checks taking too long

**Solutions**:
1. Increase timeout: `.healthCheckTimeout(10000)`
2. Optimize individual health checks
3. Use caching: `.cacheTtl(60000)`
4. Remove non-essential checks

### Kubernetes Pod Restart Loops

**Problem**: Pods constantly restarting

**Solutions**:
1. Increase `initialDelaySeconds`
2. Increase `failureThreshold`
3. Review health check logic for false failures
4. Check resource limits (CPU/memory)

## Examples

See `src/test/java/com/veccy/examples/HealthCheckExample.java` for complete examples.

## API Reference

- [HealthCheck](../src/main/java/com/veccy/health/HealthCheck.java) - Health check interface
- [HealthCheckResult](../src/main/java/com/veccy/health/HealthCheckResult.java) - Result object
- [HealthStatus](../src/main/java/com/veccy/health/HealthStatus.java) - Status enum
- [HealthCheckRegistry](../src/main/java/com/veccy/health/HealthCheckRegistry.java) - Registry and executor
- [VeccyHealthManager](../src/main/java/com/veccy/health/VeccyHealthManager.java) - High-level manager

## License

MIT License - See [LICENSE](../LICENSE) for details

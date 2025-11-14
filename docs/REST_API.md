# Veccy REST API - Complete Guide

**Version**: 1.0.0
**Last Updated**: 2025-11-12

---

## Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Security Features](#security-features)
4. [API Endpoints](#api-endpoints)
5. [Configuration](#configuration)
6. [Deployment](#deployment)
7. [Monitoring](#monitoring)

---

## Overview

The Veccy REST API provides a production-ready HTTP interface for vector database operations with enterprise-grade security, reliability, and observability.

### Key Features

✅ **Security**
- API key authentication
- Rate limiting (60 req/min default)
- Input validation
- Production-safe error messages
- Explicit CORS configuration

✅ **Reliability**
- Request timeouts (30s production, 60s dev)
- Graceful shutdown
- Transaction safety (pre-validation)
- Pagination support

✅ **Observability**
- Comprehensive metrics
- Health checks (memory, databases, disk)
- Enhanced logging with correlation IDs
- API versioning headers

✅ **Performance**
- HTTPS/TLS support
- Response compression
- Batch operations
- Efficient indexing

---

## Quick Start

### 1. Start the Server

```bash
# Development mode (default)
java -cp target/veccy-0.1-fat.jar com.veccy.rest.VeccyRestServer

# Production mode
java -cp target/veccy-0.1-fat.jar com.veccy.rest.VeccyRestServer --production

# Custom port
java -cp target/veccy-0.1-fat.jar com.veccy.rest.VeccyRestServer --port 8080

# With metrics
java -cp target/veccy-0.1-fat.jar com.veccy.rest.VeccyRestServer --metrics
```

Server starts on `http://localhost:7878` by default.

### 2. Check Health

```bash
curl http://localhost:7878/health
```

**Response:**
```json
{
  "status": "UP",
  "duration_ms": 12,
  "checks": {
    "memory": {
      "status": "UP",
      "message": "Memory usage: 45.2%"
    },
    "databases": {
      "status": "UP",
      "message": "0/0 databases healthy"
    },
    "disk_space": {
      "status": "UP",
      "message": "Disk space: 65.3% free"
    }
  }
}
```

### 3. Create a Database

```bash
curl -X POST http://localhost:7878/api/v1/databases \
  -H "Content-Type: application/json" \
  -d '{
    "name": "my_vectors",
    "dimensions": 768,
    "index_config": {
      "type": "hnsw",
      "m": 16,
      "ef_construction": 200
    },
    "storage_config": {
      "type": "memory"
    },
    "distance_metric": "cosine"
  }'
```

### 4. Insert Vectors

```bash
curl -X POST http://localhost:7878/api/v1/databases/my_vectors/vectors \
  -H "Content-Type: application/json" \
  -d '{
    "vectors": [
      [0.1, 0.2, 0.3, ...],
      [0.4, 0.5, 0.6, ...]
    ],
    "metadata": [
      {"doc_id": "1", "text": "Hello"},
      {"doc_id": "2", "text": "World"}
    ]
  }'
```

### 5. Search Vectors

```bash
curl -X GET http://localhost:7878/api/v1/databases/my_vectors/vectors/search \
  -H "Content-Type: application/json" \
  -d '{
    "queryVector": [0.1, 0.2, 0.3, ...],
    "k": 10
  }'
```

---

## Security Features

### 1. API Key Authentication

**Enable in Production:**

```java
Map<String, Object> securityConfig = new HashMap<>();
securityConfig.put("apiKeyAuth", true);
securityConfig.put("apiKeys", new String[]{
    System.getenv("API_KEY_1"),
    System.getenv("API_KEY_2")
});

RestConfig config = new RestConfig.Builder()
    .securityConfig(securityConfig)
    .build();
```

**Usage:**

```bash
curl -H "X-API-Key: your-api-key-here" \
  http://localhost:7878/api/v1/databases
```

**Responses:**
- `401 Unauthorized` - Missing or invalid API key
- Applies to all `/api/*` routes

### 2. Rate Limiting

**Configuration:**

```java
securityConfig.put("rateLimitEnabled", true);
securityConfig.put("maxRequestsPerMinute", 100);
```

**Behavior:**
- Per-IP tracking using token bucket algorithm
- Automatic token refill every minute
- Cleanup of inactive buckets every 5 minutes

**Response Headers:**
```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
```

**Error Response:**
```json
{
  "success": false,
  "message": "Rate limit exceeded. Please try again later.",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 3. Input Validation

All inputs are validated before database operations:

**Vector Validation:**
- Dimensions must match database configuration
- No NaN or Infinity values
- Max dimensions: 10,000
- Max vectors per batch: 1,000

**Metadata Validation:**
- Max size: 1MB per entry
- Max entries: 100 per vector
- Valid JSON structure

**Search Validation:**
- Max k: 1,000

**Error Response:**
```json
{
  "success": false,
  "message": "Vector validation failed: Vector dimension mismatch. Expected 768, got 384"
}
```

### 4. Production Error Messages

**Development Mode** (detailed errors):
```json
{
  "success": false,
  "message": "Database operation failed: Index out of bounds at layer 3",
  "correlationId": "uuid",
  "path": "/api/v1/databases/test/vectors",
  "stackTrace": "..."
}
```

**Production Mode** (safe errors):
```json
{
  "success": false,
  "message": "Internal server error. Please contact support with correlation ID.",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 5. CORS Configuration

**Development** (permissive):
```java
RestConfig config = new RestConfig.Builder()
    .enableCors(true)
    .allowedOrigins(new String[]{"*"})  // OK in dev
    .build();
```

**Production** (restrictive):
```java
RestConfig config = new RestConfig.Builder()
    .enableCors(true)
    .allowedOrigins(new String[]{
        "https://app.example.com",
        "https://admin.example.com"
    })
    .build();
```

Wildcard `*` is **rejected** in production mode with error logging.

---

## API Endpoints

### Database Management

#### Create Database
```
POST /api/v1/databases
```

**Request Body:**
```json
{
  "name": "my_vectors",
  "dimensions": 768,
  "index_config": {
    "type": "hnsw",
    "m": 16,
    "ef_construction": 200,
    "ef_search": 50
  },
  "storage_config": {
    "type": "hybrid",
    "path": "./data/vectors",
    "cache_size_mb": 512
  },
  "distance_metric": "cosine"
}
```

**Index Types:**
- `flat` - Exact search (no indexing)
- `hnsw` - Hierarchical Navigable Small World (fast ANN)
- `ivf` - Inverted File Index (memory efficient)
- `lsh` - Locality Sensitive Hashing
- `annoy` - Approximate Nearest Neighbors Oh Yeah

**Storage Types:**
- `memory` - In-memory only (fast, volatile)
- `disk` - Persistent storage (durable, slower)
- `hybrid` - Cache + persistence (balanced)

**Distance Metrics:**
- `cosine` - Cosine similarity
- `euclidean` - L2 distance
- `dot_product` - Dot product
- `manhattan` - L1 distance

**Response:**
```json
{
  "success": true,
  "message": "Database created successfully",
  "data": {
    "name": "my_vectors",
    "dimensions": 768,
    "index_type": "hnsw",
    "storage_type": "hybrid",
    "distance_metric": "cosine",
    "created_at": 1699876543210,
    "vector_count": 0
  }
}
```

#### List Databases
```
GET /api/v1/databases?page=1&pageSize=20
```

**Query Parameters:**
- `page` (optional): Page number (default: 1)
- `pageSize` (optional): Items per page (default: 20, max: 100)

**Response:**
```json
{
  "success": true,
  "data": {
    "data": [
      {
        "name": "my_vectors",
        "dimensions": 768,
        "vector_count": 1500,
        "index_type": "hnsw",
        "storage_type": "hybrid"
      }
    ],
    "pagination": {
      "page": 1,
      "pageSize": 20,
      "totalItems": 1,
      "totalPages": 1,
      "hasNext": false,
      "hasPrevious": false
    }
  }
}
```

#### Get Database
```
GET /api/v1/databases/{name}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "name": "my_vectors",
    "dimensions": 768,
    "index_type": "hnsw",
    "storage_type": "hybrid",
    "distance_metric": "cosine",
    "created_at": 1699876543210,
    "vector_count": 1500
  }
}
```

#### Delete Database
```
DELETE /api/v1/databases/{name}
```

**Response:**
```json
{
  "success": true,
  "message": "Database deleted successfully"
}
```

#### Get Database Stats
```
GET /api/v1/databases/{name}/stats
```

**Response:**
```json
{
  "success": true,
  "data": {
    "vector_count": 1500,
    "dimensions": 768,
    "index_type": "hnsw",
    "storage_type": "hybrid",
    "memory_usage_mb": 245.6,
    "disk_usage_mb": 512.3
  }
}
```

### Vector Operations

#### Insert Vectors
```
POST /api/v1/databases/{name}/vectors
```

**Request Body:**
```json
{
  "vectors": [
    [0.1, 0.2, 0.3, ...(768 dims)],
    [0.4, 0.5, 0.6, ...(768 dims)]
  ],
  "metadata": [
    {"doc_id": "1", "text": "Hello", "category": "greeting"},
    {"doc_id": "2", "text": "World", "category": "noun"}
  ]
}
```

**Response:**
```json
{
  "success": true,
  "message": "Vectors inserted successfully",
  "data": {
    "ids": [
      "550e8400-e29b-41d4-a716-446655440000",
      "6ba7b810-9dad-11d1-80b4-00c04fd430c8"
    ],
    "count": 2
  }
}
```

#### Search Vectors
```
GET /api/v1/databases/{name}/vectors/search
```

**Request Body:**
```json
{
  "queryVector": [0.1, 0.2, 0.3, ...(768 dims)],
  "k": 10
}
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "distance": 0.123,
      "metadata": {
        "doc_id": "1",
        "text": "Hello"
      }
    },
    {
      "id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
      "distance": 0.234,
      "metadata": {
        "doc_id": "2",
        "text": "World"
      }
    }
  ]
}
```

#### Update Vector
```
PUT /api/v1/databases/{name}/vectors/{id}
```

**Request Body:**
```json
{
  "vector": [0.1, 0.2, 0.3, ...(768 dims)],
  "metadata": {
    "doc_id": "1",
    "text": "Updated text"
  }
}
```

#### Delete Vector
```
DELETE /api/v1/databases/{name}/vectors/{id}
```

### Batch Operations

#### Batch Insert
```
POST /api/v1/databases/{name}/vectors/batch
```

Same as regular insert, optimized for large batches (up to 1,000 vectors).

#### Batch Search
```
POST /api/v1/databases/{name}/vectors/batch-search
```

**Request Body:**
```json
{
  "queryVectors": [
    [0.1, 0.2, 0.3, ...],
    [0.4, 0.5, 0.6, ...]
  ],
  "k": 10
}
```

**Response:** Array of search results (one per query vector).

#### Batch Delete
```
DELETE /api/v1/databases/{name}/vectors/batch
```

**Request Body:**
```json
{
  "ids": [
    "550e8400-e29b-41d4-a716-446655440000",
    "6ba7b810-9dad-11d1-80b4-00c04fd430c8"
  ]
}
```

### System Endpoints

#### Health Check
```
GET /health
```

No authentication required.

#### Root Info
```
GET /
```

**Response:**
```json
{
  "service": "Veccy Vector Database",
  "version": "1.0.0",
  "api_version": "v1",
  "endpoints": {
    "health": "/health",
    "api": "/api/v1",
    "documentation": "/swagger-ui"
  }
}
```

#### Metrics (if enabled)
```
GET /api/v1/metrics
```

**Response:**
```json
{
  "uptime": {
    "uptime_ms": 123456,
    "uptime_hours": 0
  },
  "databases": {
    "database_count": 3
  },
  "memory": {
    "used_mb": 512,
    "usage_percent": 12.5
  },
  "requests": {
    "total_requests": 1523,
    "success_rate": 0.986,
    "avg_response_time_ms": 23.4,
    "endpoints": {
      "POST /api/v1/databases/:id/vectors": {
        "request_count": 523,
        "avg_response_time_ms": 34.2
      }
    }
  }
}
```

#### API Documentation
```
GET /swagger-ui
```

Interactive Swagger UI for API exploration.

---

## Configuration

### Development Configuration

```java
RestConfig config = RestConfig.defaultConfig();
// or
RestConfig config = new RestConfig.Builder()
    .port(7878)
    .host("localhost")
    .enableCors(true)
    .allowedOrigins(new String[]{"*"})
    .requestTimeoutMs(60000)
    .enableMetrics(true)
    .build();
```

**Defaults:**
- Port: 7878
- Host: localhost
- API Key Auth: DISABLED
- Rate Limiting: DISABLED
- CORS: Wildcard allowed
- Request Timeout: 60 seconds
- Metrics: DISABLED
- Production Mode: FALSE

### Production Configuration

```java
RestConfig config = RestConfig.productionConfig();
```

**Production Defaults:**
- Port: 8080
- Host: 0.0.0.0
- API Key Auth: ENABLED (must configure keys)
- Rate Limiting: ENABLED (60 req/min)
- CORS: Explicit origins only (must configure)
- Request Timeout: 30 seconds
- Metrics: ENABLED
- Production Mode: TRUE
- Max Request Size: 10MB

### Custom Configuration

```java
Map<String, Object> securityConfig = new HashMap<>();
securityConfig.put("productionMode", true);
securityConfig.put("apiKeyAuth", true);
securityConfig.put("apiKeys", new String[]{
    System.getenv("API_KEY_1"),
    System.getenv("API_KEY_2")
});
securityConfig.put("rateLimitEnabled", true);
securityConfig.put("maxRequestsPerMinute", 100);

RestConfig config = new RestConfig.Builder()
    .port(8080)
    .host("0.0.0.0")
    .enableCors(true)
    .allowedOrigins(new String[]{
        "https://app.example.com",
        "https://admin.example.com"
    })
    .maxRequestSize(50 * 1024 * 1024) // 50MB
    .requestTimeoutMs(30000)
    .enableMetrics(true)
    .enableHttps(true)
    .keystorePath(System.getenv("KEYSTORE_PATH"))
    .keystorePassword(System.getenv("KEYSTORE_PASSWORD"))
    .httpsPort(8443)
    .securityConfig(securityConfig)
    .build();

VeccyRestServer server = new VeccyRestServer(config);
server.start();
```

### HTTPS/TLS Configuration

#### Generate Self-Signed Certificate (Development)

```bash
keytool -genkeypair -alias veccy \
  -keyalg RSA -keysize 2048 \
  -validity 365 \
  -keystore keystore.jks \
  -storepass changeit \
  -dname "CN=localhost,OU=Dev,O=Veccy,L=City,ST=State,C=US"
```

#### Enable HTTPS

```java
RestConfig config = new RestConfig.Builder()
    .port(8080)              // HTTP
    .enableHttps(true)
    .httpsPort(8443)         // HTTPS
    .keystorePath("/path/to/keystore.jks")
    .keystorePassword("changeit")
    .build();
```

Access via:
- `http://localhost:8080/health`
- `https://localhost:8443/health`

---

## Deployment

### Docker

**Dockerfile:**
```dockerfile
FROM openjdk:21-slim

COPY target/veccy-0.1-fat.jar /app/veccy.jar
COPY keystore.jks /app/keystore.jks

ENV KEYSTORE_PATH=/app/keystore.jks
ENV KEYSTORE_PASSWORD=changeit
ENV API_KEY_1=your-secret-key

EXPOSE 8080 8443

STOPSIGNAL SIGTERM

CMD ["java", "-cp", "/app/veccy.jar", \
     "com.veccy.rest.VeccyRestServer", \
     "--production", "--metrics"]
```

**Build and Run:**
```bash
docker build -t veccy-api .
docker run -p 8080:8080 -p 8443:8443 \
  -e API_KEY_1=your-secret-key \
  veccy-api
```

### Kubernetes

**Deployment:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: veccy-api
spec:
  replicas: 3
  template:
    spec:
      terminationGracePeriodSeconds: 30
      containers:
      - name: veccy
        image: veccy:latest
        ports:
        - containerPort: 8080
        env:
        - name: API_KEY_1
          valueFrom:
            secretKeyRef:
              name: veccy-secrets
              key: api-key
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "4Gi"
            cpu: "2000m"
```

**Service:**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: veccy-api
spec:
  selector:
    app: veccy-api
  ports:
  - port: 80
    targetPort: 8080
  type: LoadBalancer
```

### Reverse Proxy (Nginx)

```nginx
upstream veccy {
    server localhost:8080;
}

server {
    listen 443 ssl http2;
    server_name api.example.com;

    ssl_certificate /etc/ssl/certs/api.example.com.crt;
    ssl_certificate_key /etc/ssl/private/api.example.com.key;

    location / {
        proxy_pass http://veccy;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Timeouts
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
    }
}
```

---

## Monitoring

### Health Checks

The `/health` endpoint provides detailed system health:

**Checks:**
1. **Memory**: UP < 75%, DEGRADED 75-90%, DOWN > 90%
2. **Databases**: Tracks database initialization status
3. **Disk Space**: UP > 20%, DEGRADED 10-20%, DOWN < 10%

**Integration:**
```bash
# Kubernetes liveness probe
kubectl create -f - <<EOF
livenessProbe:
  httpGet:
    path: /health
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
EOF
```

### Metrics

Enable metrics to track:
- Request rates and success rates
- Response time percentiles
- Per-endpoint performance
- Rate limit hits
- Authentication failures
- Database operation counts

**Accessing Metrics:**
```bash
curl http://localhost:7878/api/v1/metrics
```

### Logging

All requests include correlation IDs for tracing:

```
2025-11-12 10:15:23 INFO  → GET /api/v1/databases from 192.168.1.100 [correlation-id=uuid]
2025-11-12 10:15:23 INFO  ← 200 OK in 23ms [correlation-id=uuid]
```

**Log Levels:**
- ERROR: Critical failures
- WARN: Rate limits, auth failures
- INFO: Request/response logging
- DEBUG: Detailed operation info

### Graceful Shutdown

The server handles shutdown signals gracefully:

```bash
# Send SIGTERM (Ctrl+C)
kill -TERM <pid>

# Logs show:
# "Shutdown signal received. Initiating graceful shutdown..."
# "Stopping Javalin server (will wait for active requests)..."
# "Shutting down timeout handler..."
# "Shutting down rate limiter..."
# "Shutting down server context..."
# "Graceful shutdown completed successfully."
```

**Shutdown Sequence:**
1. Stop accepting new requests
2. Wait for active requests to complete
3. Shutdown middleware components
4. Close database connections
5. Exit cleanly

---

## Response Headers

All API responses include:

```
X-API-Version: 1.0.0
X-API-Min-Version: 1.0.0
X-Correlation-ID: 550e8400-e29b-41d4-a716-446655440000
X-Request-Timeout-Ms: 30000
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
```

---

## Error Handling

### Standard Error Response

```json
{
  "success": false,
  "message": "Error description",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### HTTP Status Codes

- `200 OK` - Successful operation
- `201 Created` - Resource created
- `400 Bad Request` - Invalid input
- `401 Unauthorized` - Missing/invalid API key
- `404 Not Found` - Resource not found
- `429 Too Many Requests` - Rate limit exceeded
- `500 Internal Server Error` - Server error
- `501 Not Implemented` - Feature not yet available
- `503 Service Unavailable` - Health check failed

---

## Best Practices

### Security
1. ✅ Always use HTTPS in production
2. ✅ Rotate API keys regularly
3. ✅ Use environment variables for secrets
4. ✅ Enable rate limiting
5. ✅ Configure explicit CORS origins
6. ✅ Enable production mode

### Performance
1. ✅ Use batch operations for bulk inserts
2. ✅ Enable metrics for monitoring
3. ✅ Configure appropriate request timeouts
4. ✅ Use hybrid storage for balance
5. ✅ Tune HNSW parameters (M=16, ef_construction=200)

### Reliability
1. ✅ Monitor health checks
2. ✅ Set up alerts for DEGRADED/DOWN status
3. ✅ Configure graceful shutdown in K8s
4. ✅ Use pagination for large result sets
5. ✅ Validate inputs before database operations

---

## Support

For issues, questions, or feature requests:
- GitHub Issues: [github.com/skanga/veccy/issues](https://github.com/skanga/veccy/issues)
- Documentation: [docs/](../docs/)
- Examples: [examples/](../examples/)

---

## License

MIT License - see [LICENSE](../LICENSE) file for details.

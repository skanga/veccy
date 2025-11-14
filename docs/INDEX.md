# Veccy Documentation Index

Welcome to the Veccy vector database documentation!

## Quick Start

New to Veccy? Start here:

1. **[Installation & Setup](#installation)** - Get Veccy running in 5 minutes
2. **[CLI Guide](CLI.md)** - Learn the command-line interface
3. **[REST API Guide](REST_API.md)** ⭐ **Complete REST API documentation**

---

## Installation

### Prerequisites
- Java 21 or higher
- Maven 3.8+ (for building from source)

### Quick Install

```bash
# Download pre-built JAR
wget https://github.com/skanga/veccy/releases/latest/veccy-0.1-fat.jar

# OR build from source
git clone https://github.com/skanga/veccy.git
cd veccy
mvn clean package
```

### Run CLI
```bash
java -jar veccy-0.1-fat.jar help
```

### Start REST API Server
```bash
java -cp veccy-0.1-fat.jar com.veccy.rest.VeccyRestServer
```

Server starts at `http://localhost:7878`

---

## Documentation Index

### 🚀 Getting Started
- **[Building from Source](BUILDING.md)** - Complete build guide: Maven, fat JAR, CI/CD integration
- **[CLI Guide](CLI.md)** - Interactive shell and command-line usage
- **[Docker Guide](DOCKER.md)** - Container deployment, Docker Compose, Kubernetes

### 📡 REST API
- **[REST API Guide](REST_API.md)** ⭐ **START HERE** - Complete API documentation
  - Quick start (5 steps to running API)
  - All endpoints with curl examples
  - Security (authentication, rate limiting, validation, HTTPS)
  - Configuration (dev/prod/custom)
  - Deployment (Docker, Kubernetes, Nginx)
  - Monitoring (health checks, metrics, logging)

### 🔧 Advanced Topics
- **[Batch Operations](BATCH_OPERATIONS.md)** - Bulk operations (1.5x-5x faster)
- **[Health Checks](HEALTH_CHECKS.md)** - Monitoring, Kubernetes integration
- **[Pagination](PAGINATION.md)** - Efficient large result set handling

### 📚 Archive
Technical/historical docs moved to [archive/](archive/) folder

---

## Common Tasks

### Create a Database
```java
import com.veccy.client.VectorDBClient;
import com.veccy.factory.VectorDBFactory;

VectorDBClient db = VectorDBFactory.createHighPerformance();
db.initialize();
```

### Insert Vectors
```java
double[][] vectors = {{0.1, 0.2, ...}, {0.3, 0.4, ...}};
List<Map<String, Object>> metadata = List.of(
    Map.of("doc_id", "1"),
    Map.of("doc_id", "2")
);
List<String> ids = db.insert(vectors, metadata);
```

### Search
```java
double[] query = {0.1, 0.2, ...};
List<SearchResult> results = db.search(query, 10);
```

### REST API Example
```bash
# Create database
curl -X POST http://localhost:7878/api/v1/databases \
  -H "Content-Type: application/json" \
  -d '{
    "name": "my_db",
    "dimensions": 768,
    "index_config": {"type": "hnsw"},
    "storage_config": {"type": "memory"}
  }'

# Insert vectors
curl -X POST http://localhost:7878/api/v1/databases/my_db/vectors \
  -H "Content-Type: application/json" \
  -d '{
    "vectors": [[0.1, 0.2, ...]],
    "metadata": [{"doc_id": "1"}]
  }'

# Search
curl -X GET http://localhost:7878/api/v1/databases/my_db/vectors/search \
  -H "Content-Type: application/json" \
  -d '{"queryVector": [0.1, 0.2, ...], "k": 10}'
```

---

## Index Types Comparison

| Index | Use Case | Speed | Memory | Accuracy |
|-------|----------|-------|--------|----------|
| **Flat** | Small datasets (<10K) | Medium | Low | 100% |
| **HNSW** | General purpose | Fast | High | 95-99% |
| **IVF** | Large datasets (>1M) | Fast | Medium | 90-95% |
| **LSH** | High dimensions | Very Fast | Low | 85-90% |
| **Annoy** | Read-heavy workloads | Fast | Low | 90-95% |

### Recommendations
- **Development/Testing**: Flat or HNSW with memory storage
- **Production (< 100K vectors)**: HNSW with hybrid storage
- **Production (> 1M vectors)**: IVF or LSH with disk storage
- **High-throughput reads**: Annoy with disk storage

---

## Storage Backends

| Storage | Persistence | Speed | Best For |
|---------|------------|-------|----------|
| **Memory** | ❌ Volatile | Fastest | Dev, testing, caching |
| **Disk** | ✅ Durable | Slower | Production, large datasets |
| **Hybrid** | ✅ Durable | Balanced | Production (recommended) |

### Hybrid Storage Configuration
```java
import com.veccy.client.VectorDBClient;
import com.veccy.factory.VectorDBFactory;
import java.util.Map;

Map<String, Object> storageConfig = Map.of(
    "type", "hybrid",
    "data_dir", "./data",
    "cache_size", 512
);
Map<String, Object> indexConfig = Map.of(
    "type", "hnsw",
    "metric", "cosine"
);
VectorDBClient db = VectorDBFactory.createCustom(storageConfig, indexConfig, null, null);
db.initialize();
```

---

## Configuration Examples

### Development
```java
RestConfig config = RestConfig.defaultConfig();
VeccyRestServer server = new VeccyRestServer(config);
server.start();
```

**Features:**
- Port 7878
- No authentication
- Wildcard CORS
- 60s timeout
- Detailed error messages

### Production
```java
RestConfig config = RestConfig.productionConfig();
VeccyRestServer server = new VeccyRestServer(config);
server.start();
```

**Features:**
- Port 8080
- API key authentication required
- Rate limiting (60 req/min)
- Explicit CORS only
- 30s timeout
- Secure error messages
- Metrics enabled

---

## Monitoring & Health

### Health Check
```bash
curl http://localhost:7878/health
```

**Response:**
```json
{
  "status": "UP",
  "checks": {
    "memory": {"status": "UP", "message": "Memory usage: 45%"},
    "databases": {"status": "UP", "message": "3/3 databases healthy"},
    "disk_space": {"status": "UP", "message": "Disk space: 65% free"}
  }
}
```

### Metrics
```bash
curl http://localhost:7878/api/v1/metrics
```

**Includes:**
- Request rates and success rates
- Response time statistics
- Per-endpoint performance
- Rate limit hits
- Database operation counts

See **[Health Checks Guide](HEALTH_CHECKS.md)** for details.

---

## Deployment

### Docker
```bash
docker build -t veccy .
docker run -p 8080:8080 veccy
```

### Kubernetes
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: veccy-api
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: veccy
        image: veccy:latest
        ports:
        - containerPort: 8080
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
```

See **[Docker Guide](DOCKER.md)** for complete deployment documentation.

---

## Performance Tips

### 1. Use Batch Operations
```java
// 1.5x-5x faster than individual operations
db.insertBatch(vectors, metadata);
db.searchBatch(queryVectors, k);
```

See **[Batch Operations Guide](BATCH_OPERATIONS.md)**

### 2. Choose Right Index
- **< 10K vectors**: Flat (exact search)
- **10K-1M vectors**: HNSW (best balance)
- **> 1M vectors**: IVF or LSH (memory efficient)

### 3. Tune HNSW Parameters
```java
// Balanced (default)
VectorDBClient db = VectorDBFactory.createHighPerformance();

// High accuracy (slower) - use custom config
Map<String, Object> indexConfig = Map.of(
    "type", "hnsw",
    "m", 32,
    "ef_construction", 400,
    "metric", "cosine"
);
VectorDBClient dbHighAccuracy = VectorDBFactory.createCustom(
    Map.of("type", "memory"), indexConfig, null, null);

// High speed (lower accuracy)
Map<String, Object> indexConfigFast = Map.of(
    "type", "hnsw",
    "m", 8,
    "ef_construction", 100,
    "metric", "cosine"
);
VectorDBClient dbFast = VectorDBFactory.createCustom(
    Map.of("type", "memory"), indexConfigFast, null, null);
```

### 4. Use Hybrid Storage
```java
// Best of both worlds: fast cache + persistence
Map<String, Object> storageConfig = Map.of(
    "type", "hybrid",
    "data_dir", "./data",
    "cache_size", 512
);
Map<String, Object> indexConfig = Map.of("type", "hnsw", "metric", "cosine");
VectorDBClient db = VectorDBFactory.createCustom(storageConfig, indexConfig, null, null);
```

### 5. Enable Compression (for large datasets)
```java
// 8x-128x space reduction with minimal accuracy loss
VectorDBClient db = VectorDBFactory.createMemoryOptimized(8); // 8-bit quantization
```

---

## Troubleshooting

### Port Already in Use
```bash
# Change port
java -cp veccy-0.1-fat.jar com.veccy.rest.VeccyRestServer --port 8080
```

### Out of Memory
```bash
# Increase heap size
java -Xmx4g -jar veccy-0.1-fat.jar
```

### Slow Queries
1. Check index type (use HNSW for > 10K vectors)
2. Reduce `k` in search queries
3. Enable hybrid storage with larger cache
4. Consider compression for very large datasets

### Authentication Failures
```bash
# Check API key header
curl -H "X-API-Key: your-key" http://localhost:7878/api/v1/databases
```

---

## API Security

### Enable Authentication
```java
Map<String, Object> security = new HashMap<>();
security.put("apiKeyAuth", true);
security.put("apiKeys", new String[]{System.getenv("API_KEY")});

RestConfig config = new RestConfig.Builder()
    .securityConfig(security)
    .build();
```

### Use HTTPS
```java
RestConfig config = new RestConfig.Builder()
    .enableHttps(true)
    .keystorePath("/path/to/keystore.jks")
    .keystorePassword("changeit")
    .httpsPort(8443)
    .build();
```

### Configure Rate Limiting
```java
security.put("rateLimitEnabled", true);
security.put("maxRequestsPerMinute", 100);
```

---

## Testing

```bash
# Run all tests (471 tests)
mvn test

# Run specific test
mvn test -Dtest=VeccyRestServerTest

# Generate coverage report
mvn jacoco:report
```

---

## Support

- **Main README**: [../README.md](../README.md)
- **GitHub Issues**: [github.com/skanga/veccy/issues](https://github.com/skanga/veccy/issues)
- **GitHub Discussions**: [github.com/skanga/veccy/discussions](https://github.com/skanga/veccy/discussions)

---

## License

MIT License - see [../LICENSE](../LICENSE)

---

**Last Updated**: 2025-11-12

# CLAUDE.md - AI Assistant Guide for Veccy

This document provides comprehensive guidance for AI assistants working with the Veccy codebase. It covers the architecture, conventions, workflows, and key patterns that should be followed when making changes or additions to the project.

**Last Updated**: 2025-11-29
**Version**: 0.1
**Java Version**: 21+
**Build Tool**: Maven 3.8+

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Codebase Structure](#codebase-structure)
3. [Architecture & Design Patterns](#architecture--design-patterns)
4. [Development Workflow](#development-workflow)
5. [Code Conventions](#code-conventions)
6. [Key Abstractions](#key-abstractions)
7. [Testing Strategy](#testing-strategy)
8. [Common Tasks](#common-tasks)
9. [Configuration Patterns](#configuration-patterns)
10. [Important Files](#important-files)
11. [Deployment & Production](#deployment--production)
12. [Documentation Standards](#documentation-standards)

---

## Project Overview

### Purpose
Veccy is a production-ready vector database implementation in Java 21+ with multiple indexing strategies, flexible storage backends, and an enterprise-grade REST API. It's designed for semantic search, image similarity, recommendation systems, and anomaly detection.

### Tech Stack
- **Language**: Java 21 (uses modern Java features like records, pattern matching, sealed classes)
- **Build Tool**: Maven 3.8+
- **Web Framework**: Javalin 6.7.0 (lightweight, performant)
- **Logging**: SLF4J + Logback
- **Testing**: JUnit 5 (471+ tests), Mockito
- **Serialization**: Jackson (JSON)
- **Document Processing**: PDFBox, Apache POI, Jsoup
- **ML Runtime**: ONNX Runtime (for embeddings)
- **Caching**: Caffeine

### Key Features
- **5 Index Types**: Flat (exact), HNSW, IVF, LSH, Annoy
- **3 Storage Backends**: Memory, Disk, Hybrid
- **Vector Compression**: Scalar & Product Quantization
- **6 Distance Metrics**: Cosine, Euclidean, Dot Product, Manhattan, Hamming, Jaccard
- **REST API**: Full-featured with auth, rate limiting, metrics, health checks
- **CLI**: Interactive shell and command-line interface
- **Batch Operations**: 1.5x-5x performance improvement
- **Production Ready**: HTTPS, authentication, monitoring, graceful shutdown

---

## Codebase Structure

### Directory Layout

```
veccy/
├── src/
│   ├── main/java/com/veccy/
│   │   ├── base/              # Core interfaces and abstractions
│   │   ├── indices/           # Index implementations (Flat, HNSW, IVF, LSH, Annoy)
│   │   ├── storage/           # Storage backends (Memory, Disk, Hybrid)
│   │   ├── config/            # Type-safe configuration classes
│   │   ├── factory/           # Factory for creating DB instances
│   │   ├── client/            # VectorDBClient (main API)
│   │   ├── rest/              # REST API server and handlers
│   │   │   ├── handlers/      # REST endpoint handlers
│   │   │   ├── middleware/    # Auth, rate limiting, logging, metrics
│   │   │   ├── dto/           # Data transfer objects
│   │   │   ├── config/        # REST server configuration
│   │   │   ├── metrics/       # Metrics collection
│   │   │   └── validation/    # Request validation
│   │   ├── cli/               # Command-line interface
│   │   │   └── commands/      # CLI command implementations
│   │   ├── health/            # Health check system
│   │   │   └── checks/        # Health check implementations
│   │   ├── processing/        # Document processing pipeline
│   │   │   ├── parsers/       # PDF, Office, HTML, Text parsers
│   │   │   ├── chunking/      # Text chunking strategies
│   │   │   └── embeddings/    # Embedding processors (ONNX, TF-IDF, External API)
│   │   ├── quantization/      # Vector compression
│   │   ├── persistence/       # Persistence management
│   │   ├── utils/             # Utility classes
│   │   ├── exceptions/        # Custom exception hierarchy
│   │   └── examples/          # Usage examples
│   └── test/java/com/veccy/   # Mirror structure of main/
├── docs/                      # Documentation (MD files)
├── k8s/                       # Kubernetes deployment manifests
├── docker/                    # Docker configurations
├── scripts/                   # Utility scripts (Python)
├── .github/workflows/         # CI/CD pipelines
├── pom.xml                    # Maven configuration
├── Dockerfile                 # Production Docker image
├── docker-compose.yml         # Docker Compose setup
└── README.md                  # Main project documentation
```

### Package Organization

**Core Packages** (stable, rarely change):
- `com.veccy.base`: Core interfaces (`VectorDB`, `Index`, `SearchResult`, `Page`)
- `com.veccy.config`: Type-safe configuration classes using Builder pattern
- `com.veccy.exceptions`: Custom exception hierarchy extending `VeccyException`

**Index Packages** (extend for new index types):
- `com.veccy.indices`: All index implementations
- Each index extends `AbstractIndex` and implements `Index`

**Storage Packages** (extend for new backends):
- `com.veccy.storage`: All storage backend implementations
- Each storage implements `StorageBackend` interface

**API Packages** (frequently modified):
- `com.veccy.rest`: REST API server and related components
- `com.veccy.cli`: Command-line interface components

**Processing Packages** (extend for new data types):
- `com.veccy.processing`: Document processing, chunking, embeddings

---

## Architecture & Design Patterns

### Core Design Patterns

#### 1. **Factory Pattern**
- **Location**: `com.veccy.factory.VectorDBFactory`
- **Purpose**: Centralized creation of `VectorDBClient` instances
- **Convention**: Static factory methods with descriptive names
- **Example**:
  ```java
  VectorDBClient db = VectorDBFactory.createHighPerformance();
  VectorDBClient db = VectorDBFactory.createPersistent(dataDir, useHNSW);
  VectorDBClient db = VectorDBFactory.createCustom(storageConfig, indexConfig, quantConfig, persistConfig);
  ```

#### 2. **Builder Pattern**
- **Location**: All `*Config` classes in `com.veccy.config`
- **Purpose**: Type-safe, fluent configuration
- **Convention**: Immutable config objects with `.builder()` pattern
- **Example**:
  ```java
  HNSWConfig config = HNSWConfig.builder()
      .metric(Metric.COSINE)
      .m(16)
      .efConstruction(200)
      .efSearch(50)
      .build();
  ```

#### 3. **Strategy Pattern**
- **Location**: `com.veccy.processing.chunking.ChunkingStrategy`
- **Purpose**: Pluggable text chunking algorithms
- **Implementations**: `FixedSizeChunkingStrategy`, `SentenceChunkingStrategy`, `ParagraphChunkingStrategy`

#### 4. **Template Method Pattern**
- **Location**: `com.veccy.base.AbstractIndex`
- **Purpose**: Common index operations with customizable search logic
- **Pattern**: Abstract class defines skeleton, subclasses implement specific algorithms

#### 5. **Facade Pattern**
- **Location**: `com.veccy.client.VectorDBClient`
- **Purpose**: Simplified API over complex storage + index + quantization + persistence
- **Key**: Hides complexity, provides clean interface

### Layered Architecture

```
┌─────────────────────────────────────────────────┐
│          Application Layer                       │
│    (CLI, REST API, Examples)                    │
├─────────────────────────────────────────────────┤
│          Client Layer                            │
│    (VectorDBClient - main API facade)           │
├─────────────────────────────────────────────────┤
│          Service Layer                           │
│    (Index, Storage, Quantization, Persistence)  │
├─────────────────────────────────────────────────┤
│          Core Layer                              │
│    (Interfaces, Abstractions, Config)           │
└─────────────────────────────────────────────────┘
```

### Key Interfaces

1. **`VectorDB`** (`com.veccy.base.VectorDB`)
   - Core database operations
   - All implementations extend this
   - Methods: `initialize()`, `insert()`, `search()`, `delete()`, `update()`, `getStats()`, `close()`

2. **`Index`** (`com.veccy.base.Index`)
   - Vector indexing operations
   - Implementations: `FlatIndex`, `HNSWIndex`, `IVFIndex`, `LSHIndex`, `AnnoyIndex`
   - Key method: `search(double[] query, int k)`

3. **`StorageBackend`** (`com.veccy.storage.StorageBackend`)
   - Vector storage operations
   - Implementations: `MemoryStorage`, `DiskStorage`, `HybridStorage`
   - Methods: `store()`, `retrieve()`, `delete()`, `update()`

4. **`Quantizer`** (`com.veccy.quantization.Quantizer`)
   - Vector compression
   - Implementations: `ScalarQuantizer`, `ProductQuantizer`
   - Methods: `train()`, `compress()`, `decompress()`

5. **`PersistenceManager`** (`com.veccy.persistence.PersistenceManager`)
   - Save/load database state
   - Implementation: `TensorPersistence`
   - Methods: `save()`, `load()`

---

## Development Workflow

### Build Process

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package (creates fat JAR)
mvn package

# Skip tests during package
mvn package -DskipTests

# Run specific test
mvn test -Dtest=HNSWIndexTest

# Generate coverage report
mvn jacoco:report
```

**Output Artifacts**:
- `target/veccy-0.1.jar` - Standard JAR
- `target/veccy-0.1-fat.jar` - Fat JAR with all dependencies (use this!)
- `target/veccy-0.1-sources.jar` - Source JAR
- `target/veccy-0.1-javadoc.jar` - Javadoc JAR

### CI/CD Pipeline

**GitHub Actions Workflows** (`.github/workflows/`):

1. **`ci.yml`** - Main CI pipeline
   - **Triggers**: Push to main/develop, PRs
   - **Matrix**: Ubuntu/Windows/macOS × Java 21/23
   - **Jobs**:
     - Build and test
     - Code quality (checkstyle, spotbugs)
     - Integration tests
     - Example tests
     - Dependency security check
   - **Artifacts**: JAR files, test reports, coverage

2. **`release.yml`** - Release automation
   - **Trigger**: Tags matching `v*`
   - Creates GitHub release with fat JAR

3. **`performance.yml`** - Performance benchmarks
   - Tracks regression in index build/query times

4. **`docs.yml`** - Documentation checks
   - Validates Markdown links
   - Ensures docs are up-to-date

5. **`dependency-update.yml`** - Dependency management
   - Automated dependency updates

6. **`codeql.yml`** - Security scanning
   - CodeQL analysis for vulnerabilities

### Running the Application

**CLI Mode**:
```bash
# Interactive mode
java -jar target/veccy-0.1-fat.jar

# Direct command
java -jar target/veccy-0.1-fat.jar help
java -jar target/veccy-0.1-fat.jar init my_db --dimensions 768 --index hnsw
```

**REST API Mode**:
```bash
# Default configuration (development)
java -cp target/veccy-0.1-fat.jar com.veccy.rest.VeccyRestServer

# Production mode
java -cp target/veccy-0.1-fat.jar com.veccy.rest.VeccyRestServer --production

# Custom port
java -cp target/veccy-0.1-fat.jar com.veccy.rest.VeccyRestServer --port 8080
```

**Running Examples**:
```bash
mvn exec:java -Dexec.mainClass="com.veccy.examples.SimpleExample"
mvn exec:java -Dexec.mainClass="com.veccy.examples.RAGDemo"
```

---

## Code Conventions

### Naming Conventions

**Classes**:
- Interfaces: Descriptive nouns (`VectorDB`, `Index`, `StorageBackend`)
- Implementations: Interface name + strategy (`FlatIndex`, `MemoryStorage`, `HNSWIndex`)
- Abstract classes: `Abstract` prefix (`AbstractIndex`)
- Config classes: Feature + `Config` (`HNSWConfig`, `RestConfig`)
- Exceptions: Error type + `Exception` (`ConfigurationException`, `IndexException`)
- DTOs: Purpose + `Request`/`Response` (`CreateDatabaseRequest`, `SearchResponse`)
- Tests: Class name + `Test` (`HNSWIndexTest`)

**Methods**:
- Getters: `getPropertyName()` (not `property()`)
- Booleans: `is`/`has`/`can` prefix (`isInitialized()`, `hasVectors()`)
- Builders: `builder()` static method, fluent setters
- Factory methods: Descriptive verb + noun (`createHighPerformance()`, `createPersistent()`)

**Variables**:
- camelCase for all variables
- Constants: `UPPER_SNAKE_CASE`
- Private fields: prefix with `this.` when assigning in constructors

**Packages**:
- All lowercase, no underscores
- Singular nouns (except `indices`, `examples`)
- Match feature area (`rest`, `cli`, `processing`, `quantization`)

### Code Style

**Formatting**:
- Indentation: 4 spaces (NO TABS)
- Line length: 120 characters (relaxed, not strict)
- Braces: K&R style (opening brace on same line)
- Imports: Group by `java.*`, `javax.*`, third-party, `com.veccy.*`

**Comments**:
- Javadoc for all public classes and methods
- Use `/**` for class/method documentation
- Use `//` for inline explanations
- Focus on *why*, not *what* (code should be self-documenting)

**Example**:
```java
/**
 * Creates a high-performance vector database with HNSW index.
 * Optimized for sub-millisecond queries on million-vector datasets.
 *
 * @return configured and initialized VectorDBClient
 */
public static VectorDBClient createHighPerformance() {
    // HNSW provides best balance of speed and accuracy for most use cases
    Map<String, Object> storageConfig = new HashMap<>();
    StorageBackend storage = new MemoryStorage(storageConfig);

    HNSWConfig config = HNSWConfig.builder()
            .metric(Metric.COSINE)
            .m(32)  // Higher M = better accuracy, more memory
            .efConstruction(400)
            .efSearch(100)
            .build();
    Index index = new HNSWIndex(config);

    VectorDBClient client = new VectorDBClient(storage, index);
    client.initialize();

    return client;
}
```

### Error Handling

**Exception Hierarchy**:
```
VeccyException (base, extends RuntimeException)
├── ConfigurationException
├── IndexException
├── StorageException
├── QuantizationException
├── PersistenceException
├── ProcessingException
├── ParsingException
├── ValidationException
└── ResourceException
```

**Conventions**:
- Use specific exception types (not generic `Exception`)
- Include context in exception messages
- Validate input at API boundaries (REST handlers, CLI commands)
- Let exceptions bubble up from core layer
- Catch and wrap at application boundaries
- Log at the point of handling, not throwing

**Example**:
```java
// Good
if (dimensions <= 0) {
    throw new ConfigurationException(
        "Dimensions must be positive, got: " + dimensions
    );
}

// Bad - too generic
if (dimensions <= 0) {
    throw new RuntimeException("Invalid dimensions");
}
```

### Logging Conventions

**Logger Setup**:
```java
private static final Logger logger = LoggerFactory.getLogger(ClassName.class);
```

**Log Levels**:
- `ERROR`: Serious errors requiring immediate attention
- `WARN`: Degraded functionality, misconfigurations, deprecated usage
- `INFO`: Important state changes, startup/shutdown, major operations
- `DEBUG`: Detailed diagnostic information
- `TRACE`: Very fine-grained debugging (rarely used)

**Best Practices**:
- Use parameterized logging: `logger.info("Inserted {} vectors", count)`
- Include context: correlation IDs, database names, operation types
- Avoid logging sensitive data (API keys, user data)
- Log entry/exit for major operations at INFO level
- Use DEBUG for detailed flow within operations

---

## Key Abstractions

### 1. VectorDB Interface

**Location**: `com.veccy.base.VectorDB`

**Purpose**: Core contract for all vector database implementations

**Key Methods**:
```java
public interface VectorDB extends AutoCloseable {
    void initialize();
    List<String> insert(double[][] vectors, List<Map<String, Object>> metadata);
    List<SearchResult> search(double[] queryVector, int k);
    boolean delete(List<String> ids);
    boolean update(String id, double[] vector, Map<String, Object> metadata);
    Map<String, Object> getStats();
    boolean isInitialized();
    void close();
}
```

**Important**: The main implementation is `VectorDBClient`, NOT a class named `VectorDBImpl`. Users interact with `VectorDBClient`.

### 2. Index Abstraction

**Base Class**: `com.veccy.base.AbstractIndex`

**Implementations**:
- `FlatIndex`: Exact brute-force search (100% accuracy, slow for large datasets)
- `HNSWIndex`: Hierarchical Navigable Small World (best general purpose)
- `IVFIndex`: Inverted File (memory efficient for 1M+ vectors)
- `LSHIndex`: Locality-Sensitive Hashing (very fast, lower accuracy)
- `AnnoyIndex`: Tree-based (read-heavy workloads)

**Adding a New Index**:
1. Create class in `com.veccy.indices` extending `AbstractIndex`
2. Create corresponding `*Config` class in `com.veccy.config`
3. Implement `build()` and `search()` methods
4. Add factory method in `VectorDBFactory.createIndex()`
5. Write comprehensive tests in `src/test/java/com/veccy/indices/`
6. Update documentation

**Template**:
```java
package com.veccy.indices;

public class NewIndex extends AbstractIndex {
    private final NewIndexConfig config;

    public NewIndex(NewIndexConfig config) {
        this.config = config;
    }

    @Override
    public void build(double[][] vectors) {
        // Index construction logic
    }

    @Override
    public List<SearchResult> search(double[] query, int k) {
        // Search implementation
    }
}
```

### 3. Storage Backend Abstraction

**Interface**: `com.veccy.storage.StorageBackend`

**Implementations**:
- `MemoryStorage`: In-memory HashMap (fast, volatile)
- `DiskStorage`: File-based persistence (durable, slower)
- `HybridStorage`: LRU cache + disk (balanced)

**Key Methods**:
```java
void store(String id, double[] vector, Map<String, Object> metadata);
VectorWithMetadata retrieve(String id);
boolean delete(String id);
boolean update(String id, double[] vector, Map<String, Object> metadata);
Collection<String> getAllIds();
```

### 4. VectorDBClient (Main API)

**Location**: `com.veccy.client.VectorDBClient`

**Purpose**: Facade over storage, index, quantization, and persistence

**Composition**:
```java
public class VectorDBClient implements VectorDB {
    private final StorageBackend storage;
    private final Index index;
    private final Quantizer quantizer;  // optional
    private final PersistenceManager persistence;  // optional
    private final Map<String, Object> config;
}
```

**Important**: This is the class users interact with. It orchestrates all components.

---

## Testing Strategy

### Test Organization

Tests mirror the `src/main/java` structure in `src/test/java`.

**Test Types**:
1. **Unit Tests**: Test individual classes in isolation (use Mockito)
2. **Integration Tests**: Test component interactions (in `com.veccy.integration`)
3. **Example Tests**: Validate example code works (in `com.veccy.examples`)

### Testing Conventions

**File Naming**:
- Unit tests: `ClassNameTest.java`
- Integration tests: `FeatureIntegrationTest.java`
- Examples: `FeatureExample.java` (in main, runnable)

**Test Method Naming**:
Pattern: `test<Scenario>_<ExpectedBehavior>`

**Examples**:
```java
@Test
void testInsert_SingleVector_ReturnsId() { }

@Test
void testSearch_EmptyIndex_ReturnsEmptyList() { }

@Test
void testHNSW_MillionVectors_SubMillisecondQuery() { }

@Test
void testRateLimiter_ExceedsLimit_Returns429() { }
```

**Test Structure** (AAA Pattern):
```java
@Test
void testFeature_Scenario_ExpectedOutcome() {
    // Arrange
    VectorDBClient db = VectorDBFactory.createSimple();
    double[][] vectors = {{0.1, 0.2}, {0.3, 0.4}};

    // Act
    List<String> ids = db.insert(vectors, null);

    // Assert
    assertEquals(2, ids.size());
    assertNotNull(ids.get(0));
}
```

### Test Coverage Goals

- **Core Components** (base, indices, storage): 90%+ coverage
- **REST API**: 85%+ coverage (handlers, middleware, validation)
- **CLI**: 70%+ coverage (command execution)
- **Utils & Examples**: 60%+ coverage

**Current Status**: 471+ tests passing across all modules

### Mocking Guidelines

**When to Mock**:
- External dependencies (ONNX runtime, file system for unit tests)
- Slow operations (disk I/O, network calls)
- Complex dependencies in unit tests

**When NOT to Mock**:
- Core business logic (test real implementations)
- Simple data structures (SearchResult, Config objects)
- Integration tests (use real components)

**Example**:
```java
@Mock
private StorageBackend mockStorage;

@Mock
private Index mockIndex;

@BeforeEach
void setUp() {
    MockitoAnnotations.openMocks(this);
    when(mockStorage.retrieve(anyString()))
        .thenReturn(new VectorWithMetadata(id, vector, metadata));
}
```

---

## Common Tasks

### Task 1: Add a New REST Endpoint

**Steps**:

1. **Create DTO classes** (if needed) in `com.veccy.rest.dto`:
   ```java
   public record NewFeatureRequest(String param1, int param2) {}
   public record NewFeatureResponse(String result) {}
   ```

2. **Add handler method** in appropriate handler class (`com.veccy.rest.handlers`):
   ```java
   public class DatabaseHandler {
       public void handleNewFeature(Context ctx) {
           NewFeatureRequest req = ctx.bodyAsClass(NewFeatureRequest.class);
           // Validate input
           // Process request
           // Return response
           ctx.json(new ApiResponse<>(true, response, null));
       }
   }
   ```

3. **Register route** in `VeccyRestServer.setupRoutes()`:
   ```java
   app.post("/api/v1/databases/:name/new-feature",
            databaseHandler::handleNewFeature);
   ```

4. **Add validation** in handler or use `ValidationUtils`

5. **Write tests** in `src/test/java/com/veccy/rest/handlers/`:
   ```java
   @Test
   void testNewFeature_ValidInput_ReturnsSuccess() { }
   ```

6. **Update documentation** in `docs/REST_API.md`

### Task 2: Add a New Index Type

**Steps**:

1. **Create config class** in `com.veccy.config`:
   ```java
   public record NewIndexConfig(
       Metric metric,
       int customParam
   ) implements IndexConfig {
       public static Builder builder() {
           return new Builder();
       }

       public static class Builder { /* builder implementation */ }
   }
   ```

2. **Create index implementation** in `com.veccy.indices`:
   ```java
   public class NewIndex extends AbstractIndex {
       private final NewIndexConfig config;
       // Implementation
   }
   ```

3. **Add factory method** in `VectorDBFactory.createIndex()`:
   ```java
   case "new_index" -> new NewIndex(/* parse config */);
   ```

4. **Write comprehensive tests**:
   - Unit tests: `NewIndexTest.java`
   - Performance tests: Build time, query time, accuracy
   - Edge cases: Empty index, single vector, duplicate vectors

5. **Update README.md** and `docs/INDEX.md` with new index info

### Task 3: Add a New CLI Command

**Steps**:

1. **Create command class** in `com.veccy.cli.commands`:
   ```java
   public class NewCommand implements Command {
       @Override
       public String getName() { return "new-command"; }

       @Override
       public String getDescription() { return "Description"; }

       @Override
       public String getUsage() { return "new-command <args>"; }

       @Override
       public void execute(CLIContext context, String[] args) {
           // Implementation
       }
   }
   ```

2. **Register command** in `VeccyCLI`:
   ```java
   commands.put("new-command", new NewCommand());
   ```

3. **Write tests** in `src/test/java/com/veccy/cli/commands/`

4. **Update** `docs/CLI.md`

### Task 4: Add a New Health Check

**Steps**:

1. **Create health check** in `com.veccy.health.checks`:
   ```java
   public class NewHealthCheck implements HealthCheck {
       @Override
       public String getName() { return "new_check"; }

       @Override
       public HealthCheckResult check() {
           // Check logic
           return new HealthCheckResult(HealthStatus.UP, "All good");
       }
   }
   ```

2. **Register** in `VeccyHealthManager` or `VeccyRestServer`

3. **Test** in `src/test/java/com/veccy/health/checks/`

### Task 5: Add a New Distance Metric

**Steps**:

1. **Add to** `com.veccy.config.Metric` enum:
   ```java
   NEW_METRIC("new_metric")
   ```

2. **Implement calculation** in relevant index classes (likely in `AbstractIndex` or utils)

3. **Add tests** for metric calculation accuracy

4. **Update documentation**

---

## Configuration Patterns

### Type-Safe Configuration (Preferred)

**Pattern**: Immutable records with Builder pattern

**Example**:
```java
public record HNSWConfig(
    Metric metric,
    int m,
    int efConstruction,
    int efSearch
) implements IndexConfig {

    // Validation in compact constructor
    public HNSWConfig {
        if (m <= 0) throw new ConfigurationException("m must be positive");
        if (efConstruction <= 0) throw new ConfigurationException("efConstruction must be positive");
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Metric metric = Metric.COSINE;
        private int m = 16;
        private int efConstruction = 200;
        private int efSearch = 50;

        public Builder metric(Metric metric) {
            this.metric = metric;
            return this;
        }

        public Builder m(int m) {
            this.m = m;
            return this;
        }

        public HNSWConfig build() {
            return new HNSWConfig(metric, m, efConstruction, efSearch);
        }
    }
}
```

### Map-Based Configuration (Legacy, for backwards compatibility)

**Used in**: Factory methods, REST API requests

**Pattern**: Convert to type-safe config ASAP

**Example**:
```java
Map<String, Object> config = new HashMap<>();
config.put("type", "hnsw");
config.put("metric", "cosine");
config.put("m", 16);

// Convert to type-safe
Index index = VectorDBFactory.createIndex(config);
```

**Convention**: All `create*()` factory methods accept Map configs and convert internally to type-safe configs.

### REST Configuration

**Three Modes**:

1. **Development** (default):
   ```java
   RestConfig config = RestConfig.defaultConfig();
   ```
   - Port 7878
   - No authentication
   - Wildcard CORS
   - Verbose error messages

2. **Production**:
   ```java
   RestConfig config = RestConfig.productionConfig();
   ```
   - Port 8080
   - API key authentication
   - Rate limiting
   - Explicit CORS
   - Secure error messages

3. **Custom**:
   ```java
   RestConfig config = new RestConfig.Builder()
       .port(9000)
       .enableHttps(true)
       .keystorePath("/path/to/keystore.jks")
       .securityConfig(securityConfig)
       .build();
   ```

---

## Important Files

### Entry Points

1. **CLI Entry Point**:
   - `src/main/java/com/veccy/cli/VeccyCLI.java`
   - Main method for command-line interface
   - Configured in pom.xml as main class for fat JAR

2. **REST API Entry Point**:
   - `src/main/java/com/veccy/rest/VeccyRestServer.java`
   - Main method for REST server
   - Can be run standalone or embedded

### Configuration Files

1. **Maven POM**:
   - `pom.xml`
   - Dependencies, plugins, build configuration
   - Fat JAR creation via maven-shade-plugin
   - **IMPORTANT**: Main-Class is `com.veccy.cli.VeccyCLI`

2. **Logback Config**:
   - `src/main/resources/logback.xml` (if exists)
   - Logging configuration

3. **Docker**:
   - `Dockerfile` - Production image
   - `Dockerfile.dev` - Development image
   - `docker-compose.yml` - Multi-service setup
   - `docker-compose.dev.yml` - Development setup

4. **Kubernetes**:
   - `k8s/deployment.yaml` - K8s deployment
   - `k8s/service.yaml` - K8s service
   - `k8s/ingress.yaml` - Ingress configuration
   - `k8s/configmap.yaml` - Configuration
   - `k8s/hpa.yaml` - Horizontal Pod Autoscaler

### Core Interfaces

1. `src/main/java/com/veccy/base/VectorDB.java` - Main database interface
2. `src/main/java/com/veccy/base/Index.java` - Index interface
3. `src/main/java/com/veccy/storage/StorageBackend.java` - Storage interface
4. `src/main/java/com/veccy/client/VectorDBClient.java` - Main client API

### Documentation

1. `README.md` - Main project documentation
2. `docs/INDEX.md` - Documentation index
3. `docs/REST_API.md` - Complete REST API guide
4. `docs/CLI.md` - CLI usage guide
5. `docs/BUILDING.md` - Build instructions
6. `docs/DOCKER.md` - Docker deployment guide

---

## Deployment & Production

### Docker Deployment

**Build Image**:
```bash
docker build -t veccy:0.1 .
```

**Run Container**:
```bash
docker run -d \
  -p 8080:8080 \
  -e API_KEY=your-secret-key \
  -v /data:/app/data \
  veccy:0.1
```

**Docker Compose**:
```bash
docker-compose up -d
```

### Kubernetes Deployment

**Apply Manifests**:
```bash
kubectl apply -f k8s/
```

**Key Resources**:
- **Deployment**: 3 replicas, rolling updates
- **Service**: LoadBalancer type
- **Ingress**: HTTPS with cert-manager
- **ConfigMap**: Configuration
- **HPA**: Auto-scaling based on CPU/memory
- **PVC**: Persistent storage

**Health Checks**:
- Liveness: `/health` endpoint
- Readiness: `/health` endpoint
- Startup: 30s grace period

### Production Checklist

- [ ] Use `RestConfig.productionConfig()`
- [ ] Set API keys via environment variables
- [ ] Enable HTTPS with valid certificates
- [ ] Configure explicit CORS origins (no wildcards)
- [ ] Set up rate limiting
- [ ] Enable metrics collection
- [ ] Configure persistent storage (disk or hybrid)
- [ ] Set up health check monitoring
- [ ] Configure log aggregation (ELK, Splunk, etc.)
- [ ] Set appropriate JVM heap size (`-Xmx4g` or higher)
- [ ] Use a reverse proxy (Nginx, Apache)
- [ ] Set up database backups (persistence files)
- [ ] Monitor disk space for vector storage
- [ ] Configure alerts for health check failures

### Performance Tuning

**JVM Options**:
```bash
java -Xmx4g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -cp veccy-0.1-fat.jar \
     com.veccy.rest.VeccyRestServer
```

**Index Selection**:
- **< 10K vectors**: Flat (exact search)
- **10K - 1M vectors**: HNSW (best balance)
- **> 1M vectors**: IVF or LSH (memory efficient)

**Storage Selection**:
- **Development**: Memory
- **Production < 100K vectors**: Hybrid (512MB cache)
- **Production > 1M vectors**: Disk with larger cache

---

## Documentation Standards

### When to Update Documentation

**Always update docs when**:
- Adding new REST endpoints → `docs/REST_API.md`
- Adding new CLI commands → `docs/CLI.md`
- Adding new index types → `README.md`, `docs/INDEX.md`
- Changing configuration options → `docs/INDEX.md`, `README.md`
- Adding deployment options → `docs/DOCKER.md`

### Documentation Style

**Markdown**:
- Use ATX-style headers (`#`, `##`, `###`)
- Use fenced code blocks with language hints (```java, ```bash, ```yaml)
- Include working examples (copy-pasteable)
- Add table of contents for long documents

**Code Examples**:
- Keep examples simple and focused
- Include imports
- Show expected output
- Use realistic data

**API Documentation**:
- Document all parameters
- Show request/response examples
- Include curl commands
- Document error codes

### Javadoc Standards

**Required**:
- All public classes
- All public methods
- Complex private methods

**Format**:
```java
/**
 * Brief one-line description.
 * <p>
 * Longer description with details, context, and usage notes.
 * Can span multiple paragraphs.
 *
 * @param paramName description of parameter
 * @return description of return value
 * @throws ExceptionType when and why this is thrown
 */
```

---

## Best Practices for AI Assistants

### When Making Changes

1. **Understand the Context**:
   - Read related code before modifying
   - Check existing tests to understand expected behavior
   - Review similar implementations for patterns

2. **Follow Existing Patterns**:
   - Use the same design patterns (Builder, Factory, Strategy)
   - Match naming conventions
   - Mirror existing code structure

3. **Maintain Type Safety**:
   - Prefer type-safe configs over Maps where possible
   - Use enums for fixed sets of values
   - Validate input at boundaries

4. **Test Thoroughly**:
   - Write unit tests for new code
   - Run existing tests to ensure no regressions
   - Add integration tests for new features

5. **Update Documentation**:
   - Update relevant .md files
   - Add Javadoc comments
   - Update README.md if adding major features

6. **Consider Backwards Compatibility**:
   - Don't break existing APIs without discussion
   - Deprecate before removing
   - Provide migration paths

### Code Review Checklist

Before submitting changes, verify:

- [ ] Code follows naming conventions
- [ ] All public APIs have Javadoc
- [ ] Tests added and passing (`mvn test`)
- [ ] No compiler warnings
- [ ] Documentation updated
- [ ] Changes are backwards compatible (or properly deprecated)
- [ ] Error handling is appropriate
- [ ] Logging is appropriate (level and detail)
- [ ] Configuration follows existing patterns
- [ ] Performance implications considered
- [ ] Security implications considered (especially for REST API)

### Common Pitfalls to Avoid

1. **Don't use `VectorDB` interface directly** - Use `VectorDBClient`
2. **Don't mix Map configs with type-safe configs** - Convert at boundaries
3. **Don't add dependencies without careful consideration** - Check pom.xml first
4. **Don't skip validation** - Validate all user input
5. **Don't log sensitive data** - Sanitize logs in production
6. **Don't use `System.out.println`** - Use SLF4J logger
7. **Don't create your own thread pools** - Use existing infrastructure
8. **Don't hardcode paths** - Use configuration or system properties

---

## Quick Reference

### Package Purposes

| Package | Purpose | Stability |
|---------|---------|-----------|
| `com.veccy.base` | Core interfaces | Very Stable |
| `com.veccy.indices` | Index implementations | Stable, extensible |
| `com.veccy.storage` | Storage backends | Stable, extensible |
| `com.veccy.config` | Configuration classes | Stable |
| `com.veccy.factory` | Object creation | Stable |
| `com.veccy.client` | Main API | Very Stable |
| `com.veccy.rest` | REST API | Frequently modified |
| `com.veccy.cli` | CLI interface | Frequently modified |
| `com.veccy.processing` | Document processing | Extensible |
| `com.veccy.quantization` | Compression | Stable, extensible |
| `com.veccy.exceptions` | Exception types | Stable |
| `com.veccy.utils` | Utilities | Stable |
| `com.veccy.health` | Health checks | Frequently modified |

### Key Commands

```bash
# Build
mvn clean package

# Test
mvn test

# Run CLI
java -jar target/veccy-0.1-fat.jar

# Run REST API
java -cp target/veccy-0.1-fat.jar com.veccy.rest.VeccyRestServer

# Run example
mvn exec:java -Dexec.mainClass="com.veccy.examples.SimpleExample"

# Docker build
docker build -t veccy:0.1 .

# Docker run
docker run -p 8080:8080 veccy:0.1

# K8s deploy
kubectl apply -f k8s/
```

### Useful Links

- **Repository**: https://github.com/skanga/veccy
- **Issues**: https://github.com/skanga/veccy/issues
- **Discussions**: https://github.com/skanga/veccy/discussions
- **CI/CD**: https://github.com/skanga/veccy/actions

---

## Version History

- **v0.1** (2025-11-29): Initial CLAUDE.md creation
  - Comprehensive codebase documentation
  - Architecture and design patterns
  - Development workflows
  - Testing strategy
  - Common tasks and best practices

---

**End of CLAUDE.md**

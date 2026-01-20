# Veccy Examples

This directory contains example applications demonstrating various Veccy features.

---

## Available Examples

### 1. **SimpleExample.java**
Basic vector database operations - insert, search, delete.

```bash
mvn exec:java -Dexec.mainClass="com.veccy.examples.SimpleExample"
```

**Features:**
- In-memory vector storage
- Flat index (exact search)
- Basic CRUD operations

---

### 2. **FactoryExample.java**
Using VectorDBFactory to create different database configurations.

```bash
mvn exec:java -Dexec.mainClass="com.veccy.examples.FactoryExample"
```

**Features:**
- Factory pattern usage
- Multiple index types (HNSW, IVF, LSH)
- Persistent storage
- Quantization

---

### 3. **TypeSafeConfigExample.java**
Type-safe configuration examples for HNSW indexes.

```bash
mvn exec:java -Dexec.mainClass="com.veccy.examples.TypeSafeConfigExample"
```

**Features:**
- Builder pattern configuration
- Defaults and validation errors
- Deterministic behavior via random seed
- High-quality vs fast construction presets

---

### 4. **HealthCheckExample.java**
Health checks, HTTP endpoints, and custom probes.

```bash
mvn exec:java -Dexec.mainClass="com.veccy.examples.HealthCheckExample"
```

**Features:**
- Health manager checks for storage and index
- Optional HTTP health endpoints (liveness/readiness/metrics)
- Custom health check registration
- Production configuration examples

---

### 5. **BatchOperationsExample.java**
Batch insert, update, and search workflows.

```bash
mvn exec:java -Dexec.mainClass="com.veccy.examples.BatchOperationsExample"
```

**Features:**
- Bulk ingestion with batch insert
- Batch updates for vectors and metadata
- Batch search for recommendations
- Throughput logging

---

### 6. **PaginationExample.java**
Cursor-based pagination and streaming over vector IDs.

```bash
mvn exec:java -Dexec.mainClass="com.veccy.examples.PaginationExample"
```

**Features:**
- Cursor-based pagination
- Streaming API usage
- Export by pages
- Rate-limited batch processing
- Pattern matching for IDs

---

### 7. **BatchPerformanceExample.java**
Efficient batch processing of vectors.

```bash
mvn exec:java -Dexec.mainClass="com.veccy.examples.BatchPerformanceExample"
```

**Features:**
- Batch insert
- Batch search
- Performance benchmarks

---

### 8. **ONNXQuickStart.java**
Simple, minimal example for ONNX embeddings.

```bash
mvn exec:java -Dexec.mainClass="com.veccy.examples.ONNXQuickStart"
```

**Prerequisites:**
- ONNX model: `./models/all-MiniLM-L6-v2-onnx/model.onnx`

**Features:**
- Quick start template
- Step-by-step output
- Semantic search demo

---

### 9. **SentenceTransformersExample.java**
Comprehensive example using ONNX Sentence Transformers.

```bash
mvn exec:java -Dexec.mainClass="com.veccy.examples.SentenceTransformersExample"
```

**Prerequisites:**
- ONNX model exported (see [SENTENCE_TRANSFORMERS_GUIDE.md](../../../docs/SENTENCE_TRANSFORMERS_GUIDE.md))

**Features:**
- ONNX embedding processor
- Semantic text search
- Batch embedding
- Performance metrics

---

### 10. **EmbeddingComparison.java**
Compare all three embedding processor types side-by-side.

```bash
mvn exec:java -Dexec.mainClass="com.veccy.examples.EmbeddingComparison"
```

**Prerequisites:**
- Optional: ONNX model for neural embeddings
- Optional: `OPENAI_API_KEY` environment variable for API demo

**Features:**
- ONNX embedding processor demo
- External API embedding processor demo (OpenAI/Cohere)
- TF-IDF embedding processor demo
- Performance comparison
- Similarity score comparison

**Documentation:** See [EMBEDDING_OPTIONS.md](../../../docs/EMBEDDING_OPTIONS.md) for details.

---

### 11. **RAGDemoTFIDF.java**
RAG system using TF-IDF embeddings (no native libraries needed).

```bash
mvn exec:java -Dexec.mainClass="com.veccy.examples.RAGDemoTFIDF"
```

**Prerequisites:**
- None! Works immediately on any system
- Markdown files in `./docs/`

**Features:**
- Full RAG pipeline (same as RAGDemo)
- TF-IDF embeddings (keyword-based search)
- NO ONNX or native library dependencies
- Perfect for Windows systems with ONNX issues
- Training on your document corpus
- Persistent storage

**When to use:**
- Windows systems where ONNX fails
- Quick testing and development
- No access to install system libraries
- Learning/educational purposes

**Note:** TF-IDF uses keyword matching. For semantic search, use ONNX (once native issues resolved) or External API.

---

### 12. **RAGDemo.java** ⭐
Complete Retrieval-Augmented Generation (RAG) system.

```bash
mvn exec:java -Dexec.mainClass="com.veccy.examples.RAGDemo"
```

**Prerequisites:**
- ONNX model: `./models/all-MiniLM-L6-v2.onnx`
- Markdown files in `./docs/`

**Features:**
- Document processing pipeline
- Persistent vector storage
- Semantic document search
- Question answering
- Context retrieval for LLMs

**Documentation:** See [RAG_DEMO.md](../../../docs/RAG_DEMO.md) for complete guide.

---

## Getting Started

### 1. Clone and Build

```bash
git clone https://github.com/skanga/veccy.git
cd veccy
mvn clean package
```

### 2. Run Simple Example

```bash
mvn exec:java -Dexec.mainClass="com.veccy.examples.SimpleExample"
```

### 3. Try ONNX Examples

Export a Sentence Transformers model:

```bash
cd scripts
pip install sentence-transformers optimum onnx onnxruntime transformers
python export_sentence_transformer.py all-MiniLM-L6-v2
cd ..
```

Run the ONNX examples:

```bash
mvn exec:java -Dexec.mainClass="com.veccy.examples.ONNXQuickStart"
mvn exec:java -Dexec.mainClass="com.veccy.examples.RAGDemo"
```

---

## Example Use Cases

### Semantic Search
```java
// Index documents
VectorDB db = VectorDBFactory.createHighPerformance();
ONNXEmbeddingProcessor embedder = new ONNXEmbeddingProcessor();
embedder.initialize(config);

DocumentProcessor processor = new DocumentProcessor();
processor.initialize(embedder);
processor.processDocument(filePath, db);

// Search
double[] queryEmbedding = embedder.embed("your query");
List<SearchResult> results = db.search(queryEmbedding, 10);
```

### Question Answering (RAG)
See `RAGDemo.java` for complete implementation.

### Image Similarity Search
Use ONNX image embedding models (CLIP, ResNet) instead of text models.

### Recommendation Systems
Store user/item embeddings and find similar items.

---

## Documentation

- **General:** [Main README](../../../README.md)
- **RAG System:** [RAG_DEMO.md](../../../docs/RAG_DEMO.md)
- **Sentence Transformers:** [SENTENCE_TRANSFORMERS_GUIDE.md](../../../docs/SENTENCE_TRANSFORMERS_GUIDE.md)
- **REST API:** [REST_API.md](../../../docs/REST_API.md)
- **CLI:** [CLI.md](../../../docs/CLI.md)

---

## Contributing

Found a bug or want to add an example? Open an issue or pull request on GitHub!

---

## License

Examples are part of the Veccy project and follow the same license.

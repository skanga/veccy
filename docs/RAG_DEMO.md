# RAG Demo - Documentation Question Answering System

A complete demonstration of building a Retrieval-Augmented Generation (RAG) system using Veccy vector database with ONNX Sentence Transformers.

---

## Overview

This demo shows how to:

1. **Build a persistent vector database** from markdown documentation
2. **Use ONNX Sentence Transformers** for generating embeddings
3. **Process and chunk documents** for optimal retrieval
4. **Perform semantic search** to find relevant context
5. **Retrieve context** for question answering applications

The system indexes all markdown files from the `docs/` directory and allows semantic search queries to find relevant documentation chunks.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      RAG System Pipeline                     │
└─────────────────────────────────────────────────────────────┘

1. Document Ingestion
   ├─ Read markdown files from docs/
   ├─ Parse text content (TextParser)
   └─ Extract metadata (filename, size, etc.)

2. Document Chunking
   ├─ Split into 512-character chunks
   ├─ 128-character overlap between chunks
   └─ Preserve source information

3. Embedding Generation
   ├─ ONNX Runtime inference
   ├─ Sentence Transformers (all-MiniLM-L6-v2)
   └─ 384-dimensional vectors

4. Vector Storage
   ├─ Persistent disk storage (./data/rag-demo)
   ├─ HNSW index for fast similarity search
   └─ Metadata preservation

5. Question Answering
   ├─ Encode user question
   ├─ Search for top-k similar chunks
   └─ Return relevant context
```

---

## Prerequisites

### 1. ONNX Model

You need the all-MiniLM-L6-v2 model in ONNX format:

```bash
# Option 1: Use the export script (recommended)
cd scripts
python export_sentence_transformer.py all-MiniLM-L6-v2

# Option 2: Manual export with Python
pip install sentence-transformers optimum onnx onnxruntime transformers

python << EOF
from optimum.onnxruntime import ORTModelForFeatureExtraction
from transformers import AutoTokenizer

model = ORTModelForFeatureExtraction.from_pretrained(
    'sentence-transformers/all-MiniLM-L6-v2', export=True)
tokenizer = AutoTokenizer.from_pretrained(
    'sentence-transformers/all-MiniLM-L6-v2')

model.save_pretrained('./models/all-MiniLM-L6-v2-onnx')
tokenizer.save_pretrained('./models/all-MiniLM-L6-v2-onnx')
EOF
```

**Expected location:** `./models/all-MiniLM-L6-v2.onnx` or `./models/all-MiniLM-L6-v2-onnx/model.onnx`

### 2. Documentation Files

Markdown files should be in the `docs/` directory. The demo will automatically discover and process all `.md` files.

---

## Running the Demo

### Basic Run

```bash
# Compile the project
mvn compile

# Run the RAG demo
mvn exec:java -Dexec.mainClass="com.veccy.examples.RAGDemo"
```

### Expected Output

```
================================================================================
Veccy RAG Demo - Documentation Question Answering System
================================================================================

Step 1: Initialize Embedding Processor
--------------------------------------------------------------------------------
  ✓ ONNX embedding processor initialized
  ✓ Model: all-MiniLM-L6-v2 (384 dimensions)

Step 2: Create Persistent Vector Database
--------------------------------------------------------------------------------
  ✓ Persistent vector database created
  ✓ Storage location: ./data/rag-demo
  ✓ Index type: HNSW (Hierarchical Navigable Small World)

Step 3: Index Documentation
--------------------------------------------------------------------------------
  Document Processing Configuration:
    - Chunk size: 512 characters
    - Overlap: 128 characters

  Found 17 markdown files:
    - BATCH_OPERATIONS.md
    - BUILDING.md
    - CLI.md
    ...

  Processing: BATCH_OPERATIONS.md...
    ✓ Created 8 chunks
  Processing: BUILDING.md...
    ✓ Created 5 chunks
  ...

  Indexing Complete:
    ✓ Processed 17 documents
    ✓ Created 142 searchable chunks
    ✓ Time: 8432ms

Step 4: Question Answering with Semantic Search
--------------------------------------------------------------------------------

🔍 Question: "How do I use the REST API?"

   📚 Retrieved Context (in 24ms):

   1. [94.2% match] REST_API.md (chunk 0)
      # REST API Guide The Veccy REST API provides a complete HTTP interface
      for vector database operations. ## Overview The REST API runs on port
      8080 by default and provides endpoints for: - Database management -
      Vector...

   2. [89.7% match] REST_API.md (chunk 1)
      operations - Batch operations - Health checks All endpoints return JSON
      responses and support standard HTTP methods (GET, POST, PUT, DELETE). ##
      Quick Start ```bash # Start the REST server java -jar veccy.jar...

   3. [87.3% match] REST_API_REFERENCE.md (chunk 0)
      # REST API Reference Complete reference for all Veccy REST API endpoints.
      ## Base URL ``` http://localhost:8080/api/v1 ``` ## Authentication
      Currently, the REST API does not require...

   💡 In a full RAG system, this context would be sent to an LLM
      to generate a natural language answer.

--------------------------------------------------------------------------------

🔍 Question: "What are the available index types?"
...

================================================================================
Database Statistics
================================================================================
  Overall:
    - Vectors: 142

  Index:
    - Type: hnsw
    - Metric: cosine
    - M: 16
    - EF Construction: 200

  Storage:
    - Type: disk
    - Location: .\data\rag-demo

================================================================================
✅ RAG Demo Complete!
================================================================================

Next Steps:
  1. Integrate this with an LLM (OpenAI, Anthropic, etc.)
  2. Use retrieved context as input to generate answers
  3. Implement conversation history for multi-turn QA
  4. Add re-ranking for improved relevance
```

---

## Configuration

### Customizing Chunk Size

Edit `RAGDemo.java`:

```java
private static final int CHUNK_SIZE = 512;      // Characters per chunk
private static final int CHUNK_OVERLAP = 128;   // Overlap between chunks
```

**Recommendations:**
- **Small chunks (256-512)**: Better precision, more chunks
- **Large chunks (1024-2048)**: More context, fewer chunks
- **Overlap (20-30%)**: Prevents information loss at boundaries

### Changing the Model

```java
private static final String MODEL_PATH = "./models/your-model.onnx";
private static final int DIMENSIONS = 768;  // Update based on model
```

**Supported Models:**
- `all-MiniLM-L6-v2`: 384 dims, fast, general purpose (recommended)
- `all-mpnet-base-v2`: 768 dims, higher quality
- `paraphrase-MiniLM-L3-v2`: 384 dims, fastest

See [SENTENCE_TRANSFORMERS_GUIDE.md](SENTENCE_TRANSFORMERS_GUIDE.md) for more models.

### Custom Document Sources

```java
private static final String DOCS_DIR = "./my-documents";
```

The system supports any directory containing markdown files.

---

## Integration with LLMs

### OpenAI GPT Integration

```java
// After retrieving context
List<SearchResult> results = db.search(queryEmbedding, 3);

// Build context string
StringBuilder context = new StringBuilder();
for (SearchResult result : results) {
    String text = (String) result.getMetadata().get("text");
    context.append(text).append("\n\n");
}

// Call OpenAI API
String prompt = String.format(
    "Based on the following context, answer the question.\n\n" +
    "Context:\n%s\n\n" +
    "Question: %s\n\n" +
    "Answer:",
    context.toString(),
    question
);

// Use OpenAI client to get completion
// String answer = openaiClient.complete(prompt);
```

### Anthropic Claude Integration

```java
// Similar pattern with Anthropic SDK
String systemPrompt = "You are a helpful assistant that answers questions " +
                     "based on the provided documentation context.";

String userMessage = String.format(
    "Context:\n%s\n\nQuestion: %s",
    context.toString(),
    question
);

// Use Anthropic client
// String answer = anthropicClient.messages.create(
//     model="claude-3-sonnet-20240229",
//     system=systemPrompt,
//     messages=[{"role": "user", "content": userMessage}]
// );
```

---

## Performance Characteristics

### Indexing Performance

| Documents | Chunks | Indexing Time | Embeddings/sec |
|-----------|--------|---------------|----------------|
| 10        | 50     | ~2s           | ~25            |
| 17        | 142    | ~8s           | ~18            |
| 100       | 800    | ~45s          | ~18            |

**Note:** Times include document parsing, chunking, embedding generation, and vector insertion.

### Search Performance

| Vectors | Search Time (avg) | Throughput |
|---------|-------------------|------------|
| 100     | 5-10ms            | ~150 QPS   |
| 1,000   | 10-20ms           | ~75 QPS    |
| 10,000  | 20-40ms           | ~35 QPS    |

**Configuration:** HNSW index with M=16, efConstruction=200, efSearch=50

### Storage Requirements

| Component      | Size per Vector | Size for 1,000 vectors |
|----------------|-----------------|------------------------|
| Vector (384d)  | ~1.5 KB         | ~1.5 MB                |
| Metadata       | ~0.5 KB         | ~0.5 MB                |
| HNSW Index     | ~0.3 KB         | ~0.3 MB                |
| **Total**      | **~2.3 KB**     | **~2.3 MB**            |

---

## Advanced Features

### Re-ranking Results

For improved relevance, implement re-ranking:

```java
// After initial retrieval
List<SearchResult> results = db.search(queryEmbedding, 20);

// Re-rank using cross-encoder or other methods
List<SearchResult> reranked = reranker.rerank(question, results);

// Return top 3 after re-ranking
return reranked.subList(0, 3);
```

### Hybrid Search

Combine vector search with keyword search:

```java
// Vector search
List<SearchResult> vectorResults = db.search(queryEmbedding, 10);

// Keyword search (BM25 or similar)
List<SearchResult> keywordResults = keywordIndex.search(question, 10);

// Combine and deduplicate
List<SearchResult> combined = combineResults(vectorResults, keywordResults);
```

### Query Expansion

Improve recall with query expansion:

```java
// Generate query variations
List<String> expandedQueries = List.of(
    question,
    reformulateQuestion(question),
    addSynonyms(question)
);

// Search with all variations
List<SearchResult> allResults = new ArrayList<>();
for (String query : expandedQueries) {
    double[] embedding = embedder.embed(query);
    allResults.addAll(db.search(embedding, 5));
}

// Deduplicate and rank
return deduplicateAndRank(allResults);
```

---

## Troubleshooting

### Model Not Found

```
❌ ONNX model not found at: ./models/all-MiniLM-L6-v2.onnx
```

**Solution:** Export the model using the instructions in [Prerequisites](#prerequisites).

### Out of Memory

```
java.lang.OutOfMemoryError: Java heap space
```

**Solution:** Increase JVM heap size:

```bash
export MAVEN_OPTS="-Xmx4g"
mvn exec:java -Dexec.mainClass="com.veccy.examples.RAGDemo"
```

### Slow Indexing

**Solutions:**
- Reduce chunk overlap
- Increase chunk size
- Process documents in parallel
- Use batch embedding (already implemented)

### Poor Search Results

**Solutions:**
- Adjust chunk size (try 768 or 1024 characters)
- Use a better embedding model (e.g., all-mpnet-base-v2)
- Increase number of retrieved chunks (k=5 or k=10)
- Implement re-ranking
- Add query expansion

---

## Code Structure

```
src/main/java/com/veccy/examples/RAGDemo.java
├─ main()                       # Entry point
├─ runDemo()                    # Main pipeline
├─ indexDocumentation()         # Document processing
├─ performQuestionAnswering()   # Interactive Q&A
├─ answerQuestion()             # Single question handler
└─ printSetupInstructions()     # Help message
```

### Key Classes Used

- **VectorDBFactory**: Creates persistent database instances
- **ONNXEmbeddingProcessor**: Generates embeddings
- **DocumentProcessor**: Orchestrates parsing, chunking, embedding
- **TextParser**: Parses markdown files
- **FixedSizeChunkingStrategy**: Splits documents into chunks
- **VectorDB**: Main database interface
- **SearchResult**: Contains vector ID, distance, and metadata

---

## Next Steps

1. **Add More Document Types**
   - PDF files (PDFParser)
   - Office documents (OfficeParser)
   - HTML files (HTMLParser)

2. **Implement Conversation Memory**
   - Store conversation history
   - Include previous Q&A in context
   - Multi-turn interactions

3. **Add LLM Integration**
   - OpenAI GPT-4
   - Anthropic Claude
   - Local models (LLaMA, Mistral)

4. **Build a Web UI**
   - REST API endpoints for search
   - Frontend for interactive Q&A
   - Display source documents

5. **Production Deployment**
   - Docker containerization
   - Load balancing
   - Monitoring and logging
   - Rate limiting

---

## References

- [Veccy Documentation](../README.md)
- [Sentence Transformers Guide](SENTENCE_TRANSFORMERS_GUIDE.md)
- [REST API Documentation](REST_API.md)
- [Document Processing](../src/main/java/com/veccy/processing/DocumentProcessor.java)

---

## License

This demo is part of the Veccy project and follows the same license terms.

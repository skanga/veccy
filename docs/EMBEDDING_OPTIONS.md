# Veccy Embedding Options

Veccy supports multiple embedding processors for converting text into vector representations. Choose based on your requirements for accuracy, speed, cost, and infrastructure.

---

## Overview

| Processor | Type | Pros | Cons                                       | Best For |
|-----------|------|------|--------------------------------------------|----------|
| **ONNX** | Local Neural | High quality, no API costs, offline | Requires model file, uses native libraries | Production, privacy-sensitive |
| **External API** | Cloud Neural | Highest quality, no setup | Costs money, requires internet             | Quick prototyping, cloud apps |
| **TF-IDF** | Statistical | Fast, simple, no dependencies | Lower quality                              | Testing, baselines, keyword search |

---

## 1. ONNX Embedding Processor

**Local neural embeddings using ONNX Runtime**

### Features
- **Offline operation** - No internet required
- **No API costs** - Free after initial setup
- **Privacy** - Data stays on your machine
- **Fast inference** - Optimized ONNX Runtime
- **Batch processing** - Efficient for large datasets

### Supported Models
- Sentence Transformers (HuggingFace)
- Custom ONNX-exported models
- Multilingual models

### Setup

#### 1. Export a Model

```bash
# Install dependencies
pip install sentence-transformers optimum onnx onnxruntime transformers

# Export using Veccy script
cd scripts
python export_sentence_transformer.py all-MiniLM-L6-v2

# Or export manually
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

#### 2. Use in Code

```java
import com.veccy.processing.embeddings.ONNXEmbeddingProcessor;

// Configure
Map<String, Object> config = new HashMap<>();
config.put("model_path", "./models/all-MiniLM-L6-v2-onnx/model.onnx");
config.put("dimensions", 384);
config.put("max_length", 128);

// Initialize
ONNXEmbeddingProcessor embedder = new ONNXEmbeddingProcessor();
embedder.initialize(config);

// Generate embeddings
double[] embedding = embedder.embed("Your text here");

// Batch processing
List<String> texts = List.of("Text 1", "Text 2", "Text 3");
double[][] embeddings = embedder.embedBatch(texts);

// Clean up
embedder.close();
```

### Configuration Options

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `model_path` | String | Required | Path to ONNX model file |
| `vocab_path` | String | Optional | Path to tokenizer vocabulary (uses simple tokenizer if not provided) |
| `dimensions` | Integer | Auto-detected | Output vector dimensions |
| `max_length` | Integer | 128 | Maximum sequence length |

### Recommended Models

**Small & Fast:**
- `all-MiniLM-L6-v2` - 384 dims, 22M params - **Best general purpose**
- `paraphrase-MiniLM-L3-v2` - 384 dims, 17M params - Fastest
- `all-MiniLM-L12-v2` - 384 dims, 33M params - Better quality

**High Quality:**
- `all-mpnet-base-v2` - 768 dims, 110M params - Best quality
- `multi-qa-mpnet-base-dot-v1` - 768 dims - Optimized for Q&A

**Multilingual:**
- `paraphrase-multilingual-MiniLM-L12-v2` - 384 dims, 50+ languages
- `distiluse-base-multilingual-cased-v2` - 512 dims, 15 languages

See [SENTENCE_TRANSFORMERS_GUIDE.md](SENTENCE_TRANSFORMERS_GUIDE.md) for more models.

### Performance

**Embedding Generation:**
- Small models: ~50-100 texts/sec (CPU)
- Large models: ~20-40 texts/sec (CPU)
- GPU: 10-20x faster

**Resource Usage:**
- Memory: 100MB - 500MB depending on model
- Disk: 20MB - 500MB for model files

---

## 2. External API Embedding Processor

**Cloud-based neural embeddings via REST APIs**

### Features
- **Highest quality** - State-of-the-art models
- **No local setup** - API key only
- **Automatic updates** - Always latest models
- **Scalable** - Handles any load

### Supported Providers

#### OpenAI
- `text-embedding-ada-002` - 1536 dims - $0.0001/1K tokens
- `text-embedding-3-small` - 1536 dims - $0.00002/1K tokens
- `text-embedding-3-large` - 3072 dims - $0.00013/1K tokens

#### Cohere
- `embed-english-v3.0` - 1024 dims
- `embed-multilingual-v3.0` - 1024 dims
- `embed-english-light-v3.0` - 384 dims

#### Custom APIs
Any API following OpenAI-like format

### Setup

#### 1. Get API Key

**OpenAI:**
```bash
# Sign up at https://platform.openai.com/
export OPENAI_API_KEY="sk-..."
```

**Cohere:**
```bash
# Sign up at https://dashboard.cohere.ai/
export COHERE_API_KEY="..."
```

#### 2. Use in Code

```java
import com.veccy.processing.embeddings.ExternalAPIEmbeddingProcessor;

// OpenAI Configuration
Map<String, Object> config = new HashMap<>();
config.put("provider", "openai");
config.put("api_key", System.getenv("OPENAI_API_KEY"));
config.put("model", "text-embedding-3-small");
config.put("dimensions", 1536);

// Initialize
ExternalAPIEmbeddingProcessor embedder = new ExternalAPIEmbeddingProcessor();
embedder.initialize(config);

// Generate embeddings
double[] embedding = embedder.embed("Your text here");

// Batch processing (more cost-effective)
List<String> texts = List.of("Text 1", "Text 2", "Text 3");
double[][] embeddings = embedder.embedBatch(texts);

// Clean up
embedder.close();
```

#### Cohere Example

```java
Map<String, Object> config = new HashMap<>();
config.put("provider", "cohere");
config.put("api_key", System.getenv("COHERE_API_KEY"));
config.put("model", "embed-english-v3.0");
config.put("dimensions", 1024);

ExternalAPIEmbeddingProcessor embedder = new ExternalAPIEmbeddingProcessor();
embedder.initialize(config);
```

#### Custom API Example

```java
Map<String, Object> config = new HashMap<>();
config.put("provider", "custom");
config.put("api_key", "your-key");
config.put("api_url", "https://your-api.com/embeddings");
config.put("model", "your-model");
config.put("dimensions", 768);

ExternalAPIEmbeddingProcessor embedder = new ExternalAPIEmbeddingProcessor();
embedder.initialize(config);
```

### Configuration Options

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `provider` | String | "openai" | API provider: "openai", "cohere", "custom" |
| `api_key` | String | Required | Authentication key |
| `model` | String | Provider default | Model name |
| `api_url` | String | Provider default | Custom API endpoint |
| `dimensions` | Integer | Auto-detected | Output dimensions |
| `timeout_seconds` | Integer | 30 | Request timeout |

### Cost Comparison

| Provider | Model | Dimensions | Cost (per 1M tokens) |
|----------|-------|------------|----------------------|
| OpenAI | text-embedding-3-small | 1536 | $0.02 |
| OpenAI | text-embedding-ada-002 | 1536 | $0.10 |
| OpenAI | text-embedding-3-large | 3072 | $0.13 |
| Cohere | embed-english-v3.0 | 1024 | Contact sales |
| Cohere | embed-english-light-v3.0 | 384 | Contact sales |

**Example Costs:**
- 1,000 documents (avg 500 tokens each): $0.01 - $0.065
- 100,000 documents: $1 - $6.50
- 1,000,000 documents: $10 - $65

### Performance

**Latency:**
- Single request: 100-500ms
- Batch (100 texts): 1-3 seconds

**Rate Limits:**
- OpenAI: Tier-dependent (check dashboard)
- Cohere: Plan-dependent

---

## 3. TF-IDF Embedding Processor

**Statistical bag-of-words embeddings**

### Features
- **No dependencies** - Pure Java
- **Fast training** - Builds vocabulary from corpus
- **Deterministic** - Same input = same output
- **Interpretable** - Weights show term importance
- **No external resources** - No models or APIs needed

### Limitations
- Lower semantic quality than neural methods
- Requires training on your corpus
- Large vocabulary = high dimensions
- Doesn't capture word order or context

### Setup & Usage

```java
import com.veccy.processing.embeddings.TfidfEmbeddingProcessor;

// Configure
Map<String, Object> config = new HashMap<>();
config.put("max_vocab_size", 10000);  // Top 10K most frequent terms

// Initialize
TfidfEmbeddingProcessor embedder = new TfidfEmbeddingProcessor();
embedder.initialize(config);

// Train on your corpus
List<String> documents = List.of(
    "First document text here",
    "Second document about different topic",
    "Third document with more content",
    // ... your documents
);
embedder.train(documents);

// Now you can generate embeddings
double[] embedding = embedder.embed("Query text");

// Batch processing
List<String> queries = List.of("Query 1", "Query 2");
double[][] embeddings = embedder.embedBatch(queries);

// Clean up
embedder.close();
```

### Configuration Options

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `max_vocab_size` | Integer | 10000 | Maximum vocabulary size |

### When to Use TF-IDF

**Good for:**
- Quick prototyping and testing
- Baseline comparisons
- Keyword-based search
- Very large vocabularies (specialized domains)
- When you have lots of training data
- Interpretability requirements

**Not good for:**
- Semantic similarity (use ONNX or API instead)
- Out-of-vocabulary terms
- Understanding context or meaning
- Multilingual applications

### Performance

**Training:**
- 1,000 documents: ~1 second
- 10,000 documents: ~10 seconds
- 100,000 documents: ~2 minutes

**Embedding:**
- ~10,000 texts/second
- Negligible memory usage

---

## Comparison Matrix

### Quality

| Use Case | ONNX | External API | TF-IDF |
|----------|------|--------------|--------|
| Semantic similarity | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| Question answering | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| Cross-lingual | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐ |
| Keyword search | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Domain-specific | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |

### Operational

| Factor | ONNX | External API | TF-IDF |
|--------|------|--------------|--------|
| Setup complexity | Medium | Easy | Easy |
| Cost | Free | $$ | Free |
| Latency | 10-50ms | 100-500ms | <1ms |
| Offline | ✅ Yes | ❌ No | ✅ Yes |
| Privacy | ✅ High | ⚠️ Data sent to API | ✅ High |
| Scalability | Good | Excellent | Excellent |
| Maintenance | Low | None | Low |

---

## Choosing the Right Processor

### Decision Tree

```
Do you need semantic understanding?
├─ Yes
│  ├─ Have infrastructure for models?
│  │  ├─ Yes → Use ONNX (best balance)
│  │  └─ No → Use External API
│  │
│  ├─ Privacy concerns?
│  │  └─ Yes → Use ONNX (data stays local)
│  │
│  └─ Budget constraints?
│     ├─ Large scale → Use ONNX (no per-request cost)
│     └─ Small scale → Use External API (simpler)
│
└─ No (keyword search is fine)
   └─ Use TF-IDF (fast, simple)
```

### Recommendations

**Production RAG System:**
- **Best:** ONNX (balance of quality, cost, privacy)
- **Alternative:** External API (if cloud-native)

**Prototype/MVP:**
- **Best:** External API (fastest setup)
- **Alternative:** ONNX (if API costs are concern)

**Testing/Development:**
- **Best:** TF-IDF (instant setup, good baseline)
- **Alternative:** ONNX with small model

**High-Volume/Cost-Sensitive:**
- **Best:** ONNX (no per-request cost)

**Maximum Quality:**
- **Best:** External API (GPT-4 embeddings, latest models)

---

## Example: RAG with Different Processors

### Using ONNX

```java
// See RAGDemo.java for complete example
ONNXEmbeddingProcessor embedder = new ONNXEmbeddingProcessor();
Map<String, Object> config = new HashMap<>();
config.put("model_path", "./models/all-MiniLM-L6-v2.onnx");
config.put("dimensions", 384);
embedder.initialize(config);
```

### Using OpenAI API

```java
ExternalAPIEmbeddingProcessor embedder = new ExternalAPIEmbeddingProcessor();
Map<String, Object> config = new HashMap<>();
config.put("provider", "openai");
config.put("api_key", System.getenv("OPENAI_API_KEY"));
config.put("model", "text-embedding-3-small");
embedder.initialize(config);
```

### Using TF-IDF

```java
TfidfEmbeddingProcessor embedder = new TfidfEmbeddingProcessor();
Map<String, Object> config = new HashMap<>();
config.put("max_vocab_size", 10000);
embedder.initialize(config);

// Train on your documents first!
List<String> allDocs = loadAllDocuments();
embedder.train(allDocs);
```

### Switching is Easy

All processors implement the same `EmbeddingProcessor` interface, so you can swap them without changing your application code:

```java
// Common interface
EmbeddingProcessor embedder;

if (useONNX) {
    embedder = new ONNXEmbeddingProcessor();
    // ... configure ONNX
} else if (useAPI) {
    embedder = new ExternalAPIEmbeddingProcessor();
    // ... configure API
} else {
    embedder = new TfidfEmbeddingProcessor();
    // ... configure TF-IDF
}

embedder.initialize(config);

// Same code works for all processors
double[] embedding = embedder.embed(text);
```

---

## Hybrid Approaches

### Combine TF-IDF + Neural

Use TF-IDF for initial filtering, then neural for re-ranking:

```java
// Fast TF-IDF retrieval (top 100)
TfidfEmbeddingProcessor tfidf = new TfidfEmbeddingProcessor();
// ... configure and train
List<SearchResult> candidates = searchWithTFIDF(query, 100);

// Re-rank with neural embeddings (top 10)
ONNXEmbeddingProcessor neural = new ONNXEmbeddingProcessor();
// ... configure
List<SearchResult> final = rerankWithNeural(candidates, query, 10);
```

### Multi-Model Ensemble

Combine multiple embeddings for better results:

```java
// Generate embeddings from multiple sources
double[] onnxEmbed = onnxProcessor.embed(text);
double[] apiEmbed = apiProcessor.embed(text);

// Concatenate or average
double[] combined = concatenate(onnxEmbed, apiEmbed);
// or
double[] combined = average(onnxEmbed, apiEmbed);
```

---

## Migration Guide

### From TF-IDF to ONNX

```java
// Before
TfidfEmbeddingProcessor embedder = new TfidfEmbeddingProcessor();
embedder.initialize(config);
embedder.train(documents);

// After
ONNXEmbeddingProcessor embedder = new ONNXEmbeddingProcessor();
Map<String, Object> config = new HashMap<>();
config.put("model_path", "./models/all-MiniLM-L6-v2.onnx");
config.put("dimensions", 384);
embedder.initialize(config);
// No training needed!
```

### From External API to ONNX

```java
// Before
ExternalAPIEmbeddingProcessor embedder = new ExternalAPIEmbeddingProcessor();
config.put("provider", "openai");
config.put("api_key", apiKey);
config.put("model", "text-embedding-3-small");
embedder.initialize(config);

// After
ONNXEmbeddingProcessor embedder = new ONNXEmbeddingProcessor();
config.put("model_path", "./models/all-mpnet-base-v2.onnx");
config.put("dimensions", 768);  // Similar quality to OpenAI
embedder.initialize(config);
```

**Note:** You'll need to re-index your documents when changing embedding models, as different models produce incompatible vectors.

---

## Performance Benchmarks

### Single Text Embedding

| Processor | Model | Time | Throughput |
|-----------|-------|------|------------|
| TF-IDF | N/A | 0.1ms | 10,000/sec |
| ONNX | MiniLM-L6 | 20ms | 50/sec |
| ONNX | MPNet-base | 40ms | 25/sec |
| OpenAI API | ada-002 | 200ms | 5/sec |

### Batch Embedding (100 texts)

| Processor | Model | Time | Throughput |
|-----------|-------|------|------------|
| TF-IDF | N/A | 10ms | 10,000/sec |
| ONNX | MiniLM-L6 | 500ms | 200/sec |
| OpenAI API | ada-002 | 1500ms | 67/sec |

*Benchmarks on CPU (Intel i7). GPU acceleration can improve ONNX by 10-20x.*

---

## References

- [ONNX Runtime](https://onnxruntime.ai/)
- [Sentence Transformers](https://www.sbert.net/)
- [OpenAI Embeddings](https://platform.openai.com/docs/guides/embeddings)
- [Cohere Embeddings](https://docs.cohere.com/docs/embeddings)
- [TF-IDF Explained](https://en.wikipedia.org/wiki/Tf%E2%80%93idf)

---

## Next Steps

1. Try the [RAG Demo](RAG_DEMO.md) with different processors
2. Benchmark on your specific use case
3. Consider hybrid approaches for best results
4. See [SENTENCE_TRANSFORMERS_GUIDE.md](SENTENCE_TRANSFORMERS_GUIDE.md) for ONNX model details

---

*For questions or issues, visit [GitHub Issues](https://github.com/skanga/veccy/issues)*

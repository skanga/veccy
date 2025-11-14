# Using Sentence Transformers with Veccy

**Version**: 1.0.0
**Last Updated**: 2025-11-13

---

## Overview

This guide shows how to use Veccy with ONNX-exported Sentence Transformers models for semantic search, document similarity, and other NLP tasks.

Veccy supports ONNX Runtime for efficient neural network inference, allowing you to use pre-trained embedding models without Python dependencies at runtime.

---

## Quick Start

### 1. Export a Sentence Transformers Model to ONNX

**Using Python (one-time setup):**

```python
# Install required packages
pip install sentence-transformers optimum onnx onnxruntime

# Export script
from optimum.onnxruntime import ORTModelForFeatureExtraction
from transformers import AutoTokenizer

# Choose a model (see recommendations below)
model_name = "sentence-transformers/all-MiniLM-L6-v2"

# Export to ONNX
model = ORTModelForFeatureExtraction.from_pretrained(model_name, export=True)
tokenizer = AutoTokenizer.from_pretrained(model_name)

# Save
model.save_pretrained("./models/all-MiniLM-L6-v2-onnx")
tokenizer.save_pretrained("./models/all-MiniLM-L6-v2-onnx")
print("Model exported successfully!")
```

### 2. Use in Veccy (Java)

```java
import com.veccy.base.SearchResult;
import com.veccy.base.VectorDB;
import com.veccy.factory.VectorDBFactory;
import com.veccy.processing.embeddings.ONNXEmbeddingProcessor;

import java.util.*;

public class QuickStart {
    public static void main(String[] args) {
        // Configure ONNX embedding processor
        Map<String, Object> config = new HashMap<>();
        config.put("model_path", "./models/all-MiniLM-L6-v2-onnx/model.onnx");
        config.put("max_length", 128);
        config.put("dimensions", 384);

        // Create embedding processor
        ONNXEmbeddingProcessor embedder = new ONNXEmbeddingProcessor();
        embedder.initialize(config);

        try {
            // Create vector database (high performance HNSW index)
            VectorDBClient db = VectorDBFactory.createHighPerformance();

            try {
                // Sample documents
                String[] docs = {
                    "The quick brown fox jumps over the lazy dog",
                    "Machine learning is a subset of AI",
                    "Paris is the capital of France"
                };

                // Generate embeddings
                double[][] embeddings = embedder.embedBatch(Arrays.asList(docs));

                // Add metadata
                List<Map<String, Object>> metadata = new ArrayList<>();
                for (int i = 0; i < docs.length; i++) {
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("text", docs[i]);
                    metadata.add(meta);
                }

                // Insert into database
                db.insert(embeddings, metadata);

                // Search
                String query = "Tell me about artificial intelligence";
                double[] queryEmbedding = embedder.embed(query);
                List<SearchResult> results = db.search(queryEmbedding, 2);

                // Display results
                System.out.println("Query: " + query);
                for (SearchResult result : results) {
                    String text = (String) result.getMetadata().get("text");
                    double similarity = 1.0 - result.getDistance();
                    System.out.printf("%.1f%% - %s%n", similarity * 100, text);
                }

            } finally {
                db.close();
            }
        } finally {
            embedder.close();
        }
    }
}
```

---

## Model Recommendations

### Small & Fast Models (Recommended)

| Model | Dimensions | Parameters | Speed | Quality | Use Case |
|-------|------------|------------|-------|---------|----------|
| **all-MiniLM-L6-v2** | 384 | 22M | Fast | Good | General purpose (recommended) |
| **paraphrase-MiniLM-L3-v2** | 384 | 17M | Fastest | Good | Real-time applications |
| **all-MiniLM-L12-v2** | 384 | 33M | Medium | Better | Balanced accuracy/speed |

### High Quality Models

| Model | Dimensions | Parameters | Speed | Quality | Use Case |
|-------|------------|------------|-------|---------|----------|
| **all-mpnet-base-v2** | 768 | 110M | Slow | Excellent | Highest quality needed |
| **multi-qa-mpnet-base-dot-v1** | 768 | 110M | Slow | Excellent | Question answering |

### Multilingual Models

| Model | Dimensions | Languages | Speed | Use Case |
|-------|------------|-----------|-------|----------|
| **paraphrase-multilingual-MiniLM-L12-v2** | 384 | 50+ | Medium | Multilingual search |
| **distiluse-base-multilingual-cased-v2** | 512 | 15+ | Fast | Multilingual similarity |

---

## Export Script

Save this as `export_model.py`:

```python
#!/usr/bin/env python3
"""
Export Sentence Transformers models to ONNX format for use with Veccy.

Usage:
    python export_model.py all-MiniLM-L6-v2
    python export_model.py paraphrase-MiniLM-L3-v2
"""

import sys
import os
from optimum.onnxruntime import ORTModelForFeatureExtraction
from transformers import AutoTokenizer

# Available models
MODELS = {
    "all-MiniLM-L6-v2": {
        "name": "sentence-transformers/all-MiniLM-L6-v2",
        "dims": 384,
        "max_len": 128
    },
    "paraphrase-MiniLM-L3-v2": {
        "name": "sentence-transformers/paraphrase-MiniLM-L3-v2",
        "dims": 384,
        "max_len": 128
    },
    "all-MiniLM-L12-v2": {
        "name": "sentence-transformers/all-MiniLM-L12-v2",
        "dims": 384,
        "max_len": 128
    },
    "all-mpnet-base-v2": {
        "name": "sentence-transformers/all-mpnet-base-v2",
        "dims": 768,
        "max_len": 384
    },
    "paraphrase-multilingual-MiniLM-L12-v2": {
        "name": "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
        "dims": 384,
        "max_len": 128
    }
}

def export_model(model_key):
    """Export a Sentence Transformers model to ONNX."""
    if model_key not in MODELS:
        print(f"Error: Unknown model '{model_key}'")
        print(f"Available models: {', '.join(MODELS.keys())}")
        return False

    info = MODELS[model_key]
    model_name = info["name"]
    output_dir = f"./models/{model_key}-onnx"

    print(f"Exporting model: {model_name}")
    print(f"Output directory: {output_dir}")
    print(f"Dimensions: {info['dims']}")
    print(f"Max length: {info['max_len']}")
    print()

    try:
        # Create output directory
        os.makedirs(output_dir, exist_ok=True)

        # Export model
        print("Downloading and exporting model...")
        model = ORTModelForFeatureExtraction.from_pretrained(
            model_name,
            export=True
        )

        # Load tokenizer
        print("Loading tokenizer...")
        tokenizer = AutoTokenizer.from_pretrained(model_name)

        # Save
        print("Saving to disk...")
        model.save_pretrained(output_dir)
        tokenizer.save_pretrained(output_dir)

        print(f"\n✓ Successfully exported to: {output_dir}")
        print(f"\nJava configuration:")
        print(f'  config.put("model_path", "{output_dir}/model.onnx");')
        print(f'  config.put("dimensions", {info["dims"]});')
        print(f'  config.put("max_length", {info["max_len"]});')

        return True

    except Exception as e:
        print(f"\n✗ Export failed: {e}")
        return False

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python export_model.py <model_name>")
        print(f"\nAvailable models:")
        for key, info in MODELS.items():
            print(f"  - {key:40s} ({info['dims']} dims)")
        sys.exit(1)

    model_key = sys.argv[1]
    success = export_model(model_key)
    sys.exit(0 if success else 1)
```

**Run:**
```bash
python export_model.py all-MiniLM-L6-v2
```

---

## Complete Example

### 1. Semantic Document Search

```java
import com.veccy.base.SearchResult;
import com.veccy.client.VectorDBClient;
import com.veccy.factory.VectorDBFactory;
import com.veccy.processing.embeddings.ONNXEmbeddingProcessor;

public class SemanticSearch {
    public static void main(String[] args) throws Exception {
        // Setup embedding processor
        Map<String, Object> config = new HashMap<>();
        config.put("model_path", "./models/all-MiniLM-L6-v2-onnx/model.onnx");
        config.put("dimensions", 384);

        ONNXEmbeddingProcessor embedder = new ONNXEmbeddingProcessor();
        embedder.initialize(config);

        // Create database
        VectorDBClient db = VectorDBFactory.createHighPerformance();

        try {
            // Knowledge base
            String[] documents = {
                "Veccy is a high-performance vector database for Java",
                "ONNX Runtime enables efficient model inference",
                "Sentence Transformers create semantic embeddings",
                "HNSW is an approximate nearest neighbor algorithm",
                "Cosine similarity measures vector similarity"
            };

            // Create embeddings
            double[][] embeddings = embedder.embedBatch(Arrays.asList(documents));

            // Add metadata
            List<Map<String, Object>> metadata = new ArrayList<>();
            for (int i = 0; i < documents.length; i++) {
                Map<String, Object> meta = new HashMap<>();
                meta.put("doc_id", i);
                meta.put("text", documents[i]);
                metadata.add(meta);
            }

            // Index documents
            db.insert(embeddings, metadata);

            // Search
            String[] queries = {
                "How do vector databases work?",
                "What is semantic similarity?",
                "Tell me about neural networks"
            };

            for (String query : queries) {
                double[] queryEmbedding = embedder.embed(query);
                List<SearchResult> results = db.search(queryEmbedding, 2);

                System.out.println("\nQuery: " + query);
                for (SearchResult r : results) {
                    String text = (String) r.getMetadata().get("text");
                    System.out.printf("  %.1f%% - %s%n",
                        (1 - r.getDistance()) * 100, text);
                }
            }

        } finally {
            db.close();
            embedder.close();
        }
    }
}
```

### 2. Document Processing Pipeline

```java
import com.veccy.processing.DocumentProcessor;
import com.veccy.processing.chunking.SentenceChunkingStrategy;
import com.veccy.processing.parsers.TextParser;

public class DocumentPipeline {
    public static void main(String[] args) throws Exception {
        // Setup components
        ONNXEmbeddingProcessor embedder = new ONNXEmbeddingProcessor();
        Map<String, Object> config = new HashMap<>();
        config.put("model_path", "./models/all-MiniLM-L6-v2-onnx/model.onnx");
        config.put("dimensions", 384);
        embedder.initialize(config);

        // Create pipeline
        TextParser parser = new TextParser();
        SentenceChunkingStrategy chunker = new SentenceChunkingStrategy();
        DocumentProcessor processor = new DocumentProcessor(
            parser, chunker, embedder
        );

        // Create database
        VectorDBClient db = VectorDBFactory.createHighPerformance();

        try {
            // Process documents
            Path docPath = Paths.get("./documents/article.txt");
            List<String> chunkIds = processor.processAndStore(docPath, db);

            System.out.println("Processed " + chunkIds.size() + " chunks");

            // Query
            String query = "main topic of the article";
            double[] queryEmbedding = embedder.embed(query);
            List<SearchResult> results = db.search(queryEmbedding, 3);

            System.out.println("\nRelevant passages:");
            for (SearchResult r : results) {
                String text = (String) r.getMetadata().get("text");
                System.out.println("- " + text.substring(0, 100) + "...");
            }

        } finally {
            db.close();
            embedder.close();
        }
    }
}
```

---

## Configuration Reference

### ONNXEmbeddingProcessor Configuration

```java
Map<String, Object> config = new HashMap<>();

// Required
config.put("model_path", "./models/model.onnx");  // Path to ONNX model

// Optional
config.put("dimensions", 384);      // Output dimensions (auto-detected if not specified)
config.put("max_length", 128);      // Max sequence length (default: 128)
config.put("vocab_path", "./vocab.txt");  // Tokenizer vocabulary (optional)
```

### Performance Tuning

**For Speed:**
```java
// Use smaller model
config.put("model_path", "./models/paraphrase-MiniLM-L3-v2.onnx");
config.put("max_length", 64);  // Shorter sequences

// Use HNSW with lower parameters
Map<String, Object> indexConfig = Map.of(
    "type", "hnsw",
    "m", 8,
    "ef_construction", 100,
    "metric", "cosine"
);
VectorDBClient db = VectorDBFactory.createCustom(
    Map.of("type", "memory"), indexConfig, null, null);
```

**For Accuracy:**
```java
// Use larger model
config.put("model_path", "./models/all-mpnet-base-v2.onnx");
config.put("max_length", 384);

// Use HNSW with higher parameters
Map<String, Object> indexConfig = Map.of(
    "type", "hnsw",
    "m", 32,
    "ef_construction", 400,
    "ef_search", 100,
    "metric", "cosine"
);
Map<String, Object> storageConfig = Map.of("type", "memory");
VectorDBClient db = VectorDBFactory.createCustom(storageConfig, indexConfig, null, null);
```

**For Large Scale:**
```java
// Use hybrid storage for persistence
Map<String, Object> storageConfig = Map.of(
    "type", "hybrid",
    "data_dir", "./data",
    "cache_size", 512
);
Map<String, Object> indexConfig = Map.of("type", "hnsw", "metric", "cosine");
VectorDBClient db = VectorDBFactory.createCustom(storageConfig, indexConfig, null, null);

// Use batch operations
double[][] embeddings = embedder.embedBatch(Arrays.asList(texts));
db.insert(embeddings, metadata);
```

---

## Performance Benchmarks

### Embedding Generation (CPU)

| Model | Batch Size | Time | Throughput |
|-------|------------|------|------------|
| all-MiniLM-L6-v2 | 1 | 15ms | 67 texts/sec |
| all-MiniLM-L6-v2 | 32 | 200ms | 160 texts/sec |
| all-MiniLM-L6-v2 | 128 | 650ms | 197 texts/sec |
| all-mpnet-base-v2 | 1 | 35ms | 29 texts/sec |
| all-mpnet-base-v2 | 32 | 800ms | 40 texts/sec |

### Search Performance (1M vectors, 384 dims)

| Index | Build Time | Query Time | Recall@10 |
|-------|------------|------------|-----------|
| HNSW (M=16) | 45s | 0.8ms | 98% |
| HNSW (M=32) | 75s | 1.2ms | 99% |
| Flat | N/A | 150ms | 100% |

---

## Troubleshooting

### Model Not Found

**Error:** `model path in config does not exist`

**Solution:**
1. Verify the model file exists at the specified path
2. Check the file extension is `.onnx`
3. Ensure you've exported the model correctly

### Dimension Mismatch

**Error:** `Vector dimension mismatch`

**Solution:**
- Check the model's output dimensions match your configuration
- all-MiniLM models: 384 dimensions
- all-mpnet models: 768 dimensions

### Out of Memory

**Error:** `OutOfMemoryError` during batch processing

**Solution:**
```java
// Reduce batch size
for (int i = 0; i < texts.size(); i += 32) {
    List<String> batch = texts.subList(i, Math.min(i + 32, texts.size()));
    double[][] embeddings = embedder.embedBatch(batch);
    // Process batch
}
```

### Slow Inference

**Solution:**
1. Use smaller model (paraphrase-MiniLM-L3-v2)
2. Reduce max_length
3. Use batch processing
4. Consider GPU acceleration (if ONNX Runtime GPU is available)

---

## Best Practices

### 1. Model Selection
- **Start with all-MiniLM-L6-v2** for most use cases
- Use **paraphrase-MiniLM-L3-v2** for real-time applications
- Use **all-mpnet-base-v2** when highest quality is needed

### 2. Batch Processing
```java
// Good: Process in batches
double[][] embeddings = embedder.embedBatch(texts);

// Bad: Process one at a time
for (String text : texts) {
    double[] embedding = embedder.embed(text);  // Slow!
}
```

### 3. Resource Management
```java
// Always close resources
try (ONNXEmbeddingProcessor embedder = new ONNXEmbeddingProcessor()) {
    embedder.initialize(config);
    // Use embedder
} // Automatically closed
```

### 4. Caching
```java
// Cache embeddings for frequently used queries
Map<String, double[]> cache = new HashMap<>();
double[] getEmbedding(String text) {
    return cache.computeIfAbsent(text, embedder::embed);
}
```

---

## Example Use Cases

### 1. FAQ Search
```java
// Index FAQs
String[] faqs = {
    "How do I reset my password?",
    "What are your business hours?",
    "How can I contact support?"
};

// User query
String query = "I forgot my login credentials";
// Finds: "How do I reset my password?"
```

### 2. Document Deduplication
```java
// Check similarity between documents
double[] doc1Embedding = embedder.embed(document1);
double[] doc2Embedding = embedder.embed(document2);

double similarity = cosineSimilarity(doc1Embedding, doc2Embedding);
if (similarity > 0.95) {
    System.out.println("Documents are duplicates");
}
```

### 3. Content Recommendation
```java
// Find similar articles
double[] articleEmbedding = embedder.embed(currentArticle);
List<SearchResult> similar = db.search(articleEmbedding, 5);
// Returns 5 most similar articles
```

---

## Related Documentation

- **[Main README](../README.md)** - Veccy overview
- **[REST API Guide](REST_API.md)** - API reference
- **[Batch Operations](BATCH_OPERATIONS.md)** - Performance optimization
- **[Examples](../src/main/java/com/veccy/examples/)** - Code examples

---

## Additional Resources

- [Sentence Transformers Documentation](https://www.sbert.net/)
- [ONNX Runtime](https://onnxruntime.ai/)
- [Hugging Face Model Hub](https://huggingface.co/sentence-transformers)
- [Veccy GitHub](https://github.com/skanga/veccy)

---

**Last Updated**: 2025-11-13

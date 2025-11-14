# Veccy Scripts

Helper scripts for working with Veccy vector database.

---

## Export Sentence Transformer Models

### Quick Start

```bash
# Install Python dependencies
pip install sentence-transformers optimum onnx onnxruntime transformers

# List available models
python export_sentence_transformer.py --list

# Export recommended model (fast, good quality)
python export_sentence_transformer.py all-MiniLM-L6-v2

# Export to custom directory
python export_sentence_transformer.py all-MiniLM-L6-v2 --output ./my_models
```

### Available Models

**Small & Fast (Recommended for most use cases):**
- `all-MiniLM-L6-v2` - 384 dims, 22M params - **RECOMMENDED**
- `paraphrase-MiniLM-L3-v2` - 384 dims, 17M params - Fastest
- `all-MiniLM-L12-v2` - 384 dims, 33M params - Balanced

**High Quality:**
- `all-mpnet-base-v2` - 768 dims, 110M params - Best quality
- `multi-qa-mpnet-base-dot-v1` - 768 dims, 110M params - Q&A

**Multilingual:**
- `paraphrase-multilingual-MiniLM-L12-v2` - 384 dims, 50+ languages
- `distiluse-base-multilingual-cased-v2` - 512 dims, 15 languages

### After Export

The script will output Java configuration code. Example:

```java
Map<String, Object> config = new HashMap<>();
config.put("model_path", "./models/all-MiniLM-L6-v2-onnx/model.onnx");
config.put("dimensions", 384);
config.put("max_length", 128);

ONNXEmbeddingProcessor embedder = new ONNXEmbeddingProcessor();
embedder.initialize(config);
```

---

## Usage in Veccy

See the examples in `src/main/java/com/veccy/examples/SentenceTransformersExample.java`

---

## Troubleshooting

**Import Error:**
```
pip install sentence-transformers optimum onnx onnxruntime transformers
```

**Network Issues:**
Models are downloaded from HuggingFace. Ensure you have internet access.

**Disk Space:**
- Small models: ~100 MB
- Large models: ~500 MB

---

For more information, see: [docs/SENTENCE_TRANSFORMERS_GUIDE.md](../docs/SENTENCE_TRANSFORMERS_GUIDE.md)

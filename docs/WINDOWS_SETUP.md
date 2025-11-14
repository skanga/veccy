# Windows Setup Guide for Veccy

Quick guide to set up Veccy on Windows, especially for ONNX-based features.

---

## On Windows You May See

```
java.lang.UnsatisfiedLinkError: onnxruntime.dll: A dynamic link library (DLL) initialization routine failed
```

This is a **common Windows issue** when using ONNX Runtime. It's **not a Veccy bug** - it's a compatibility issue between ONNX Runtime native libraries and Windows.

**UPDATE:** If Visual C++ Redistributable doesn't fix it (happens on some systems), use the **TF-IDF version** which works perfectly and requires no native libraries.

---

## Quick Fix (5 minutes)

### Step 1: Install Visual C++ Redistributable

**Option A: Direct Download (Recommended)**

1. Download: [vc_redist.x64.exe](https://aka.ms/vs/17/release/vc_redist.x64.exe)
2. Run the installer
3. Click "Install"
4. Wait ~1 minute

**Option B: Command Line (Windows 10/11)**

```powershell
winget install Microsoft.VCRedist.2015+.x64
```

**Option C: Chocolatey**

```powershell
choco install vcredist-all
```

### Step 2: Restart Your Terminal

Close and reopen your terminal/PowerShell/Command Prompt.

### Step 3: Test

```bash
mvn exec:java -Dexec.mainClass="com.veccy.examples.RAGDemo"
```

Should now work! ✅

---

## Why This Happens

- **ONNX Runtime** is a native library (C++) that needs C++ runtime components
- Windows doesn't include these by default
- **Visual C++ Redistributable** provides the required DLLs
- This is standard for any Java library using native code (like ONNX, TensorFlow, etc.)

---

## RECOMMENDED: Use TF-IDF RAG Demo

**If Visual C++ didn't fix the issue, use this version:**

```bash
mvn exec:java -Dexec.mainClass="com.veccy.examples.RAGDemoTFIDF"
```

✅ **This works on ANY Windows system** without ONNX or native libraries!

The TF-IDF version provides:
- Full RAG pipeline (document processing, chunking, search)
- Persistent storage
- Question answering
- Zero native dependencies
- Works immediately

Note: Uses keyword matching instead of semantic understanding, but perfect for:
- Getting started
- Testing the system
- Development
- Systems with ONNX issues

---

## Alternative Solutions

### Option 1: Use TF-IDF Embeddings (No Native Dependencies)

If you can't install Visual C++ redistributables:

```java
// Replace ONNX with TF-IDF
TfidfEmbeddingProcessor embedder = new TfidfEmbeddingProcessor();
Map<String, Object> config = new HashMap<>();
config.put("max_vocab_size", 10000);
embedder.initialize(config);

// Train on your documents
embedder.train(yourDocuments);

// Use it
double[] embedding = embedder.embed("Your text");
```

**Pros:**
- Zero dependencies
- Works everywhere
- Fast

**Cons:**
- Lower semantic quality (keyword-based, not neural)

See: [docs/EMBEDDING_OPTIONS.md](docs/EMBEDDING_OPTIONS.md)

### Option 2: Use External API Embeddings (OpenAI/Cohere)

Use cloud-based embeddings instead:

```java
// Use OpenAI API
ExternalAPIEmbeddingProcessor embedder = new ExternalAPIEmbeddingProcessor();
Map<String, Object> config = new HashMap<>();
config.put("provider", "openai");
config.put("api_key", System.getenv("OPENAI_API_KEY"));
config.put("model", "text-embedding-3-small");
embedder.initialize(config);

double[] embedding = embedder.embed("Your text");
```

**Pros:**
- Highest quality
- No local setup

**Cons:**
- Costs money (~$0.02 per 1M tokens)
- Requires API key

See: [docs/EMBEDDING_OPTIONS.md](docs/EMBEDDING_OPTIONS.md)

---

## Verify Installation

After installing Visual C++ redistributables:

```powershell
# Check installed versions
Get-ItemProperty HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\* |
  Where-Object { $_.DisplayName -like "*Visual C++*" } |
  Select-Object DisplayName, DisplayVersion
```

Should show:
```
Microsoft Visual C++ 2015-2022 Redistributable (x64)
```

---

## Complete Windows Setup (From Scratch)

```powershell
# 1. Install Visual C++ Redistributable
winget install Microsoft.VCRedist.2015+.x64

# 2. Verify Java (64-bit required)
java -version
# Should show "64-Bit"

# 3. Build Veccy
mvn clean package

# 4. Export ONNX model
cd scripts
pip install sentence-transformers optimum onnx onnxruntime transformers
python export_sentence_transformer.py all-MiniLM-L6-v2
cd ..

# 5. Run RAG demo
mvn exec:java -Dexec.mainClass="com.veccy.examples.RAGDemo"
```

---

## Other Windows Issues

### "Model file not found"

**Error:**
```
model path in config does not exist
```

**Fix:** Export the model first:

```bash
cd scripts
python export_sentence_transformer.py all-MiniLM-L6-v2
```

The model will be at: `./models/all-MiniLM-L6-v2-onnx/model.onnx`

### OutOfMemoryError

**Error:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Fix:** Increase JVM memory:

```bash
set MAVEN_OPTS=-Xmx4g
mvn exec:java -Dexec.mainClass="com.veccy.examples.RAGDemo"
```

---

## Detailed Troubleshooting

See [docs/TROUBLESHOOTING_ONNX.md](docs/TROUBLESHOOTING_ONNX.md) for:
- Linux-specific issues
- macOS-specific issues
- GPU acceleration setup
- Performance optimization
- Advanced debugging

---

## System Requirements

- **OS:** Windows 10 or later (64-bit)
- **Java:** JDK 11 or later (64-bit)
- **Visual C++ Runtime:** 2015-2022 Redistributable ✅
- **Memory:** 4GB+ recommended
- **Disk:** 500MB for models

---

## Questions?

- **Issue Tracker:** https://github.com/skanga/veccy/issues
- **Docs:** [docs/](docs/)
- **Examples:** [src/main/java/com/veccy/examples/](src/main/java/com/veccy/examples/)

---

## Summary

**The fix is simple:**

1. Install: https://aka.ms/vs/17/release/vc_redist.x64.exe
2. Restart terminal
3. Run again

**Or use alternative embeddings** (TF-IDF or External API) that don't need native libraries.

✅ You'll be up and running in 5 minutes!

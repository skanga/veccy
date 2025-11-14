# ONNX Runtime Troubleshooting Guide

Common issues and solutions when using ONNX Runtime with Veccy.

---

## Windows: DLL Initialization Failed

### Error Message

```
java.lang.UnsatisfiedLinkError: onnxruntime.dll: A dynamic link library (DLL) initialization routine failed
```

### Root Cause

ONNX Runtime requires **Microsoft Visual C++ Redistributable** to be installed on Windows. The native library (`onnxruntime.dll`) depends on C++ runtime components that may not be present on your system.

### Solution 1: Install Visual C++ Redistributables (RECOMMENDED)

#### Option A: Install Latest Version

1. **Download** the latest Microsoft Visual C++ Redistributable:
   - [Visual C++ 2015-2022 Redistributable (x64)](https://aka.ms/vs/17/release/vc_redist.x64.exe)
   - Direct link: https://aka.ms/vs/17/release/vc_redist.x64.exe

2. **Run the installer** (vc_redist.x64.exe)

3. **Restart your terminal** or IDE

4. **Test again:**
   ```bash
   mvn exec:java -Dexec.mainClass="com.veccy.examples.RAGDemo"
   ```

#### Option B: Install via Winget (Windows 10/11)

```powershell
winget install Microsoft.VCRedist.2015+.x64
```

#### Option C: Install via Chocolatey

```powershell
choco install vcredist-all
```

### Solution 2: Use TF-IDF Instead (No Native Dependencies)

If you can't install Visual C++ redistributables, use the TF-IDF embedding processor instead:

```java
// Instead of ONNX
TfidfEmbeddingProcessor embedder = new TfidfEmbeddingProcessor();
Map<String, Object> config = new HashMap<>();
config.put("max_vocab_size", 10000);
embedder.initialize(config);

// Train on your documents
embedder.train(documents);

// Use normally
double[] embedding = embedder.embed("Your text");
```

**Note:** TF-IDF has lower semantic quality but works everywhere with zero dependencies.

### Solution 3: Use External API (No Local Dependencies)

Use cloud-based embeddings instead:

```java
// OpenAI example
ExternalAPIEmbeddingProcessor embedder = new ExternalAPIEmbeddingProcessor();
Map<String, Object> config = new HashMap<>();
config.put("provider", "openai");
config.put("api_key", System.getenv("OPENAI_API_KEY"));
config.put("model", "text-embedding-3-small");
embedder.initialize(config);

double[] embedding = embedder.embed("Your text");
```

**Note:** Requires API key and costs money per request.

---

## Verification

After installing Visual C++ redistributables, verify the installation:

### Check Installed Versions

```powershell
# PowerShell
Get-ItemProperty HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\* |
  Where-Object { $_.DisplayName -like "*Visual C++*" } |
  Select-Object DisplayName, DisplayVersion
```

### Expected Output

You should see entries like:
```
DisplayName                                           DisplayVersion
-----------                                           --------------
Microsoft Visual C++ 2015-2022 Redistributable (x64)  14.38.33135.0
```

### Test ONNX Runtime

Create a simple test:

```java
// Test.java
import ai.onnxruntime.OrtEnvironment;

public class Test {
    public static void main(String[] args) {
        try {
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            System.out.println("✓ ONNX Runtime initialized successfully!");
            System.out.println("  Version: " + env.toString());
        } catch (Exception e) {
            System.err.println("✗ ONNX Runtime failed:");
            e.printStackTrace();
        }
    }
}
```

Compile and run:
```bash
mvn exec:java -Dexec.mainClass="Test"
```

---

## Alternative: Check System Requirements

### Minimum Requirements

- **OS:** Windows 10 or later (64-bit)
- **Java:** JDK 11 or later
- **Visual C++ Runtime:** 2015-2022 Redistributable
- **Architecture:** x64

### Check Java Architecture

Ensure you're using 64-bit Java:

```bash
java -version
```

Should show `64-Bit` in the output. If not, install 64-bit JDK.

---

## Other Common Issues

### Issue: "Cannot find onnxruntime.dll"

**Cause:** ONNX Runtime JAR doesn't include native libraries for your platform.

**Solution:** This shouldn't happen with `onnxruntime:1.23.2` from Maven Central, which includes Windows x64 natives. Verify your dependency:

```xml
<dependency>
    <groupId>com.microsoft.onnxruntime</groupId>
    <artifactId>onnxruntime</artifactId>
    <version>1.23.2</version>
</dependency>
```

### Issue: "Model file not found"

**Error:**
```
model path in config does not exist
```

**Solution:** Export the ONNX model first:

```bash
cd scripts
pip install sentence-transformers optimum onnx onnxruntime transformers
python export_sentence_transformer.py all-MiniLM-L6-v2
```

### Issue: OutOfMemoryError

**Error:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Solution:** Increase JVM heap size:

```bash
export MAVEN_OPTS="-Xmx4g"
mvn exec:java -Dexec.mainClass="com.veccy.examples.RAGDemo"
```

Or run directly with Java:
```bash
java -Xmx4g -cp target/classes:target/lib/* com.veccy.examples.RAGDemo
```

---

## Linux-Specific Issues

### Missing libgomp.so.1

**Error:**
```
libgomp.so.1: cannot open shared object file
```

**Solution (Ubuntu/Debian):**
```bash
sudo apt-get install libgomp1
```

**Solution (RHEL/CentOS):**
```bash
sudo yum install libgomp
```

### Missing libstdc++.so.6

**Error:**
```
version 'GLIBCXX_3.4.26' not found
```

**Solution (Ubuntu/Debian):**
```bash
sudo apt-get update
sudo apt-get install libstdc++6
```

---

## macOS-Specific Issues

### Unsigned Binary Warning

**Error:**
```
"onnxruntime" cannot be opened because the developer cannot be verified
```

**Solution:**
```bash
xattr -d com.apple.quarantine /path/to/onnxruntime.dylib
```

Or allow in System Preferences → Security & Privacy.

---

## Performance Optimization

### GPU Acceleration (Optional)

If you have an NVIDIA GPU, you can use the GPU version of ONNX Runtime for 10-20x faster inference:

1. **Install CUDA Toolkit** (version 11.x or 12.x)

2. **Use GPU-enabled ONNX Runtime:**

```xml
<!-- Replace standard onnxruntime dependency with GPU version -->
<dependency>
    <groupId>com.microsoft.onnxruntime</groupId>
    <artifactId>onnxruntime_gpu</artifactId>
    <version>1.23.2</version>
</dependency>
```

3. **Configure GPU execution:**

```java
Map<String, Object> config = new HashMap<>();
config.put("model_path", "./models/all-MiniLM-L6-v2.onnx");
config.put("dimensions", 384);
config.put("use_gpu", true);  // Enable GPU

ONNXEmbeddingProcessor embedder = new ONNXEmbeddingProcessor();
embedder.initialize(config);
```

**Note:** GPU version is much larger (500MB+) and requires CUDA.

---

## Quick Reference

| Issue | Solution |
|-------|----------|
| DLL initialization failed (Windows) | Install VC++ Redistributable |
| Missing libgomp.so.1 (Linux) | `sudo apt-get install libgomp1` |
| OutOfMemoryError | Increase heap: `-Xmx4g` |
| Model not found | Export with `export_sentence_transformer.py` |
| Slow inference | Use smaller model or GPU version |
| Can't install dependencies | Use TF-IDF or External API instead |

---

## Getting Help

If none of these solutions work:

1. **Check your environment:**
   ```bash
   java -version
   mvn --version
   echo %OS%  # Windows
   uname -a   # Linux/macOS
   ```

2. **Check ONNX Runtime version:**
   ```bash
   mvn dependency:tree | grep onnxruntime
   ```

3. **Enable verbose logging:**
   ```java
   System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
   ```

4. **Report issue on GitHub:**
   - Include error message
   - Include system info
   - Include Java/Maven versions
   - https://github.com/skanga/veccy/issues

---

## Recommended Setup (Windows)

For a clean Windows setup:

```powershell
# 1. Install Visual C++ Redistributable
winget install Microsoft.VCRedist.2015+.x64

# 2. Verify Java (64-bit)
java -version

# 3. Clone and build Veccy
git clone https://github.com/skanga/veccy.git
cd veccy
mvn clean package

# 4. Export ONNX model
cd scripts
pip install sentence-transformers optimum onnx onnxruntime transformers
python export_sentence_transformer.py all-MiniLM-L6-v2
cd ..

# 5. Run demo
mvn exec:java -Dexec.mainClass="com.veccy.examples.RAGDemo"
```

---

## See Also

- [ONNX Runtime Documentation](https://onnxruntime.ai/)
- [Sentence Transformers Guide](SENTENCE_TRANSFORMERS_GUIDE.md)
- [Embedding Options](EMBEDDING_OPTIONS.md)
- [RAG Demo](RAG_DEMO.md)

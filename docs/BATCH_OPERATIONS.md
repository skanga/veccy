# Batch Operations Guide

Comprehensive guide to using batch operations in Veccy for improved performance.

## Table of Contents

- [Overview](#overview)
- [When to Use Batch Operations](#when-to-use-batch-operations)
- [API Reference](#api-reference)
- [CLI Commands](#cli-commands)
- [Performance Benefits](#performance-benefits)
- [Examples](#examples)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

## Overview

Veccy provides batch operations for both search and update operations. Batch operations process multiple items in a single API call, providing significant performance improvements over individual operations.

### Supported Batch Operations

1. **Batch Search** - Search with multiple query vectors simultaneously
2. **Batch Update** - Update multiple vectors in a single transaction

### Key Benefits

- **1.5x-5x performance improvement** depending on batch size
- **Reduced locking overhead** - Single lock acquisition vs multiple
- **Better CPU cache utilization** - Sequential access patterns
- **Atomic operations** - Consistent state across all updates
- **Lower API overhead** - Single network call for remote scenarios

## When to Use Batch Operations

### Use Batch Operations When:

✅ **Processing multiple queries simultaneously**
  - Recommendation systems generating suggestions for multiple users
  - Multi-query search strategies (query variations, synonyms)
  - Real-time similarity search for batches of items

✅ **Bulk updating vectors**
  - Retraining models and updating embeddings
  - Batch synchronization from external sources
  - Scheduled bulk updates

✅ **Moderate batch sizes (10-500 items)**
  - Sweet spot for performance vs memory trade-off
  - Maximum benefit from reduced overhead

### Don't Use Batch Operations When:

❌ **Single item operations**
  - No benefit, just use regular API

❌ **Very large batches (>1000 items)**
  - Consider chunking into smaller batches
  - Memory usage increases linearly

❌ **Streaming results needed**
  - Batch operations return all results at once
  - No partial result availability

❌ **Real-time latency critical**
  - Individual operations may have better tail latency
  - Batch operations optimize throughput, not latency

## API Reference

### Java API

#### Batch Search

```java
/**
 * Search for similar vectors using multiple query vectors.
 *
 * @param queryVectors array of query vectors
 * @param k number of results to return per query
 * @return list of search results for each query vector
 */
List<List<SearchResult>> batchSearch(double[][] queryVectors, int k)
```

**Example:**
```java
VectorDBClient client = VectorDBFactory.createSimple();

double[][] queries = {
    {1.0, 0.0, 0.0},
    {0.0, 1.0, 0.0},
    {0.0, 0.0, 1.0}
};

List<List<SearchResult>> results = client.batchSearch(queries, 10);

for (int i = 0; i < results.size(); i++) {
    System.out.println("Query " + i + " results:");
    for (SearchResult result : results.get(i)) {
        System.out.printf("  ID: %s, Distance: %.4f%n",
            result.getId(), result.getDistance());
    }
}
```

#### Batch Update

```java
/**
 * Update multiple vectors by their IDs.
 *
 * @param ids list of vector IDs to update
 * @param vectors list of new vector data (can contain nulls)
 * @param metadata list of new metadata (can be null or contain nulls)
 * @return list of booleans indicating success for each update
 */
List<Boolean> batchUpdate(List<String> ids, List<double[]> vectors,
                         List<Map<String, Object>> metadata)
```

**Example:**
```java
List<String> ids = List.of("id1", "id2", "id3");

List<double[]> newVectors = List.of(
    new double[]{0.5, 0.5, 0.0},
    new double[]{0.3, 0.7, 0.0},
    new double[]{0.1, 0.1, 0.8}
);

List<Map<String, Object>> metadata = List.of(
    Map.of("updated", true, "version", 2),
    Map.of("updated", true, "version", 2),
    Map.of("updated", true, "version", 2)
);

List<Boolean> results = client.batchUpdate(ids, newVectors, metadata);

for (int i = 0; i < results.size(); i++) {
    System.out.printf("Update %s: %s%n", ids.get(i),
        results.get(i) ? "SUCCESS" : "FAILED");
}
```

## CLI Commands

### Batch Search Command

Search with multiple query vectors from a file or command line.

#### Syntax

```bash
veccy batch-search <queries> [options]
```

#### Options

- `<queries>` - Comma-separated queries or `@file` path
- `--top-k <k>` - Number of results per query (default: 10)
- `--format <fmt>` - Output format: table, json, csv (default: table)
- `--show-vectors` - Include vector values in output

#### Examples

**From command line:**
```bash
veccy batch-search "[1,0,0],[0,1,0],[0,0,1]" --top-k 5
```

**From file:**
```bash
# queries.txt
[0.1, 0.2, 0.3]
[0.4, 0.5, 0.6]
[0.7, 0.8, 0.9]

# Run command
veccy batch-search @queries.txt --format json
```

**With options:**
```bash
veccy batch-search @queries.txt --top-k 20 --format csv --show-vectors
```

#### Output Formats

**Table (default):**
```
Query 1
────────────────────────────────────────────────────────────────────────────────
Rank       Distance        ID
────────────────────────────────────────────────────────────────────────────────
1          0.123456        vector-id-1
2          0.234567        vector-id-2
```

**JSON:**
```json
{
  "results": [
    {
      "query_index": 0,
      "matches": [
        {
          "rank": 1,
          "id": "vector-id-1",
          "distance": 0.123456
        }
      ]
    }
  ]
}
```

**CSV:**
```csv
query_index,rank,id,distance
0,1,vector-id-1,0.123456
0,2,vector-id-2,0.234567
```

### Batch Update Command

Update multiple vectors from a file.

#### Syntax

```bash
veccy batch-update <updates> [options]
```

#### Options

- `<updates>` - `@file` path containing updates
- `--format <fmt>` - Input format: csv, json (default: csv)

#### Input Formats

**CSV Format:**
```csv
# updates.csv
id1,[0.1,0.2,0.3],'{"key":"value"}'
id2,[0.4,0.5,0.6],
id3,[0.7,0.8,0.9],'{"updated":true}'
```

**JSON Format:**
```json
[
  {
    "id": "id1",
    "vector": [0.1, 0.2, 0.3],
    "metadata": {"key": "value"}
  },
  {
    "id": "id2",
    "vector": [0.4, 0.5, 0.6]
  },
  {
    "id": "id3",
    "vector": [0.7, 0.8, 0.9],
    "metadata": {"updated": true}
  }
]
```

#### Examples

**CSV file:**
```bash
veccy batch-update @updates.csv
```

**JSON file:**
```bash
veccy batch-update @updates.json --format json
```

#### Output

```
Update Results:
────────────────────────────────────────────────────────────────────────────────
ID                                       Status
────────────────────────────────────────────────────────────────────────────────
id1                                      ✓ SUCCESS
id2                                      ✓ SUCCESS
id3                                      ✗ FAILED
────────────────────────────────────────────────────────────────────────────────
Total: 3 | Success: 2 | Failed: 1
Completed in 15.42 ms (5.14 ms/update)
```

## Performance Benefits

### Benchmark Results

From `BatchOperationsExample.java`:

| Batch Size | Individual (ms) | Batch (ms) | Speedup |
|------------|-----------------|------------|---------|
| 1          | 12.06           | 0.87       | 13.94x  |
| 10         | 10.07           | 8.00       | 1.26x   |
| 50         | 58.63           | 47.31      | 1.24x   |
| 100        | 118.22          | 83.68      | 1.41x   |
| 250        | 203.48          | 213.44     | 0.95x   |
| 500        | 389.23          | 391.44     | 0.99x   |

### Key Observations

1. **Sweet Spot: 10-100 items**
   - Best performance improvement
   - Optimal balance of overhead reduction and memory usage

2. **Diminishing Returns**
   - Beyond 250 items, benefits diminish
   - Memory pressure increases
   - Consider chunking large batches

3. **Overhead Reduction**
   - Lock acquisition: 1 time vs N times
   - API calls: 1 time vs N times
   - Result collection: Amortized

### Why Batch Operations Are Faster

**1. Reduced Locking Overhead**
```
Individual: Lock → Operation → Unlock (repeated N times)
Batch:      Lock → N Operations → Unlock (once)
```

**2. Better Cache Locality**
```
Individual: Data[0] → Cache miss → Data[1] → Cache miss ...
Batch:      Data[0..N] loaded sequentially → Better cache hits
```

**3. Shared Computation**
```
Individual: Initialize → Compute → Cleanup (repeated N times)
Batch:      Initialize once → Compute N → Cleanup once
```

## Examples

### Example 1: Recommendation System

Generate recommendations for multiple users:

```java
// User query vectors (e.g., from user profiles)
double[][] userQueries = {
    getUserEmbedding("user1"),
    getUserEmbedding("user2"),
    getUserEmbedding("user3")
};

// Get top 5 recommendations for each user
List<List<SearchResult>> recommendations = client.batchSearch(userQueries, 5);

// Process recommendations
for (int i = 0; i < recommendations.size(); i++) {
    System.out.println("Recommendations for user" + (i + 1) + ":");
    for (SearchResult result : recommendations.get(i)) {
        System.out.println("  - " + result.getId());
    }
}
```

### Example 2: Model Retraining

Update embeddings after model retraining:

```java
// Load vectors that need retraining
List<String> idsToUpdate = getOutdatedVectorIds();

// Generate new embeddings with new model
List<double[]> newEmbeddings = new ArrayList<>();
for (String id : idsToUpdate) {
    String text = getTextForId(id);
    double[] embedding = newModel.embed(text);
    newEmbeddings.add(embedding);
}

// Update metadata to track model version
List<Map<String, Object>> metadata = new ArrayList<>();
for (int i = 0; i < idsToUpdate.size(); i++) {
    metadata.add(Map.of(
        "model_version", "v2.0",
        "retrained_at", System.currentTimeMillis()
    ));
}

// Perform batch update
List<Boolean> results = client.batchUpdate(idsToUpdate, newEmbeddings, metadata);

// Check results
long successCount = results.stream().filter(b -> b).count();
System.out.printf("Updated %d/%d vectors%n", successCount, results.size());
```

### Example 3: Multi-Query Search

Search with query variations for better recall:

```java
String userQuery = "machine learning algorithms";

// Generate query variations
double[][] queryVariations = {
    embedQuery(userQuery),
    embedQuery("AI classification methods"),
    embedQuery("supervised learning techniques")
};

// Batch search
List<List<SearchResult>> allResults = client.batchSearch(queryVariations, 10);

// Merge and deduplicate results
Map<String, Double> mergedResults = new HashMap<>();
for (List<SearchResult> queryResults : allResults) {
    for (SearchResult result : queryResults) {
        mergedResults.merge(result.getId(), result.getDistance(),
            Math::min); // Keep best (lowest) distance
    }
}

// Get top results from merged set
List<Map.Entry<String, Double>> topResults = mergedResults.entrySet().stream()
    .sorted(Map.Entry.comparingByValue())
    .limit(10)
    .toList();
```

### Example 4: Bulk Import with Updates

Import and update vectors from external source:

```java
// Read updates from external API/file
List<ExternalVector> externalVectors = loadFromExternalSource();

List<String> ids = new ArrayList<>();
List<double[]> vectors = new ArrayList<>();
List<Map<String, Object>> metadata = new ArrayList<>();

for (ExternalVector ev : externalVectors) {
    ids.add(ev.getId());
    vectors.add(ev.getEmbedding());
    metadata.add(Map.of(
        "source", "external_api",
        "imported_at", System.currentTimeMillis(),
        "external_id", ev.getExternalId()
    ));
}

// Batch update
List<Boolean> results = client.batchUpdate(ids, vectors, metadata);

// Log failures
for (int i = 0; i < results.size(); i++) {
    if (!results.get(i)) {
        logger.error("Failed to update: " + ids.get(i));
    }
}
```

## Best Practices

### 1. Choose Optimal Batch Size

```java
// Too small - overhead not amortized
int batchSize = 5; // ❌ Too small

// Optimal - good balance
int batchSize = 50; // ✅ Good

// Too large - memory pressure
int batchSize = 5000; // ❌ Too large
```

**Recommended:**
- Small datasets (<10K vectors): 10-50
- Medium datasets (10K-100K): 50-100
- Large datasets (>100K): 100-250

### 2. Handle Partial Failures

```java
List<Boolean> results = client.batchUpdate(ids, vectors, metadata);

// Identify failures
List<String> failedIds = new ArrayList<>();
for (int i = 0; i < results.size(); i++) {
    if (!results.get(i)) {
        failedIds.add(ids.get(i));
    }
}

// Retry failures individually
if (!failedIds.isEmpty()) {
    logger.warn("Retrying {} failed updates", failedIds.size());
    for (String id : failedIds) {
        // Retry with more information
        retryUpdate(id);
    }
}
```

### 3. Chunk Large Batches

```java
int chunkSize = 100;
List<String> largeIdList = getAllIds(); // 10,000 items

// Process in chunks
for (int i = 0; i < largeIdList.size(); i += chunkSize) {
    int end = Math.min(i + chunkSize, largeIdList.size());
    List<String> chunk = largeIdList.subList(i, end);

    // Process chunk
    List<Boolean> results = client.batchUpdate(
        chunk,
        getVectorsForIds(chunk),
        getMetadataForIds(chunk)
    );

    // Log progress
    logger.info("Processed chunk {}/{}", i / chunkSize + 1,
        (largeIdList.size() + chunkSize - 1) / chunkSize);
}
```

### 4. Monitor Performance

```java
long startTime = System.nanoTime();

List<List<SearchResult>> results = client.batchSearch(queries, k);

long endTime = System.nanoTime();
double totalTime = (endTime - startTime) / 1_000_000.0;

// Log metrics
logger.info("Batch search: {} queries in {:.2f} ms ({:.3f} ms/query)",
    queries.length, totalTime, totalTime / queries.length);

// Alert if slow
if (totalTime / queries.length > THRESHOLD_MS) {
    logger.warn("Batch search performance degraded");
}
```

### 5. Use Appropriate Metadata

```java
// ✅ Good - relevant metadata
Map<String, Object> metadata = Map.of(
    "updated_at", System.currentTimeMillis(),
    "version", 2,
    "source", "batch_import"
);

// ❌ Bad - too much metadata
Map<String, Object> metadata = Map.of(
    "entire_document_text", largeText, // Too large!
    "full_history", historyList // Too complex!
);
```

## Troubleshooting

### Issue: Batch Operations Slower Than Individual

**Symptoms:**
- Batch operations take longer than individual operations
- Minimal or negative speedup

**Possible Causes:**
1. **Batch too small** - Overhead not amortized
2. **Contention** - Multiple threads accessing same index
3. **Memory pressure** - Large batches causing GC
4. **Cold cache** - First batch after restart

**Solutions:**
```java
// Increase batch size
int batchSize = 50; // From 5

// Use single-threaded access for batch
synchronized (indexLock) {
    results = client.batchSearch(queries, k);
}

// Reduce batch size if memory constrained
int batchSize = 25; // From 500

// Warm up cache
client.search(warmupQuery, 10);
```

### Issue: Out of Memory Errors

**Symptoms:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Possible Causes:**
- Batch size too large
- High-dimensional vectors
- Too many concurrent batches

**Solutions:**
```java
// Reduce batch size
int batchSize = 50; // From 500

// Increase heap size
// -Xmx8g -Xms2g

// Process sequentially
for (List<double[]> batch : chunks) {
    client.batchSearch(batch.toArray(new double[0][]), k);
    // Results processed immediately
}
```

### Issue: Partial Failures Not Detected

**Symptoms:**
- Some updates silently fail
- Inconsistent database state

**Solutions:**
```java
// Always check results
List<Boolean> results = client.batchUpdate(ids, vectors, metadata);

// Validate all succeeded
if (!results.stream().allMatch(b -> b)) {
    // Handle failures
    for (int i = 0; i < results.size(); i++) {
        if (!results.get(i)) {
            logger.error("Failed: {}", ids.get(i));
        }
    }
}
```

### Issue: Slow File-Based Batch Operations

**Symptoms:**
- CLI batch commands slow with files
- High I/O wait time

**Solutions:**
```bash
# Use SSD storage for input files
mv queries.txt /fast-storage/

# Reduce file size (split large files)
split -l 100 large_queries.txt chunk_

# Use binary format instead of text
# (Custom implementation needed)
```

## Advanced Topics

### Parallel Batch Processing

For very large datasets, process multiple batches in parallel:

```java
ExecutorService executor = Executors.newFixedThreadPool(4);
List<Future<List<List<SearchResult>>>> futures = new ArrayList<>();

for (double[][] batch : batches) {
    Future<List<List<SearchResult>>> future = executor.submit(() ->
        client.batchSearch(batch, k)
    );
    futures.add(future);
}

// Collect results
for (Future<List<List<SearchResult>>> future : futures) {
    List<List<SearchResult>> batchResults = future.get();
    processResults(batchResults);
}

executor.shutdown();
```

### Custom Batch Implementations

Create optimized batch operations for specific indices:

```java
public class OptimizedHNSWIndex extends HNSWIndex {
    @Override
    public List<List<SearchResult>> batchSearch(double[][] queries, int k) {
        // Custom optimized implementation
        // - Pre-compute entry points
        // - Share visited sets
        // - Parallel search with work stealing
        return customBatchSearch(queries, k);
    }
}
```

## See Also

- [Performance Guide](PERFORMANCE.md)
- [API Documentation](../README.md#api-documentation)
- [CLI Guide](CLI.md)
- [Examples](../src/main/java/com/veccy/examples/)
- [Integration Tests](../src/test/java/com/veccy/integration/BatchOperationsTest.java)

## Support

For issues and questions:
- GitHub Issues: https://github.com/skanga/veccy/issues
- Example Code: [BatchOperationsExample.java](../src/main/java/com/veccy/examples/BatchOperationsExample.java)

# Pagination in Veccy

This document describes the pagination support in Veccy for efficiently listing large sets of vector IDs.

## Overview

Veccy provides cursor-based pagination for listing vector IDs, which is more efficient than offset-based pagination for large datasets and provides consistent results even when data is being modified.

## Features

- **Cursor-based pagination**: More efficient than offset-based for large datasets
- **Consistent ordering**: Sorted results for predictable pagination
- **Memory efficient**: Only loads one page at a time
- **Concurrent-safe**: Works correctly even with concurrent modifications
- **Flexible page sizes**: Configure page size based on your needs
- **Multiple output formats**: Table, JSON, and CSV support in CLI

## API Usage

### Simple Listing

List all vectors or up to a limit:

```java
VectorDBClient client = VectorDBFactory.createSimple();
client.initialize();

// List all vector IDs
List<String> allIds = client.listVectorIds(null);

// List first 100 IDs
List<String> firstHundred = client.listVectorIds(100);
```

### Paginated Listing

Efficiently iterate through large result sets:

```java
import com.veccy.base.Page;
import java.util.Optional;

VectorDBClient client = VectorDBFactory.createSimple();
client.initialize();

// First page
Page<String> page = client.listVectorIdsPaginated(50, Optional.empty());

System.out.println("Items: " + page.items());
System.out.println("Size: " + page.size());
System.out.println("Has more: " + page.hasMore());

// Next page
if (page.hasMore()) {
    Page<String> nextPage = client.listVectorIdsPaginated(50, page.nextCursor());
    // Process next page...
}
```

### Iterating Through All Pages

```java
Optional<String> cursor = Optional.empty();
int pageSize = 100;
int totalProcessed = 0;

while (true) {
    Page<String> page = client.listVectorIdsPaginated(pageSize, cursor);

    if (page.isEmpty()) {
        break;
    }

    // Process page items
    for (String id : page.items()) {
        System.out.println("Processing: " + id);
        totalProcessed++;
    }

    // Check if there are more pages
    if (!page.hasMore()) {
        break;
    }

    cursor = page.nextCursor();
}

System.out.println("Total processed: " + totalProcessed);
```

### Streaming API

For processing all IDs without explicit pagination:

```java
try (Stream<String> stream = client.streamVectorIds()) {
    stream.forEach(id -> {
        System.out.println("Processing: " + id);
    });
}
```

## CLI Usage

### Simple Listing

```bash
# List all vectors
veccy list-vectors

# List first 10 vectors
veccy list-vectors --limit 10
```

### Paginated Listing

```bash
# First page (50 items)
veccy list-vectors --page-size 50

# Output example:
# Vector IDs (Page):
# ────────────────────────────────────────────────────────────────────────────────
#    1. vec_001
#    2. vec_002
#    ...
#   50. vec_050
# ────────────────────────────────────────────────────────────────────────────────
# Page size: 50
# Has more: Yes
# Next cursor: vec_050

# Next page
veccy list-vectors --page-size 50 --cursor vec_050
```

### Output Formats

#### Table Format (Default)

```bash
veccy list-vectors --page-size 20

# Output:
# Vector IDs (Page):
# ────────────────────────────────────────────────────────────────────────────────
#    1. vec_001
#    2. vec_002
#    ...
#   20. vec_020
# ────────────────────────────────────────────────────────────────────────────────
# Page size: 20
# Has more: Yes
# Next cursor: vec_020
```

#### JSON Format

```bash
veccy list-vectors --page-size 20 --format json

# Output:
# {
#   "items": [
#     "vec_001",
#     "vec_002",
#     ...
#     "vec_020"
#   ],
#   "size": 20,
#   "hasMore": true,
#   "nextCursor": "vec_020"
# }
```

#### CSV Format

```bash
veccy list-vectors --page-size 20 --format csv

# Output:
# index,id
# 1,vec_001
# 2,vec_002
# ...
# 20,vec_020
# # Page size: 20
# # Has more: true
# # Next cursor: vec_020
```

## Page Object

The `Page<T>` record provides:

```java
public record Page<T>(
    List<T> items,           // Items in this page
    Optional<String> nextCursor,  // Cursor for next page
    boolean hasMore          // True if more pages exist
) {
    public int size();       // Number of items in page
    public boolean isEmpty(); // True if no items
}
```

### Creating Pages

```java
// Page with more items after it
Page<String> page = Page.of(items, nextCursor);

// Last page (no more items)
Page<String> lastPage = Page.last(items);

// Empty page
Page<String> emptyPage = Page.empty();
```

## Storage Backend Implementation

### Default Implementation

The `StorageBackend` interface provides a default implementation that works with any backend:

```java
default Page<String> listVectorsPaginated(int pageSize, Optional<String> cursor) {
    List<String> allIds = listVectors(null);

    int startIndex = 0;
    if (cursor.isPresent()) {
        int cursorIndex = allIds.indexOf(cursor.get());
        if (cursorIndex >= 0) {
            startIndex = cursorIndex + 1;
        }
    }

    if (startIndex >= allIds.size()) {
        return Page.empty();
    }

    int endIndex = Math.min(startIndex + pageSize, allIds.size());
    List<String> pageItems = allIds.subList(startIndex, endIndex);

    if (endIndex < allIds.size()) {
        return Page.of(pageItems, allIds.get(endIndex - 1));
    } else {
        return Page.last(pageItems);
    }
}
```

### Optimized Implementations

Storage backends can override with more efficient implementations:

#### MemoryStorage

```java
@Override
public Page<String> listVectorsPaginated(int pageSize, Optional<String> cursor) {
    lock.readLock().lock();
    try {
        // Get sorted list for consistent ordering
        List<String> allIds = new ArrayList<>(vectors.keySet());
        Collections.sort(allIds);

        // Find start position based on cursor
        int startIndex = 0;
        if (cursor.isPresent()) {
            int cursorIndex = allIds.indexOf(cursor.get());
            if (cursorIndex >= 0) {
                startIndex = cursorIndex + 1;
            }
        }

        // Extract page
        if (startIndex >= allIds.size()) {
            return Page.empty();
        }

        int endIndex = Math.min(startIndex + pageSize, allIds.size());
        List<String> pageItems = allIds.subList(startIndex, endIndex);

        if (endIndex < allIds.size()) {
            return Page.of(pageItems, allIds.get(endIndex - 1));
        } else {
            return Page.last(pageItems);
        }
    } finally {
        lock.readLock().unlock();
    }
}
```

#### DiskStorage

```java
@Override
public Page<String> listVectorsPaginated(int pageSize, Optional<String> cursor) {
    try (Stream<Path> files = Files.list(vectorsDir)) {
        // Get sorted list of IDs
        List<String> allIds = files
                .filter(p -> p.toString().endsWith(".vec"))
                .map(p -> {
                    String fileName = p.getFileName().toString();
                    return fileName.substring(0, fileName.length() - 4);
                })
                .sorted()
                .collect(Collectors.toList());

        // Same pagination logic as above
        // ...
    }
}
```

## Performance Considerations

### Page Size Selection

Choose page size based on your use case:

- **Small pages (10-50)**: Interactive UIs, real-time updates
- **Medium pages (50-200)**: Batch processing with progress updates
- **Large pages (200-1000)**: Bulk operations, minimizing API calls

### Memory Usage

Cursor-based pagination only loads one page at a time:

```
Memory usage = Page size × Average ID length
```

For 100 IDs averaging 20 bytes each: ~2 KB per page

### Cursor Invalidation

If the vector corresponding to a cursor is deleted, pagination will restart from the beginning. The implementation logs a warning in this case.

## Best Practices

### 1. Use Consistent Page Sizes

Keep page sizes consistent throughout pagination:

```java
int pageSize = 100;
Optional<String> cursor = Optional.empty();

while (true) {
    Page<String> page = client.listVectorIdsPaginated(pageSize, cursor);

    if (page.isEmpty()) break;

    // Process page...

    if (!page.hasMore()) break;
    cursor = page.nextCursor();
}
```

### 2. Handle Errors Gracefully

```java
try {
    Page<String> page = client.listVectorIdsPaginated(100, cursor);
    // Process page...
} catch (VeccyException e) {
    logger.error("Pagination failed: {}", e.getMessage());
    // Retry from beginning or handle appropriately
}
```

### 3. Limit Total Results

Prevent unbounded iteration:

```java
int maxPages = 100;
int pagesProcessed = 0;
Optional<String> cursor = Optional.empty();

while (pagesProcessed < maxPages) {
    Page<String> page = client.listVectorIdsPaginated(50, cursor);

    if (page.isEmpty() || !page.hasMore()) break;

    // Process page...
    pagesProcessed++;
    cursor = page.nextCursor();
}
```

### 4. Use Streaming for Full Iteration

When processing all IDs, streaming is simpler:

```java
try (Stream<String> stream = client.streamVectorIds()) {
    stream
        .filter(id -> id.startsWith("user_"))
        .limit(1000)
        .forEach(this::processVector);
}
```

## Examples

### Example 1: Display All Vectors with Progress

```java
public void displayAllVectors(VectorDBClient client) {
    int pageSize = 100;
    int totalDisplayed = 0;
    Optional<String> cursor = Optional.empty();

    System.out.println("Listing all vectors...");

    while (true) {
        Page<String> page = client.listVectorIdsPaginated(pageSize, cursor);

        if (page.isEmpty()) {
            break;
        }

        for (String id : page.items()) {
            System.out.println(++totalDisplayed + ". " + id);
        }

        if (!page.hasMore()) {
            break;
        }

        cursor = page.nextCursor();
        System.out.println("... loading more ...");
    }

    System.out.println("\nTotal vectors: " + totalDisplayed);
}
```

### Example 2: Export Vectors by Pages

```java
public void exportVectorsByPage(VectorDBClient client, Path outputDir)
        throws IOException {
    int pageSize = 1000;
    int pageNumber = 1;
    Optional<String> cursor = Optional.empty();

    while (true) {
        Page<String> page = client.listVectorIdsPaginated(pageSize, cursor);

        if (page.isEmpty()) {
            break;
        }

        // Export page to file
        Path pageFile = outputDir.resolve("vectors_page_" + pageNumber + ".txt");
        Files.write(pageFile, page.items());

        System.out.println("Exported page " + pageNumber +
                         " (" + page.size() + " vectors)");

        if (!page.hasMore()) {
            break;
        }

        cursor = page.nextCursor();
        pageNumber++;
    }

    System.out.println("Export complete: " + pageNumber + " pages");
}
```

### Example 3: Find Vectors Matching Pattern

```java
public List<String> findMatchingVectors(VectorDBClient client, String pattern) {
    List<String> matches = new ArrayList<>();
    int pageSize = 500;
    Optional<String> cursor = Optional.empty();

    while (true) {
        Page<String> page = client.listVectorIdsPaginated(pageSize, cursor);

        if (page.isEmpty()) {
            break;
        }

        // Filter matching IDs
        page.items().stream()
            .filter(id -> id.matches(pattern))
            .forEach(matches::add);

        if (!page.hasMore()) {
            break;
        }

        cursor = page.nextCursor();
    }

    return matches;
}
```

### Example 4: Batch Processing with Rate Limiting

```java
public void processBatch(VectorDBClient client,
                        Consumer<String> processor,
                        int rateLimit) throws InterruptedException {
    int pageSize = 50;
    int processed = 0;
    Optional<String> cursor = Optional.empty();
    long startTime = System.currentTimeMillis();

    while (true) {
        Page<String> page = client.listVectorIdsPaginated(pageSize, cursor);

        if (page.isEmpty()) {
            break;
        }

        for (String id : page.items()) {
            processor.accept(id);
            processed++;

            // Rate limiting
            if (processed % rateLimit == 0) {
                long elapsed = System.currentTimeMillis() - startTime;
                long expectedTime = (processed * 1000L) / rateLimit;
                if (elapsed < expectedTime) {
                    Thread.sleep(expectedTime - elapsed);
                }
            }
        }

        if (!page.hasMore()) {
            break;
        }

        cursor = page.nextCursor();
    }

    System.out.println("Processed " + processed + " vectors");
}
```

## Testing

### Unit Tests

```java
@Test
void testPaginationFirstPage() {
    VectorDBClient client = createAndInitializeClient();

    // Insert test vectors
    for (int i = 0; i < 100; i++) {
        double[] vector = {i, i + 1, i + 2};
        client.insert(new double[][]{vector}, null);
    }

    // Test first page
    Page<String> page = client.listVectorIdsPaginated(20, Optional.empty());

    assertEquals(20, page.size());
    assertTrue(page.hasMore());
    assertTrue(page.nextCursor().isPresent());
}

@Test
void testPaginationLastPage() {
    VectorDBClient client = createAndInitializeClient();

    // Insert 50 vectors
    for (int i = 0; i < 50; i++) {
        double[] vector = {i, i + 1};
        client.insert(new double[][]{vector}, null);
    }

    // Get first page
    Page<String> page1 = client.listVectorIdsPaginated(30, Optional.empty());
    assertEquals(30, page1.size());
    assertTrue(page1.hasMore());

    // Get last page
    Page<String> page2 = client.listVectorIdsPaginated(30, page1.nextCursor());
    assertEquals(20, page2.size());
    assertFalse(page2.hasMore());
    assertTrue(page2.nextCursor().isEmpty());
}

@Test
void testPaginationEmptyResult() {
    VectorDBClient client = createAndInitializeClient();

    Page<String> page = client.listVectorIdsPaginated(10, Optional.empty());

    assertTrue(page.isEmpty());
    assertEquals(0, page.size());
    assertFalse(page.hasMore());
}
```

## Troubleshooting

### Issue: Cursor not found warning

**Cause**: The vector referenced by the cursor was deleted between page requests.

**Solution**: The implementation automatically restarts from the beginning. If this is frequent, consider:
- Reducing page size for faster iteration
- Implementing pagination at application level with snapshots

### Issue: Inconsistent ordering between pages

**Cause**: Concurrent modifications affecting sort order.

**Solution**: All implementations use sorted IDs for consistent ordering. For strict consistency, use snapshot isolation at application level.

### Issue: Out of memory with large page sizes

**Cause**: Page size too large for available memory.

**Solution**: Reduce page size. Memory usage = page size × average ID length.

## See Also

- [Batch Operations](BATCH_OPERATIONS.md) - Efficient batch processing
- [Storage Backends](../README.md#storage-backends) - Available storage options
- [API Reference](../README.md#api-reference) - Complete API documentation

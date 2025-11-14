# Import/Export API Guide

**Version**: 1.0.0
**Last Updated**: 2025-11-13

---

## Overview

The Veccy REST API provides import and export endpoints to facilitate bulk data migration, backup/restore operations, and database transfers.

---

## Import Endpoint

**Endpoint**: `POST /api/v1/databases/:name/import`

Import vectors into an existing database from a JSON payload.

### Request Format

```json
{
  "vectors": [
    {
      "vector": [1.0, 2.0, 3.0],
      "metadata": {
        "label": "example",
        "category": "test"
      }
    },
    {
      "vector": [4.0, 5.0, 6.0],
      "metadata": {
        "label": "another",
        "category": "demo"
      }
    }
  ]
}
```

### Field Descriptions

- `vectors` (required): Array of vector objects to import
  - `vector` (required): Array of doubles representing the vector
  - `metadata` (optional): Key-value pairs for vector metadata
  - `id` (optional): Custom vector ID (auto-generated if not provided)

### Example: Import Vectors

```bash
curl -X POST http://localhost:7878/api/v1/databases/my_db/import \
  -H "Content-Type: application/json" \
  -d '{
    "vectors": [
      {
        "vector": [0.1, 0.2, 0.3, 0.4, 0.5],
        "metadata": {"doc_id": "doc1", "title": "First Document"}
      },
      {
        "vector": [0.6, 0.7, 0.8, 0.9, 1.0],
        "metadata": {"doc_id": "doc2", "title": "Second Document"}
      }
    ]
  }'
```

### Response (Success - 201 Created)

```json
{
  "success": true,
  "message": "Successfully imported 2 vectors",
  "data": {
    "imported_count": 2,
    "ids": ["uuid-1", "uuid-2"],
    "database": "my_db"
  },
  "timestamp": 1699900000000
}
```

### Response (Error - 400 Bad Request)

```json
{
  "success": false,
  "message": "Vector at index 0 validation failed: Vector dimension mismatch",
  "data": null,
  "timestamp": 1699900000000
}
```

### Validation

The import endpoint validates:
- ✅ Vector dimensions match database configuration
- ✅ Vector values are valid (no NaN or Infinity)
- ✅ Metadata size limits (max 1MB per vector)
- ✅ Batch size limits (max 1000 vectors per request)
- ✅ Metadata key/value length limits

---

## Export Endpoint

**Endpoint**: `GET /api/v1/databases/:name/export?limit=1000`

Export vectors from a database to JSON format.

### Query Parameters

- `limit` (optional): Maximum number of vectors to export
  - Default: `1000`
  - Maximum: `100000`
  - Must be positive integer

### Example: Export All Vectors (Default Limit)

```bash
curl -X GET http://localhost:7878/api/v1/databases/my_db/export
```

### Example: Export with Custom Limit

```bash
curl -X GET "http://localhost:7878/api/v1/databases/my_db/export?limit=100"
```

### Response (Success - 200 OK)

```json
{
  "success": true,
  "message": "Successfully exported 2 vectors",
  "data": {
    "database": "my_db",
    "count": 2,
    "vectors": [
      {
        "id": "uuid-1",
        "vector": [0.1, 0.2, 0.3, 0.4, 0.5],
        "metadata": {
          "doc_id": "doc1",
          "title": "First Document"
        }
      },
      {
        "id": "uuid-2",
        "vector": [0.6, 0.7, 0.8, 0.9, 1.0],
        "metadata": {
          "doc_id": "doc2",
          "title": "Second Document"
        }
      }
    ],
    "stats": {
      "vector_count": 2,
      "dimensions": 5,
      "index_type": "hnsw"
    }
  },
  "timestamp": 1699900000000
}
```

### Response (Error - 400 Bad Request)

```json
{
  "success": false,
  "message": "Limit exceeds maximum allowed: 100000",
  "data": null,
  "timestamp": 1699900000000
}
```

---

## Use Cases

### 1. Database Backup

```bash
# Export database to file
curl -X GET "http://localhost:7878/api/v1/databases/production_db/export?limit=50000" \
  > backup_$(date +%Y%m%d).json
```

### 2. Database Migration

```bash
# Export from source
curl -X GET http://source-server:7878/api/v1/databases/old_db/export \
  > migration_data.json

# Import to destination
curl -X POST http://dest-server:7878/api/v1/databases/new_db/import \
  -H "Content-Type: application/json" \
  -d @migration_data.json
```

### 3. Incremental Export

```bash
# Export in batches
for i in {0..10}; do
  offset=$((i * 1000))
  curl -X GET "http://localhost:7878/api/v1/databases/my_db/export?limit=1000" \
    > batch_${i}.json
done
```

### 4. Data Transformation

```bash
# Export data
curl -X GET http://localhost:7878/api/v1/databases/my_db/export > data.json

# Process with jq or Python
cat data.json | jq '.data.vectors[] | select(.metadata.category == "important")'

# Import transformed data
curl -X POST http://localhost:7878/api/v1/databases/filtered_db/import \
  -H "Content-Type: application/json" \
  -d @transformed_data.json
```

---

## Error Codes

| Code | Meaning | Common Causes |
|------|---------|---------------|
| 200 | OK | Export successful |
| 201 | Created | Import successful |
| 400 | Bad Request | Invalid vector dimensions, malformed JSON, invalid parameters |
| 404 | Not Found | Database does not exist |
| 500 | Internal Server Error | Database operation failed, storage backend error |

---

## Limits and Constraints

### Import Limits
- **Max vectors per request**: 1,000
- **Max vector dimensions**: 10,000
- **Max metadata size**: 1 MB per vector
- **Max metadata entries**: 100 per vector
- **Max metadata key length**: 256 characters
- **Max metadata value length**: 10,000 characters

### Export Limits
- **Default export limit**: 1,000 vectors
- **Max export limit**: 100,000 vectors
- **Pagination**: Cursor-based (internal)

---

## Best Practices

### Import Best Practices

1. **Batch Size**: Import in batches of 100-1000 vectors for optimal performance
2. **Validation**: Validate vector dimensions before importing
3. **Error Handling**: Check response codes and retry failed imports
4. **Metadata**: Keep metadata concise to reduce payload size
5. **Idempotency**: Imports are not idempotent - duplicate calls create duplicate vectors

### Export Best Practices

1. **Limit Parameter**: Use appropriate limit based on memory constraints
2. **Incremental Export**: For large databases, export in batches
3. **Storage**: Save exported data to file for backup/migration
4. **Filtering**: Export supports stats - use them to validate completeness
5. **Timing**: Export during low-traffic periods for large databases

---

## Performance

### Import Performance
- **Throughput**: ~1,000 vectors/second (varies by dimensions and hardware)
- **Memory**: Proportional to batch size and vector dimensions
- **Validation**: Adds ~10-20% overhead

### Export Performance
- **Throughput**: ~2,000 vectors/second (varies by storage backend)
- **Memory**: Minimal (uses pagination internally)
- **Streaming**: Exports use cursor-based pagination for efficiency

---

## Authentication

If API key authentication is enabled:

```bash
# With authentication
curl -X POST http://localhost:7878/api/v1/databases/my_db/import \
  -H "X-API-Key: your-api-key-here" \
  -H "Content-Type: application/json" \
  -d @import_data.json
```

See [REST API Guide](REST_API.md) for authentication details.

---

## Examples

### Complete Import/Export Workflow

```bash
# 1. Create database
curl -X POST http://localhost:7878/api/v1/databases \
  -H "Content-Type: application/json" \
  -d '{
    "name": "my_vectors",
    "dimensions": 768,
    "index_config": {"type": "hnsw"},
    "storage_config": {"type": "memory"}
  }'

# 2. Import vectors
curl -X POST http://localhost:7878/api/v1/databases/my_vectors/import \
  -H "Content-Type: application/json" \
  -d '{
    "vectors": [
      {"vector": [/* 768 values */], "metadata": {"id": "1"}},
      {"vector": [/* 768 values */], "metadata": {"id": "2"}}
    ]
  }'

# 3. Verify import
curl -X GET http://localhost:7878/api/v1/databases/my_vectors/info

# 4. Export for backup
curl -X GET http://localhost:7878/api/v1/databases/my_vectors/export \
  > backup.json

# 5. Restore to new database
curl -X POST http://localhost:7878/api/v1/databases \
  -H "Content-Type: application/json" \
  -d '{
    "name": "my_vectors_restored",
    "dimensions": 768,
    "index_config": {"type": "hnsw"}
  }'

curl -X POST http://localhost:7878/api/v1/databases/my_vectors_restored/import \
  -H "Content-Type: application/json" \
  -d @backup.json
```

---

## Troubleshooting

### Import Issues

**Problem**: "Vector dimension mismatch"
- **Solution**: Ensure all vectors have the same dimensions as the database configuration

**Problem**: "Metadata validation failed"
- **Solution**: Check metadata size limits and key/value lengths

**Problem**: "Batch size exceeds maximum"
- **Solution**: Split import into smaller batches (max 1000 vectors)

### Export Issues

**Problem**: "Limit exceeds maximum allowed"
- **Solution**: Use limit ≤ 100,000 or export in multiple batches

**Problem**: "Database does not support export operation"
- **Solution**: Ensure database was created via VectorDBFactory (should not occur in normal use)

**Problem**: Out of memory during export
- **Solution**: Reduce limit parameter or increase server memory

---

## Related Documentation

- **[REST API Guide](REST_API.md)** - Complete REST API documentation
- **[CLI Guide](CLI.md)** - Command-line import/export commands
- **[Batch Operations](BATCH_OPERATIONS.md)** - Bulk operation optimization

---

**Last Updated**: 2025-11-13

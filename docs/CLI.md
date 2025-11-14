# Veccy CLI Documentation

The Veccy CLI provides a command-line interface for managing vector databases with better developer experience.

## Table of Contents

- [Installation](#installation)
- [Quick Start](#quick-start)
- [Usage Modes](#usage-modes)
- [Commands](#commands)
- [Examples](#examples)
- [Configuration](#configuration)
- [Best Practices](#best-practices)

## Installation

### Prerequisites

- Java 21 or later
- Maven (for building from source)

### Building

```bash
# Clone the repository
git clone https://github.com/yourusername/veccy.git
cd veccy

# Build the project
mvn clean package

# Make the launcher executable (Unix/Linux/macOS)
chmod +x veccy

# Add to PATH (optional)
export PATH=$PATH:$(pwd)
```

### Windows

Use `veccy.bat` instead of `veccy` for all commands.

## Quick Start

### Interactive Mode

Start the interactive REPL:

```bash
./veccy
```

This opens an interactive session:

```
╔════════════════════════════════════════╗
║                                        ║
║   VECCY - Vector Database CLI          ║
║   Version 1.0.0                        ║
║                                        ║
╚════════════════════════════════════════╝

Type 'help' for available commands, 'exit' to quit.

veccy> init --index hnsw
✓ Database initialized successfully
  Storage: memory
  Index:   hnsw
  Metric:  cosine

veccy> insert [1.0,2.0,3.0] --metadata label=example
✓ Inserted 1 vector
  ID: a1b2c3d4-e5f6-7890-abcd-ef1234567890

veccy> search [1.0,2.0,3.0] --k 5
Search Results:

Rank ID                                   Distance     Metadata
────────────────────────────────────────────────────────────────────────────────
1    a1b2c3d4-e5f6-7890-abcd-ef1234567890 0.000000     {label=example}

Found 1 result(s)

veccy> exit
Goodbye!
```

### Single Command Mode

Execute individual commands:

```bash
# Initialize database
./veccy init --index hnsw --storage memory

# Insert a vector
./veccy insert "[1.0,2.0,3.0]" --metadata "label=test"

# Search for similar vectors
./veccy search "[1.0,2.0,3.0]" --k 10

# Show statistics
./veccy stats

# Export data
./veccy export vectors.json --format json
```

## Usage Modes

### Interactive Mode (REPL)

Launch without arguments to start the Read-Eval-Print Loop:

```bash
./veccy
```

**Features:**
- Command history (arrow keys)
- Tab completion (planned)
- Multi-line support for complex commands
- Persistent session state
- Graceful error handling

**Tips:**
- Use `help` to see all commands
- Use `help <command>` for specific command help
- Use `exit` or `Ctrl+D` to quit

### Single Command Mode

Execute one command and exit:

```bash
./veccy <command> [options]
```

**Use Cases:**
- Scripting and automation
- CI/CD pipelines
- Batch operations
- System monitoring

## Commands

### Database Management

#### `init` - Initialize Database

Initialize a new vector database.

**Usage:**
```bash
init [--path <path>] [--index <type>] [--storage <type>] [--metric <metric>]
```

**Options:**
- `--path, -p <path>`: Database path (required for disk/hybrid storage)
- `--index, -i <type>`: Index type (flat, hnsw, ivf, lsh, annoy) [default: hnsw]
- `--storage, -s <type>`: Storage type (memory, disk, hybrid) [default: memory]
- `--metric, -m <metric>`: Distance metric (cosine, euclidean, dot_product, manhattan) [default: cosine]

**Examples:**
```bash
# In-memory database with HNSW index
init --index hnsw --storage memory

# Persistent database with IVF index
init --path ./mydb --index ivf --storage disk --metric euclidean

# Hybrid storage for large datasets
init --path ./mydb --storage hybrid --index hnsw
```

#### `info` - Show Database Info

Display information about the current database.

**Usage:**
```bash
info
```

**Example:**
```bash
veccy> info
Database Information:
  Status:      Open
  Path:        ./mydb
  Output Mode: table
  Verbose:     false
```

#### `stats` - Show Statistics

Display detailed database statistics.

**Usage:**
```bash
stats [--format <format>]
```

**Options:**
- `--format, -f <format>`: Output format (table, json) [default: table]

**Examples:**
```bash
# Table format
stats

# JSON format
stats --format json
```

**Sample Output:**
```
Database Statistics:

Storage:
  type                : MemoryStorage
  vector_count        : 1000
  memory_usage_bytes  : 524288

Index:
  type                : HNSWIndex
  vector_count        : 1000
  dimensions          : 128
  metric              : cosine
  m                   : 16
  ef_construction     : 200
  ef_search           : 50
```

### Vector Operations

#### `insert` - Insert Vectors

Add vectors to the database.

**Usage:**
```bash
insert <vector> [--id <id>] [--metadata <key=value>...]
insert --file <path>
```

**Options:**
- `--id <id>`: Custom ID for the vector (optional)
- `--metadata, -m <key=value>`: Metadata key-value pair (can be repeated)
- `--file, -f <path>`: Import from file (use `import` command instead)

**Vector Format:**
- With brackets: `[1.0, 2.0, 3.0]`
- Without brackets: `1.0,2.0,3.0`
- Spaces optional

**Examples:**
```bash
# Simple insert
insert [1.0,2.0,3.0]

# With metadata
insert [1.0,2.0,3.0] --metadata label=example --metadata category=test

# Without brackets
insert 1.0,2.0,3.0 --metadata type=demo
```

#### `search` - Search Similar Vectors

Find vectors similar to a query vector.

**Usage:**
```bash
search <vector> [--k <count>] [--format <format>]
```

**Options:**
- `--k, -k <count>`: Number of results to return [default: 10]
- `--format, -f <format>`: Output format (table, json, csv) [default: table]

**Examples:**
```bash
# Basic search
search [1.0,2.0,3.0]

# Return top 5 results
search [1.0,2.0,3.0] --k 5

# JSON output for scripting
search [1.0,2.0,3.0] --k 20 --format json

# CSV output for spreadsheets
search [1.0,2.0,3.0] --format csv > results.csv
```

**Sample Output (Table):**
```
Search Results:

Rank ID                                   Distance     Metadata
────────────────────────────────────────────────────────────────────────────────
1    a1b2c3d4-e5f6-7890-abcd-ef1234567890 0.000000     {label=example}
2    b2c3d4e5-f678-90ab-cdef-123456789012 0.125432     {label=test}
3    c3d4e5f6-7890-abcd-ef12-3456789012ab 0.234567     {label=demo}

Found 3 result(s)
```

**Sample Output (JSON):**
```json
[
  {"id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890", "distance": 0.0, "metadata": {"label": "example"}},
  {"id": "b2c3d4e5-f678-90ab-cdef-123456789012", "distance": 0.125432, "metadata": {"label": "test"}},
  {"id": "c3d4e5f6-7890-abcd-ef12-3456789012ab", "distance": 0.234567, "metadata": {"label": "demo"}}
]
```

#### `update` - Update Vector

Update a vector or its metadata.

**Usage:**
```bash
update <id> --vector <vector> | --metadata <key=value>...
```

**Options:**
- `--vector <vector>`: New vector values
- `--metadata, -m <key=value>`: Metadata to update (can be repeated)

**Examples:**
```bash
# Update vector only
update a1b2c3d4-e5f6-7890-abcd-ef1234567890 --vector [4.0,5.0,6.0]

# Update metadata only
update a1b2c3d4-e5f6-7890-abcd-ef1234567890 --metadata label=updated --metadata status=active

# Update both
update a1b2c3d4-e5f6-7890-abcd-ef1234567890 --vector [4.0,5.0,6.0] --metadata label=new
```

#### `delete` - Delete Vectors

Remove vectors by ID.

**Usage:**
```bash
delete <id> [<id>...]
```

**Aliases:** `remove`, `rm`

**Examples:**
```bash
# Delete single vector
delete a1b2c3d4-e5f6-7890-abcd-ef1234567890

# Delete multiple vectors
delete id1 id2 id3

# Using alias
rm a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

#### `list` - List Vectors

List vectors in the database.

**Usage:**
```bash
list [--limit <n>] [--format <format>]
```

**Aliases:** `ls`

**Options:**
- `--limit <n>`: Maximum number of vectors to list [default: 100]
- `--format, -f <format>`: Output format (table, json, csv) [default: table]

**Examples:**
```bash
# List first 100 vectors
list

# List first 50 vectors
list --limit 50

# JSON format
list --limit 20 --format json
```

### Data Management

#### `import` - Import Vectors

Import vectors from a file.

**Usage:**
```bash
import <file> [--format <format>]
```

**Options:**
- `--format, -f <format>`: File format (csv, json) [auto-detected from extension]

**CSV Format:**
```csv
1.0,2.0,3.0
4.0,5.0,6.0,label=test
7.0,8.0,9.0,label=demo,category=example
```

**JSON Format:**
```json
[
  {
    "vector": [1.0, 2.0, 3.0],
    "metadata": {"label": "test"}
  },
  {
    "vector": [4.0, 5.0, 6.0],
    "metadata": {"label": "demo", "category": "example"}
  }
]
```

**Examples:**
```bash
# Import CSV file
import vectors.csv

# Import JSON file
import data.json

# Explicit format
import mydata.txt --format csv
```

#### `export` - Export Vectors

Export vectors to a file.

**Usage:**
```bash
export <file> [--format <format>]
```

**Options:**
- `--format, -f <format>`: File format (csv, json) [auto-detected from extension]

**Examples:**
```bash
# Export to CSV
export vectors.csv

# Export to JSON
export data.json

# Explicit format
export backup.txt --format json
```

### General Commands

#### `help` - Show Help

Display help information.

**Usage:**
```bash
help [command]
```

**Aliases:** `h`, `?`

**Examples:**
```bash
# General help
help

# Command-specific help
help search
help init
```

#### `version` - Show Version

Display version information.

**Usage:**
```bash
version
```

**Aliases:** `v`, `--version`

#### `exit` - Exit CLI

Exit the interactive mode.

**Usage:**
```bash
exit
```

**Aliases:** `quit`, `q`

**Note:** Use `Ctrl+D` as alternative in interactive mode.

## Examples

### Example 1: Text Similarity Search

```bash
# Start interactive mode
./veccy

# Initialize with HNSW index
veccy> init --index hnsw --metric cosine

# Insert document embeddings
veccy> insert [0.1,0.2,0.3,0.4] --metadata doc=readme.md --metadata type=documentation
veccy> insert [0.15,0.18,0.32,0.39] --metadata doc=tutorial.md --metadata type=documentation
veccy> insert [0.8,0.1,0.05,0.05] --metadata doc=api.md --metadata type=api-reference

# Search for similar documents
veccy> search [0.12,0.21,0.31,0.38] --k 3

# Export results
veccy> export embeddings.json

veccy> exit
```

### Example 2: Batch Operations

```bash
# Initialize persistent database
./veccy init --path ./db --storage disk --index ivf

# Import large dataset
./veccy import large_dataset.csv

# Check statistics
./veccy stats

# Export specific results
./veccy search [1.0,2.0,3.0] --k 100 --format csv > top100.csv
```

### Example 3: Recommendation System

```bash
# Interactive mode
./veccy

# Setup
veccy> init --index hnsw --metric dot_product

# Insert user preference vectors
veccy> insert [0.8,0.1,0.5,0.3] --metadata user=alice --metadata category=movies
veccy> insert [0.2,0.9,0.1,0.7] --metadata user=bob --metadata category=movies
veccy> insert [0.7,0.2,0.6,0.2] --metadata user=charlie --metadata category=movies

# Find similar users to Alice
veccy> search [0.8,0.1,0.5,0.3] --k 5

# Update user preferences
veccy> update <alice-id> --vector [0.85,0.15,0.55,0.25]

# List all users
veccy> list --limit 100
```

### Example 4: Image Similarity

```bash
# One-liner initialization
./veccy init --index annoy --metric euclidean --storage memory

# Import image embeddings
./veccy import image_features.json

# Find similar images
./veccy search [0.123,0.456,...] --k 20 --format json > similar_images.json

# Get database stats
./veccy stats --format json
```

## Configuration

### Environment Variables

```bash
# Set Java home
export JAVA_HOME=/path/to/java

# Increase JVM memory
export JAVA_OPTS="-Xmx4g"
```

### Output Formats

The CLI supports multiple output formats:

1. **Table** (default): Human-readable tabular format
   - Best for interactive use
   - Clear visualization of results
   - Automatically truncates long values

2. **JSON**: Machine-readable JSON format
   - Best for scripting and automation
   - Preserves all data
   - Easy to parse with tools like `jq`

3. **CSV**: Comma-separated values
   - Best for spreadsheet import
   - Compatible with Excel, Google Sheets
   - Easy to process with standard tools

### Setting Default Format

In interactive mode:

```bash
# Not implemented yet, but planned:
veccy> set format json
veccy> set verbose true
```

## Best Practices

### Performance Tips

1. **Use Appropriate Index Types:**
   - `flat`: Small datasets (<10K vectors), exact search
   - `hnsw`: Medium datasets (10K-1M vectors), high accuracy
   - `ivf`: Large datasets (100K-10M vectors), good balance
   - `lsh`: Very large datasets (>1M vectors), approximate search
   - `annoy`: Read-heavy workloads, memory-mapped

2. **Choose the Right Storage:**
   - `memory`: Fast, for datasets that fit in RAM
   - `disk`: Persistent, for datasets larger than RAM
   - `hybrid`: Best of both, frequently accessed in memory

3. **Batch Operations:**
   - Use `import` for bulk loading instead of individual `insert`
   - Use batch `delete` with multiple IDs
   - Export to JSON for backup

4. **Optimize Search:**
   - Adjust `--k` parameter based on needs
   - Use appropriate metric for your use case
   - Consider index-specific tuning (HNSW ef_search, IVF num_probes)

### Scripting Tips

1. **Error Handling:**
```bash
#!/bin/bash
if ! ./veccy init --index hnsw; then
    echo "Failed to initialize database"
    exit 1
fi
```

2. **JSON Processing:**
```bash
# Search and process results with jq
./veccy search "[1,2,3]" --k 10 --format json | jq '.[] | select(.distance < 0.5)'
```

3. **Automation:**
```bash
# Daily backup script
#!/bin/bash
DATE=$(date +%Y%m%d)
./veccy export "backup_${DATE}.json"
```

### Security Considerations

1. **File Permissions:**
   - Protect database directory: `chmod 700 ./db`
   - Secure backup files: `chmod 600 backup.json`

2. **Data Validation:**
   - Validate input vectors before insertion
   - Sanitize metadata values
   - Check file sizes before import

3. **Resource Limits:**
   - Set memory limits in production
   - Monitor disk usage for persistent storage
   - Use `--limit` to prevent excessive output

## Troubleshooting

### Common Issues

**Issue: "No database is open"**
```
Solution: Initialize database first with `init` command
```

**Issue: Out of memory**
```
Solution: Increase JVM memory with JAVA_OPTS="-Xmx8g"
Or use disk/hybrid storage instead of memory
```

**Issue: Slow search performance**
```
Solution:
1. Use appropriate index type (HNSW for most cases)
2. Reduce --k parameter
3. Ensure database is properly initialized
4. Consider index-specific tuning
```

**Issue: Import fails with large files**
```
Solution:
1. Split file into smaller chunks
2. Increase JVM memory
3. Use streaming import (planned feature)
```

## Future Features

Planned enhancements for the CLI:

- [ ] Configuration file support (.veccyrc)
- [ ] Tab completion for commands
- [ ] Command history persistence
- [ ] Progress bars for long operations
- [ ] Streaming import/export for large files
- [ ] Query optimization suggestions
- [ ] Database compaction and optimization
- [ ] Multi-database support
- [ ] Distributed operations
- [ ] Web UI integration

## Contributing

We welcome contributions! See [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

## License

Veccy is licensed under the MIT License. See [LICENSE](../LICENSE) for details.

## Support

- **Documentation**: https://github.com/yourusername/veccy/docs
- **Issues**: https://github.com/yourusername/veccy/issues
- **Discussions**: https://github.com/yourusername/veccy/discussions

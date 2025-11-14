# Building Veccy

Guide for building Veccy from source.

## Prerequisites

- JDK 21 or higher
- Maven 3.9 or higher
- Git

## Quick Build

```bash
# Clone repository
git clone https://github.com/your-org/veccy.git
cd veccy

# Build with tests
mvn clean package

# Build without tests (faster)
mvn clean package -DskipTests
```

## Build Artifacts

After building, you'll find two JAR files in `target/`:

### 1. Regular JAR (`veccy-0.1.jar`)
- Size: ~255KB
- Contains only compiled classes
- Requires classpath configuration
- Not self-executable

### 2. Fat JAR (`veccy-0.1-fat.jar`)
- Size: ~105MB
- Contains all dependencies bundled
- Self-executable with `java -jar`
- Main-Class: `com.veccy.cli.VeccyCLI`
- **Recommended for deployment**

## Maven Shade Plugin

Veccy uses Maven Shade plugin to create the fat JAR:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.6.0</version>
    ...
</plugin>
```

**Features**:
- Bundles all dependencies into single JAR
- Relocates conflicting packages (if needed)
- Merges META-INF service files
- Excludes signature files from signed JARs
- Sets Main-Class manifest attribute

## Running the Fat JAR

### CLI Mode (Default)

The fat JAR's Main-Class is set to VeccyCLI:

```bash
# Show help
java -jar target/veccy-0.1-fat.jar help

# Interactive mode
java -jar target/veccy-0.1-fat.jar

# Initialize database
java -jar target/veccy-0.1-fat.jar init /path/to/db --type hnsw

# Search
java -jar target/veccy-0.1-fat.jar search "[0.1,0.2,0.3]" --top-k 10
```

### Running Specific Classes

You can run any class from the fat JAR:

```bash
# Run a specific class
java -cp target/veccy-0.1-fat.jar com.veccy.VeccyApplication

# Run with custom classpath
java -cp "target/veccy-0.1-fat.jar:custom.jar" com.example.MyClass
```

### With JVM Options

```bash
# Set memory options
java -Xmx8g -Xms2g -jar target/veccy-0.1-fat.jar

# Enable GC logging
java -Xlog:gc*:file=gc.log -jar target/veccy-0.1-fat.jar

# Remote debugging
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
  -jar target/veccy-0.1-fat.jar
```

## Build Profiles

### Default Profile

Standard build with all features:

```bash
mvn clean package
```

### Skip Tests

Faster build without running tests:

```bash
mvn clean package -DskipTests
```

### Offline Build

Build without downloading dependencies:

```bash
mvn clean package -o
```

### Clean Build

Remove all build artifacts and rebuild:

```bash
mvn clean install
```

## Build Customization

### Change Version

Edit `pom.xml`:

```xml
<version>1.0.0</version>
```

Then rebuild:

```bash
mvn clean package
```

### Custom Fat JAR Name

Edit `pom.xml` shade plugin configuration:

```xml
<finalName>veccy-custom-name</finalName>
```

### Exclude Dependencies

To exclude specific dependencies from fat JAR:

```xml
<filters>
    <filter>
        <artifact>group:artifact</artifact>
        <excludes>
            <exclude>**</exclude>
        </excludes>
    </filter>
</filters>
```

### Relocate Packages

To avoid dependency conflicts:

```xml
<relocations>
    <relocation>
        <pattern>com.google</pattern>
        <shadedPattern>veccy.shaded.google</shadedPattern>
    </relocation>
</relocations>
```

## Troubleshooting

### Build Fails

```bash
# Clean and retry
mvn clean

# Update dependencies
mvn dependency:purge-local-repository

# Force update
mvn clean package -U
```

### Out of Memory During Build

```bash
# Increase Maven memory
export MAVEN_OPTS="-Xmx2g"
mvn clean package
```

Or on Windows:

```cmd
set MAVEN_OPTS=-Xmx2g
mvn clean package
```

### Dependency Conflicts

Check dependency tree:

```bash
# Full tree
mvn dependency:tree

# With conflicts
mvn dependency:tree -Ddetail=true

# Specific dependency
mvn dependency:tree -Dincludes=groupId:artifactId
```

### Test Failures

```bash
# Run specific test
mvn test -Dtest=MyTest

# Run with debug output
mvn test -X

# Skip specific test
mvn test -Dtest=!FailingTest
```

## Distribution

### Create Distribution Package

```bash
# Build everything
mvn clean package

# Create distribution directory
mkdir -p dist
cp target/veccy-*-fat.jar dist/veccy.jar
cp README.md LICENSE dist/
cp -r docs dist/

# Create tarball
tar czf veccy-0.1-dist.tar.gz dist/

# Create zip
zip -r veccy-0.1-dist.zip dist/
```

### Installation Script

Create `install.sh`:

```bash
#!/bin/bash
# Simple installation script

INSTALL_DIR=${INSTALL_DIR:-/usr/local/lib/veccy}
BIN_DIR=${BIN_DIR:-/usr/local/bin}

# Copy JAR
sudo mkdir -p "$INSTALL_DIR"
sudo cp veccy.jar "$INSTALL_DIR/"

# Create wrapper script
cat > veccy << 'EOF'
#!/bin/bash
exec java -jar /usr/local/lib/veccy/veccy.jar "$@"
EOF

# Install wrapper
sudo mv veccy "$BIN_DIR/"
sudo chmod +x "$BIN_DIR/veccy"

echo "Veccy installed to $INSTALL_DIR"
echo "Run with: veccy <command>"
```

## CI/CD Integration

### GitHub Actions

```yaml
name: Build

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Cache Maven packages
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}

      - name: Build with Maven
        run: mvn clean package -B

      - name: Upload fat JAR
        uses: actions/upload-artifact@v3
        with:
          name: veccy-fat-jar
          path: target/veccy-*-fat.jar
```

### GitLab CI

```yaml
build:
  image: maven:3.9-eclipse-temurin-21
  stage: build
  script:
    - mvn clean package -DskipTests -B
  artifacts:
    paths:
      - target/veccy-*-fat.jar
    expire_in: 1 week
  cache:
    paths:
      - .m2/repository
```

## Docker Integration

The fat JAR is used in Docker builds:

```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
COPY --from=builder /build/target/veccy-*-fat.jar app.jar
CMD ["java", "-jar", "app.jar"]
```

## Performance Considerations

### Build Time

- **Clean build**: ~30-60 seconds
- **Incremental build**: ~10-20 seconds
- **With tests**: +30-60 seconds

### Optimization Tips

1. **Use offline mode** for repeated builds:
   ```bash
   mvn dependency:go-offline
   mvn clean package -o
   ```

2. **Skip unnecessary plugins**:
   ```bash
   mvn package -Dmaven.test.skip=true -Dmaven.javadoc.skip=true
   ```

3. **Parallel builds**:
   ```bash
   mvn clean package -T 4  # Use 4 threads
   ```

4. **Local repository**:
   - Keep ~/.m2/repository clean
   - Use repository manager (Nexus/Artifactory)

## Verification

After building, verify the fat JAR:

```bash
# Check JAR integrity
jar tf target/veccy-0.1-fat.jar | wc -l

# Check Main-Class
unzip -p target/veccy-0.1-fat.jar META-INF/MANIFEST.MF | grep Main-Class

# List dependencies
jar tf target/veccy-0.1-fat.jar | grep "^com/" | cut -d/ -f2 | sort -u

# Test execution
java -jar target/veccy-0.1-fat.jar --version
```

## Next Steps

- [Docker Deployment](DOCKER.md)
- [CLI Usage](CLI.md)
- [Development Guide](CONTRIBUTING.md)
- [API Documentation](API.md)

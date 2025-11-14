#!/bin/bash
# Veccy REST API Server launcher for Unix/Linux/macOS

# Find Java
if [ -n "$JAVA_HOME" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
else
    JAVA_CMD="java"
fi

# Get the directory where this script is located
VECCY_HOME="$(cd "$(dirname "$0")" && pwd)"

# Find the fat JAR
FAT_JAR=$(find "$VECCY_HOME/target" -name "veccy-*-fat.jar" | head -n 1)

if [ -z "$FAT_JAR" ]; then
    echo "Error: Fat JAR not found. Please build the project first:"
    echo "  mvn clean package"
    exit 1
fi

# Set default JVM options if not set
if [ -z "$JAVA_OPTS" ]; then
    JAVA_OPTS="-Xmx2g -Xms512m"
fi

# Default port and host
PORT=7878
HOST="localhost"

# Parse command line arguments
show_help() {
    echo "Veccy REST API Server"
    echo ""
    echo "Usage: veccy-server.sh [options]"
    echo ""
    echo "Options:"
    echo "  -p, --port <port>       Server port (default: 7878)"
    echo "  -h, --host <host>       Server host (default: localhost)"
    echo "  --help                  Show this help message"
    echo ""
    echo "Example:"
    echo "  ./veccy-server.sh --port 8080 --host 0.0.0.0"
    exit 0
}

while [[ $# -gt 0 ]]; do
    case $1 in
        -p|--port)
            PORT="$2"
            shift 2
            ;;
        -h|--host)
            HOST="$2"
            shift 2
            ;;
        --help)
            show_help
            ;;
        *)
            shift
            ;;
    esac
done

echo "Starting Veccy REST API Server on $HOST:$PORT..."
echo ""

# Run the REST server
$JAVA_CMD $JAVA_OPTS -cp "$FAT_JAR" com.veccy.rest.VeccyRestServer --port $PORT --host $HOST

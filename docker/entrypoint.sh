#!/bin/bash
set -e

# Veccy Docker Entrypoint Script

# Print banner
echo "╔════════════════════════════════════════╗"
echo "║                                        ║"
echo "║   VECCY - Vector Database              ║"
echo "║   Docker Container Starting...         ║"
echo "║                                        ║"
echo "╚════════════════════════════════════════╝"
echo ""

# Environment variables with defaults
export VECCY_DATA_DIR="${VECCY_DATA_DIR:-/data}"
export VECCY_LOG_DIR="${VECCY_LOG_DIR:-/logs}"
export VECCY_HEALTH_PORT="${VECCY_HEALTH_PORT:-8080}"
export VECCY_METRICS_PORT="${VECCY_METRICS_PORT:-8081}"
export JAVA_OPTS="${JAVA_OPTS:--Xmx2g -Xms512m}"

# Print configuration
echo "Configuration:"
echo "  Data Directory: $VECCY_DATA_DIR"
echo "  Log Directory: $VECCY_LOG_DIR"
echo "  Health Port: $VECCY_HEALTH_PORT"
echo "  Metrics Port: $VECCY_METRICS_PORT"
echo "  Java Options: $JAVA_OPTS"
echo ""

# Ensure directories exist and are writable
mkdir -p "$VECCY_DATA_DIR" "$VECCY_LOG_DIR"

# Function to handle different commands
case "$1" in
    server)
        echo "Starting Veccy server..."
        # Fat JAR has Main-Class set to VeccyCLI, but we can run any class
        exec java $JAVA_OPTS -cp /app/app.jar com.veccy.VeccyApplication
        ;;

    cli)
        echo "Starting Veccy CLI..."
        shift
        # Fat JAR's Main-Class is VeccyCLI, so we can use -jar
        exec java $JAVA_OPTS -jar /app/app.jar "$@"
        ;;

    health-check)
        echo "Running health check..."
        curl -f "http://localhost:${VECCY_HEALTH_PORT}/health" || exit 1
        ;;

    version)
        echo "Veccy Version:"
        java -jar /app/app.jar --version
        ;;

    shell)
        echo "Starting interactive shell..."
        exec /bin/bash
        ;;

    *)
        echo "Unknown command: $1"
        echo ""
        echo "Available commands:"
        echo "  server        - Start Veccy server (default)"
        echo "  cli [args]    - Run Veccy CLI with arguments"
        echo "  health-check  - Check application health"
        echo "  version       - Display version information"
        echo "  shell         - Start interactive shell"
        echo ""
        echo "Usage: docker run veccy [command] [args]"
        exit 1
        ;;
esac

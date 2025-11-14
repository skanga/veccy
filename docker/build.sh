#!/bin/bash
# Docker build script for Veccy

set -e

# Configuration
IMAGE_NAME="${IMAGE_NAME:-veccy}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
REGISTRY="${REGISTRY:-}"
PUSH="${PUSH:-false}"
PLATFORM="${PLATFORM:-linux/amd64}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--tag)
            IMAGE_TAG="$2"
            shift 2
            ;;
        -r|--registry)
            REGISTRY="$2"
            shift 2
            ;;
        -p|--push)
            PUSH=true
            shift
            ;;
        --platform)
            PLATFORM="$2"
            shift 2
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  -t, --tag TAG        Image tag (default: latest)"
            echo "  -r, --registry REG   Registry to push to (e.g., docker.io/username)"
            echo "  -p, --push           Push image to registry after build"
            echo "  --platform PLATFORM  Target platform (default: linux/amd64)"
            echo "  -h, --help           Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0 -t v1.0.0"
            echo "  $0 -t v1.0.0 -r myregistry.com/veccy -p"
            echo "  $0 --platform linux/amd64,linux/arm64"
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Build full image name
if [ -n "$REGISTRY" ]; then
    FULL_IMAGE_NAME="${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
else
    FULL_IMAGE_NAME="${IMAGE_NAME}:${IMAGE_TAG}"
fi

log_info "Building Docker image..."
log_info "Image: ${FULL_IMAGE_NAME}"
log_info "Platform: ${PLATFORM}"

# Check if buildx is available for multi-platform builds
if [[ "$PLATFORM" == *","* ]]; then
    if ! docker buildx version &> /dev/null; then
        log_error "docker buildx is required for multi-platform builds"
        exit 1
    fi

    log_info "Using buildx for multi-platform build"

    # Create builder if it doesn't exist
    if ! docker buildx ls | grep -q "veccy-builder"; then
        log_info "Creating buildx builder..."
        docker buildx create --name veccy-builder --use
    else
        docker buildx use veccy-builder
    fi

    # Build and optionally push
    if [ "$PUSH" = true ]; then
        log_info "Building and pushing multi-platform image..."
        docker buildx build \
            --platform "$PLATFORM" \
            --tag "$FULL_IMAGE_NAME" \
            --push \
            .
    else
        log_warn "Multi-platform builds require --push flag"
        log_info "Building and pushing to local registry..."
        docker buildx build \
            --platform "$PLATFORM" \
            --tag "$FULL_IMAGE_NAME" \
            --load \
            .
    fi
else
    # Standard single-platform build
    log_info "Building image..."
    docker build \
        --platform "$PLATFORM" \
        --tag "$FULL_IMAGE_NAME" \
        --tag "${IMAGE_NAME}:latest" \
        .

    if [ "$PUSH" = true ]; then
        log_info "Pushing image to registry..."
        docker push "$FULL_IMAGE_NAME"

        if [ "$IMAGE_TAG" != "latest" ]; then
            log_info "Also pushing as latest..."
            docker tag "$FULL_IMAGE_NAME" "${REGISTRY:+${REGISTRY}/}${IMAGE_NAME}:latest"
            docker push "${REGISTRY:+${REGISTRY}/}${IMAGE_NAME}:latest"
        fi
    fi
fi

log_info "Build complete!"
log_info ""
log_info "To run the image:"
log_info "  docker run -d -p 8080:8080 -p 8081:8081 ${FULL_IMAGE_NAME}"
log_info ""
log_info "To test:"
log_info "  curl http://localhost:8080/health"

#!/bin/bash

set -e

REGISTRY="mahdikheirkhah"
IMAGE_TAG="${1:-latest}"

echo "🐳 Building and pushing Docker images (tag: $IMAGE_TAG)..."

# Function to build and push service using docker-compose
build_and_push_service() {
    local service=$1
    
    echo ""
    echo "📦 Building $service image..."
    docker compose build $service
    echo "✅ $service image built"
    
    echo "🚀 Pushing $service to Docker Hub..."
    docker compose push $service
    echo "✅ $service pushed"
}

# Build and push all backend services
echo ""
echo "=== Building Backend Services ==="
build_and_push_service "discovery-service"
build_and_push_service "api-gateway"
build_and_push_service "user-service"
build_and_push_service "product-service"
build_and_push_service "media-service"
build_and_push_service "dummy-data"

# Build and push frontend
echo ""
echo "=== Building Frontend ==="
build_and_push_service "frontend"

echo ""
echo "✅ All images built and pushed successfully to Docker Hub!"
echo ""
echo "To run containers locally:"
echo "  docker compose up -d"
echo ""
echo "To view logs:"
echo "  docker compose logs -f"
echo ""
echo "To stop containers:"
echo "  docker compose down"

#!/bin/bash

set -e

REGISTRY="mahdikheirkhah"

echo "🐳 Building and pushing Docker images..."

# Function to build and push service
build_and_push() {
    local service=$1
    local port=${2:-8080}
    
    echo ""
    echo "📦 Building $service image..."
    
    # Create Dockerfile for the service
    cat > /tmp/Dockerfile.$service << EOF
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY backend/$service/target/$service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE $port
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF
    
    # Build image
    docker build -f /tmp/Dockerfile.$service -t $REGISTRY/$service:latest .
    echo "✅ $service image built"
    
    # Push image
    echo "🚀 Pushing $service to Docker Hub..."
    docker push $REGISTRY/$service:latest
    echo "✅ $service pushed"
}

# Build and push all backend services
build_and_push "discovery-service"
build_and_push "api-gateway" "8443"
build_and_push "user-service"
build_and_push "product-service"
build_and_push "media-service"
build_and_push "dummy-data"

# Build and push frontend
echo ""
echo "📦 Building frontend image..."
docker build -t $REGISTRY/frontend:latest frontend/
echo "✅ frontend image built"
echo "🚀 Pushing frontend to Docker Hub..."
docker push $REGISTRY/frontend:latest
echo "✅ frontend pushed"

echo ""
echo "✅ All images built and pushed successfully!"

#!/bin/bash

# Build all Docker images locally

set -e

echo "🐳 Building all Docker images..."
echo ""

# Build parent pom first so dependencies are available
echo "📦 Building parent pom..."
cd backend
mvn -q clean install -DskipTests
cd ..
echo "✅ Parent pom built"
echo ""

echo "📦 Building discovery-service..."
docker build -f backend/discovery-service/Dockerfile -t mahdikheirkhah/discovery-service:latest backend/
echo "✅ discovery-service built"
echo ""

echo "📦 Building api-gateway..."
docker build -f backend/api-gateway/Dockerfile -t mahdikheirkhah/api-gateway:latest backend/
echo "✅ api-gateway built"
echo ""

echo "📦 Building user-service..."
docker build -f backend/user-service/Dockerfile -t mahdikheirkhah/user-service:latest backend/
echo "✅ user-service built"
echo ""

echo "📦 Building product-service..."
docker build -f backend/product-service/Dockerfile -t mahdikheirkhah/product-service:latest backend/
echo "✅ product-service built"
echo ""

echo "📦 Building media-service..."
docker build -f backend/media-service/Dockerfile -t mahdikheirkhah/media-service:latest backend/
echo "✅ media-service built"
echo ""

echo "📦 Building orders-service..."
docker build -f backend/orders-service/Dockerfile -t mahdikheirkhah/orders-service:latest backend/
echo "✅ orders-service built"
echo ""

echo "📦 Building dummy-data..."
docker build -f backend/dummy-data/Dockerfile -t mahdikheirkhah/dummy-data:latest backend/
echo "✅ dummy-data built"
echo ""

echo "📦 Building frontend..."
docker build -t mahdikheirkhah/frontend:latest frontend/
echo "✅ frontend built"
echo ""

echo "========================================="
echo "✅ All images built successfully!"
echo "========================================="
echo ""
echo "📊 Images created:"
docker images | grep mahdikheirkhah
echo ""
echo "🚀 Next steps:"
echo "1. Push to Docker Hub: ./push-all-images.sh"
echo "2. Or run with dev compose: docker-compose -f docker-compose.dev.yml up -d --build"

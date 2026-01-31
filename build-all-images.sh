#!/bin/bash

# Build all Docker images locally

set -e

echo "🐳 Building all Docker images..."
echo ""

cd backend

echo "📦 Building discovery-service..."
docker build -t mahdikheirkhah/discovery-service:latest discovery-service/
echo "✅ discovery-service built"
echo ""

echo "📦 Building api-gateway..."
docker build -t mahdikheirkhah/api-gateway:latest api-gateway/
echo "✅ api-gateway built"
echo ""

echo "📦 Building user-service..."
docker build -t mahdikheirkhah/user-service:latest user-service/
echo "✅ user-service built"
echo ""

echo "📦 Building product-service..."
docker build -t mahdikheirkhah/product-service:latest product-service/
echo "✅ product-service built"
echo ""

echo "📦 Building media-service..."
docker build -t mahdikheirkhah/media-service:latest media-service/
echo "✅ media-service built"
echo ""

echo "📦 Building orders-service..."
docker build -t mahdikheirkhah/orders-service:latest orders-service/
echo "✅ orders-service built"
echo ""

echo "📦 Building dummy-data..."
docker build -t mahdikheirkhah/dummy-data:latest dummy-data/
echo "✅ dummy-data built"
echo ""

cd ..

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

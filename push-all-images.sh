#!/bin/bash

# Push all Docker images to Docker Hub

set -e

echo "========================================="
echo "📤 Pushing all images to Docker Hub..."
echo "========================================="
echo ""

echo "⏳ Pushing discovery-service..."
docker push mahdikheirkhah/discovery-service:latest
echo "✅ discovery-service pushed"
echo ""

echo "⏳ Pushing api-gateway..."
docker push mahdikheirkhah/api-gateway:latest
echo "✅ api-gateway pushed"
echo ""

echo "⏳ Pushing user-service..."
docker push mahdikheirkhah/user-service:latest
echo "✅ user-service pushed"
echo ""

echo "⏳ Pushing product-service..."
docker push mahdikheirkhah/product-service:latest
echo "✅ product-service pushed"
echo ""

echo "⏳ Pushing media-service..."
docker push mahdikheirkhah/media-service:latest
echo "✅ media-service pushed"
echo ""

echo "⏳ Pushing orders-service..."
docker push mahdikheirkhah/orders-service:latest
echo "✅ orders-service pushed"
echo ""

echo "⏳ Pushing dummy-data..."
docker push mahdikheirkhah/dummy-data:latest
echo "✅ dummy-data pushed"
echo ""

echo "⏳ Pushing frontend..."
docker push mahdikheirkhah/frontend:latest
echo "✅ frontend pushed"
echo ""

echo "========================================="
echo "✅ All images pushed to Docker Hub!"
echo "========================================="
echo ""
echo "🌐 View your repositories:"
echo "https://hub.docker.com/r/mahdikheirkhah"

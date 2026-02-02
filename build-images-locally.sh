#!/bin/bash

# Build Docker images locally from pre-built JAR files
set -e

WORKSPACE="/Users/mohammad.kheirkhah/Desktop/safe-zone"
REGISTRY="mahdikheirkhah"
TAG="latest"

echo "Building Docker images locally from pre-built JARs..."

# Build Discovery Service
echo "Building discovery-service..."
docker build \
  --build-arg JAR_FILE=backend/discovery-service/target/discovery-service-0.0.1-SNAPSHOT.jar \
  -f - \
  -t ${REGISTRY}/discovery-service:${TAG} \
  ${WORKSPACE} << 'EOF'
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# Build API Gateway
echo "Building api-gateway..."
docker build \
  --build-arg JAR_FILE=backend/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar \
  -f - \
  -t ${REGISTRY}/api-gateway:${TAG} \
  ${WORKSPACE} << 'EOF'
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# Build User Service
echo "Building user-service..."
docker build \
  --build-arg JAR_FILE=backend/user-service/target/user-service-0.0.1-SNAPSHOT.jar \
  -f - \
  -t ${REGISTRY}/user-service:${TAG} \
  ${WORKSPACE} << 'EOF'
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# Build Product Service
echo "Building product-service..."
docker build \
  --build-arg JAR_FILE=backend/product-service/target/product-service-0.0.1-SNAPSHOT.jar \
  -f - \
  -t ${REGISTRY}/product-service:${TAG} \
  ${WORKSPACE} << 'EOF'
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# Build Media Service
echo "Building media-service..."
docker build \
  --build-arg JAR_FILE=backend/media-service/target/media-service-0.0.1-SNAPSHOT.jar \
  -f - \
  -t ${REGISTRY}/media-service:${TAG} \
  ${WORKSPACE} << 'EOF'
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# Build Dummy Data Service
echo "Building dummy-data..."
docker build \
  --build-arg JAR_FILE=backend/dummy-data/target/dummy-data-0.0.1-SNAPSHOT.jar \
  -f - \
  -t ${REGISTRY}/dummy-data:${TAG} \
  ${WORKSPACE} << 'EOF'
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

# Build Frontend
echo "Building frontend..."
docker build \
  -f frontend/Dockerfile \
  -t ${REGISTRY}/frontend:${TAG} \
  ${WORKSPACE}/frontend

echo ""
echo "✓ All images built successfully!"
echo ""
echo "Built images:"
docker images | grep ${REGISTRY} | grep -E "discovery-service|api-gateway|user-service|product-service|media-service|dummy-data|frontend"

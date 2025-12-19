#!/bin/bash
set -e

# --- Configuration ---
# Your Docker repository prefix (Docker Hub username/org)
DOCKER_REPO_PREFIX="mahdikheirkhah"

# Fallback tag if IMAGE_TAG is not set.
# CRITICAL FIX: The 'export' command makes this variable available to 'docker compose'
export IMAGE_TAG=${IMAGE_TAG:-latest-prod}
# 1. Your Docker Hub Username
export DOCKER_USERNAME="mahdikheirkhah"

# 3. Registry URL:
# If you are using Docker Hub, you can omit this, as the script defaults to 'docker.io'.
# If you are using a different service (like GitHub's registry), you would set it to that URL.
# Since you likely use Docker Hub, we can skip setting DOCKER_REGISTRY,
# or explicitly set it like this (optional):
export DOCKER_REGISTRY="docker.io"

#export IMAGE_TAG="v1.0.0"
SKIP_BUILD="true"
DELETE_REMOTE_TAG="false"


echo "Starting build and push process for tag: $IMAGE_TAG"

# --- Authentication ---
echo "Attempting to log in to Docker Hub (docker.io)..."
# Requires DOCKER_USERNAME and DOCKER_PASSWORD (Docker Hub credentials)
if echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin > /dev/null 2>&1; then
    echo "Login Succeeded."
else
    echo "Deployment FAILED: Docker login failed for Docker Hub. Check DOCKER_USERNAME and DOCKER_PASSWORD."
    exit 1
fi

cleanup_remote_tag() {
  local image="$1"
  if [ "$DELETE_REMOTE_TAG" = "true" ]; then
    echo "Deleting remote tag $image before re-pushing (if it exists)..."
    if command -v oras >/dev/null 2>&1; then
      oras delete -r $image || echo "Warning: could not delete $image (may not exist or insufficient permission)."
    else
      echo "oras CLI not available; skipping remote delete for $image."
    fi
  fi
}

# --- 1. Build and Tag Images ---
echo "--- 1. Building custom images locally with tag: $IMAGE_TAG ---"

if [ "$SKIP_BUILD" != "true" ]; then
  services=("discovery-service" "api-gateway" "user-service" "product-service" "media-service" "dummy-data")

  for service in "${services[@]}"; do
      echo "Building backend image for $service"
      docker build \
        -t ${DOCKER_REPO_PREFIX}/${service}:${IMAGE_TAG} \
        -f Dockerfile.java \
        --build-arg SERVICE_NAME=${service} .
      if [ $? -ne 0 ]; then
          echo "Deployment FAILED: Docker build failed for ${service}" && exit 1
      fi
      cleanup_remote_tag ${DOCKER_REPO_PREFIX}/${service}:${IMAGE_TAG}
      echo "Pushing ${service}"
      docker push ${DOCKER_REPO_PREFIX}/${service}:${IMAGE_TAG}
      if [ $? -ne 0 ]; then
          echo "Deployment FAILED: Docker push failed for ${service}" && exit 1
      fi
      echo "--- ${service} image pushed ---"
    done

  echo "Building frontend image"
  docker build -t ${DOCKER_REPO_PREFIX}/frontend:${IMAGE_TAG} -f frontend/Dockerfile frontend/
  if [ $? -ne 0 ]; then
    echo "Deployment FAILED: Docker build failed for frontend" && exit 1
  fi
  cleanup_remote_tag ${DOCKER_REPO_PREFIX}/frontend:${IMAGE_TAG}
  echo "Pushing frontend image"
  docker push ${DOCKER_REPO_PREFIX}/frontend:${IMAGE_TAG}
  if [ $? -ne 0 ]; then
    echo "Deployment FAILED: Docker push failed for frontend" && exit 1
  fi

  echo "--- Images pushed successfully ---"
else
  echo "SKIP_BUILD=true -> Skipping local build/push stage. Assuming images already exist in Docker Hub."
fi

# --- 3. Deployment (Pull and Run) ---
echo "--- 3. Deploying updated services from Docker Hub ---"

NETWORK_NAME="FKML_secret_network"
if docker network ls | grep -q "$NETWORK_NAME"; then
    echo "Docker network '$NETWORK_NAME' already exists."
else
    echo "Creating Docker network '$NETWORK_NAME'..."
    docker network create "$NETWORK_NAME"
fi

# Pull the latest images from Docker Hub
echo "Pulling latest images..."
if ! docker compose pull; then
    echo "Deployment FAILED: Failed to pull images from Docker Hub. Ensure the repository name is correct."
    exit 1
fi

# Start the services
echo "Starting microservices using docker-compose.yml..."
if ! docker compose up -d; then
    echo "Deployment FAILED."
    exit 1
fi

echo "Deployment completed successfully. Services are running in detached mode."
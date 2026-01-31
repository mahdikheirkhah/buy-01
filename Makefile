# This Makefile is placed in the project root directory.

# --- Configuration Variables ---

# List of all Java services, defined by their path relative to the project root.
# Each service has its own Dockerfile in its directory.
# NOTE: 'backend/auth-service' has been removed as authentication logic is now in 'user-service'.
# NOTE: Ensure these folders exist: backend/api-gateway, backend/user-service, etc.
JAVA_SERVICE_PATHS = backend/api-gateway backend/user-service backend/product-service backend/media-service backend/dummy-data backend/discovery-service

FRONTEND_DIR = frontend

# --- Primary Targets ---

# Default target: Run 'make build' then 'make up'.
all: build up

# Build all Docker images (Java services and Frontend).
build: build-java build-frontend

# Start all services defined in docker-compose.yml in detached mode (-d).
up:
	@echo "--- Starting Docker Compose Services ---"
	docker-compose up -d

# Stop and remove all containers, networks, and volumes defined in docker-compose.yml.
down:
	@echo "--- Stopping and Removing Containers ---"
	docker-compose down

# --- Build Sub-Targets ---

# Build Java Microservice images.
# Each service has its own Dockerfile in its service directory.
build-java:
	@echo "--- Building Java Microservices ---"
	@for service_path in $(JAVA_SERVICE_PATHS); do \
		service_name=$$(basename $$service_path); \
		echo "Building $$service_name..."; \
		docker build --file $$service_path/Dockerfile \
		--tag mahdikheirkhah/$$service_name:latest \
		--tag mahdikheirkhah/$$service_name:$${IMAGE_TAG:-latest} \
		$$service_path ; \
	done

# Build Angular Frontend image.
build-frontend:
	@echo "--- Building Angular Frontend ---"
	docker build --file $(FRONTEND_DIR)/Dockerfile \
		--tag mahdikheirkhah/frontend:latest \
		--tag mahdikheirkhah/frontend:$${IMAGE_TAG:-latest} \
		$(FRONTEND_DIR)

# --- Clean Target ---

# Clean up local Maven artifacts and remove custom Docker images.
clean: down
	@echo "--- Cleaning Maven build artifacts ---"
	mvn clean
	@echo "--- Pruning custom backend images ---"
	docker rmi -f $$(docker images -q --filter reference='mahdikheirkhah/*') 2> /dev/null || true

# Define Phony targets to ensure make runs the commands even if files with these names exist.
.PHONY: all build build-java build-frontend up down clean
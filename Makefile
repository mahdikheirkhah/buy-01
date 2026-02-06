# This Makefile is placed in the project root directory.

# --- Configuration Variables ---

JAVA_SERVICE_PATHS = backend/api-gateway backend/user-service backend/product-service backend/media-service backend/orders-service backend/dummy-data backend/discovery-service

FRONTEND_DIR = frontend

# --- Primary Targets ---

all: build up

build: build-java build-frontend

up:
	@echo "--- Starting Docker Compose Services ---"
	docker-compose up -d

# Build locally and start services (without pulling from Docker Hub)
local: build-java
	@echo "--- Starting Docker Compose Services (Local Build) ---"
	docker-compose build
	docker-compose up -d

# Build locally and start backend services only (without frontend)
backend-local: build-java
	@echo "--- Starting Backend Services Only (Local Build) ---"
	docker-compose build --no-deps discovery-service kafka zookeeper buy-01 sonarqube api-gateway user-service product-service media-service dummy-data
	docker-compose up -d discovery-service kafka zookeeper buy-01 sonarqube api-gateway user-service product-service media-service dummy-data

# Stop and remove all containers, networks, and volumes defined in docker-compose.yml.
down:
	@echo "--- Stopping and Removing Containers ---"
	docker-compose down

# --- Build Sub-Targets ---

build-java:
	@echo "--- Building Java Microservices ---"
	@for service_path in $(JAVA_SERVICE_PATHS); do \
		service_name=$$(basename $$service_path); \
		echo "Building $$service_name..."; \
		docker build --file $$service_path/Dockerfile \
		--tag mahdikheirkhah/$$service_name:latest \
		--tag mahdikheirkhah/$$service_name:$${IMAGE_TAG:-latest} \
		. ; \
	done

build-frontend:
	@echo "--- Building Angular Frontend ---"
	docker build --file $(FRONTEND_DIR)/Dockerfile \
		--tag mahdikheirkhah/frontend:latest \
		--tag mahdikheirkhah/frontend:$${IMAGE_TAG:-latest} \
		$(FRONTEND_DIR)

# --- Clean Target ---

clean: down
	@echo "--- Cleaning Maven build artifacts ---"
	mvn clean
	@echo "--- Pruning custom backend images ---"
	docker rmi -f $$(docker images -q --filter reference='mahdikheirkhah/*') 2> /dev/null || true

# Define Phony targets to ensure make runs the commands even if files with these names exist.
.PHONY: all build build-java build-frontend up down clean local backend-local
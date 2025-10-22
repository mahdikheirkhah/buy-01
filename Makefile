# Makefile for running the full-stack application on macOS

# --- Backend Configuration ---
BACKEND_DIR = ./backend
MAVEN_CMD = mvn

# --- Frontend Configuration ---
FRONTEND_DIR = ./frontend
NPM_CMD = npm

# --- Docker Configuration ---
DOCKER_COMPOSE_CMD = docker-compose

# ==============================================================================
# Main Targets
# ==============================================================================

all: docker-up clean-backend run-backend run-frontend

all-docker: docker-up clean-backend run-backend run-frontend

clean-backend:
	@echo "--- Cleaning backend projects... ---"
	$(MAVEN_CMD) -f $(BACKEND_DIR)/pom.xml clean

run-backend:
	@echo "--- Building all backend microservices... ---"
	$(MAVEN_CMD) -f $(BACKEND_DIR)/pom.xml clean install -DskipTests
	@echo "--- Starting all backend microservices... ---"
	@osascript -e 'tell application "Terminal" to do script "cd $(CURDIR)/$(BACKEND_DIR)/discovery-service && $(MAVEN_CMD) spring-boot:run"'
	@echo "Waiting 15 seconds for Discovery Service to start..."
	@sleep 15
	@osascript -e 'tell application "Terminal" to do script "cd $(CURDIR)/$(BACKEND_DIR)/api-gateway && $(MAVEN_CMD) spring-boot:run"'
	@osascript -e 'tell application "Terminal" to do script "cd $(CURDIR)/$(BACKEND_DIR)/user-service && $(MAVEN_CMD) spring-boot:run"'
	@osascript -e 'tell application "Terminal" to do script "cd $(CURDIR)/$(BACKEND_DIR)/product-service && $(MAVEN_CMD) spring-boot:run"'
	@osascript -e 'tell application "Terminal" to do script "cd $(CURDIR)/$(BACKEND_DIR)/media-service && $(MAVEN_CMD) spring-boot:run"'
	@echo "--- Backend services are starting in new Terminal windows. ---"

run-frontend:
	@echo "--- Starting frontend Angular application... ---"
	@osascript -e 'tell application "Terminal" to do script "cd $(CURDIR)/$(FRONTEND_DIR) && $(NPM_CMD) install && $(NPM_CMD) start"'

docker-up:
	@echo "--- Starting Docker Compose services (Kafka, Zookeeper)... ---"
	$(DOCKER_COMPOSE_CMD) up -d
	@echo "--- Docker services started successfully. ---"

docker-down:
	@echo "--- Stopping Docker Compose services... ---"
	$(DOCKER_COMPOSE_CMD) down
	@echo "--- Docker services stopped. ---"

docker-logs:
	@echo "--- Showing Docker Compose logs... ---"
	$(DOCKER_COMPOSE_CMD) logs -f

docker-status:
	@echo "--- Docker Compose services status... ---"
	$(DOCKER_COMPOSE_CMD) ps

stop:
	@echo "--- Stopping all microservices and frontend by port... ---"
	@kill -9 $$(lsof -ti:8761) || echo "Discovery Service was not running."
	@kill -9 $$(lsof -ti:8080) || echo "API Gateway was not running."
	@kill -9 $$(lsof -ti:8444) || echo "User Service was not running."
	@kill -9 $$(lsof -ti:8082) || echo "Product Service was not running."
	@kill -9 $$(lsof -ti:8083) || echo "Media Service was not running."
	@kill -9 $$(lsof -ti:4200) || echo "Frontend was not running."
	@echo "--- All services stopped. ---"

stop-all: stop docker-down
	@echo "--- All services and Docker containers stopped. ---"

.PHONY: all all-docker clean-backend run-backend run-frontend docker-up docker-down docker-logs docker-status stop stop-all
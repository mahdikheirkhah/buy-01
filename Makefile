# Makefile for running the full-stack application on macOS

# --- Backend Configuration ---
BACKEND_DIR = ./backend
MAVEN_CMD = mvn

# --- Frontend Configuration ---
FRONTEND_DIR = ./frontend
NPM_CMD = npm

# ==============================================================================
# Main Targets
# ==============================================================================

all: clean-backend run-backend run-frontend

clean-backend:
	@echo "--- Cleaning backend projects... ---"
	$(MAVEN_CMD) -f $(BACKEND_DIR)/pom.xml clean

run-backend:
	@echo "--- Building all backend microservices... ---"
	$(MAVEN_CMD) -f $(BACKEND_DIR)/pom.xml clean install -DskipTests
	@echo "--- Starting all backend microservices... ---"
	@osascript -e 'tell application "Terminal" to do script "cd $(CURDIR)/$(BACKEND_DIR)/discovery-service && $(MAVEN_CMD) spring-boot:run"'
	@sleep 10
	@osascript -e 'tell application "Terminal" to do script "cd $(CURDIR)/$(BACKEND_DIR)/api-gateway && $(MAVEN_CMD) spring-boot:run"'
	@osascript -e 'tell application "Terminal" to do script "cd $(CURDIR)/$(BACKEND_DIR)/user-service && $(MAVEN_CMD) spring-boot:run"'
	@osascript -e 'tell application "Terminal" to do script "cd $(CURDIR)/$(BACKEND_DIR)/product-service && $(MAVEN_CMD) spring-boot:run"'
	@osascript -e 'tell application "Terminal" to do script "cd $(CURDIR)/$(BACKEND_DIR)/media-service && $(MAVEN_CMD) spring-boot:run"'
	@echo "--- Backend services are starting in new Terminal windows. ---"

run-frontend:
	@echo "--- Starting frontend Angular application... ---"
	@osascript -e 'tell application "Terminal" to do script "cd $(CURDIR)/$(FRONTEND_DIR) && $(NPM_CMD) install && $(NPM_CMD) start"'

stop:
	@echo "--- Stopping all Java (backend) and Node (frontend) processes... ---"
	@killall -9 java || true
	@killall -9 node || true
	@echo "--- All services stopped. ---"

.PHONY: all clean-backend run-backend run-frontend stop
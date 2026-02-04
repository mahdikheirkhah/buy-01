#!/bin/bash

# Script to analyze individual microservices with SonarQube
# Each service becomes its own project in SonarQube

# Note: don't exit on error for individual service failures
SONAR_HOST="${SONAR_HOST:-http://localhost:9000}"
SONAR_USER="${SONAR_USER:-admin}"
SONAR_PASS="${SONAR_PASS:-admin}"
PROJECT_ROOT="/Users/mohammad.kheirkhah/Desktop/buy-01"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔍 SonarQube Microservice Analysis Started${NC}"
echo "SonarQube Host: $SONAR_HOST"
echo "-------------------------------------------"

# Services to analyze (excluding common and dummy-data)
SERVICES=(
    "api-gateway"
    "discovery-service"
    "user-service"
    "product-service"
    "media-service"
    "orders-service"
)

# Function to run SonarQube analysis for a service
analyze_service() {
    local SERVICE=$1
    local PROJECT_KEY="buy-01-${SERVICE}"
    
    # Map service names to display names (compatible with all shells)
    local PROJECT_NAME
    case "$SERVICE" in
        "api-gateway") PROJECT_NAME="API Gateway Service" ;;
        "discovery-service") PROJECT_NAME="Discovery Service" ;;
        "user-service") PROJECT_NAME="User Service" ;;
        "product-service") PROJECT_NAME="Product Service" ;;
        "media-service") PROJECT_NAME="Media Service" ;;
        "orders-service") PROJECT_NAME="Orders Service" ;;
        *) PROJECT_NAME="$SERVICE Service" ;;
    esac
    
    local SERVICE_PATH="$PROJECT_ROOT/backend/$SERVICE"

    echo -e "${YELLOW}📦 Analyzing: $SERVICE${NC}"
    
    # Check if service directory exists
    if [ ! -d "$SERVICE_PATH" ]; then
        echo -e "${YELLOW}⚠️  Service directory not found: $SERVICE_PATH${NC}"
        return 1
    fi

    # Build the service first (if not already built)
    echo "  Building $SERVICE..."
    cd "$SERVICE_PATH"
    mvn clean compile -q -DskipTests 2>/dev/null || {
        echo "  Note: Build had issues, continuing with analysis..."
    }

    # Run SonarQube analysis (make sonar.tests optional if dir doesn't exist)
    echo "  Running SonarQube scan..."
    
    local SONAR_ARGS=(
        "-Dsonar.projectKey=$PROJECT_KEY"
        "-Dsonar.projectName=$PROJECT_NAME"
        "-Dsonar.projectVersion=1.0.0"
        "-Dsonar.host.url=$SONAR_HOST"
        "-Dsonar.login=$SONAR_USER"
        "-Dsonar.password=$SONAR_PASS"
        "-Dsonar.sources=src/main"
        "-Dsonar.java.binaries=target/classes"
        "-q"
    )
    
    # Only add test source if directory exists
    if [ -d "src/test" ]; then
        SONAR_ARGS+=("-Dsonar.tests=src/test")
    fi
    
    mvn sonar:sonar "${SONAR_ARGS[@]}"

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ $SERVICE analysis complete${NC}"
    else
        echo -e "${YELLOW}⚠️  $SERVICE analysis had issues${NC}"
    fi
    
    echo "-------------------------------------------"
}

# Analyze each service
for SERVICE in "${SERVICES[@]}"; do
    analyze_service "$SERVICE"
done

echo -e "${GREEN}🎉 All microservice analyses completed!${NC}"
echo ""
echo "View results at: ${SONAR_HOST}/dashboard"
echo ""

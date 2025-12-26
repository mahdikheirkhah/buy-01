#!/bin/bash

# SonarQube Project Audit Verification Script
# This script verifies that all audit requirements are met

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}SonarQube Project Audit Verification${NC}"
echo -e "${BLUE}========================================${NC}\n"

# Initialize counters
PASSED=0
FAILED=0
WARNINGS=0

# Utility function to print status
check_status() {
    local name=$1
    local status=$2
    local details=$3
    
    if [ "$status" = "PASS" ]; then
        echo -e "${GREEN}✓ PASS${NC} - $name"
        ((PASSED++))
    elif [ "$status" = "FAIL" ]; then
        echo -e "${RED}✗ FAIL${NC} - $name"
        if [ ! -z "$details" ]; then
            echo -e "  ${RED}Details: $details${NC}"
        fi
        ((FAILED++))
    else
        echo -e "${YELLOW}⚠ WARN${NC} - $name"
        if [ ! -z "$details" ]; then
            echo -e "  ${YELLOW}Details: $details${NC}"
        fi
        ((WARNINGS++))
    fi
}

echo -e "${BLUE}FUNCTIONAL REQUIREMENTS${NC}\n"

# Check 1: SonarQube Web Interface
echo "1. Checking SonarQube Web Interface Accessibility..."
if curl -s http://localhost:9000 > /dev/null 2>&1; then
    check_status "SonarQube running on http://localhost:9000" "PASS"
else
    check_status "SonarQube running on http://localhost:9000" "FAIL" "Cannot connect to http://localhost:9000. Is it running? Try: docker compose up -d sonarqube"
fi

# Check 2: Docker Container
echo -e "\n2. Checking Docker Configuration..."
if docker ps | grep -q sonarqube; then
    check_status "SonarQube Docker container is running" "PASS"
else
    check_status "SonarQube Docker container is running" "FAIL" "Docker container not running"
fi

# Check 3: SonarQube System Status
echo -e "\n3. Checking SonarQube System Status..."
if curl -s http://localhost:9000/api/system/status | grep -q '"status":"UP"'; then
    check_status "SonarQube system status is UP" "PASS"
else
    check_status "SonarQube system status is UP" "FAIL" "System is not responding as UP"
fi

# Check 4: Jenkinsfile Configuration
echo -e "\n4. Checking Jenkinsfile Configuration..."
if grep -q "📊 SonarQube Analysis" Jenkinsfile; then
    check_status "SonarQube Analysis stage in Jenkinsfile" "PASS"
else
    check_status "SonarQube Analysis stage in Jenkinsfile" "FAIL" "SonarQube stage not found in Jenkinsfile"
fi

# Check 5: Docker Compose Configuration
echo -e "\n5. Checking Docker Compose Configuration..."
if grep -q "sonarqube:" docker-compose.yml; then
    check_status "SonarQube configured in docker-compose.yml" "PASS"
else
    check_status "SonarQube configured in docker-compose.yml" "FAIL" "SonarQube service not in docker-compose.yml"
fi

# Check 6: Pull Request Template
echo -e "\n6. Checking Pull Request Process..."
if [ -f ".github/pull_request_template.md" ]; then
    check_status "Pull request template exists" "PASS"
else
    check_status "Pull request template exists" "FAIL" "Template file not found at .github/pull_request_template.md"
fi

# Check 7: GitHub Actions Workflow
echo -e "\n7. Checking GitHub Actions Configuration..."
if [ -f ".github/workflows/build-test.yml" ] || [ -f ".github/workflows/sonarqube.yml" ]; then
    check_status "GitHub Actions workflow configured" "PASS"
else
    check_status "GitHub Actions workflow configured" "FAIL" "No workflow files found in .github/workflows/"
fi

echo -e "\n${BLUE}DOCUMENTATION & COMPREHENSION${NC}\n"

# Check 8: Audit Guide Documentation
echo "8. Checking Documentation..."
if [ -f "SONARQUBE_AUDIT_GUIDE.md" ]; then
    check_status "SonarQube Audit Guide documented" "PASS"
else
    check_status "SonarQube Audit Guide documented" "FAIL" "SONARQUBE_AUDIT_GUIDE.md not found"
fi

# Check 9: Quick Start Guide
if [ -f "SONARQUBE_SETUP_QUICK_START.md" ]; then
    check_status "Quick Start Guide provided" "PASS"
else
    check_status "Quick Start Guide provided" "FAIL" "SONARQUBE_SETUP_QUICK_START.md not found"
fi

echo -e "\n${BLUE}PROJECT STRUCTURE${NC}\n"

# Check 10: Backend Microservices
echo "10. Checking Backend Services..."
SERVICES=("common" "discovery-service" "api-gateway" "user-service" "product-service" "media-service")
for service in "${SERVICES[@]}"; do
    if [ -d "backend/$service" ]; then
        echo -e "  ${GREEN}✓${NC} $service"
    else
        echo -e "  ${RED}✗${NC} $service (missing)"
    fi
done

# Check 11: Frontend
echo -e "\n11. Checking Frontend..."
if [ -d "frontend" ]; then
    check_status "Frontend Angular project" "PASS"
else
    check_status "Frontend Angular project" "FAIL" "Frontend directory not found"
fi

echo -e "\n${BLUE}SECURITY & QUALITY${NC}\n"

# Check 12: .gitignore Configuration
echo "12. Checking .gitignore Configuration..."
if [ -f ".gitignore" ]; then
    if grep -q "node_modules" .gitignore && grep -q "target" .gitignore; then
        check_status ".gitignore properly configured" "PASS"
    else
        check_status ".gitignore properly configured" "WARN" "Missing some common exclusions"
    fi
else
    check_status ".gitignore exists" "FAIL" ".gitignore file not found"
fi

# Check 13: Environment Configuration
echo -e "\n13. Checking Environment Configuration..."
if [ -f ".env.example" ]; then
    check_status "Environment configuration template" "PASS"
else
    check_status "Environment configuration template" "WARN" ".env.example not found (recommended)"
fi

# Check 14: sonar-project.properties
echo -e "\n14. Checking SonarQube Project Properties..."
if [ -f "sonar-project.properties" ]; then
    check_status "Backend sonar-project.properties" "PASS"
else
    check_status "Backend sonar-project.properties" "WARN" "sonar-project.properties not found (optional but recommended)"
fi

echo -e "\n${BLUE}========================================${NC}"
echo -e "${BLUE}SUMMARY${NC}"
echo -e "${BLUE}========================================${NC}\n"

echo -e "${GREEN}Passed: $PASSED${NC}"
echo -e "${YELLOW}Warnings: $WARNINGS${NC}"
echo -e "${RED}Failed: $FAILED${NC}"

TOTAL=$((PASSED + WARNINGS + FAILED))
echo -e "\nTotal Checks: $TOTAL"

if [ $FAILED -eq 0 ]; then
    echo -e "\n${GREEN}✓ All critical checks passed!${NC}"
    if [ $WARNINGS -gt 0 ]; then
        echo -e "${YELLOW}⚠ $WARNINGS warnings found (review recommended)${NC}"
    fi
    exit 0
else
    echo -e "\n${RED}✗ $FAILED checks failed. Please review the issues above.${NC}"
    exit 1
fi

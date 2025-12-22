#!/bin/bash

# Jenkins CI/CD Diagnostic Script
# This script checks your Jenkins setup and identifies issues

echo "============================================"
echo "Jenkins CI/CD Diagnostic Tool"
echo "============================================"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print status
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✅ PASS${NC}: $2"
    else
        echo -e "${RED}❌ FAIL${NC}: $2"
    fi
}

print_warning() {
    echo -e "${YELLOW}⚠️  WARNING${NC}: $1"
}

echo "1. Checking Docker Installation..."
if command -v docker &> /dev/null; then
    print_status 0 "Docker is installed"
    docker --version
else
    print_status 1 "Docker is not installed"
fi
echo ""

echo "2. Checking Jenkins Container..."
if docker ps | grep -q jenkins-cicd; then
    print_status 0 "Jenkins container is running"
    docker ps | grep jenkins-cicd
else
    print_status 1 "Jenkins container is not running"
    echo "   Start Jenkins with: docker start jenkins-cicd"
fi
echo ""

echo "3. Checking Docker Access from Jenkins..."
if docker exec jenkins-cicd docker ps &> /dev/null; then
    print_status 0 "Jenkins can access Docker daemon"
else
    print_status 1 "Jenkins cannot access Docker daemon"
    echo "   This is a critical issue - Jenkins needs Docker access"
    echo "   Solution: See JENKINS_TROUBLESHOOTING.md - Solution 1"
fi
echo ""

echo "4. Checking Docker Compose in Jenkins..."
if docker exec jenkins-cicd docker compose version &> /dev/null; then
    print_status 0 "Docker Compose is available in Jenkins"
    docker exec jenkins-cicd docker compose version
else
    print_status 1 "Docker Compose is not available in Jenkins"
    echo "   Solution: See JENKINS_TROUBLESHOOTING.md - Solution 4"
fi
echo ""

echo "5. Checking Jenkins Accessibility..."
if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080 | grep -q "200\|403"; then
    print_status 0 "Jenkins is accessible on port 8080"
else
    print_status 1 "Jenkins is not accessible on port 8080"
    echo "   Check if Jenkins is running and port is not blocked"
fi
echo ""

echo "6. Checking Docker Hub Images..."
echo "   Checking if your Docker Hub images exist..."
for service in discovery-service api-gateway user-service product-service media-service dummy-data frontend; do
    if curl -s "https://hub.docker.com/v2/repositories/mahdikheirkhah/$service/tags" | grep -q "stable"; then
        echo "   ✅ mahdikheirkhah/$service:stable exists"
    else
        echo "   ⚠️  mahdikheirkhah/$service:stable not found (may not be published yet)"
    fi
done
echo ""

echo "7. Checking MongoDB Container..."
if docker ps | grep -q buy-01; then
    print_status 0 "MongoDB container is running"
else
    print_status 1 "MongoDB container is not running"
    echo "   Start with: docker compose up -d buy-01"
fi
echo ""

echo "8. Checking Kafka Container..."
if docker ps | grep -q kafka; then
    print_status 0 "Kafka container is running"
else
    print_status 1 "Kafka container is not running"
    echo "   Start with: docker compose up -d kafka"
fi
echo ""

echo "9. Checking Jenkins Workspace..."
if docker exec jenkins-cicd test -d /var/jenkins_home/workspace/e-commerce-microservices-ci-cd &> /dev/null; then
    print_status 0 "Jenkins workspace exists"
else
    print_warning "Jenkins workspace doesn't exist yet (will be created on first build)"
fi
echo ""

echo "10. Checking Jenkins Logs for Errors..."
echo "    Last 10 lines of Jenkins logs:"
docker logs jenkins-cicd --tail 10 2>&1 | sed 's/^/    /'
echo ""

echo "============================================"
echo "Diagnostic Summary"
echo "============================================"

# Count issues
ISSUES=0

# Docker
command -v docker &> /dev/null || ((ISSUES++))

# Jenkins running
docker ps | grep -q jenkins-cicd || ((ISSUES++))

# Docker access
docker exec jenkins-cicd docker ps &> /dev/null || ((ISSUES++))

# Docker Compose
docker exec jenkins-cicd docker compose version &> /dev/null || ((ISSUES++))

# Jenkins accessible
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080 | grep -q "200\|403" || ((ISSUES++))

echo ""
if [ $ISSUES -eq 0 ]; then
    echo -e "${GREEN}✅ All critical checks passed!${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Access Jenkins at http://localhost:8080"
    echo "2. Create/configure your pipeline job"
    echo "3. Run a test build"
else
    echo -e "${RED}❌ Found $ISSUES critical issue(s)${NC}"
    echo ""
    echo "Priority fixes needed:"
    echo "1. See JENKINS_TROUBLESHOOTING.md for detailed solutions"
    echo "2. Fix Docker/Jenkins connectivity issues first"
    echo "3. Install Docker Compose in Jenkins container"
    echo "4. Then test the pipeline"
fi

echo ""
echo "============================================"
echo "Useful Commands"
echo "============================================"
echo ""
echo "# View Jenkins logs:"
echo "docker logs jenkins-cicd -f"
echo ""
echo "# Restart Jenkins:"
echo "docker restart jenkins-cicd"
echo ""
echo "# Access Jenkins container:"
echo "docker exec -it jenkins-cicd bash"
echo ""
echo "# Check Jenkins workspace:"
echo "docker exec jenkins-cicd ls -la /var/jenkins_home/workspace/"
echo ""
echo "# Manual deployment:"
echo "export IMAGE_TAG=stable"
echo "docker compose up -d"
echo ""
echo "============================================"
echo "For detailed troubleshooting, see:"
echo "- JENKINS_TROUBLESHOOTING.md"
echo "- TODO.md (check 'In Progress' section)"
echo "- README.md (Quick Start guide)"
echo "============================================"


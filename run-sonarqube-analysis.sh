#!/bin/bash
# SonarQube Analysis Script
# This script runs SonarQube analysis for both backend and frontend projects

set -e

# Configuration
SONAR_HOST="http://localhost:9000"
SONAR_TOKEN="${SONAR_TOKEN:-sqa_9704c7b566a8a230d9ca0b6955e5b635528648f0}"

echo "🔍 Starting SonarQube Analysis..."
echo "=========================================="

# Backend Analysis
echo ""
echo "📊 Running Backend Analysis..."
cd "$(dirname "$0")/backend"
mvn sonar:sonar \
  -Dsonar.projectKey=buy-01-backend \
  -Dsonar.projectName="buy-01 Backend" \
  -Dsonar.host.url="${SONAR_HOST}" \
  -Dsonar.login="${SONAR_TOKEN}" \
  -Dsonar.sources=. \
  -Dsonar.exclusions="**/target/**,**/test/**,**/*Test.java,**/*Tests.java" \
  -Dsonar.java.binaries=*/target/classes \
  -Dsonar.coverage.exclusions="**/dto/**,**/config/**,**/entity/**,**/model/**" \
  -B

echo "✅ Backend analysis completed"

# Frontend Analysis
echo ""
echo "📊 Running Frontend Analysis..."
cd "$(dirname "$0")/frontend"

# Install sonar-scanner if not available
which sonar-scanner >/dev/null 2>&1 || {
  echo "📦 Installing sonar-scanner..."
  npm install -g sonar-scanner
}

sonar-scanner \
  -Dsonar.projectKey=buy-01-frontend \
  -Dsonar.projectName="buy-01 Frontend" \
  -Dsonar.host.url="${SONAR_HOST}" \
  -Dsonar.login="${SONAR_TOKEN}" \
  -Dsonar.sources=src \
  -Dsonar.exclusions="node_modules/**,dist/**,coverage/**,**/*.spec.ts" \
  -Dsonar.javascript.lcov.reportPaths=coverage/lcov.info

echo "✅ Frontend analysis completed"

echo ""
echo "=========================================="
echo "✅ All SonarQube analyses completed!"
echo ""
echo "📊 View results at:"
echo "  - Backend:  ${SONAR_HOST}/dashboard?id=buy-01-backend"
echo "  - Frontend: ${SONAR_HOST}/dashboard?id=buy-01-frontend"

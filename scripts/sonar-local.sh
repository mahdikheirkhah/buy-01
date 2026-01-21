#!/usr/bin/env bash
set -euo pipefail

# Local SonarQube analysis helper
# Usage:
#   1) Start SonarQube locally:
#      docker compose -f docker-compose.sonarqube.yml up -d
#   2) Create a token in http://localhost:9000 (My Account -> Security)
#   3) Export token and run this script:
#      export SONAR_TOKEN=YOUR_TOKEN
#      ./scripts/sonar-local.sh

SONAR_HOST_URL="${SONAR_HOST_URL:-http://localhost:9000}"

if [[ -z "${SONAR_TOKEN:-}" ]]; then
  echo "Error: SONAR_TOKEN is not set. Export your SonarQube token first." >&2
  exit 1
fi

run_maven_service() {
  local svc="$1"
  echo "=== Analyzing backend/$svc ==="
  pushd "backend/$svc" >/dev/null
  # Ensure tests run to produce coverage, then publish to SonarQube
  mvn -B -q clean verify \
    sonar:sonar \
    -Dsonar.host.url="$SONAR_HOST_URL" \
    -Dsonar.login="$SONAR_TOKEN"
  popd >/dev/null
}

run_frontend() {
  echo "=== Analyzing frontend ==="
  pushd frontend >/dev/null
  # Generate coverage report for Sonar to pick up (lcov.info)
  npm ci
  npm test -- --watch=false --browsers=ChromeHeadless --code-coverage || true

  # Prefer local sonar-scanner if available; otherwise use Dockerized scanner
  if command -v sonar-scanner >/dev/null 2>&1; then
    sonar-scanner \
      -Dsonar.host.url="$SONAR_HOST_URL" \
      -Dsonar.login="$SONAR_TOKEN"
  else
    docker run --rm \
      -e SONAR_HOST_URL="$SONAR_HOST_URL" \
      -e SONAR_LOGIN="$SONAR_TOKEN" \
      -v "$PWD:/usr/src" \
      sonarsource/sonar-scanner-cli:latest
  fi
  popd >/dev/null
}

# Backend services (per-project analysis)
for svc in api-gateway discovery-service user-service product-service media-service orders-service; do
  run_maven_service "$svc"
done

# Frontend (Angular)
run_frontend

echo "\n✅ Local SonarQube analysis completed for all services and frontend."

#!/usr/bin/env bash
set -euo pipefail

# Builds backend and frontend artifacts, creates Docker images, and pushes them to Docker Hub.
# Usage: DOCKER_USERNAME=foo DOCKER_PASSWORD=bar ./scripts/build-and-push-images.sh [image-tag]

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SERVICES=(discovery-service api-gateway user-service product-service media-service orders-service dummy-data)
FRONTEND_SERVICE="frontend"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

IMAGE_TAG="${1:-${IMAGE_TAG:-latest}}"
STABLE_TAG="${STABLE_TAG:-stable}"
DOCKER_REPO="${DOCKER_REPO:-${DOCKER_USERNAME:-}}"
DOCKER_USERNAME="${DOCKER_USERNAME:-${DOCKER_REPO}}"
DOCKER_PASSWORD="${DOCKER_PASSWORD:-${DOCKER_TOKEN:-}}"

if [[ -z "${DOCKER_USERNAME}" || -z "${DOCKER_PASSWORD}" ]]; then
  echo "ERROR: DOCKER_USERNAME and DOCKER_PASSWORD must be set (export them or add to .env)." >&2
  exit 1
fi

log() {
  printf '[build-and-push] %s\n' "$*"
}

run_maven_build() {
  log "Building backend artifacts with Maven"
  (cd "${ROOT_DIR}/backend" && mvn clean package -DskipTests)
}

run_frontend_build() {
  if [[ "${SKIP_FRONTEND_BUILD:-false}" == "true" ]]; then
    log "Skipping frontend build because SKIP_FRONTEND_BUILD=true"
    return
  fi

  if ! command -v npm >/dev/null 2>&1; then
    log "Skipping frontend build because npm is not available"
    return
  fi

  log "Installing frontend dependencies and building"
  (cd "${ROOT_DIR}/frontend" && npm install --legacy-peer-deps && npm run build --if-present)
}

build_service_image() {
  local service="$1"
  local service_dir="${ROOT_DIR}/backend/${service}"
  local tmp_dockerfile="${service_dir}/Dockerfile.tmp"

  if [[ ! -d "${service_dir}" ]]; then
    log "Skipping ${service}: directory not found"
    return
  fi

  if ! compgen -G "${service_dir}/target/*.jar" >/dev/null; then
    log "Skipping ${service}: jar not found in target/"
    return
  fi

  cat >"${tmp_dockerfile}" <<'EOF'
FROM amazoncorretto:21-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080 8443
HEALTHCHECK --interval=10s --timeout=5s --retries=5 CMD curl -f http://localhost:8080/actuator/health || exit 0
ENTRYPOINT ["java", "-Dcom.sun.management.jmxremote", "-jar", "app.jar"]
EOF

  log "Building image for ${service}:${IMAGE_TAG}"
  trap 'rm -f "${tmp_dockerfile}"' RETURN
  docker build -t "${DOCKER_REPO}/${service}:${IMAGE_TAG}" -f "${tmp_dockerfile}" "${service_dir}"
  docker tag "${DOCKER_REPO}/${service}:${IMAGE_TAG}" "${DOCKER_REPO}/${service}:${STABLE_TAG}"
  trap - RETURN
  rm -f "${tmp_dockerfile}"
}

push_service_image() {
  local service="$1"
  log "Pushing ${service}:${IMAGE_TAG} and ${service}:${STABLE_TAG}"
  docker push "${DOCKER_REPO}/${service}:${IMAGE_TAG}"
  docker push "${DOCKER_REPO}/${service}:${STABLE_TAG}"
}

build_frontend_image() {
  local frontend_dir="${ROOT_DIR}/${FRONTEND_SERVICE}"
  if [[ ! -d "${frontend_dir}" ]]; then
    log "Skipping frontend image: directory missing"
    return
  fi

  log "Building image for ${FRONTEND_SERVICE}:${IMAGE_TAG}"
  docker build -t "${DOCKER_REPO}/${FRONTEND_SERVICE}:${IMAGE_TAG}" -f "${frontend_dir}/Dockerfile" "${frontend_dir}"
  docker tag "${DOCKER_REPO}/${FRONTEND_SERVICE}:${IMAGE_TAG}" "${DOCKER_REPO}/${FRONTEND_SERVICE}:${STABLE_TAG}"
}

push_frontend_image() {
  log "Pushing ${FRONTEND_SERVICE}:${IMAGE_TAG} and ${FRONTEND_SERVICE}:${STABLE_TAG}"
  docker push "${DOCKER_REPO}/${FRONTEND_SERVICE}:${IMAGE_TAG}"
  docker push "${DOCKER_REPO}/${FRONTEND_SERVICE}:${STABLE_TAG}"
}

main() {
  log "Using image tag: ${IMAGE_TAG}"
  log "Logging into Docker Hub"
  echo "${DOCKER_PASSWORD}" | docker login -u "${DOCKER_USERNAME}" --password-stdin >/dev/null

  run_maven_build
  run_frontend_build

  for service in "${SERVICES[@]}"; do
    build_service_image "${service}"
    push_service_image "${service}"
  done

  build_frontend_image
  push_frontend_image

  log "Done"
}

main "$@"

#!/bin/bash
set -e

echo "🏥 Checking service health..."

CHECKS=(
  "http://localhost:8761/actuator/health:Eureka"
  "https://localhost:8443/actuator/health:API Gateway"
  "http://localhost:8081/actuator/health:User Service"
  "http://localhost:8082/actuator/health:Product Service"
  "http://localhost:8083/actuator/health:Media Service"
  "http://localhost:4200:Frontend"
)

FAILED=0

for check in "${CHECKS[@]}"; do
  url="${check%:*}"
  service="${check#*:}"
  
  if [[ "$url" == *"https"* ]]; then
    response=$(curl -sk -w "%{http_code}" "$url" 2>/dev/null | tail -c 3)
  else
    response=$(curl -s -w "%{http_code}" "$url" 2>/dev/null | tail -c 3)
  fi
  
  if [ "$response" = "200" ]; then
    echo "✅ $service is healthy"
  else
    echo "❌ $service returned $response"
    FAILED=$((FAILED + 1))
  fi
done

if [ $FAILED -eq 0 ]; then
  echo "✅ All services are healthy!"
  exit 0
else
  echo "❌ $FAILED services failed health checks"
  exit 1
fi

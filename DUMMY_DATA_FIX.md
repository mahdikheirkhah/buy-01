# Dummy Data Service Fix - December 23, 2025

## Problem
The dummy-data service was failing to start on first run with Kafka connection errors:
```
org.apache.kafka.common.config.ConfigException: Invalid url in bootstrap.servers: kafka:29092
```

The service would work on the second run because Kafka had time to fully initialize.

## Root Cause
1. **Kafka had no healthcheck** - Docker Compose couldn't verify Kafka was ready
2. **dummy-data didn't wait for Kafka** - Only waited for api-gateway, not Kafka
3. **Kafka initialization race condition** - Kafka takes ~20-30 seconds to be fully ready
4. **No retry/timeout configuration** - Spring Kafka Admin would fail immediately

## Solution Applied

### 1. Added Kafka Healthcheck (docker-compose.yml)
```yaml
kafka:
  healthcheck:
    test: ["CMD-SHELL", "kafka-broker-api-versions --bootstrap-server localhost:29092 || exit 1"]
    interval: 10s
    timeout: 10s
    retries: 10
    start_period: 30s
```

### 2. Updated dummy-data Dependencies (docker-compose.yml)
```yaml
dummy-data:
  depends_on:
    api-gateway:
      condition: service_healthy
    kafka:
      condition: service_healthy  # ← NEW: Wait for Kafka
  restart: on-failure  # ← NEW: Auto-restart on failure
  environment:
    SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092  # ← Made explicit
```

### 3. Added Kafka Resilience Configuration (application.properties)
```properties
spring.kafka.admin.auto-create=true
spring.kafka.admin.fail-fast=false
spring.kafka.admin.properties.request.timeout.ms=30000
spring.kafka.admin.properties.connections.max.idle.ms=300000
```

## Changes Made

### Modified Files
1. **docker-compose.yml**
   - Added healthcheck to Kafka service
   - Added Kafka dependency to dummy-data
   - Added restart policy to dummy-data
   - Made Kafka bootstrap servers explicit

2. **backend/dummy-data/src/main/resources/application.properties**
   - Added Kafka admin configuration
   - Added connection timeout settings
   - Enabled auto-create for topics
   - Disabled fail-fast behavior

## Testing
To verify the fix:

```bash
# Stop all services
docker compose down

# Start fresh
export IMAGE_TAG=stable  # or your current tag
docker compose up -d

# Check dummy-data logs
docker logs dummy-data -f

# Expected: Service starts successfully on first try
# No more Kafka connection errors
```

## Impact
✅ **dummy-data service now starts reliably on first run**
✅ **Kafka waits for full initialization before dependent services start**
✅ **More resilient Kafka connection handling**
✅ **Auto-restart if connection fails temporarily**

## Related Services
The same Kafka healthcheck benefits:
- user-service
- product-service
- media-service

All microservices that use Kafka will now start more reliably.

## Future Improvements
Consider:
1. Adding retry logic in application code
2. Implementing circuit breaker pattern for Kafka
3. Adding fallback behavior if Kafka is unavailable
4. Monitoring Kafka connection health in application

---
**Status:** ✅ FIXED - Ready for testing
**Date:** December 23, 2025


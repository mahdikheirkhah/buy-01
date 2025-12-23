#!/bin/bash

# Test script for dummy-data fix
# This verifies that the dummy-data service starts successfully on first run

echo "=================================================="
echo "Testing Dummy-Data Service Fix"
echo "=================================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Step 1: Stop all services
echo -e "${YELLOW}Step 1: Stopping all services...${NC}"
docker compose down
echo ""

# Step 2: Set IMAGE_TAG
if [ -z "$IMAGE_TAG" ]; then
    export IMAGE_TAG=stable
    echo -e "${YELLOW}IMAGE_TAG not set, using: stable${NC}"
else
    echo -e "${YELLOW}Using IMAGE_TAG: $IMAGE_TAG${NC}"
fi
echo ""

# Step 3: Start services
echo -e "${YELLOW}Step 2: Starting services with fresh state...${NC}"
docker compose up -d
echo ""

# Step 4: Wait for Kafka
echo -e "${YELLOW}Step 3: Waiting for Kafka to be healthy (max 60s)...${NC}"
KAFKA_READY=false
for i in {1..60}; do
    if docker compose ps kafka | grep -q "healthy"; then
        echo -e "${GREEN}✓ Kafka is healthy after ${i} seconds${NC}"
        KAFKA_READY=true
        break
    fi
    echo -n "."
    sleep 1
done
echo ""

if [ "$KAFKA_READY" = false ]; then
    echo -e "${RED}✗ Kafka failed to become healthy${NC}"
    exit 1
fi

# Step 5: Wait for dummy-data
echo -e "${YELLOW}Step 4: Waiting for dummy-data to start (max 60s)...${NC}"
DUMMY_DATA_STARTED=false
for i in {1..60}; do
    if docker logs dummy-data 2>&1 | grep -q "Started DummyDataApplication"; then
        echo -e "${GREEN}✓ dummy-data started successfully after ${i} seconds${NC}"
        DUMMY_DATA_STARTED=true
        break
    fi

    # Check for errors
    if docker logs dummy-data 2>&1 | grep -q "ConfigException"; then
        echo -e "${RED}✗ dummy-data has Kafka connection error${NC}"
        echo ""
        echo "Last 20 lines of dummy-data logs:"
        docker logs dummy-data --tail 20
        exit 1
    fi

    echo -n "."
    sleep 1
done
echo ""

if [ "$DUMMY_DATA_STARTED" = false ]; then
    echo -e "${RED}✗ dummy-data failed to start within 60 seconds${NC}"
    echo ""
    echo "Last 30 lines of dummy-data logs:"
    docker logs dummy-data --tail 30
    exit 1
fi

# Step 6: Check for errors in logs
echo -e "${YELLOW}Step 5: Checking for Kafka errors in dummy-data logs...${NC}"
if docker logs dummy-data 2>&1 | grep -q "ConfigException"; then
    echo -e "${RED}✗ Found Kafka ConfigException in logs${NC}"
    echo ""
    echo "Full dummy-data logs:"
    docker logs dummy-data
    exit 1
else
    echo -e "${GREEN}✓ No Kafka connection errors found${NC}"
fi
echo ""

# Step 7: Show service status
echo -e "${YELLOW}Step 6: Service status:${NC}"
docker compose ps
echo ""

# Success
echo "=================================================="
echo -e "${GREEN}✅ TEST PASSED!${NC}"
echo "=================================================="
echo ""
echo "Summary:"
echo "✓ Kafka started and became healthy"
echo "✓ dummy-data waited for Kafka"
echo "✓ dummy-data started successfully on FIRST run"
echo "✓ No Kafka connection errors"
echo ""
echo "The fix is working correctly!"
echo ""


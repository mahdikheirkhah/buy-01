#!/bin/bash

# MailHog Setup Script for Jenkins Email Testing
# This script helps you set up MailHog and configure Jenkins

set -e

echo "=================================================="
echo "MailHog Setup for Jenkins Email Testing"
echo "=================================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if Docker is running
echo "1️⃣  Checking Docker..."
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker is not running. Please start Docker Desktop first.${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker is running${NC}"
echo ""

# Start MailHog
echo "2️⃣  Starting MailHog..."
cd "$(dirname "$0")"
export IMAGE_TAG=stable

if docker compose ps mailhog | grep -q "Up"; then
    echo -e "${YELLOW}⚠️  MailHog is already running${NC}"
else
    docker compose up -d mailhog
    echo -e "${GREEN}✅ MailHog started successfully${NC}"
fi
echo ""

# Wait for MailHog to be ready
echo "3️⃣  Waiting for MailHog to be ready..."
sleep 3

# Check if MailHog is accessible
if curl -s http://localhost:8025 > /dev/null; then
    echo -e "${GREEN}✅ MailHog is accessible${NC}"
else
    echo -e "${RED}❌ MailHog is not accessible on port 8025${NC}"
    exit 1
fi
echo ""

# Display MailHog info
echo "=================================================="
echo "✅ MailHog Setup Complete!"
echo "=================================================="
echo ""
echo "📧 MailHog SMTP Server: host.docker.internal:1025"
echo "🌐 MailHog Web UI: http://localhost:8025"
echo ""
echo "Next Steps:"
echo "1. Configure Jenkins SMTP settings:"
echo "   - Go to: http://localhost:8080/manage/configure"
echo "   - Scroll to: 'Extended E-mail Notification'"
echo "   - SMTP server: host.docker.internal"
echo "   - SMTP Port: 1025"
echo "   - No authentication needed"
echo "   - Uncheck SSL and TLS"
echo ""
echo "2. Test email configuration:"
echo "   - Click 'Test configuration by sending test e-mail'"
echo "   - Enter: test@example.com"
echo "   - Check MailHog at: http://localhost:8025"
echo ""
echo "3. Run a Jenkins build and check MailHog for emails!"
echo ""
echo "=================================================="
echo ""

# Offer to open MailHog
read -p "Would you like to open MailHog Web UI now? (y/n) " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    if [[ "$OSTYPE" == "darwin"* ]]; then
        open http://localhost:8025
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        xdg-open http://localhost:8025 2>/dev/null || echo "Please open http://localhost:8025 in your browser"
    else
        echo "Please open http://localhost:8025 in your browser"
    fi
fi

echo ""
echo "📚 For detailed instructions, see: MAILHOG_SETUP.md"
echo ""


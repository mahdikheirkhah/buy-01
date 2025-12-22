#!/bin/bash

# Test ngrok webhook configuration
# This script verifies your ngrok URL is properly configured for GitHub webhooks

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "============================================"
echo "GitHub Webhook + ngrok Configuration Test"
echo "============================================"
echo ""

# Get ngrok URL from user or use default
read -p "Enter your ngrok URL (e.g., https://your-subdomain.ngrok-free.dev): " NGROK_URL

if [ -z "$NGROK_URL" ]; then
    echo -e "${RED}❌ ERROR: ngrok URL is required${NC}"
    exit 1
fi

# Remove trailing slash if present
NGROK_URL="${NGROK_URL%/}"

echo ""
echo -e "${BLUE}Testing ngrok URL: ${NGROK_URL}${NC}"
echo ""

# Test 1: Check if ngrok URL is accessible
echo "1. Testing ngrok URL accessibility..."
response=$(curl -s -o /dev/null -w "%{http_code}" "${NGROK_URL}/" 2>&1 || echo "000")

if [ "$response" = "403" ] || [ "$response" = "200" ]; then
    echo -e "${GREEN}✅ ngrok URL is accessible (HTTP $response)${NC}"
    echo "   This is expected - Jenkins requires authentication at root"
elif [ "$response" = "000" ]; then
    echo -e "${RED}❌ Cannot reach ngrok URL${NC}"
    echo "   Make sure ngrok is running: ngrok http 8080"
    exit 1
else
    echo -e "${YELLOW}⚠️  Unexpected response: $response${NC}"
fi
echo ""

# Test 2: Check GitHub webhook endpoint
echo "2. Testing GitHub webhook endpoint..."
webhook_response=$(curl -s -o /dev/null -w "%{http_code}" "${NGROK_URL}/github-webhook/" 2>&1 || echo "000")

if [ "$webhook_response" = "200" ] || [ "$webhook_response" = "405" ]; then
    echo -e "${GREEN}✅ GitHub webhook endpoint is accessible (HTTP $webhook_response)${NC}"
    echo "   This endpoint is ready to receive webhooks"
elif [ "$webhook_response" = "403" ]; then
    echo -e "${RED}❌ Getting 403 Forbidden on webhook endpoint${NC}"
    echo "   This means CSRF is still blocking webhooks"
    echo "   Run: ./fix-webhook-csrf.sh and choose option 2"
elif [ "$webhook_response" = "404" ]; then
    echo -e "${RED}❌ Webhook endpoint not found (404)${NC}"
    echo "   Make sure GitHub plugin is installed in Jenkins"
    echo "   Go to: Manage Jenkins → Plugins → Available → GitHub Integration"
else
    echo -e "${YELLOW}⚠️  Unexpected response: $webhook_response${NC}"
fi
echo ""

# Test 3: Check if Jenkins is running
echo "3. Checking if Jenkins is running locally..."
if docker ps | grep -q jenkins-cicd; then
    echo -e "${GREEN}✅ Jenkins container is running${NC}"

    # Check if ngrok is running
    if command -v ngrok &> /dev/null; then
        if pgrep -x "ngrok" > /dev/null; then
            echo -e "${GREEN}✅ ngrok is running${NC}"
        else
            echo -e "${YELLOW}⚠️  ngrok command exists but may not be running${NC}"
            echo "   Start ngrok: ngrok http 8080"
        fi
    else
        echo -e "${YELLOW}⚠️  ngrok command not found${NC}"
        echo "   Install: brew install ngrok (on macOS)"
    fi
else
    echo -e "${RED}❌ Jenkins container is not running${NC}"
    echo "   Start Jenkins: docker-compose -f docker-compose.jenkins.yml up -d"
fi
echo ""

# Test 4: Check GitHub plugin
echo "4. Checking if GitHub plugin is installed..."
if docker exec jenkins-cicd test -d /var/jenkins_home/plugins/github 2>/dev/null; then
    echo -e "${GREEN}✅ GitHub plugin is installed${NC}"
else
    echo -e "${RED}❌ GitHub plugin is NOT installed${NC}"
    echo "   Install it: Manage Jenkins → Plugins → Available → GitHub Integration"
fi
echo ""

echo "============================================"
echo "Configuration Summary"
echo "============================================"
echo ""
echo "🔗 Your ngrok URL: ${NGROK_URL}"
echo ""
echo "📝 GitHub Webhook Configuration:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Payload URL:"
echo -e "  ${GREEN}${NGROK_URL}/github-webhook/${NC}"
echo ""
echo "Content type:"
echo "  application/json"
echo ""
echo "Secret:"
echo "  (leave empty or add one)"
echo ""
echo "SSL verification:"
echo "  ✅ Enable SSL verification"
echo ""
echo "Events:"
echo "  ⚪ Just the push event"
echo ""
echo "Active:"
echo "  ✅ Active"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

if [ "$webhook_response" = "200" ] || [ "$webhook_response" = "405" ]; then
    echo -e "${GREEN}✅ Your webhook endpoint is ready!${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Update your GitHub webhook URL to:"
    echo -e "   ${GREEN}${NGROK_URL}/github-webhook/${NC}"
    echo ""
    echo "2. Test the webhook:"
    echo "   • GitHub → Settings → Webhooks → Recent Deliveries"
    echo "   • Click 'Redeliver' → Should see ✅ 200 OK"
    echo ""
    echo "3. Push a commit:"
    echo "   git commit --allow-empty -m 'Test webhook'"
    echo "   git push origin main"
    echo ""
    echo "4. Check Jenkins:"
    echo "   ${NGROK_URL}"
    echo "   Build should start automatically!"
else
    echo -e "${YELLOW}⚠️  Webhook endpoint needs attention${NC}"
    echo ""
    echo "Troubleshooting steps:"
    echo ""
    echo "If you got 403 Forbidden:"
    echo "  → Run: ./fix-webhook-csrf.sh"
    echo "  → Choose option 2 (Enable CSRF Proxy Compatibility)"
    echo ""
    echo "If you got 404 Not Found:"
    echo "  → Install GitHub plugin in Jenkins"
    echo "  → Manage Jenkins → Plugins → Available → GitHub Integration"
    echo ""
    echo "If ngrok is not running:"
    echo "  → Start ngrok: ngrok http 8080"
    echo "  → Get new URL from ngrok dashboard"
    echo "  → Update GitHub webhook with new URL"
fi

echo ""
echo "============================================"
echo "📚 Documentation:"
echo "   • WEBHOOK_SETUP.md"
echo "   • WEBHOOK_CSRF_FIX.md"
echo "============================================"


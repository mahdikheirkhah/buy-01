#!/bin/bash

# Jenkins Proxy Fix - Quick Setup Script
# This script helps configure Jenkins to work properly with webhooks

set -e

echo "🔧 Jenkins Proxy Configuration Helper"
echo "======================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Jenkins is running
echo "Checking if Jenkins is running..."
if docker ps | grep -q jenkins-cicd; then
    echo -e "${GREEN}✓ Jenkins container is running${NC}"
else
    echo -e "${RED}✗ Jenkins container is not running${NC}"
    echo "Starting Jenkins..."
    docker start jenkins-cicd || {
        echo -e "${RED}Failed to start Jenkins. Please check Docker.${NC}"
        exit 1
    }
    echo -e "${GREEN}✓ Jenkins started${NC}"
    echo "Waiting 30 seconds for Jenkins to initialize..."
    sleep 30
fi

echo ""
echo "📋 Your Jenkins Information:"
echo "======================================"
echo "URL: http://localhost:8080"
echo "Username: admin"
echo "Password: 40cdde478c6c49f0adcfdd34875e62a9"
echo ""

# Check if we can access Jenkins
echo "Testing Jenkins connectivity..."
if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080 | grep -q "200\|403"; then
    echo -e "${GREEN}✓ Jenkins is accessible${NC}"
else
    echo -e "${RED}✗ Cannot connect to Jenkins${NC}"
    echo "Please ensure Jenkins is running and try again."
    exit 1
fi

echo ""
echo "🎯 Recommended Solution: Use Polling (No Proxy Issues)"
echo "======================================"
echo "The Jenkinsfile has been updated to use polling instead of webhooks."
echo "This avoids all proxy/CSRF issues and works reliably."
echo ""
echo "What this means:"
echo "  • Jenkins checks GitHub every 5 minutes for new commits"
echo "  • No ngrok or webhook setup needed"
echo "  • No CSRF/proxy configuration needed"
echo "  • Builds trigger automatically within 5 minutes of push"
echo ""

echo -e "${YELLOW}Next Steps:${NC}"
echo "1. Login to Jenkins: http://localhost:8080"
echo "2. Go to your job: e-commerce-microservices-ci-cd"
echo "3. Click 'Build Now' once to activate the trigger"
echo "4. After that, Jenkins will automatically check GitHub every 5 minutes"
echo ""
echo "5. Test it:"
echo "   git commit -m 'test auto build' --allow-empty"
echo "   git push origin main"
echo "   # Wait up to 5 minutes and check Jenkins"
echo ""

read -p "Do you want to switch to webhook mode instead? (requires ngrok) [y/N]: " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "🌐 Webhook Mode Setup"
    echo "======================================"

    # Check if ngrok is installed
    if ! command -v ngrok &> /dev/null; then
        echo -e "${RED}✗ ngrok is not installed${NC}"
        echo "Install ngrok:"
        echo "  brew install ngrok"
        echo "  # or download from https://ngrok.com/download"
        exit 1
    fi

    echo -e "${GREEN}✓ ngrok is installed${NC}"
    echo ""
    echo "Starting ngrok..."
    echo "This will expose Jenkins on a public URL for GitHub webhooks"
    echo ""

    # Check if ngrok is already running
    if pgrep -x "ngrok" > /dev/null; then
        echo -e "${YELLOW}ngrok is already running${NC}"
        echo "Stop it first: pkill ngrok"
        exit 1
    fi

    # Start ngrok in background
    ngrok http 8080 > /dev/null &
    NGROK_PID=$!

    echo "Waiting for ngrok to start..."
    sleep 3

    # Get ngrok URL
    NGROK_URL=$(curl -s http://localhost:4040/api/tunnels | grep -o "https://[^\"]*\.ngrok-free\.app" | head -1)

    if [ -z "$NGROK_URL" ]; then
        echo -e "${RED}✗ Could not get ngrok URL${NC}"
        kill $NGROK_PID 2>/dev/null || true
        exit 1
    fi

    echo -e "${GREEN}✓ ngrok is running${NC}"
    echo ""
    echo "======================================"
    echo "📡 Your ngrok URL: ${NGROK_URL}"
    echo "======================================"
    echo ""
    echo "Next steps:"
    echo ""
    echo "1. Configure Jenkins URL:"
    echo "   • Open: http://localhost:8080/configure"
    echo "   • Find 'Jenkins Location'"
    echo "   • Set 'Jenkins URL' to: ${NGROK_URL}/"
    echo "   • Click Save"
    echo ""
    echo "2. Enable proxy compatibility:"
    echo "   • Open: http://localhost:8080/configureSecurity"
    echo "   • Under 'CSRF Protection', click 'Advanced'"
    echo "   • Check 'Enable proxy compatibility'"
    echo "   • Click Save"
    echo ""
    echo "3. Add GitHub webhook:"
    echo "   • Open: https://github.com/mahdikheirkhah/buy-01/settings/hooks"
    echo "   • Click 'Add webhook'"
    echo "   • Payload URL: ${NGROK_URL}/github-webhook/"
    echo "   • Content type: application/json"
    echo "   • Click 'Add webhook'"
    echo ""
    echo "4. Update Jenkinsfile:"
    echo "   • Uncomment 'githubPush()' in triggers section"
    echo "   • Comment out 'pollSCM(...)' line"
    echo "   • Commit and push"
    echo ""
    echo "5. Test:"
    echo "   git commit -m 'test webhook' --allow-empty"
    echo "   git push origin main"
    echo "   # Build should trigger immediately"
    echo ""
    echo "🌐 ngrok dashboard: http://localhost:4040"
    echo ""
    echo -e "${YELLOW}Note: Keep this terminal open to keep ngrok running${NC}"
    echo "Press Ctrl+C to stop ngrok"
    echo ""

    # Keep script running
    wait $NGROK_PID
else
    echo ""
    echo -e "${GREEN}✓ Using polling mode (recommended)${NC}"
    echo ""
    echo "You're all set! Just:"
    echo "1. Login to Jenkins: http://localhost:8080"
    echo "2. Click 'Build Now' on your job once"
    echo "3. Jenkins will automatically build on every push (within 5 minutes)"
    echo ""
fi

echo ""
echo "📚 For detailed troubleshooting, see:"
echo "   • JENKINS_PROXY_FIX.md"
echo "   • QUICK_FIX_403.md"
echo ""
echo "Done! 🚀"


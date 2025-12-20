#!/bin/bash

# Jenkins CSRF Configuration Script
# This script helps you properly configure CSRF protection for Jenkins webhooks

set -e

echo "🔐 Jenkins CSRF Configuration"
echo "======================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Jenkins credentials
JENKINS_URL="http://localhost:8080"
JENKINS_USER="admin"
JENKINS_PASSWORD="40cdde478c6c49f0adcfdd34875e62a9"

# Check if Jenkins is running
echo "Checking Jenkins status..."
if ! docker ps | grep -q jenkins-cicd; then
    echo -e "${RED}✗ Jenkins container is not running${NC}"
    echo "Starting Jenkins..."
    docker start jenkins-cicd
    echo "Waiting for Jenkins to start..."
    sleep 30
fi

echo -e "${GREEN}✓ Jenkins is running${NC}"
echo ""

# Function to get Jenkins crumb
get_crumb() {
    curl -s -u "${JENKINS_USER}:${JENKINS_PASSWORD}" \
        "${JENKINS_URL}/crumbIssuer/api/json" | \
        grep -o '"crumb":"[^"]*"' | \
        cut -d'"' -f4
}

# Function to check current CSRF configuration
check_csrf_config() {
    echo "Checking current CSRF configuration..."
    docker exec jenkins-cicd cat /var/jenkins_home/config.xml | grep -A 5 "crumbIssuer" || echo "CSRF configuration not found in config"
}

echo "📋 Current CSRF Configuration:"
echo "======================================"
check_csrf_config
echo ""

# Main menu
echo "Choose your configuration approach:"
echo ""
echo "1. ✅ Enable Polling (RECOMMENDED - No CSRF issues)"
echo "   - Builds trigger within 5 minutes of push"
echo "   - No public URL needed"
echo "   - No CSRF configuration needed"
echo ""
echo "2. 🌐 Enable Proxy Compatibility (For Webhooks)"
echo "   - Immediate build triggers"
echo "   - Requires public URL (ngrok)"
echo "   - Keeps CSRF protection active"
echo ""
echo "3. 📊 View Current Configuration"
echo "   - See current Jenkins settings"
echo ""
echo "4. ⚠️  Disable CSRF (NOT RECOMMENDED)"
echo "   - Only for isolated dev environments"
echo "   - Security risk"
echo ""
echo "5. 🔄 Reset to Default CSRF Settings"
echo ""
read -p "Enter your choice (1-5): " choice
echo ""

case $choice in
    1)
        echo "✅ Configuring Polling Mode"
        echo "======================================"
        echo ""
        echo "The Jenkinsfile is already configured for polling."
        echo ""
        echo "Next steps:"
        echo "1. Go to: ${JENKINS_URL}"
        echo "2. Open your job: e-commerce-microservices-ci-cd"
        echo "3. Click 'Build Now' once to activate the trigger"
        echo "4. Jenkins will now poll GitHub every 5 minutes"
        echo ""
        echo "Test it:"
        echo "  git commit -m 'test polling' --allow-empty"
        echo "  git push origin main"
        echo "  # Wait up to 5 minutes"
        echo ""
        echo -e "${GREEN}✓ No additional configuration needed!${NC}"
        ;;

    2)
        echo "🌐 Configuring Webhook Mode with CSRF Protection"
        echo "======================================"
        echo ""

        # Check if ngrok is installed
        if ! command -v ngrok &> /dev/null; then
            echo -e "${RED}✗ ngrok is not installed${NC}"
            echo "Install it with: brew install ngrok"
            echo "Or download from: https://ngrok.com/download"
            exit 1
        fi

        echo -e "${GREEN}✓ ngrok found${NC}"
        echo ""

        # Get Jenkins crumb for authenticated requests
        echo "Getting Jenkins crumb..."
        CRUMB=$(get_crumb)

        if [ -z "$CRUMB" ]; then
            echo -e "${YELLOW}⚠️  Could not get Jenkins crumb${NC}"
            echo "You may need to configure this manually"
        else
            echo -e "${GREEN}✓ Got Jenkins crumb${NC}"
        fi

        echo ""
        echo "Starting ngrok..."

        # Check if ngrok is already running
        if pgrep -x "ngrok" > /dev/null; then
            echo -e "${YELLOW}⚠️  ngrok is already running${NC}"
            echo "Stop it first: pkill ngrok"
            exit 1
        fi

        # Start ngrok in background
        ngrok http 8080 > /dev/null 2>&1 &
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

        echo "🔧 Manual Configuration Steps:"
        echo ""
        echo "1. Configure CSRF Protection:"
        echo "   a. Open: ${JENKINS_URL}/configureSecurity/"
        echo "   b. Find 'CSRF Protection' section"
        echo "   c. ✓ Check 'Enable proxy compatibility'"
        echo "   d. Click 'Save'"
        echo ""
        echo "2. Set Jenkins URL:"
        echo "   a. Open: ${JENKINS_URL}/configure"
        echo "   b. Find 'Jenkins Location'"
        echo "   c. Set 'Jenkins URL' to: ${NGROK_URL}/"
        echo "   d. Click 'Save'"
        echo ""
        echo "3. Configure GitHub Webhook:"
        echo "   a. Open: https://github.com/mahdikheirkhah/buy-01/settings/hooks"
        echo "   b. Click 'Add webhook'"
        echo "   c. Payload URL: ${NGROK_URL}/github-webhook/"
        echo "   d. Content type: application/json"
        echo "   e. Select 'Just the push event'"
        echo "   f. Check 'Active'"
        echo "   g. Click 'Add webhook'"
        echo ""
        echo "4. Test the webhook:"
        echo "   git commit -m 'test webhook' --allow-empty"
        echo "   git push origin main"
        echo "   # Build should trigger immediately"
        echo ""
        echo "🌐 ngrok dashboard: http://localhost:4040"
        echo ""
        echo -e "${YELLOW}⚠️  Keep this terminal open to keep ngrok running${NC}"
        echo "Press Ctrl+C to stop"
        echo ""

        # Keep running
        wait $NGROK_PID
        ;;

    3)
        echo "📊 Current Jenkins Configuration"
        echo "======================================"
        echo ""
        echo "CSRF Configuration:"
        check_csrf_config
        echo ""
        echo "Jenkins URL:"
        docker exec jenkins-cicd cat /var/jenkins_home/jenkins.model.JenkinsLocationConfiguration.xml 2>/dev/null || echo "Location config not found"
        echo ""
        echo "To view full configuration:"
        echo "  docker exec jenkins-cicd cat /var/jenkins_home/config.xml"
        ;;

    4)
        echo "⚠️  Disabling CSRF Protection"
        echo "======================================"
        echo ""
        echo -e "${RED}WARNING: This is NOT recommended for production!${NC}"
        echo "Only use this in isolated development environments."
        echo ""
        read -p "Are you sure you want to disable CSRF? (yes/no): " confirm

        if [ "$confirm" = "yes" ]; then
            echo ""
            echo "To disable CSRF protection:"
            echo "1. Go to: ${JENKINS_URL}/configureSecurity/"
            echo "2. Find 'CSRF Protection' section"
            echo "3. Uncheck 'Prevent Cross Site Request Forgery exploits'"
            echo "4. Click 'Save'"
            echo ""
            echo -e "${YELLOW}⚠️  Your Jenkins will be vulnerable to CSRF attacks${NC}"
        else
            echo "Cancelled. CSRF protection remains enabled."
        fi
        ;;

    5)
        echo "🔄 Resetting CSRF to Default Settings"
        echo "======================================"
        echo ""
        echo "This will enable standard CSRF protection with:"
        echo "  ✓ Default crumb issuer"
        echo "  ✓ Client IP included in crumb"
        echo "  ✗ Proxy compatibility disabled"
        echo ""
        read -p "Continue? (yes/no): " confirm

        if [ "$confirm" = "yes" ]; then
            echo ""
            echo "To reset CSRF to defaults:"
            echo "1. Go to: ${JENKINS_URL}/configureSecurity/"
            echo "2. Find 'CSRF Protection' section"
            echo "3. Check 'Prevent Cross Site Request Forgery exploits'"
            echo "4. Select 'Default Crumb Issuer'"
            echo "5. Uncheck 'Enable proxy compatibility'"
            echo "6. Click 'Save'"
            echo ""
            echo "Then restart Jenkins:"
            echo "  docker restart jenkins-cicd"
        else
            echo "Cancelled."
        fi
        ;;

    *)
        echo -e "${RED}Invalid choice${NC}"
        exit 1
        ;;
esac

echo ""
echo "======================================"
echo "📚 For more information, see:"
echo "  • JENKINS_CSRF_SETUP.md"
echo "======================================"
echo ""
echo "Done! 🚀"


#!/bin/bash

# Gmail SMTP Connection Tester
# Tests if Jenkins can connect to Gmail SMTP server

echo "============================================"
echo "Gmail SMTP Connection Test"
echo "============================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test 1: Check if Jenkins container is running
echo "1. Checking Jenkins container..."
if docker ps | grep -q jenkins-cicd; then
    echo -e "${GREEN}✅ PASS: Jenkins container is running${NC}"
else
    echo -e "${RED}❌ FAIL: Jenkins container is not running${NC}"
    exit 1
fi
echo ""

# Test 2: Test Gmail SMTP port 465 (SSL)
echo "2. Testing Gmail SMTP connection (port 465 - SSL)..."
if docker exec jenkins-cicd timeout 5 bash -c "echo -n > /dev/tcp/smtp.gmail.com/465" 2>/dev/null; then
    echo -e "${GREEN}✅ PASS: Can connect to smtp.gmail.com:465${NC}"
else
    echo -e "${RED}❌ FAIL: Cannot connect to smtp.gmail.com:465${NC}"
    echo "   Possible issues:"
    echo "   - Firewall blocking outbound connections"
    echo "   - Network issue"
    echo "   - Try port 587 instead"
fi
echo ""

# Test 3: Test Gmail SMTP port 587 (TLS)
echo "3. Testing Gmail SMTP connection (port 587 - TLS)..."
if docker exec jenkins-cicd timeout 5 bash -c "echo -n > /dev/tcp/smtp.gmail.com/587" 2>/dev/null; then
    echo -e "${GREEN}✅ PASS: Can connect to smtp.gmail.com:587${NC}"
    echo "   ${YELLOW}💡 If port 465 fails, use port 587 with TLS instead of SSL${NC}"
else
    echo -e "${RED}❌ FAIL: Cannot connect to smtp.gmail.com:587${NC}"
fi
echo ""

# Test 4: Check if email-ext plugin is installed
echo "4. Checking Email Extension Plugin..."
if docker exec jenkins-cicd test -d /var/jenkins_home/plugins/email-ext; then
    echo -e "${GREEN}✅ PASS: Email Extension Plugin is installed${NC}"
else
    echo -e "${YELLOW}⚠️  WARNING: Email Extension Plugin might not be installed${NC}"
    echo "   Install it from: Manage Jenkins → Plugins → Available Plugins"
fi
echo ""

# Test 5: Check Jenkins configuration
echo "5. Checking Jenkins email configuration..."
if docker exec jenkins-cicd test -f /var/jenkins_home/config.xml; then
    echo -e "${GREEN}✅ PASS: Jenkins config file exists${NC}"

    # Check if SMTP is configured
    if docker exec jenkins-cicd grep -q "smtp.gmail.com" /var/jenkins_home/config.xml 2>/dev/null; then
        echo -e "${GREEN}✅ Gmail SMTP is configured in Jenkins${NC}"
    else
        echo -e "${YELLOW}⚠️  Gmail SMTP not configured yet${NC}"
        echo "   Configure in: Manage Jenkins → System → E-mail Notification"
    fi
else
    echo -e "${RED}❌ Jenkins config file not found${NC}"
fi
echo ""

# Summary
echo "============================================"
echo "Summary & Next Steps"
echo "============================================"
echo ""
echo "To complete Gmail setup in Jenkins:"
echo ""
echo "1. Get Gmail App Password:"
echo "   → https://myaccount.google.com/apppasswords"
echo ""
echo "2. Configure in Jenkins:"
echo "   → Manage Jenkins → System"
echo "   → Extended E-mail Notification section"
echo "   → SMTP server: smtp.gmail.com"
echo "   → SMTP port: 465 (SSL) or 587 (TLS)"
echo "   → Enable authentication"
echo "   → Enter your Gmail and App Password"
echo ""
echo "3. Test configuration:"
echo "   → Check 'Test configuration by sending test e-mail'"
echo "   → Enter: mohammad.kheirkhah@gritlab.ax"
echo "   → Click 'Test configuration'"
echo ""
echo "4. Check your email (and spam folder!)"
echo ""
echo "============================================"
echo "Detailed guide: GMAIL_SETUP.md"
echo "============================================"


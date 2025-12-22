#!/bin/bash

echo "============================================"
echo "Testing SMTP Connection from Host vs Container"
echo "============================================"
echo ""

# Test from host machine
echo "1. Testing from your Mac (host machine)..."
nc -zv smtp.gmail.com 587 2>&1 | head -1
if [ $? -eq 0 ]; then
    echo "✅ Host can connect to Gmail SMTP"
else
    echo "❌ Host cannot connect to Gmail SMTP"
    echo "   This might be a firewall or network issue"
fi

echo ""

# Test from Jenkins container
echo "2. Testing from Jenkins container..."
docker exec jenkins-cicd nc -zv smtp.gmail.com 587 2>&1 | head -1
if [ $? -eq 0 ]; then
    echo "✅ Jenkins container can connect to Gmail SMTP"
else
    echo "❌ Jenkins container cannot connect to Gmail SMTP"
    echo "   This is why emails aren't working!"
    echo ""
    echo "Possible solutions:"
    echo "1. Use port 465 with SSL instead of 587 with TLS"
    echo "2. Check Docker network settings"
    echo "3. Use alternative: SendGrid, Mailgun, or Mailhog"
fi

echo ""
echo "3. Testing alternative port (465)..."
docker exec jenkins-cicd nc -zv smtp.gmail.com 465 2>&1 | head -1
if [ $? -eq 0 ]; then
    echo "✅ Port 465 works! Use this instead"
    echo ""
    echo "Configure in Jenkins:"
    echo "- SMTP Port: 465"
    echo "- Use SSL: ✓ (not TLS)"
else
    echo "❌ Port 465 also blocked"
fi

echo ""
echo "============================================"
echo "Recommended Actions"
echo "============================================"
echo ""
echo "Option 1: Try port 465 with SSL"
echo "   Jenkins → System → Email Configuration"
echo "   - Change port from 587 to 465"
echo "   - Use SSL instead of TLS"
echo ""
echo "Option 2: Use SendGrid (Free, more reliable)"
echo "   1. Sign up: https://sendgrid.com/free/"
echo "   2. Create API key"
echo "   3. Configure in Jenkins:"
echo "      - SMTP server: smtp.sendgrid.net"
echo "      - Port: 587"
echo "      - Username: apikey"
echo "      - Password: [Your API key]"
echo ""
echo "Option 3: Use Mailhog (Local testing)"
echo "   docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog"
echo "   - SMTP server: host.docker.internal"
echo "   - Port: 1025"
echo "   - No authentication needed"
echo "   - View emails: http://localhost:8025"
echo ""


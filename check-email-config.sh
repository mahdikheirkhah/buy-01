#!/bin/bash

echo "============================================"
echo "Jenkins Email Configuration Diagnostic"
echo "============================================"
echo ""

# Check if Jenkins container is running
if ! docker ps | grep -q jenkins-cicd; then
    echo "❌ Jenkins container 'jenkins-cicd' is not running"
    echo "   Start it with: docker compose -f docker-compose.jenkins.yml up -d"
    exit 1
fi

echo "✅ Jenkins container is running"
echo ""

# Check if telnet is available in Jenkins container
echo "Checking network connectivity from Jenkins..."
docker exec jenkins-cicd which telnet &>/dev/null
if [ $? -ne 0 ]; then
    echo "Installing telnet in Jenkins container..."
    docker exec -u root jenkins-cicd bash -c "apt-get update -qq && apt-get install -y -qq telnet" &>/dev/null
fi

# Test SMTP connection
echo ""
echo "Testing connection to Gmail SMTP server..."
timeout 5 docker exec jenkins-cicd telnet smtp.gmail.com 587 2>/dev/null | head -1 | grep -q "220"
if [ $? -eq 0 ]; then
    echo "✅ Can connect to smtp.gmail.com:587"
else
    echo "❌ Cannot connect to smtp.gmail.com:587"
    echo "   Possible issues:"
    echo "   - Firewall blocking port 587"
    echo "   - Network connectivity issue"
    echo "   - Try using port 465 with SSL instead"
fi

echo ""
echo "============================================"
echo "Email Configuration Checklist"
echo "============================================"
echo ""
echo "To configure email in Jenkins:"
echo ""
echo "1. Generate Gmail App Password:"
echo "   https://myaccount.google.com/apppasswords"
echo ""
echo "2. Configure in Jenkins:"
echo "   Jenkins → Manage Jenkins → Configure System"
echo ""
echo "3. Extended E-mail Notification:"
echo "   - SMTP server: smtp.gmail.com"
echo "   - SMTP Port: 587"
echo "   - Use SMTP Authentication: ✓"
echo "   - User Name: your-email@gmail.com"
echo "   - Password: [16-character App Password]"
echo "   - Use TLS: ✓"
echo ""
echo "4. E-mail Notification (standard):"
echo "   - SMTP server: smtp.gmail.com"
echo "   - Use SMTP Authentication: ✓"
echo "   - User Name: your-email@gmail.com"
echo "   - Password: [16-character App Password]"
echo "   - Use TLS: ✓"
echo "   - SMTP Port: 587"
echo ""
echo "5. Test configuration:"
echo "   - Click 'Test configuration by sending test e-mail'"
echo "   - Check inbox and spam folder"
echo ""
echo "============================================"
echo "Troubleshooting"
echo "============================================"
echo ""
echo "If emails still don't work:"
echo ""
echo "1. Check Jenkins logs:"
echo "   docker logs jenkins-cicd | grep -i 'mail\\|smtp'"
echo ""
echo "2. Check spam folder"
echo ""
echo "3. Try different email provider (SendGrid, Mailgun)"
echo ""
echo "4. See EMAIL_SETUP.md for detailed guide"
echo ""
echo "5. Enable debug logging:"
echo "   Manage Jenkins → System Log → Add new log recorder"
echo "   Logger: hudson.tasks.Mailer, Level: ALL"
echo ""

# Check if Email Extension Plugin is installed
echo "Checking if Email Extension Plugin is installed..."
docker exec jenkins-cicd test -d /var/jenkins_home/plugins/email-ext &>/dev/null
if [ $? -eq 0 ]; then
    echo "✅ Email Extension Plugin is installed"
else
    echo "⚠️  Email Extension Plugin may not be installed"
    echo "   Install it from: Manage Jenkins → Plugins"
fi

echo ""
echo "============================================"


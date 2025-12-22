#!/bin/bash

echo "============================================"
echo "Setting up Mailhog for Email Testing"
echo "============================================"
echo ""
echo "Mailhog is a local SMTP server that captures emails"
echo "Perfect for testing Jenkins email notifications!"
echo ""

# Check if mailhog is already running
if docker ps | grep -q mailhog; then
    echo "✅ Mailhog is already running"
    echo "   View emails at: http://localhost:8025"
else
    echo "Starting Mailhog..."
    docker run -d \
        --name mailhog \
        -p 1025:1025 \
        -p 8025:8025 \
        --restart unless-stopped \
        mailhog/mailhog

    if [ $? -eq 0 ]; then
        echo "✅ Mailhog started successfully!"
        echo ""
        echo "📧 Email Web UI: http://localhost:8025"
        echo "📮 SMTP Server: host.docker.internal:1025"
    else
        echo "❌ Failed to start Mailhog"
        exit 1
    fi
fi

echo ""
echo "============================================"
echo "Configure Jenkins to Use Mailhog"
echo "============================================"
echo ""
echo "1. Open Jenkins: http://localhost:8080"
echo ""
echo "2. Go to: Manage Jenkins → Configure System"
echo ""
echo "3. Extended E-mail Notification:"
echo "   - SMTP server: host.docker.internal"
echo "   - SMTP Port: 1025"
echo "   - ☐ Use SMTP Authentication (UNCHECK THIS!)"
echo "   - ☐ Use SSL (UNCHECK THIS!)"
echo "   - ☐ Use TLS (UNCHECK THIS!)"
echo ""
echo "4. E-mail Notification:"
echo "   - SMTP server: host.docker.internal"
echo "   - ☐ Use SMTP Authentication (UNCHECK THIS!)"
echo "   - SMTP Port: 1025"
echo ""
echo "5. Click 'Test configuration' - should work immediately!"
echo ""
echo "6. View captured emails:"
echo "   Open: http://localhost:8025"
echo ""
echo "============================================"
echo "Test It!"
echo "============================================"
echo ""
echo "1. Run a Jenkins build"
echo "2. Open http://localhost:8025 in your browser"
echo "3. You should see the email Jenkins sent!"
echo ""
echo "No spam folder, no App Passwords, no hassle! 🎉"
echo ""


#!/bin/bash
echo "============================================"
echo "Webhook URL Checker"
echo "============================================"
echo ""
# Check if Jenkins is running
if docker ps | grep -q jenkins-cicd; then
    echo "✅ Jenkins is running"
    echo ""
    # Try to get ngrok URL if available
    if command -v curl &> /dev/null; then
        echo "Checking for ngrok tunnel..."
        NGROK_URL=$(curl -s http://localhost:4040/api/tunnels 2>/dev/null | grep -o 'https://[^"]*\.ngrok-free\.app' | head -1)
        if [ -n "$NGROK_URL" ]; then
            echo "✅ Found ngrok tunnel: $NGROK_URL"
            echo ""
            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            echo "YOUR CORRECT WEBHOOK URL:"
            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
            echo ""
            echo "  $NGROK_URL/github-webhook/"
            echo ""
            echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        else
            echo "⚠️  ngrok not running or not found"
            echo ""
            echo "If using ngrok, your webhook URL should be:"
            echo "  https://your-domain.ngrok-free.app/github-webhook/"
            echo ""
            echo "If deploying locally:"
            echo "  http://localhost:8080/github-webhook/"
        fi
    fi
else
    echo "❌ Jenkins is not running"
    echo "Start with: docker-compose -f docker-compose.jenkins.yml up -d"
fi
echo ""
echo "To update your webhook:"
echo "1. Go to: https://github.com/mahdikheirkhah/buy-01/settings/hooks"
echo "2. Edit your webhook"
echo "3. Update Payload URL to the URL shown above"
echo "4. Make sure it ends with /github-webhook/"
echo "5. Save changes"
echo ""

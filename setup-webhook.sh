
#!/bin/bash

# GitHub Webhook Setup Helper Script
# This script helps you set up ngrok for GitHub webhooks

set -e

echo "🔗 GitHub Webhook Setup Helper"
echo "================================"
echo ""

# Check if Jenkins is running
if ! docker ps | grep -q jenkins-cicd; then
    echo "❌ Error: Jenkins container is not running!"
    echo "   Please start Jenkins first: docker start jenkins-cicd"
    exit 1
fi

echo "✅ Jenkins is running"
echo ""

# Get Jenkins URL
JENKINS_PORT=8080
JENKINS_URL="http://localhost:${JENKINS_PORT}"

echo "📍 Jenkins is running at: ${JENKINS_URL}"
echo ""

# Check if ngrok is installed
if ! command -v ngrok &> /dev/null; then
    echo "⚠️  ngrok is not installed"
    echo ""
    echo "To install ngrok:"
    echo "  macOS:   brew install ngrok"
    echo "  Linux:   snap install ngrok"
    echo "  Windows: Download from https://ngrok.com/download"
    echo ""
    echo "After installing, run this script again."
    exit 1
fi

echo "✅ ngrok is installed"
echo ""

# Check if ngrok is already running
if pgrep -x "ngrok" > /dev/null; then
    echo "⚠️  ngrok is already running!"
    echo ""
    echo "To get your webhook URL:"
    echo "  1. Open: http://localhost:4040"
    echo "  2. Or run: curl http://localhost:4040/api/tunnels | jq -r '.tunnels[0].public_url'"
    echo ""

    # Try to get the URL
    NGROK_URL=$(curl -s http://localhost:4040/api/tunnels 2>/dev/null | grep -o 'https://[^"]*\.ngrok\.io' | head -1)

    if [ -n "$NGROK_URL" ]; then
        echo "Your webhook URL: ${NGROK_URL}/github-webhook/"
        echo ""
        echo "📋 Next Steps:"
        echo "1. Go to: https://github.com/mahdikheirkhah/buy-01/settings/hooks"
        echo "2. Click 'Add webhook'"
        echo "3. Paste this URL: ${NGROK_URL}/github-webhook/"
        echo "4. Content type: application/json"
        echo "5. Click 'Add webhook'"
    fi

    exit 0
fi

echo "🚀 Starting ngrok tunnel..."
echo ""

# Start ngrok in the background
ngrok http ${JENKINS_PORT} > /dev/null &

NGROK_PID=$!
echo "✅ ngrok started (PID: ${NGROK_PID})"
echo ""

# Wait for ngrok to start
echo "⏳ Waiting for ngrok to initialize..."
sleep 3

# Get the public URL
NGROK_URL=$(curl -s http://localhost:4040/api/tunnels 2>/dev/null | grep -o 'https://[^"]*\.ngrok\.io' | head -1)

if [ -z "$NGROK_URL" ]; then
    echo "❌ Failed to get ngrok URL"
    echo "   Try opening http://localhost:4040 in your browser"
    exit 1
fi

echo "✅ ngrok tunnel is ready!"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🌐 Your Jenkins is now accessible at:"
echo "   ${NGROK_URL}"
echo ""
echo "🔗 Your GitHub Webhook URL:"
echo "   ${NGROK_URL}/github-webhook/"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📋 Next Steps:"
echo ""
echo "⚠️  IMPORTANT: To avoid 403 errors, configure Jenkins first!"
echo ""
echo "1. Configure Jenkins Job:"
echo "   • Open: ${JENKINS_URL}"
echo "   • Login with admin password (run: docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword)"
echo "   • Install 'GitHub plugin' if not installed (Manage Jenkins > Manage Plugins)"
echo "   • Go to: e-commerce-microservices-ci-cd > Configure"
echo "   • Build Triggers: ✅ Enable 'GitHub hook trigger for GITScm polling'"
echo "   • Save"
echo ""
echo "2. Add Webhook to GitHub:"
echo "   • Open: https://github.com/mahdikheirkhah/buy-01/settings/hooks"
echo "   • Click 'Add webhook'"
echo "   • Payload URL: ${NGROK_URL}/github-webhook/"
echo "   • Content type: application/json"
echo "   • Click 'Add webhook'"
echo ""
echo "3. Test the Webhook:"
echo "   • Make a commit: git commit -m \"test webhook\" --allow-empty"
echo "   • Push to GitHub: git push origin main"
echo "   • Watch Jenkins build automatically! 🎉"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📊 Monitor ngrok:"
echo "   Web Interface: http://localhost:4040"
echo "   Inspect traffic, see webhook requests, debug issues"
echo ""
echo "⚠️  Important Notes:"
echo "   • Free ngrok URLs change when you restart ngrok"
echo "   • Keep this terminal open to maintain the tunnel"
echo "   • Press Ctrl+C to stop ngrok"
echo ""
echo "📖 Full Guide: See WEBHOOK_SETUP.md for detailed instructions"
echo ""

# Save URL to file for easy access
echo "$NGROK_URL" > .ngrok_url
echo "💾 Webhook URL saved to .ngrok_url"
echo ""

echo "✅ Setup complete! Ready to receive webhooks from GitHub! 🚀"
echo ""

# Keep script running (ngrok runs in background)
echo "⏳ ngrok is running... Press Ctrl+C to stop"
echo ""

# Wait for user to press Ctrl+C
trap "echo ''; echo '🛑 Stopping ngrok...'; kill $NGROK_PID 2>/dev/null; rm -f .ngrok_url; echo '✅ Stopped'; exit 0" INT TERM

# Keep the script running
wait $NGROK_PID


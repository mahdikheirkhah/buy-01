#!/bin/bash

# Quick Test Script for GitHub Webhook
# Tests if your webhook is working correctly

set -e

echo "🧪 GitHub Webhook Test Script"
echo "=============================="
echo ""

# Check if we're in a git repository
if ! git rev-parse --is-inside-work-tree > /dev/null 2>&1; then
    echo "❌ Error: Not in a git repository!"
    exit 1
fi

echo "✅ Git repository detected"
echo ""

# Check current branch
CURRENT_BRANCH=$(git branch --show-current)
echo "📍 Current branch: $CURRENT_BRANCH"
echo ""

# Check if Jenkins is running
if ! docker ps | grep -q jenkins-cicd; then
    echo "❌ Warning: Jenkins container is not running!"
    echo "   Start Jenkins: docker start jenkins-cicd"
    echo ""
fi

# Check if ngrok is running
if ! pgrep -x "ngrok" > /dev/null; then
    echo "⚠️  Warning: ngrok is not running!"
    echo "   Start ngrok: ./setup-webhook.sh"
    echo ""
else
    echo "✅ ngrok is running"

    # Try to get ngrok URL
    NGROK_URL=$(curl -s http://localhost:4040/api/tunnels 2>/dev/null | grep -o 'https://[^"]*\.ngrok\.io' | head -1)

    if [ -n "$NGROK_URL" ]; then
        echo "🌐 Webhook URL: ${NGROK_URL}/github-webhook/"
        echo ""
    fi
fi

# Offer to make a test commit
echo "🧪 Ready to test webhook?"
echo ""
echo "This will:"
echo "  1. Create an empty commit"
echo "  2. Push to origin/$CURRENT_BRANCH"
echo "  3. Trigger Jenkins build automatically"
echo ""

read -p "Continue? (y/n) " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Test cancelled."
    exit 0
fi

echo ""
echo "🚀 Creating test commit..."

# Create test commit
git commit --allow-empty -m "test: webhook trigger from test script"

echo "✅ Commit created"
echo ""

# Check if there are any changes to push
if ! git status | grep -q "Your branch is ahead"; then
    echo "⚠️  No commits to push"
    exit 0
fi

echo "📤 Pushing to origin/$CURRENT_BRANCH..."
git push origin "$CURRENT_BRANCH"

echo ""
echo "✅ Push completed!"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🎯 What to check now:"
echo ""
echo "1. GitHub Webhook Delivery:"
echo "   https://github.com/mahdikheirkhah/buy-01/settings/hooks"
echo "   → Click your webhook → Recent Deliveries"
echo "   → Should show ✅ green checkmark"
echo ""
echo "2. ngrok Activity:"
echo "   http://localhost:4040"
echo "   → Should see POST request to /github-webhook/"
echo ""
echo "3. Jenkins Build:"
echo "   http://localhost:8080/job/e-commerce-microservices-ci-cd/"
echo "   → Should see new build starting"
echo "   → Build log should show: 'Started by GitHub push'"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Wait a bit
echo "⏳ Waiting 5 seconds for webhook to trigger..."
sleep 5

# Check Jenkins
echo ""
echo "🔍 Checking Jenkins..."

JENKINS_URL="http://localhost:8080"
BUILD_URL="${JENKINS_URL}/job/e-commerce-microservices-ci-cd/lastBuild/api/json"

if curl -s "${BUILD_URL}" > /dev/null 2>&1; then
    echo "✅ Jenkins is accessible"

    # Try to get build info
    BUILD_INFO=$(curl -s "${BUILD_URL}" 2>/dev/null || echo "")

    if [ -n "$BUILD_INFO" ]; then
        BUILD_NUMBER=$(echo "$BUILD_INFO" | grep -o '"number":[0-9]*' | grep -o '[0-9]*' | head -1)

        if [ -n "$BUILD_NUMBER" ]; then
            echo "🏗️  Latest build: #${BUILD_NUMBER}"
            echo "   View: ${JENKINS_URL}/job/e-commerce-microservices-ci-cd/${BUILD_NUMBER}/console"
        fi
    fi
else
    echo "⚠️  Cannot access Jenkins API (might need authentication)"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "✅ Test complete!"
echo ""
echo "If the build started automatically, your webhook is working! 🎉"
echo ""
echo "If not, check:"
echo "  • GitHub webhook delivery status"
echo "  • ngrok is running and forwarding correctly"
echo "  • Jenkins job has 'GitHub hook trigger' enabled"
echo ""
echo "📖 Full troubleshooting guide: WEBHOOK_SETUP.md"
echo ""


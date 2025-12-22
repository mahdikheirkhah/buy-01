#!/bin/bash

# GitHub Webhook CSRF Fix Script
# This script helps fix the "403 No valid crumb" error

set -e

echo "============================================"
echo "GitHub Webhook CSRF Error Fix"
echo "============================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if Jenkins is running
if ! docker ps | grep -q jenkins-cicd; then
    echo -e "${RED}❌ ERROR: Jenkins container is not running${NC}"
    echo "Start Jenkins with: docker-compose -f docker-compose.jenkins.yml up -d"
    exit 1
fi

echo -e "${GREEN}✅ Jenkins container is running${NC}"
echo ""

# Get Jenkins URL
JENKINS_URL="http://localhost:8080"
echo -e "${BLUE}Jenkins URL: ${JENKINS_URL}${NC}"
echo ""

# Menu
echo "Choose a fix option:"
echo ""
echo "1) ${GREEN}Fix GitHub Webhook URL${NC} (Easiest - Recommended)"
echo "   Just update your GitHub webhook URL to: ${JENKINS_URL}/github-webhook/"
echo ""
echo "2) ${GREEN}Enable CSRF Proxy Compatibility${NC} (Automated)"
echo "   Configure Jenkins to allow webhooks with CSRF enabled"
echo ""
echo "3) ${YELLOW}Temporarily Disable CSRF${NC} (Testing only - NOT recommended)"
echo "   Disable CSRF protection (remember to re-enable it!)"
echo ""
echo "4) ${BLUE}Show Webhook Configuration Instructions${NC}"
echo ""
echo "5) ${BLUE}Test Current Webhook Configuration${NC}"
echo ""
echo "6) Exit"
echo ""
read -p "Select option (1-6): " option

case $option in
    1)
        echo ""
        echo -e "${BLUE}=== SOLUTION 1: Fix GitHub Webhook URL ===${NC}"
        echo ""
        echo "Follow these steps:"
        echo ""
        echo "1. Go to your GitHub repository"
        echo "2. Click: Settings → Webhooks → Edit your webhook"
        echo "3. Change Payload URL to:"
        echo ""
        echo -e "   ${GREEN}${JENKINS_URL}/github-webhook/${NC}"
        echo ""
        echo "   ${RED}Important: Include the trailing slash!${NC}"
        echo ""
        echo "4. Content type: application/json"
        echo "5. Events: Just the push event"
        echo "6. Active: ✅ Checked"
        echo "7. Click 'Update webhook'"
        echo ""
        echo "8. Test by clicking 'Recent Deliveries' → 'Redeliver'"
        echo "   You should see: ✅ 200 OK (not 403)"
        echo ""
        read -p "Press Enter after updating webhook URL..."
        echo ""
        echo -e "${GREEN}✅ If you updated the URL, test it by pushing to GitHub${NC}"
        ;;

    2)
        echo ""
        echo -e "${BLUE}=== SOLUTION 2: Configure CSRF for Webhooks ===${NC}"
        echo ""
        echo "This will enable CSRF proxy compatibility in Jenkins..."
        echo ""

        # Create Groovy script
        cat > /tmp/jenkins-csrf-fix.groovy << 'EOF'
import jenkins.model.Jenkins
import hudson.security.csrf.DefaultCrumbIssuer

def jenkins = Jenkins.instance
def crumbIssuer = jenkins.getCrumbIssuer()

if (crumbIssuer instanceof DefaultCrumbIssuer) {
    // Enable proxy compatibility
    crumbIssuer.setExcludeClientIPFromCrumb(true)
    jenkins.setCrumbIssuer(crumbIssuer)
    jenkins.save()
    println "✅ SUCCESS: CSRF configured with proxy compatibility"
    println "GitHub webhook should now work at: /github-webhook/"
} else if (crumbIssuer == null) {
    // No crumb issuer, create one with proxy compatibility
    def newCrumbIssuer = new DefaultCrumbIssuer(true)
    jenkins.setCrumbIssuer(newCrumbIssuer)
    jenkins.save()
    println "✅ SUCCESS: CSRF enabled with proxy compatibility"
} else {
    println "⚠️  WARNING: Unexpected crumb issuer type: ${crumbIssuer.getClass().getName()}"
    println "Manual configuration may be needed"
}
EOF

        echo "Applying CSRF fix to Jenkins..."
        docker exec jenkins-cicd bash -c "cat > /tmp/csrf-fix.groovy" < /tmp/jenkins-csrf-fix.groovy
        docker exec jenkins-cicd java -jar /var/jenkins_home/war/WEB-INF/jenkins-cli.jar -s http://localhost:8080/ groovy = < /tmp/jenkins-csrf-fix.groovy 2>&1 || {
            echo ""
            echo -e "${YELLOW}⚠️  Automated script failed. Manual configuration needed:${NC}"
            echo ""
            echo "1. Go to: ${JENKINS_URL}"
            echo "2. Navigate to: Manage Jenkins → Security → Configure Global Security"
            echo "3. Under 'CSRF Protection', check: ✅ Enable proxy compatibility"
            echo "4. Click 'Save'"
            echo ""
            echo "Or run this in Jenkins Script Console (Manage Jenkins → Script Console):"
            echo ""
            cat /tmp/jenkins-csrf-fix.groovy
        }

        rm -f /tmp/jenkins-csrf-fix.groovy
        echo ""
        echo -e "${GREEN}✅ Configuration applied${NC}"
        echo ""
        echo "Now update your GitHub webhook URL to:"
        echo -e "   ${GREEN}${JENKINS_URL}/github-webhook/${NC}"
        ;;

    3)
        echo ""
        echo -e "${RED}=== WARNING: Disabling CSRF Protection ===${NC}"
        echo ""
        echo -e "${YELLOW}⚠️  This is NOT recommended for production!${NC}"
        echo -e "${YELLOW}⚠️  Only use for testing!${NC}"
        echo ""
        read -p "Are you sure? (yes/no): " confirm

        if [ "$confirm" = "yes" ]; then
            cat > /tmp/jenkins-disable-csrf.groovy << 'EOF'
import jenkins.model.Jenkins

def jenkins = Jenkins.instance
jenkins.setCrumbIssuer(null)
jenkins.save()

println "⚠️  WARNING: CSRF protection DISABLED"
println "Remember to re-enable it after testing!"
EOF

            echo "Disabling CSRF..."
            docker exec jenkins-cicd bash -c "cat > /tmp/disable-csrf.groovy" < /tmp/jenkins-disable-csrf.groovy
            docker exec jenkins-cicd java -jar /var/jenkins_home/war/WEB-INF/jenkins-cli.jar -s http://localhost:8080/ groovy = < /tmp/jenkins-disable-csrf.groovy 2>&1 || {
                echo ""
                echo -e "${YELLOW}Automated script failed. Manual steps:${NC}"
                echo "1. Go to: ${JENKINS_URL}"
                echo "2. Manage Jenkins → Security → Configure Global Security"
                echo "3. Uncheck: 'Prevent Cross Site Request Forgery exploits'"
                echo "4. Save"
            }

            rm -f /tmp/jenkins-disable-csrf.groovy
            echo ""
            echo -e "${RED}⚠️  CSRF DISABLED - Remember to re-enable it!${NC}"
            echo ""
            echo "To re-enable CSRF, run this script again and choose option 2"
        else
            echo "Cancelled."
        fi
        ;;

    4)
        echo ""
        echo -e "${BLUE}=== GitHub Webhook Configuration ===${NC}"
        echo ""
        echo "Step-by-step instructions:"
        echo ""
        echo "1. Open your GitHub repository"
        echo "2. Go to: Settings → Webhooks"
        echo "3. Click 'Add webhook' or edit existing one"
        echo ""
        echo "Configuration:"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "Payload URL:"
        echo -e "  ${GREEN}${JENKINS_URL}/github-webhook/${NC}"
        echo ""
        echo "Content type:"
        echo "  application/json"
        echo ""
        echo "Secret:"
        echo "  (leave empty or add if you prefer)"
        echo ""
        echo "SSL verification:"
        echo "  ✅ Enable SSL verification"
        echo ""
        echo "Events:"
        echo "  ⚪ Just the push event"
        echo ""
        echo "Active:"
        echo "  ✅ Active"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo ""
        echo "4. Click 'Add webhook' or 'Update webhook'"
        echo "5. Test: Recent Deliveries → Redeliver"
        echo ""
        echo "Expected result: ✅ 200 OK"
        ;;

    5)
        echo ""
        echo -e "${BLUE}=== Testing Webhook Configuration ===${NC}"
        echo ""

        # Test local Jenkins webhook endpoint
        echo "Testing Jenkins webhook endpoint..."
        response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/github-webhook/ 2>&1 || echo "000")

        if [ "$response" = "200" ] || [ "$response" = "405" ]; then
            echo -e "${GREEN}✅ Jenkins webhook endpoint is accessible${NC}"
            echo "   Response code: $response"
        elif [ "$response" = "403" ]; then
            echo -e "${RED}❌ Still getting 403 Forbidden${NC}"
            echo "   CSRF protection is blocking webhooks"
            echo "   → Run option 2 to fix CSRF configuration"
        else
            echo -e "${YELLOW}⚠️  Unexpected response: $response${NC}"
        fi
        echo ""

        # Check if GitHub plugin is installed
        echo "Checking GitHub plugin..."
        if docker exec jenkins-cicd test -d /var/jenkins_home/plugins/github 2>/dev/null; then
            echo -e "${GREEN}✅ GitHub plugin is installed${NC}"
        else
            echo -e "${RED}❌ GitHub plugin is NOT installed${NC}"
            echo "   Install it: Manage Jenkins → Plugins → Available → GitHub"
        fi
        echo ""

        echo "Next steps:"
        echo "1. Make sure your GitHub webhook URL is: ${JENKINS_URL}/github-webhook/"
        echo "2. Check 'Recent Deliveries' in GitHub webhook settings"
        echo "3. Push a commit to test: git push origin main"
        ;;

    6)
        echo "Exiting..."
        exit 0
        ;;

    *)
        echo -e "${RED}Invalid option${NC}"
        exit 1
        ;;
esac

echo ""
echo "============================================"
echo "Next Steps:"
echo "============================================"
echo ""
echo "1. Update GitHub webhook URL to:"
echo -e "   ${GREEN}${JENKINS_URL}/github-webhook/${NC}"
echo ""
echo "2. Test webhook:"
echo "   • GitHub → Settings → Webhooks → Recent Deliveries"
echo "   • Click 'Redeliver' on latest delivery"
echo "   • Should see: ✅ 200 OK"
echo ""
echo "3. Test with a real push:"
echo "   cd $(pwd)"
echo "   git push origin main"
echo ""
echo "4. Check Jenkins:"
echo "   • Build should trigger automatically"
echo "   • Build log should show: 'Started by GitHub push'"
echo ""
echo "For detailed documentation, see:"
echo "   • WEBHOOK_CSRF_FIX.md"
echo "   • WEBHOOK_SETUP.md"
echo ""
echo "============================================"


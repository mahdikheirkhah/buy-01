# GitHub Webhook Setup Guide

This guide will help you set up automatic builds in Jenkins when you push code to GitHub.

## Prerequisites

- Jenkins is running and accessible
- You have admin access to your GitHub repository
- Jenkins is accessible from the internet (or use ngrok for local testing)

## Step 1: Get Jenkins URL

### For Local Development with ngrok:

```bash
# Install ngrok (macOS)
brew install ngrok

# Start ngrok tunnel to Jenkins
ngrok http 8080

# Note the public URL (e.g., https://xxxx-xx-xx-xx-xx.ngrok.io)
```

### For Production:
Use your actual Jenkins URL (e.g., `http://jenkins.yourcompany.com`)

## Step 2: Configure Jenkins

### 2.1 Install Required Plugins

1. Go to Jenkins → Manage Jenkins → Plugin Manager
2. Install these plugins if not already installed:
   - GitHub Plugin
   - GitHub Integration Plugin
   - Git Plugin

### 2.2 Configure GitHub Plugin

1. Go to: Manage Jenkins → System
2. Scroll to "GitHub" section
3. Click "Add GitHub Server"
4. Configure:
   - Name: `GitHub`
   - API URL: `https://api.github.com`
   - Credentials: Add GitHub Personal Access Token (see below)

### 2.3 Create GitHub Personal Access Token

1. Go to GitHub → Settings → Developer settings → Personal access tokens
2. Generate new token (classic)
3. Select scopes:
   - ✅ `repo` (all)
   - ✅ `admin:repo_hook` (all)
4. Copy the token (you won't see it again!)
5. Add to Jenkins:
   - Manage Jenkins → Credentials → System → Global credentials
   - Add Credentials → Kind: Secret text
   - Secret: Paste your token
   - ID: `github-token`
   - Description: `GitHub API Token`

### 2.4 Configure Your Pipeline Job

1. Go to your pipeline job → Configure
2. Under "Build Triggers", enable:
   - ✅ GitHub hook trigger for GITScm polling
3. Save

## Step 3: Configure GitHub Webhook

### 3.1 Access GitHub Repository Settings

1. Go to your repository: `https://github.com/mahdikheirkhah/buy-01`
2. Click "Settings" → "Webhooks"
3. Click "Add webhook"

### 3.2 Configure Webhook

Fill in the webhook form:

**Payload URL**:
```
http://your-jenkins-url:8080/github-webhook/
```
Examples:
- Local with ngrok: `https://xxxx-xx-xx-xx-xx.ngrok.io/github-webhook/`
- Production: `http://jenkins.yourcompany.com/github-webhook/`

**Content type**:
- Select: `application/json`

**Secret**: (Optional but recommended)
- Leave empty for now, or use Jenkins API token

**SSL verification**:
- For ngrok or valid SSL: Enable SSL verification
- For HTTP or self-signed: Disable SSL verification (not recommended for production)

**Which events would you like to trigger this webhook?**
- Select: "Just the push event"
- ✅ Active (make sure this is checked)

Click "Add webhook"

## Step 4: Test the Webhook

### 4.1 View Webhook Delivery

1. In GitHub webhook settings, click on your webhook
2. Go to "Recent Deliveries" tab
3. Click on a delivery to see request/response

### 4.2 Test with a Commit

```bash
# Make a simple change
echo "# Test webhook" >> README.md

# Commit and push
git add README.md
git commit -m "test: trigger webhook"
git push origin main

# Check Jenkins for automatic build
```

### 4.3 Verify in Jenkins

1. Go to your Jenkins job
2. You should see a new build starting automatically
3. Check build console output for:
   ```
   Started by GitHub push by username
   ```

## Step 5: Troubleshooting

### Webhook Not Triggering

**Check GitHub Webhook Delivery**:
1. Go to webhook settings → Recent Deliveries
2. Look for errors:
   - ❌ Red X: Delivery failed
   - ✅ Green check: Delivery successful

**Common Issues**:

1. **Connection timeout**:
   ```
   We couldn't deliver this payload: Timeout
   ```
   - Solution: Jenkins is not accessible from internet
   - Use ngrok or make Jenkins publicly accessible

2. **404 Not Found**:
   ```
   Response: 404 Not Found
   ```
   - Solution: Wrong URL, should end with `/github-webhook/`
   - Check: `http://your-jenkins-url:8080/github-webhook/`

3. **403 Forbidden**:
   ```
   Response: 403 No valid crumb was included
   ```
   - Solution: Disable CSRF for webhook endpoint
   - Or: Configure GitHub plugin to handle CSRF

### Fix CSRF Issues

**Option 1: Disable CSRF for webhook** (Quick fix)
1. Manage Jenkins → Security
2. Under "CSRF Protection", add exception:
   ```
   /github-webhook/
   ```

**Option 2: Use proper authentication** (Recommended)
1. Create Jenkins API token:
   ```
   Jenkins → User → Configure → API Token → Add new Token
   ```
2. Use token in webhook URL:
   ```
   http://username:api-token@jenkins-url:8080/github-webhook/
   ```

### Jenkins Not Building

**Check Job Configuration**:
1. Job → Configure → Build Triggers
2. Ensure "GitHub hook trigger for GITScm polling" is enabled

**Check Jenkins Logs**:
```bash
# For Docker Jenkins
docker logs jenkins-cicd -f

# Look for webhook received messages
```

**Manual Test**:
```bash
# Test webhook manually
curl -X POST http://localhost:8080/github-webhook/ \
  -H "Content-Type: application/json" \
  -d '{"repository":{"url":"https://github.com/mahdikheirkhah/buy-01"}}'
```

## Step 6: Advanced Configuration

### 6.1 Branch-Specific Webhooks

To build only specific branches:

1. In Jenkinsfile, use `when` condition:
   ```groovy
   when {
       branch 'main'
   }
   ```

2. Or in job configuration:
   - "Branches to build" → Specify: `main`

### 6.2 Multiple Webhooks

You can configure different webhooks for:
- Push events (automatic builds)
- Pull requests (preview deployments)
- Release events (production deployments)

### 6.3 Webhook Security

**Best Practices**:
1. Use HTTPS for webhook URL
2. Set webhook secret
3. Verify webhook signatures in Jenkins
4. Limit webhook IP addresses

**Configure Webhook Secret**:
```groovy
// In Jenkinsfile
properties([
    githubProjectProperty(displayName: '', projectUrlStr: 'https://github.com/mahdikheirkhah/buy-01/')
])
```

### 6.4 Webhook Payload Processing

Create a webhook receiver script:

```groovy
// In Jenkinsfile
stage('Process Webhook') {
    steps {
        script {
            echo "Webhook received from: ${env.GIT_BRANCH}"
            echo "Commit SHA: ${env.GIT_COMMIT}"
            echo "Commit message: ${env.GIT_COMMIT_MSG}"
        }
    }
}
```

## Step 7: Alternative: Polling (Fallback)

If webhooks don't work, use Git polling:

1. Job → Configure → Build Triggers
2. Enable "Poll SCM"
3. Schedule: `H/5 * * * *` (every 5 minutes)

**Note**: Polling is less efficient than webhooks.

## Verification Checklist

- [ ] Jenkins is accessible from internet
- [ ] GitHub webhook is created
- [ ] Webhook shows recent successful deliveries
- [ ] Jenkins job has "GitHub hook trigger" enabled
- [ ] Test commit triggers automatic build
- [ ] Build console shows "Started by GitHub push"

## Common Webhook Payloads

### Push Event
```json
{
  "ref": "refs/heads/main",
  "repository": {
    "name": "buy-01",
    "url": "https://github.com/mahdikheirkhah/buy-01"
  },
  "pusher": {
    "name": "mahdikheirkhah"
  },
  "commits": [
    {
      "message": "feat: add new feature",
      "url": "https://github.com/mahdikheirkhah/buy-01/commit/abc123"
    }
  ]
}
```

## Next Steps

After webhook is working:
1. Configure branch protection rules
2. Set up pull request builds
3. Add build status badges to README
4. Configure notifications (Slack, email)

## Support

If issues persist:
1. Check GitHub webhook deliveries
2. Check Jenkins system logs
3. Verify network connectivity
4. Review Jenkins security settings

## Resources

- [Jenkins GitHub Plugin](https://plugins.jenkins.io/github/)
- [GitHub Webhooks Documentation](https://docs.github.com/en/webhooks)
- [ngrok Documentation](https://ngrok.com/docs)

---

**Last Updated**: December 20, 2025


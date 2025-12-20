# GitHub Webhook Setup Guide

This guide helps you set up automatic Jenkins builds triggered by GitHub pushes.

## The 403 Error Explained

The "403 No valid crumb" error occurs because Jenkins has CSRF (Cross-Site Request Forgery) protection enabled. GitHub webhooks need special configuration to work with this security feature.

## Quick Setup (Recommended)

### Option 1: Configure GitHub Plugin in Jenkins

1. **Install GitHub Plugin**
   - Go to Jenkins → Manage Jenkins → Manage Plugins
   - Search for "GitHub plugin"
   - Install if not already installed

2. **Configure Jenkins System**
   - Go to Jenkins → Manage Jenkins → Configure System
   - Scroll to "GitHub" section
   - Click "Add GitHub Server" → "GitHub Server"
   - Leave "API URL" as default: `https://api.github.com`
   - Save

3. **Configure Your Job**
   - Open your job: `e-commerce-microservices-ci-cd`
   - Click "Configure"
   - Under "Build Triggers":
     - ✅ Check "GitHub hook trigger for GITScm polling"
   - Under "Source Code Management" → Git:
     - Repository URL: `https://github.com/mahdikheirkhah/buy-01.git`
   - Save

4. **Add Webhook to GitHub**
   - Go to: https://github.com/mahdikheirkhah/buy-01/settings/hooks
   - Click "Add webhook"
   - Payload URL: `https://your-ngrok-url/github-webhook/`
   - Content type: `application/json`
   - Secret: Leave empty (or add if you configured one)
   - Which events: `Just the push event`
   - Active: ✅
   - Click "Add webhook"

### Option 2: Disable CSRF for Webhooks (Less Secure)

⚠️ **Warning**: This makes Jenkins less secure. Use only for development/testing.

1. **Add CSRF Exception**
   - Go to Jenkins → Manage Jenkins → Configure Global Security
   - Under "CSRF Protection":
     - Uncheck "Prevent Cross Site Request Forgery exploits" (NOT RECOMMENDED)
     - OR keep it checked and add to "Crumb Issuer" exceptions
   - Save

2. **Add GitHub webhook URL to whitelist**
   - Add this to Jenkins startup: `-Dhudson.security.csrf.GlobalCrumbIssuerConfiguration.DISABLE_CSRF_PROTECTION=true`

## Using ngrok for Local Development

### Step 1: Start ngrok

```bash
# Run the helper script
chmod +x setup-webhook.sh
./setup-webhook.sh
```

Or manually:
```bash
ngrok http 8080
```

### Step 2: Get Your Webhook URL

ngrok will show:
```
Forwarding   https://abc123.ngrok.io -> http://localhost:8080
```

Your webhook URL is: `https://abc123.ngrok.io/github-webhook/`

### Step 3: Add to GitHub

1. Go to: https://github.com/mahdikheirkhah/buy-01/settings/hooks
2. Click "Add webhook"
3. Paste the ngrok URL: `https://abc123.ngrok.io/github-webhook/`
4. Content type: `application/json`
5. Click "Add webhook"

## Testing the Webhook

### Method 1: Make a Real Commit
```bash
git commit -m "test webhook" --allow-empty
git push origin main
```

### Method 2: Redeliver from GitHub
1. Go to webhook settings on GitHub
2. Click on your webhook
3. Scroll down to "Recent Deliveries"
4. Click "Redeliver"

### Method 3: Manual Trigger
```bash
# Get your ngrok URL
NGROK_URL="https://your-ngrok-url.ngrok.io"

# Trigger webhook manually
curl -X POST "${NGROK_URL}/github-webhook/" \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Event: push" \
  -d '{
    "ref": "refs/heads/main",
    "repository": {
      "url": "https://github.com/mahdikheirkhah/buy-01"
    }
  }'
```

## Troubleshooting

### ❌ 403 Error: "No valid crumb"

**Problem**: Jenkins CSRF protection blocking webhook

**Solutions**:
1. ✅ Use GitHub Plugin (recommended - see Option 1 above)
2. Add GitHub webhook to CSRF whitelist
3. Disable CSRF (not recommended for production)

### ❌ 404 Error: "Not Found"

**Problem**: Wrong webhook URL

**Check**:
- URL should end with `/github-webhook/` (note the trailing slash)
- Correct format: `https://your-url/github-webhook/`

### ❌ Build Not Triggering

**Check**:
1. Jenkins job has "GitHub hook trigger" enabled
2. Repository URL in Jenkins matches GitHub repo
3. Webhook shows successful delivery in GitHub
4. Check Jenkins logs: `/var/jenkins_home/logs/`

### ❌ ngrok "ERR_NGROK_108"

**Problem**: ngrok session expired (free tier)

**Solution**:
```bash
# Stop ngrok
killall ngrok

# Restart
./setup-webhook.sh
```

Then update the webhook URL in GitHub with the new ngrok URL.

## Verifying Webhook Works

### Check GitHub Delivery
1. Go to webhook settings
2. Click on your webhook
3. Check "Recent Deliveries"
4. Should show green checkmark ✅ and response code 200

### Check Jenkins Logs
```bash
# View Jenkins logs
docker logs jenkins-cicd -f

# Should see lines like:
# "Received PushEvent for https://github.com/mahdikheirkhah/buy-01"
# "Scheduling build for job e-commerce-microservices-ci-cd"
```

### Check ngrok Dashboard
1. Open: http://localhost:4040
2. Click on the webhook request
3. Check request/response details

## Alternative: Use Polling (No Webhook Needed)

If webhooks are too complex, use polling instead:

1. Edit Jenkinsfile, replace triggers:
```groovy
triggers {
    pollSCM('H/5 * * * *')  // Poll every 5 minutes
}
```

2. Or configure in Jenkins UI:
   - Job → Configure
   - Build Triggers → Poll SCM
   - Schedule: `H/5 * * * *`

## Production Setup (No ngrok)

For production with a public Jenkins server:

1. **Jenkins on public IP/domain**:
   - Webhook URL: `https://jenkins.yourcompany.com/github-webhook/`
   - No ngrok needed

2. **Add GitHub webhook**:
   - Use your actual Jenkins URL
   - Add webhook secret for security

3. **Configure Jenkins Security**:
   - Enable HTTPS
   - Use GitHub plugin with OAuth
   - Set up proper authentication

## Getting Jenkins Password

The admin password was shown when you first started Jenkins. To retrieve it:

```bash
# Get initial admin password
docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword
```

Or view logs:
```bash
docker logs jenkins-cicd | grep -A 5 "password"
```

## Summary

✅ **Recommended for Development**:
- Use ngrok + GitHub Plugin
- Enable "GitHub hook trigger for GITScm polling"
- Add webhook with ngrok URL

✅ **Recommended for Production**:
- Public Jenkins server with HTTPS
- GitHub Plugin with OAuth
- Webhook secret for security

⚠️ **Avoid**:
- Disabling CSRF protection
- Using ngrok URLs in production
- Exposing Jenkins without authentication

## Need Help?

Check these logs:
```bash
# Jenkins logs
docker logs jenkins-cicd -f

# ngrok requests
open http://localhost:4040

# GitHub webhook deliveries
https://github.com/mahdikheirkhah/buy-01/settings/hooks
```

---

## Quick Reference

| Task | Command |
|------|---------|
| Start ngrok | `./setup-webhook.sh` |
| Get Jenkins password | `docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword` |
| View Jenkins logs | `docker logs jenkins-cicd -f` |
| ngrok dashboard | http://localhost:4040 |
| Jenkins URL | http://localhost:8080 |
| GitHub webhooks | https://github.com/mahdikheirkhah/buy-01/settings/hooks |


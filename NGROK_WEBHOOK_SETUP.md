# 🌐 GitHub Webhooks with ngrok for Local Jenkins

This guide explains how to set up GitHub webhooks to trigger your **local Jenkins** instance using **ngrok**.

## 📋 Table of Contents

- [Why ngrok?](#why-ngrok)
- [Prerequisites](#prerequisites)
- [Quick Setup](#quick-setup)
- [Step-by-Step Setup](#step-by-step-setup)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Free vs Paid ngrok](#free-vs-paid-ngrok)

---

## Why ngrok?

When Jenkins runs **locally** (on your laptop/desktop):
- It's not accessible from the internet
- GitHub webhooks can't reach it
- You need a **tunnel** to expose it publicly

**ngrok** creates a secure tunnel from the internet to your local Jenkins.

```
GitHub → ngrok tunnel → Your Local Jenkins
```

---

## Prerequisites

✅ **Required:**
- Jenkins running locally (via Docker)
- Docker installed
- GitHub repository
- ngrok account (free tier works!)

---

## Quick Setup

```bash
# 1. Install ngrok (macOS)
brew install ngrok

# 2. Set up ngrok auth token (one-time)
ngrok config add-authtoken YOUR_NGROK_TOKEN

# 3. Start Jenkins (if not running)
docker-compose -f docker-compose.jenkins.yml up -d

# 4. Start ngrok tunnel
ngrok http 8080

# 5. Use the ngrok URL in GitHub webhook
# Copy the "Forwarding" URL (e.g., https://abc123.ngrok-free.dev)
```

---

## Step-by-Step Setup

### 1. Install ngrok

**macOS:**
```bash
brew install ngrok
```

**Linux:**
```bash
curl -s https://ngrok-agent.s3.amazonaws.com/ngrok.asc | \
  sudo tee /etc/apt/trusted.gpg.d/ngrok.asc >/dev/null && \
  echo "deb https://ngrok-agent.s3.amazonaws.com buster main" | \
  sudo tee /etc/apt/sources.list.d/ngrok.list && \
  sudo apt update && sudo apt install ngrok
```

**Windows:**
- Download from: https://ngrok.com/download
- Extract and add to PATH

### 2. Get ngrok Auth Token

1. Sign up at: https://dashboard.ngrok.com/signup
2. Go to: https://dashboard.ngrok.com/get-started/your-authtoken
3. Copy your auth token

### 3. Configure ngrok (One-time)

```bash
ngrok config add-authtoken YOUR_TOKEN_HERE
```

### 4. Start Jenkins

```bash
# Make sure Jenkins is running
docker-compose -f docker-compose.jenkins.yml up -d

# Verify it's accessible
curl http://localhost:8080
```

### 5. Start ngrok Tunnel

```bash
ngrok http 8080
```

You'll see output like:
```
ngrok                                                                                           

Session Status                online
Account                       Your Name (Plan: Free)
Version                       3.x.x
Region                        United States (us)
Latency                       -
Web Interface                 http://127.0.0.1:4040
Forwarding                    https://abc123xyz.ngrok-free.dev -> http://localhost:8080

Connections                   ttl     opn     rt1     rt5     p50     p90
                              0       0       0.00    0.00    0.00    0.00
```

**Important:** Copy the `Forwarding` URL (e.g., `https://abc123xyz.ngrok-free.dev`)

⚠️ **Keep this terminal window open!** If you close it, the tunnel stops.

### 6. Configure GitHub Webhook

1. Go to your GitHub repository
2. Click: **Settings** → **Webhooks** → **Add webhook** (or Edit existing)

3. **Configure:**
   ```
   Payload URL:    https://abc123xyz.ngrok-free.dev/github-webhook/
   Content type:   application/json
   Secret:         (optional - leave empty or add one)
   SSL:            ✅ Enable SSL verification
   Events:         ⚪ Just the push event
   Active:         ✅ Active
   ```

   ⚠️ **Critical:** The URL MUST end with `/github-webhook/`
   
   ✅ Correct: `https://abc123xyz.ngrok-free.dev/github-webhook/`
   ❌ Wrong: `https://abc123xyz.ngrok-free.dev/`

4. Click **Add webhook** or **Update webhook**

### 7. Configure Jenkins Job

In your Jenkins job configuration:

1. **Source Code Management:**
   - Git: `https://github.com/YOUR_USERNAME/YOUR_REPO.git`

2. **Build Triggers:**
   - ✅ Check: **GitHub hook trigger for GITScm polling**

3. Save the job

---

## Testing

### Test 1: Check ngrok Dashboard

1. Open in browser: http://localhost:4040
2. This shows all HTTP requests to your ngrok tunnel
3. You should see webhook requests from GitHub here

### Test 2: Redeliver Webhook

1. GitHub → Settings → Webhooks → Your webhook
2. Scroll to **Recent Deliveries**
3. Click the latest delivery → **Redeliver**
4. Check response: Should be **✅ 200 OK** (not 403)

### Test 3: Push a Commit

```bash
# Make a test commit
git commit --allow-empty -m "Test webhook trigger"
git push origin main
```

**Expected Result:**
- ✅ Jenkins job starts automatically
- ✅ Build log shows: "Started by GitHub push"
- ✅ Build completes successfully

### Automated Test Script

```bash
# Run the test script
./test-ngrok-webhook.sh

# Enter your ngrok URL when prompted
# Example: https://abc123xyz.ngrok-free.dev
```

---

## Troubleshooting

### Problem: Getting 403 Forbidden

**Symptom:** GitHub webhook shows "403 No valid crumb was included"

**Solution:**
```bash
./fix-webhook-csrf.sh
# Choose option 2: Enable CSRF Proxy Compatibility
```

Or manually:
1. Jenkins → Manage Jenkins → Security → Configure Global Security
2. Under **CSRF Protection**: ✅ Enable proxy compatibility
3. Save

### Problem: Getting 404 Not Found

**Symptom:** GitHub webhook shows "404 Not Found"

**Causes:**
1. GitHub plugin not installed
2. Wrong webhook URL

**Solutions:**
```bash
# Check if plugin is installed
docker exec jenkins-cicd test -d /var/jenkins_home/plugins/github && echo "Installed" || echo "Not installed"

# If not installed:
# Jenkins → Manage Jenkins → Plugins → Available → Search "GitHub Integration" → Install
```

Make sure URL ends with `/github-webhook/`

### Problem: ngrok URL Changes Every Time

**Symptom:** You have to update GitHub webhook URL every time you restart ngrok

**Cause:** Free ngrok gives you a random URL each time

**Solutions:**

**Option 1 - Use ngrok Pro (Paid):**
- Get a **permanent subdomain**: `https://yourname.ngrok.io`
- Set up once, never change

**Option 2 - Use GitHub Webhook Secret:**
- Even with changing URLs, use a secret for security
- Update URL each time you restart ngrok

**Option 3 - Keep ngrok Running:**
```bash
# Run ngrok in the background
nohup ngrok http 8080 > ngrok.log 2>&1 &

# Get the URL
curl -s http://localhost:4040/api/tunnels | jq -r '.tunnels[0].public_url'
```

**Option 4 - Create Update Script:**
```bash
#!/bin/bash
# auto-update-webhook.sh
NGROK_URL=$(curl -s http://localhost:4040/api/tunnels | jq -r '.tunnels[0].public_url')
echo "New ngrok URL: $NGROK_URL/github-webhook/"
echo "Update this in GitHub webhook settings"
```

### Problem: Jenkins Build Not Triggering

**Checklist:**
- ✅ ngrok tunnel is running
- ✅ Jenkins is running
- ✅ GitHub plugin installed in Jenkins
- ✅ Job configured with "GitHub hook trigger"
- ✅ Webhook URL correct: `https://xxx.ngrok-free.dev/github-webhook/`
- ✅ Webhook active and delivering successfully (200 OK)

**Debug:**
```bash
# 1. Check ngrok is running
curl -s http://localhost:4040/api/tunnels | jq

# 2. Check Jenkins webhook endpoint
curl -I http://localhost:8080/github-webhook/

# 3. Check ngrok dashboard for requests
open http://localhost:4040

# 4. Check Jenkins logs
docker logs jenkins-cicd -f | grep -i github
```

### Problem: ngrok Warning Page

**Symptom:** Browser shows ngrok warning page before reaching Jenkins

**Cause:** Free ngrok shows a warning page on first visit

**Solution:**
- This doesn't affect webhooks
- Webhooks bypass the warning page
- Or upgrade to ngrok paid plan

---

## Free vs Paid ngrok

### Free Tier ✅

**Includes:**
- ✅ 1 online ngrok process
- ✅ 4 tunnels/ngrok process
- ✅ 40 connections/minute
- ✅ HTTP/TCP tunnels
- ❌ Random URL (changes each restart)
- ❌ ngrok branding/warning page

**Good for:** Development, testing, demos

### Paid Tiers 💰

**Personal ($10/month):**
- ✅ **Custom subdomain** (permanent URL!)
- ✅ Reserved TCP addresses
- ✅ No branding
- ✅ More tunnels

**Good for:** Continuous development, multiple projects

**Pro ($15/month):**
- All Personal features +
- ✅ IP whitelisting
- ✅ Custom domains
- ✅ More connections

---

## Alternative: Deploy to Cloud

If you don't want to use ngrok, consider deploying Jenkins to:

- **AWS EC2** (free tier available)
- **DigitalOcean** ($6/month)
- **Heroku** (has free tier)
- **Azure** (free tier)

These give you a **permanent public IP** and don't require tunneling.

---

## Quick Reference

### Start ngrok
```bash
ngrok http 8080
```

### Get Current ngrok URL
```bash
curl -s http://localhost:4040/api/tunnels | jq -r '.tunnels[0].public_url'
```

### ngrok Dashboard
```
http://localhost:4040
```

### Correct Webhook URL Format
```
https://YOUR-SUBDOMAIN.ngrok-free.dev/github-webhook/
                                    ^^^^^^^^^^^^^^^^^
                                    ⚠️ Don't forget this!
```

### Test Webhook Endpoint
```bash
curl -I https://YOUR-SUBDOMAIN.ngrok-free.dev/github-webhook/
# Should return: 200 OK or 405 Method Not Allowed (both are fine)
```

---

## Summary

1. **Install ngrok** → `brew install ngrok`
2. **Configure auth token** → `ngrok config add-authtoken TOKEN`
3. **Start tunnel** → `ngrok http 8080`
4. **Copy URL** → `https://xxx.ngrok-free.dev`
5. **Update GitHub webhook** → `https://xxx.ngrok-free.dev/github-webhook/`
6. **Test** → Push a commit, watch Jenkins build!

---

## Need Help?

- 📚 **ngrok Documentation:** https://ngrok.com/docs
- 🐛 **Webhook Issues:** Run `./fix-webhook-csrf.sh`
- 🧪 **Test Setup:** Run `./test-ngrok-webhook.sh`
- 📧 **Email Issues:** See `EMAIL_SETUP.md`

---

**Happy Building! 🚀**


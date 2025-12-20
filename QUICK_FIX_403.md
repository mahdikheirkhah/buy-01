# Quick Fix for 403 Webhook Error

## Your Jenkins Password
```
40cdde478c6c49f0adcfdd34875e62a9
```

## The Problem
The 403 "No valid crumb" error happens because Jenkins CSRF protection blocks GitHub webhooks.

## Quick Solution (5 Steps)

### Step 1: Login to Jenkins
```
URL: http://localhost:8080
Username: admin
Password: 40cdde478c6c49f0adcfdd34875e62a9
```

### Step 2: Configure GitHub Plugin
1. Go to: **Manage Jenkins** → **Configure System**
2. Scroll to **GitHub** section
3. If you don't see it, install the GitHub plugin:
   - **Manage Jenkins** → **Manage Plugins** → **Available** tab
   - Search for "GitHub plugin"
   - Install and restart Jenkins

### Step 3: Enable Webhook Trigger in Your Job
1. Click on **e-commerce-microservices-ci-cd**
2. Click **Configure** (left sidebar)
3. Under **Build Triggers** section:
   - ✅ Check **"GitHub hook trigger for GITScm polling"**
4. Click **Save**

### Step 4: Start ngrok
```bash
cd /Users/mohammad.kheirkhah/Desktop/buy-01
./setup-webhook.sh
```

This will show you a URL like: `https://abc123.ngrok.io`

### Step 5: Add Webhook to GitHub
1. Go to: https://github.com/mahdikheirkhah/buy-01/settings/hooks
2. Click **"Add webhook"**
3. Fill in:
   - **Payload URL**: `https://your-ngrok-url.ngrok.io/github-webhook/`
     (Replace with YOUR actual ngrok URL from step 4)
   - **Content type**: `application/json`
   - **Secret**: (leave empty)
   - **Which events**: Just the push event
   - **Active**: ✅
4. Click **"Add webhook"**

## Test It!

```bash
# Make a test commit
git commit -m "test webhook trigger" --allow-empty
git push origin main
```

Watch Jenkins automatically start a build! 🎉

## If It Still Doesn't Work

Try this alternative (less secure, but works for development):

### Disable CSRF Protection for Webhooks Only

1. In Jenkins, go to **Manage Jenkins** → **Configure Global Security**
2. Under **CSRF Protection**:
   - Keep **"Prevent Cross Site Request Forgery exploits"** CHECKED
   - But configure **Crumb Issuer** to be more lenient
3. OR use the GitHub Plugin's built-in crumb exemption

### Or Use Polling Instead (No Webhook Needed)

Edit your Jenkinsfile and change the triggers section to:

```groovy
triggers {
    pollSCM('H/5 * * * *')  // Check GitHub every 5 minutes
}
```

Then Jenkins will automatically check for changes every 5 minutes. No webhook needed!

## Verifying Success

### Check GitHub Webhook Delivery
1. Go to: https://github.com/mahdikheirkhah/buy-01/settings/hooks
2. Click on your webhook
3. Check **"Recent Deliveries"** tab
4. You should see ✅ with response code **200 or 302** (not 403)

### Check Jenkins
- You should see builds triggering automatically when you push to GitHub
- Look for log message: "Started by GitHub push"

## Local Deployment (No SSH)

Good news! Your Jenkinsfile already supports local deployment. Just set:
- **DEPLOY_LOCALLY**: `true` ✅
- **SKIP_DEPLOY**: `true` ✅ (to skip SSH deployment)

This means Jenkins will build and deploy directly on the local machine without needing SSH.

## Need More Help?

See the full guide: [WEBHOOK_SETUP.md](./WEBHOOK_SETUP.md)

---

**Quick Links**:
- Jenkins: http://localhost:8080
- ngrok Dashboard: http://localhost:4040 (when ngrok is running)
- GitHub Webhooks: https://github.com/mahdikheirkhah/buy-01/settings/hooks


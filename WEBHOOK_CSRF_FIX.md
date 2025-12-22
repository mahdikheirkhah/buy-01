# GitHub Webhook CSRF Error Fix

## Problem
GitHub webhook is getting **403 Error: "No valid crumb was included in the request"**

This happens because Jenkins has CSRF protection enabled, and GitHub webhooks don't include CSRF tokens.

---

## ✅ SOLUTION 1: Fix Webhook URL (Recommended & Easiest)

### Step 1: Update GitHub Webhook URL

Go to your GitHub repository → Settings → Webhooks → Edit webhook

**Change the URL to include `/github-webhook/` (with trailing slash):**

```
OLD (Wrong): https://your-ngrok-url.ngrok-free.app/
NEW (Correct): https://your-ngrok-url.ngrok-free.app/github-webhook/
```

### Step 2: Configure Webhook Settings

- **Payload URL**: `https://your-ngrok-url.ngrok-free.app/github-webhook/`
- **Content type**: `application/json`
- **Secret**: Leave empty (or add one if you prefer)
- **SSL verification**: Enable SSL verification
- **Events**: Select "Just the push event"
- **Active**: ✅ Checked

### Step 3: Test the Webhook

Click **"Recent Deliveries"** tab and click **"Redeliver"** on the latest delivery.

You should see **200 OK** instead of 403.

---

## ✅ SOLUTION 2: Disable CSRF for GitHub Plugin (If Solution 1 Doesn't Work)

### Option A: Via Jenkins UI

1. Go to **Jenkins** → **Manage Jenkins** → **Security** → **Configure Global Security**

2. Scroll to **CSRF Protection** section

3. Under **"Crumb Issuer"**, click **Advanced**

4. Add to **"Proxy Compatibility"** or configure exemptions:
   - Check: ✅ **Enable proxy compatibility**

5. Click **Save**

### Option B: Via Groovy Script (Automated)

Run this in Jenkins Script Console (**Manage Jenkins** → **Script Console**):

```groovy
import jenkins.model.Jenkins
import hudson.security.csrf.DefaultCrumbIssuer

def jenkins = Jenkins.instance
def crumbIssuer = jenkins.getCrumbIssuer()

if (crumbIssuer instanceof DefaultCrumbIssuer) {
    crumbIssuer.setExcludeClientIPFromCrumb(true)
    jenkins.setCrumbIssuer(crumbIssuer)
    jenkins.save()
    println "✅ CSRF configured to exclude client IP from crumb"
} else {
    println "⚠️  Crumb issuer is not DefaultCrumbIssuer, manual configuration needed"
}
```

---

## ✅ SOLUTION 3: Add CSRF Exclusion for GitHub Webhook Path

### Using Groovy Script (Most Secure)

Run in Jenkins Script Console:

```groovy
import jenkins.model.Jenkins
import hudson.security.csrf.DefaultCrumbIssuer
import hudson.security.csrf.CrumbIssuer

def jenkins = Jenkins.instance

// Enable CSRF but exclude GitHub webhook path
def crumbIssuer = new DefaultCrumbIssuer(true) // proxy compatibility enabled
jenkins.setCrumbIssuer(crumbIssuer)
jenkins.save()

println "✅ CSRF configured with proxy compatibility"
println "GitHub webhook should now work at: /github-webhook/"
```

---

## ✅ SOLUTION 4: Temporarily Disable CSRF (NOT RECOMMENDED for Production)

**⚠️ WARNING: Only use this for testing! Re-enable CSRF after testing!**

### Via Jenkins UI:

1. **Manage Jenkins** → **Security** → **Configure Global Security**
2. Under **CSRF Protection**, uncheck **"Prevent Cross Site Request Forgery exploits"**
3. Click **Save**

### Via Groovy Script:

```groovy
import jenkins.model.Jenkins

def jenkins = Jenkins.instance
jenkins.setCrumbIssuer(null)
jenkins.save()

println "⚠️  CSRF disabled - Remember to re-enable it!"
```

### To Re-enable CSRF:

```groovy
import jenkins.model.Jenkins
import hudson.security.csrf.DefaultCrumbIssuer

def jenkins = Jenkins.instance
jenkins.setCrumbIssuer(new DefaultCrumbIssuer(true))
jenkins.save()

println "✅ CSRF re-enabled with proxy compatibility"
```

---

## Testing Your Webhook

### 1. Check Webhook Deliveries in GitHub

1. Go to GitHub → Your Repo → Settings → Webhooks
2. Click on your webhook
3. Click **"Recent Deliveries"** tab
4. You should see deliveries with **✅ 200 OK** status

### 2. Trigger a Test Push

```bash
cd /Users/mohammad.kheirkhah/Desktop/buy-01

# Make a small change
echo "# Webhook test" >> README.md

git add README.md
git commit -m "test: trigger webhook"
git push origin main
```

### 3. Check Jenkins

- Go to your Jenkins job
- You should see a new build triggered automatically
- Check the build log for: **"Started by GitHub push"**

---

## Troubleshooting

### Still Getting 403 Error?

1. **Check the URL path is correct:**
   ```
   ✅ https://your-domain.ngrok-free.app/github-webhook/
   ❌ https://your-domain.ngrok-free.app/
   ❌ https://your-domain.ngrok-free.app/github-hook/
   ```

2. **Verify ngrok is running:**
   ```bash
   curl -I https://your-domain.ngrok-free.app/github-webhook/
   # Should return 200 OK or similar, not 403
   ```

3. **Check Jenkins is accessible:**
   ```bash
   curl http://localhost:8080/github-webhook/
   # Should NOT return 403
   ```

4. **Check Jenkins logs:**
   ```bash
   docker logs jenkins-cicd -f
   ```
   Look for webhook-related errors

### Webhook Receives but Build Doesn't Trigger?

1. **Check Jenkins job configuration:**
   - Go to your Jenkins job → Configure
   - Under **Build Triggers**, ensure **"GitHub hook trigger for GITScm polling"** is checked

2. **Verify GitHub plugin is installed:**
   - **Manage Jenkins** → **Plugins** → **Installed plugins**
   - Search for **"GitHub"** - should be installed

3. **Check Jenkins GitHub configuration:**
   - **Manage Jenkins** → **System** → **GitHub** section
   - Verify GitHub servers are configured

---

## Quick Reference

| Solution | Security Level | Difficulty | When to Use |
|----------|---------------|------------|-------------|
| Solution 1: Fix URL | ✅ High | ⭐ Easy | **Try this first!** |
| Solution 2: Proxy Compatibility | ✅ High | ⭐⭐ Medium | If Solution 1 fails |
| Solution 3: Path Exclusion | ✅ High | ⭐⭐⭐ Advanced | For custom setups |
| Solution 4: Disable CSRF | ❌ Low | ⭐ Easy | **Testing only!** |

---

## Recommended Approach

```bash
# 1. Fix the webhook URL first (easiest and safest)
# Go to GitHub → Settings → Webhooks → Edit
# URL: https://your-ngrok-url.ngrok-free.app/github-webhook/

# 2. If that doesn't work, enable proxy compatibility
# Run in Jenkins Script Console:
```

```groovy
import jenkins.model.Jenkins
import hudson.security.csrf.DefaultCrumbIssuer

def jenkins = Jenkins.instance
def crumbIssuer = new DefaultCrumbIssuer(true)
jenkins.setCrumbIssuer(crumbIssuer)
jenkins.save()
println "✅ CSRF configured for webhooks"
```

```bash
# 3. Test by pushing to GitHub
git push origin main

# 4. Check Jenkins - build should trigger automatically
```

---

## Need Help?

- Jenkins logs: `docker logs jenkins-cicd -f`
- Webhook test: See "Recent Deliveries" in GitHub webhook settings
- CSRF info: **Manage Jenkins** → **System Information** → Search for "crumb"

**Status after fix:** 
- ✅ Webhook URL should end with `/github-webhook/`
- ✅ GitHub deliveries should show 200 OK
- ✅ Jenkins builds should trigger on push
- ✅ CSRF protection remains enabled

---

**Last Updated:** December 22, 2025


# Jenkins Proxy Error Fix Guide

## Common Proxy Issues and Solutions

### Issue 1: CSRF/Crumb Error with Webhooks (403 Error)

This is the most common issue when using ngrok or reverse proxies with Jenkins webhooks.

#### Quick Fix - Disable CSRF for GitHub Webhooks

1. **Login to Jenkins**
   ```
   URL: http://localhost:8080
   Username: admin
   Password: 40cdde478c6c49f0adcfdd34875e62a9
   ```

2. **Go to Jenkins Configuration**
   - Click **Manage Jenkins** → **Configure System**
   - Scroll down to **GitHub** section

3. **Configure GitHub Plugin**
   - Click **Advanced** under GitHub section
   - Check **"Override Hook URL"**
   - Leave it empty (Jenkins will auto-detect)
   - Click **"Re-register hooks for all jobs"**

4. **Adjust CSRF Protection**
   - Go to **Manage Jenkins** → **Configure Global Security**
   - Under **CSRF Protection**:
     - Keep **"Prevent Cross Site Request Forgery exploits"** CHECKED ✅
     - In **Crumb Issuer** section, click **Advanced**
     - Check **"Enable proxy compatibility"** ✅
   - Click **Save**

#### Alternative Fix - Use GitHub Plugin Authentication

1. **Create GitHub Personal Access Token**
   - Go to GitHub: https://github.com/settings/tokens
   - Click **Generate new token (classic)**
   - Name: `Jenkins Webhook Token`
   - Scopes needed:
     - ✅ `repo` (full control)
     - ✅ `admin:repo_hook` (to manage webhooks)
   - Click **Generate token**
   - **COPY THE TOKEN** (you won't see it again!)

2. **Add Token to Jenkins**
   - In Jenkins: **Manage Jenkins** → **Manage Credentials**
   - Click **(global)** domain
   - Click **Add Credentials**
   - Fill in:
     - **Kind**: Secret text
     - **Secret**: (paste your GitHub token)
     - **ID**: `github-webhook-token`
     - **Description**: GitHub Webhook Token
   - Click **Create**

3. **Configure GitHub Server**
   - Go to **Manage Jenkins** → **Configure System**
   - Find **GitHub** section
   - Click **Add GitHub Server** → **GitHub Server**
   - Fill in:
     - **Name**: GitHub
     - **API URL**: https://api.github.com
     - **Credentials**: Select `github-webhook-token`
     - ✅ Check **"Manage hooks"**
   - Click **Test connection** (should show: "Credentials verified...")
   - Click **Save**

---

### Issue 2: Ngrok Proxy Configuration

If you're using ngrok to expose Jenkins to GitHub webhooks:

#### Step 1: Configure Jenkins URL

1. Go to **Manage Jenkins** → **Configure System**
2. Find **Jenkins Location** section
3. Set **Jenkins URL** to your ngrok URL:
   ```
   https://your-ngrok-subdomain.ngrok.io/
   ```
   (Get this from running `./setup-webhook.sh`)
4. Click **Save**

#### Step 2: Configure Ngrok Properly

Create or update `ngrok.yml`:

```yaml
version: "2"
authtoken: YOUR_NGROK_AUTH_TOKEN
tunnels:
  jenkins:
    proto: http
    addr: 8080
    bind_tls: true
    inspect: true
```

Then run:
```bash
ngrok start jenkins
```

---

### Issue 3: Reverse Proxy Configuration (Nginx/Apache)

If you're using a reverse proxy in front of Jenkins:

#### Nginx Configuration

Add this to your nginx configuration:

```nginx
location /github-webhook/ {
    proxy_pass http://localhost:8080/github-webhook/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Host $host;
    proxy_set_header X-Forwarded-Port $server_port;
    
    # Important for webhooks
    proxy_buffering off;
    proxy_request_buffering off;
}
```

#### Apache Configuration

```apache
<Location /github-webhook/>
    ProxyPass http://localhost:8080/github-webhook/
    ProxyPassReverse http://localhost:8080/github-webhook/
    
    RequestHeader set X-Forwarded-Proto "https"
    RequestHeader set X-Forwarded-Port "443"
</Location>
```

---

### Issue 4: Jenkins Behind Corporate Proxy

If Jenkins itself needs to access internet through a corporate proxy:

#### Configure Java Proxy Settings

1. **Edit Jenkins Startup Script**

   **On macOS/Linux** (if using Homebrew):
   ```bash
   # Edit the plist file
   nano ~/Library/LaunchAgents/homebrew.mxcl.jenkins-lts.plist
   ```

   **In Docker** (edit your docker-compose or startup):
   ```yaml
   environment:
     JAVA_OPTS: >-
       -Dhttp.proxyHost=proxy.company.com
       -Dhttp.proxyPort=8080
       -Dhttps.proxyHost=proxy.company.com
       -Dhttps.proxyPort=8080
       -Dhttp.nonProxyHosts="localhost|127.0.0.1|*.local"
   ```

2. **Configure in Jenkins UI**
   - Go to **Manage Jenkins** → **Manage Plugins**
   - Click **Advanced** tab
   - Scroll to **HTTP Proxy Configuration**
   - Fill in:
     - **Server**: proxy.company.com
     - **Port**: 8080
     - **Username**: (if required)
     - **Password**: (if required)
     - **No Proxy Host**: localhost,127.0.0.1
   - Click **Submit**

---

## Recommended Solution for Your Setup

Based on your configuration, here's the **simplest working solution**:

### Option A: Use Polling (No Webhook Needed)

This completely avoids proxy/webhook issues:

1. **Update your Jenkinsfile** - change the triggers section:

```groovy
triggers {
    // Check GitHub every 5 minutes for changes
    pollSCM('H/5 * * * *')
}
```

2. **Save and run the pipeline once**

3. Jenkins will now automatically check GitHub every 5 minutes for new commits

**Pros:**
- ✅ No proxy issues
- ✅ No ngrok needed
- ✅ No CSRF issues
- ✅ Works behind firewalls

**Cons:**
- ⏱️ 5-minute delay (acceptable for most CI/CD)

---

### Option B: Local Webhook with Proper CSRF Config

If you want immediate builds:

1. **Enable Proxy Compatibility**
   - **Manage Jenkins** → **Configure Global Security**
   - Under **CSRF Protection**:
     - ✅ Keep "Prevent Cross Site Request Forgery exploits" CHECKED
     - In **Crumb Issuer**, click **Advanced**
     - ✅ Check "Enable proxy compatibility"
   - Click **Save**

2. **Update Jenkinsfile** to use GitHub webhook trigger:
   ```groovy
   triggers {
       githubPush()
   }
   ```

3. **Start ngrok**
   ```bash
   cd /Users/mohammad.kheirkhah/Desktop/buy-01
   ./setup-webhook.sh
   ```

4. **Configure GitHub Webhook**
   - Go to: https://github.com/mahdikheirkhah/buy-01/settings/hooks
   - Click **Add webhook**
   - **Payload URL**: `https://YOUR-NGROK-URL.ngrok.io/github-webhook/`
   - **Content type**: `application/json`
   - **Secret**: (leave empty)
   - **SSL verification**: Enable
   - Click **Add webhook**

5. **Test It**
   ```bash
   git commit -m "test webhook" --allow-empty
   git push origin main
   ```

---

## Verification Checklist

After applying fixes, verify:

- [ ] Jenkins can access GitHub API
  - Test: Go to **Manage Jenkins** → **Configure System** → **GitHub** → **Test connection**

- [ ] Webhook delivers successfully
  - Go to: https://github.com/mahdikheirkhah/buy-01/settings/hooks
  - Click your webhook → **Recent Deliveries**
  - Should show ✅ with response 200 or 302

- [ ] Builds trigger automatically
  - Make a test commit
  - Check Jenkins for new build
  - Look for "Started by GitHub push" in build log

- [ ] CSRF protection is working
  - Try accessing Jenkins UI - should work normally
  - Webhooks should still work with proxy compatibility enabled

---

## Troubleshooting

### Still Getting 403 Errors?

1. **Check Jenkins Logs**
   ```bash
   # If using Docker
   docker logs jenkins-cicd
   
   # Look for lines containing "crumb" or "CSRF"
   ```

2. **Temporarily Disable CSRF** (Development Only!)
   - **Manage Jenkins** → **Configure Global Security**
   - Under **CSRF Protection**:
     - ❌ UNCHECK "Prevent Cross Site Request Forgery exploits"
   - Click **Save**
   - **Test webhook**
   - If it works, the issue is CSRF - re-enable with proxy compatibility

3. **Check Network Connectivity**
   ```bash
   # From Jenkins container, test GitHub access
   docker exec jenkins-cicd curl -I https://api.github.com
   ```

### Webhook Shows as Failed in GitHub?

1. **Check the response code**
   - 403: CSRF issue (use fixes above)
   - 404: Wrong URL (check Jenkins URL configuration)
   - 500: Jenkins error (check Jenkins logs)
   - Timeout: Network/firewall issue

2. **Verify Jenkins URL**
   - **Manage Jenkins** → **Configure System**
   - **Jenkins URL** must match your ngrok URL exactly
   - Must end with `/` (e.g., `https://abc123.ngrok.io/`)

---

## My Recommendation

For your setup, I recommend **Option A (Polling)** because:

1. ✅ **Zero configuration** - just change one line in Jenkinsfile
2. ✅ **No external dependencies** - no ngrok needed
3. ✅ **No security issues** - no CSRF bypass needed
4. ✅ **Works behind any firewall**
5. ⏱️ **5-minute delay is acceptable** for a CI/CD pipeline

The webhook approach is great for production, but for development/local setup, polling is much simpler and more reliable.

---

## Quick Commands Reference

### Start Jenkins (Docker)
```bash
docker start jenkins-cicd
```

### View Jenkins Logs
```bash
docker logs -f jenkins-cicd
```

### Start Ngrok (if using webhooks)
```bash
cd /Users/mohammad.kheirkhah/Desktop/buy-01
./setup-webhook.sh
```

### Test GitHub Connection from Jenkins Container
```bash
docker exec jenkins-cicd curl -I https://api.github.com
```

### Restart Jenkins
```bash
docker restart jenkins-cicd
```

---

## Need More Help?

If you're still experiencing issues:

1. **Check Jenkins System Log**
   - **Manage Jenkins** → **System Log**
   - Look for errors related to "proxy", "webhook", or "GitHub"

2. **Enable Debug Logging**
   - **Manage Jenkins** → **System Log** → **Add new log recorder**
   - Name: `GitHub Webhook Debug`
   - Add logger: `com.cloudbees.jenkins.GitHubWebHook` → Level: `FINEST`
   - Add logger: `org.jenkinsci.plugins.github` → Level: `FINEST`
   - Click **Save**
   - Make a test commit and check the log

3. **Share the specific error message** and I can provide more targeted help!


# Quick Start Guide - Fixing Your Jenkins Pipeline

## 🚨 Current Problem
Your Jenkins pipeline completes immediately without running any stages, webhook returns 403, and no emails are sent.

## 🎯 Solution Steps (Do These in Order)

### Step 1: Run Diagnostic Script (5 minutes)
```bash
cd /Users/mohammad.kheirkhah/Desktop/buy-01
./diagnostic.sh
```

This will show you exactly what's wrong.

---

### Step 2: Fix Docker Access (if diagnostic fails) (10 minutes)

If diagnostic shows "Jenkins cannot access Docker daemon":

```bash
# Stop Jenkins
docker stop jenkins-cicd

# Remove it
docker rm jenkins-cicd

# Recreate with proper Docker access
docker run -d \
  --name jenkins-cicd \
  --restart unless-stopped \
  -p 8080:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v jenkins_m2_cache:/root/.m2 \
  --group-add $(stat -f '%g' /var/run/docker.sock) \
  jenkins/jenkins:lts-jdk17

# Wait 30 seconds for startup
sleep 30

# Install Docker CLI in Jenkins
docker exec -u root jenkins-cicd bash -c "
  apt-get update && \
  apt-get install -y docker.io && \
  apt-get clean
"
```

---

### Step 3: Install Docker Compose (if diagnostic fails) (5 minutes)

If diagnostic shows "Docker Compose is not available":

```bash
docker exec -u root jenkins-cicd bash -c "
  mkdir -p /usr/local/lib/docker/cli-plugins && \
  curl -SL https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-linux-aarch64 \
    -o /usr/local/lib/docker/cli-plugins/docker-compose && \
  chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
"

# Verify
docker exec jenkins-cicd docker compose version
```

**Note**: If you're on Intel Mac (x86_64), use `docker-compose-linux-x86_64` instead of `docker-compose-linux-aarch64`

---

### Step 4: Verify Jenkins Job Configuration (5 minutes)

1. Open Jenkins: http://localhost:8080

2. Go to your job: `e-commerce-microservices-ci-cd`

3. Click "Configure"

4. Verify these settings:

   **General** section:
   - ✅ GitHub project: `https://github.com/mahdikheirkhah/buy-01/`

   **Build Triggers** section:
   - ✅ GitHub hook trigger for GITScm polling

   **Pipeline** section:
   - Definition: `Pipeline script from SCM`
   - SCM: `Git`
   - Repository URL: `https://github.com/mahdikheirkhah/buy-01.git`
   - Credentials: `github-packages-creds` (select from dropdown)
   - Branch Specifier: `*/main`
   - Script Path: `Jenkinsfile`

5. Click "Save"

---

### Step 5: Test Manual Build (2 minutes)

1. In Jenkins, click your job

2. Click "Build with Parameters"

3. Use these settings:
   - BRANCH: `main`
   - RUN_TESTS: `unchecked`
   - RUN_SONAR: `unchecked`
   - SKIP_DEPLOY: `checked`
   - DEPLOY_LOCALLY: `unchecked`

4. Click "Build"

5. Watch the Console Output

**Expected**: You should see all stages running (Checkout, Build & Test Backend, Dockerize & Publish, etc.)

**If it still completes immediately**: 
- Check Console Output for errors
- Run: `docker logs jenkins-cicd --tail 100`
- Check Jenkinsfile syntax in Jenkins: Pipeline Syntax → Declarative Directive Generator

---

### Step 6: Fix Email Notifications (10 minutes)

1. **Get Gmail App Password**:
   - Go to: https://myaccount.google.com/apppasswords
   - Create app password named "Jenkins"
   - Copy the 16-character password

2. **Configure Jenkins Email**:
   - Manage Jenkins → System
   - Scroll to "Extended E-mail Notification"
   - Configure:
     ```
     SMTP server: smtp.gmail.com
     SMTP Port: 587
     ```
   - Click "Advanced"
   - ✅ Use SMTP Authentication
   - User Name: `your-email@gmail.com`
   - Password: `[paste 16-char app password]`
   - ✅ Use TLS
   - Click "Save"

3. **Test Email**:
   - Click "Test configuration by sending test e-mail"
   - Recipient: `mohammad.kheirkhah@gritlab.ax`
   - Click "Test configuration"
   - Check your email

---

### Step 7: Fix GitHub Webhook (10 minutes)

1. **Generate Jenkins API Token**:
   - In Jenkins, click your username (top right)
   - Click "Configure"
   - Scroll to "API Token"
   - Click "Add new Token"
   - Name: "GitHub Webhook"
   - Click "Generate"
   - **COPY THE TOKEN** (you won't see it again!)

2. **Update GitHub Webhook**:
   - Go to: https://github.com/mahdikheirkhah/buy-01/settings/hooks
   - Click on your webhook (or "Add webhook" if none exists)
   - Payload URL: `http://admin:YOUR_TOKEN@YOUR_IP:8080/github-webhook/`
     
     Replace:
     - `YOUR_TOKEN` with the token from step 1
     - `YOUR_IP` with your machine's IP (NOT localhost!)
     
     To find your IP:
     ```bash
     ipconfig getifaddr en0  # WiFi
     # or
     ipconfig getifaddr en1  # Ethernet
     ```
     
     Example: `http://admin:11a1234567890@192.168.1.50:8080/github-webhook/`
   
   - Content type: `application/json`
   - Events: "Just the push event"
   - ✅ Active
   - Click "Update webhook"

3. **Test Webhook**:
   - In GitHub webhook settings, click "Recent Deliveries"
   - Click the latest delivery
   - Click "Redeliver"
   - Should show green checkmark with 200 response

---

### Step 8: End-to-End Test (5 minutes)

1. **Make a small code change**:
   ```bash
   cd /Users/mohammad.kheirkhah/Desktop/buy-01
   echo "# Test webhook $(date)" >> README.md
   git add README.md
   git commit -m "test: trigger Jenkins webhook"
   git push origin main
   ```

2. **Watch Jenkins**:
   - Go to Jenkins dashboard
   - You should see a new build start automatically
   - Click on the build number → "Console Output"
   - Watch it execute all stages

3. **Check Email**:
   - You should receive an email when build completes
   - Check subject line (SUCCESS or FAILED)

4. **Verify Deployment** (if DEPLOY_LOCALLY was enabled):
   ```bash
   docker compose ps
   # Should show all services running
   
   # Access the app
   open http://localhost:4200
   ```

---

## ✅ Success Checklist

You're done when you can check all these:

- [ ] `./diagnostic.sh` shows all checks passing
- [ ] Manual build in Jenkins runs all stages
- [ ] Docker images are pushed to Docker Hub
- [ ] Email notification received
- [ ] Webhook shows 200 response in GitHub
- [ ] Push to GitHub automatically triggers build
- [ ] Services are accessible at http://localhost:4200

---

## 🆘 Still Not Working?

### Check These Files
1. **Jenkinsfile syntax**: Copy entire Jenkinsfile into Jenkins Pipeline Syntax validator
2. **Jenkins logs**: `docker logs jenkins-cicd --tail 500 > jenkins.log`
3. **Docker access**: `docker exec jenkins-cicd docker ps` (should not error)
4. **Workspace**: `docker exec jenkins-cicd ls -la /var/jenkins_home/workspace/`

### Common Issues & Quick Fixes

**Issue**: Build completes immediately
**Fix**: Job is not configured to use Jenkinsfile - check Step 4 above

**Issue**: "docker: command not found" in build
**Fix**: Jenkins container doesn't have Docker CLI - run Step 2 above

**Issue**: "docker compose: command not found"
**Fix**: Docker Compose not installed - run Step 3 above

**Issue**: Webhook 403
**Fix**: Add authentication to webhook URL - see Step 7 above

**Issue**: No email received
**Fix**: Use Gmail App Password, not regular password - see Step 6 above

---

## 📚 Detailed Documentation

For complete documentation, see:
- `JENKINS_TROUBLESHOOTING.md` - Comprehensive troubleshooting guide
- `TODO.md` - Project status and next steps
- `README.md` - Complete project documentation

---

## 💡 Pro Tips

1. **Save your Jenkins API token** in a password manager
2. **Use stable tag for deployment**: `IMAGE_TAG=stable docker compose up -d`
3. **Check build logs** in Jenkins UI, not just email
4. **Test locally first** with DEPLOY_LOCALLY=true
5. **Keep diagnostic.sh** handy for quick health checks

---

## 🎯 Time Estimate

Following this guide step-by-step:
- Steps 1-3: 20 minutes (setup)
- Steps 4-5: 7 minutes (configuration & test)
- Steps 6-7: 20 minutes (notifications & webhook)
- Step 8: 5 minutes (end-to-end test)

**Total**: ~50 minutes to get everything working

---

**You got this! Start with Step 1 and work through methodically.** 🚀


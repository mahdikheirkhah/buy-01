# Jenkins CI/CD Troubleshooting Guide

## Issues You're Facing:
1. ✅ Pipeline completes immediately without running stages
2. ✅ Webhook returns 403 Forbidden
3. ✅ Email notifications not working
4. ✅ docker-compose vs docker compose command

---

## 🔧 Solution 1: Fix Pipeline Not Running

### Problem
Your pipeline shows:
```
Started by user admin
Obtained Jenkinsfile from git https://github.com/mahdikheirkhah/buy-01.git
[Pipeline] Start of Pipeline
[Pipeline] End of Pipeline
Finished: SUCCESS
```

This means Jenkins is not parsing the Jenkinsfile correctly.

### Solutions

#### Option A: Verify Pipeline Job Configuration
1. Go to your Jenkins job configuration
2. Under "Pipeline" section, ensure:
   - **Definition**: Pipeline script from SCM
   - **SCM**: Git
   - **Repository URL**: https://github.com/mahdikheirkhah/buy-01.git
   - **Credentials**: Select your GitHub credentials
   - **Branch**: */main
   - **Script Path**: Jenkinsfile (exactly this, case-sensitive)

3. Click "Save"
4. Click "Build Now"

#### Option B: Check Jenkins Container has Docker Access
Your Jenkinsfile uses Docker commands. Verify your Jenkins container can access Docker:

```bash
# Check if Jenkins container is running
docker ps | grep jenkins

# Access Jenkins container
docker exec -it jenkins-cicd bash

# Inside container, test Docker access
docker ps
docker info

# Exit container
exit
```

If Docker commands fail, your Jenkins container needs Docker socket access. Restart with:

```bash
docker stop jenkins-cicd
docker rm jenkins-cicd

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
```

Then install Docker CLI inside Jenkins:

```bash
docker exec -u root jenkins-cicd bash -c "
  apt-get update && \
  apt-get install -y docker.io && \
  apt-get clean
"
```

---

## 🔧 Solution 2: Fix Webhook 403 Error

### Problem
GitHub webhook returns 403 Forbidden when trying to trigger Jenkins.

### Root Cause
Jenkins CSRF protection blocks unauthenticated webhook requests.

### Solution: Configure GitHub Webhook Properly

#### Step 1: Generate Jenkins API Token
1. In Jenkins, click your username (top right)
2. Click "Configure"
3. Scroll to "API Token" section
4. Click "Add new Token"
5. Give it a name (e.g., "GitHub Webhook")
6. Click "Generate"
7. **COPY THE TOKEN** (you won't see it again!)

#### Step 2: Configure GitHub Webhook with Authentication
1. Go to your GitHub repository: https://github.com/mahdikheirkhah/buy-01
2. Click "Settings" → "Webhooks" → "Add webhook"
3. Configure:
   - **Payload URL**: `http://admin:YOUR_API_TOKEN@your-jenkins-ip:8080/github-webhook/`
     
     Replace:
     - `admin` with your Jenkins username
     - `YOUR_API_TOKEN` with the token from Step 1
     - `your-jenkins-ip` with your Jenkins server IP
     
     Example: `http://admin:11a1234567890abcdef@192.168.1.50:8080/github-webhook/`
   
   - **Content type**: application/json
   - **Which events**: Just the push event
   - **Active**: ✅ Checked

4. Click "Add webhook"
5. Test it by clicking "Recent Deliveries" → Click on the delivery → "Redeliver"

#### Alternative: Disable CSRF for Webhooks (Less Secure)
If you're on a private network:

1. In Jenkins: Manage Jenkins → Security → Configure Global Security
2. Under "CSRF Protection", check "Enable proxy compatibility"
3. Or add `-Dhudson.security.csrf.GlobalCrumbIssuerConfiguration.DISABLE_CSRF_PROTECTION=true` to Jenkins startup

**Note**: Only do this on private networks!

---

## 🔧 Solution 3: Fix Email Notifications

### Problem
Email notifications not being sent after build completion.

### Solution: Verify Extended Email Plugin Configuration

#### Step 1: Install Extended Email Plugin
1. Manage Jenkins → Plugins → Available plugins
2. Search for "Email Extension Plugin"
3. Install and restart Jenkins

#### Step 2: Configure Email Settings
1. Manage Jenkins → System → Extended E-mail Notification
2. Configure:
   ```
   SMTP server: smtp.gmail.com
   SMTP Port: 587
   
   Click "Advanced":
   - Use SMTP Authentication: ✅
   - User Name: your-email@gmail.com
   - Password: [App Password, NOT your Gmail password]
   - Use TLS: ✅
   - Charset: UTF-8
   ```

3. Set Default Recipients: `mohammad.kheirkhah@gritlab.ax`

4. Click "Test configuration by sending test e-mail"
   - Test e-mail recipient: mohammad.kheirkhah@gritlab.ax
   - Click "Test configuration"

#### Step 3: Get Gmail App Password
1. Go to https://myaccount.google.com/security
2. Enable 2-Step Verification (if not already enabled)
3. Go to "App passwords"
4. Select "Mail" and "Other (Custom name)"
5. Enter "Jenkins" as the name
6. Click "Generate"
7. **COPY THE 16-CHARACTER PASSWORD**
8. Use this in Jenkins (not your Gmail password)

#### Step 4: Verify Jenkinsfile Email Configuration
Your Jenkinsfile already has email configuration in the `post` section. Verify it's correct:

```groovy
post {
    success {
        emailext (
            subject: "✅ Build SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            body: "...",
            to: "mohammad.kheirkhah@gritlab.ax",
            mimeType: 'text/html'
        )
    }
    failure {
        emailext (
            subject: "❌ Build FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            body: "...",
            to: "mohammad.kheirkhah@gritlab.ax",
            mimeType: 'text/html'
        )
    }
}
```

---

## 🔧 Solution 4: Fix docker-compose Command

### Problem
Error: `docker: 'compose' is not a docker command`

### Root Cause
Older Docker versions use `docker-compose` (with hyphen), newer versions use `docker compose` (with space).

### Solution: Install Docker Compose Plugin

Your Jenkins container needs the Docker Compose plugin:

```bash
# Access Jenkins container as root
docker exec -u root -it jenkins-cicd bash

# Install Docker Compose plugin
DOCKER_CONFIG=${DOCKER_CONFIG:-/usr/local/lib/docker}
mkdir -p $DOCKER_CONFIG/cli-plugins
curl -SL https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-linux-aarch64 -o $DOCKER_CONFIG/cli-plugins/docker-compose
chmod +x $DOCKER_CONFIG/cli-plugins/docker-compose

# Verify installation
docker compose version

# Exit
exit
```

**Note**: If you're on x86_64 instead of ARM (aarch64):
```bash
curl -SL https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-linux-x86_64 -o $DOCKER_CONFIG/cli-plugins/docker-compose
```

---

## 📋 Complete Restart Checklist

If nothing works, follow this complete setup:

### 1. Stop and Remove Existing Jenkins
```bash
docker stop jenkins-cicd
docker rm jenkins-cicd
```

### 2. Create Fresh Jenkins with All Requirements
```bash
# Create volumes
docker volume create jenkins_home
docker volume create jenkins_m2_cache

# Start Jenkins with Docker access
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

# Wait for Jenkins to start (30 seconds)
sleep 30

# Install Docker CLI and Docker Compose
docker exec -u root jenkins-cicd bash -c "
  apt-get update && \
  apt-get install -y docker.io curl && \
  mkdir -p /usr/local/lib/docker/cli-plugins && \
  curl -SL https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-linux-x86_64 -o /usr/local/lib/docker/cli-plugins/docker-compose && \
  chmod +x /usr/local/lib/docker/cli-plugins/docker-compose && \
  apt-get clean
"

# Get initial admin password
docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword
```

### 3. Configure Jenkins
1. Access http://localhost:8080
2. Enter admin password
3. Install suggested plugins + Email Extension Plugin
4. Create admin user
5. Configure System:
   - Extended E-mail Notification (Gmail settings)
   - Global Security (API token for webhooks)

### 4. Create Pipeline Job
1. New Item → Pipeline → Name: "e-commerce-microservices-ci-cd"
2. Configure:
   - General: GitHub project: https://github.com/mahdikheirkhah/buy-01/
   - Build Triggers: ✅ GitHub hook trigger for GITScm polling
   - Pipeline:
     - Definition: Pipeline script from SCM
     - SCM: Git
     - Repository URL: https://github.com/mahdikheirkhah/buy-01.git
     - Credentials: Add GitHub credentials
     - Branch: */main
     - Script Path: Jenkinsfile
3. Save

### 5. Add Credentials
1. Manage Jenkins → Credentials → System → Global credentials
2. Add:
   - **Docker Hub**: Username with password
     - ID: `dockerhub-credentials`
     - Username: mahdikheirkhah
     - Password: [Your Docker Hub password/token]
   
   - **GitHub**: Username with password (or Personal Access Token)
     - ID: `github-packages-creds`
     - Username: mahdikheirkhah
     - Password: [GitHub Personal Access Token]

### 6. Setup GitHub Webhook
1. Generate Jenkins API token (User → Configure → API Token)
2. GitHub Repository → Settings → Webhooks
3. Add webhook:
   - URL: `http://admin:YOUR_TOKEN@YOUR_IP:8080/github-webhook/`
   - Content type: application/json
   - Events: Push events
   - Active: ✅

### 7. Test Everything
```bash
# Test Docker access from Jenkins
docker exec jenkins-cicd docker ps

# Test Docker Compose
docker exec jenkins-cicd docker compose version

# Trigger a build
# In Jenkins: Click "Build with Parameters" → Build

# Push to GitHub to test webhook
cd /Users/mohammad.kheirkhah/Desktop/buy-01
echo "# Test webhook" >> README.md
git add README.md
git commit -m "test: trigger Jenkins webhook"
git push origin main
```

---

## 🎯 Quick Diagnostic Commands

Run these to diagnose your current setup:

```bash
# Check Jenkins is running
docker ps | grep jenkins

# Check Jenkins can access Docker
docker exec jenkins-cicd docker ps

# Check Docker Compose
docker exec jenkins-cicd docker compose version

# Check Jenkins logs for errors
docker logs jenkins-cicd --tail 100

# Check if Docker socket is accessible
docker exec jenkins-cicd ls -la /var/run/docker.sock

# View Jenkins system info
# In Jenkins: Manage Jenkins → System Information
```

---

## 📧 Test Email Configuration

Create a simple test job:

1. New Item → Freestyle project → "Email-Test"
2. Build Steps → Execute shell:
   ```bash
   echo "Testing email notifications"
   exit 0
   ```
3. Post-build Actions → Editable Email Notification
   - Project Recipient List: mohammad.kheirkhah@gritlab.ax
   - Advanced → Triggers → Always
4. Save and Build

Check your email for notifications.

---

## 🐛 Common Errors and Solutions

### Error: "docker: 'compose' is not a docker command"
**Solution**: Install Docker Compose plugin (see Solution 4 above)

### Error: "permission denied while trying to connect to Docker"
**Solution**: Add Jenkins user to docker group:
```bash
docker exec -u root jenkins-cicd usermod -aG docker jenkins
docker restart jenkins-cicd
```

### Error: "Couldn't find any revision to build"
**Solution**: 
- Check GitHub credentials in Jenkins
- Verify repository URL is correct
- Check branch name is correct (main vs master)

### Error: "No suitable checks publisher found"
**Solution**: This is just a warning, ignore it or install GitHub Checks plugin

---

## 📞 Still Having Issues?

If you're still facing problems, collect this information:

1. Jenkins version:
   ```bash
   docker exec jenkins-cicd cat /var/jenkins_home/config.xml | grep version
   ```

2. Jenkins logs:
   ```bash
   docker logs jenkins-cicd --tail 500 > jenkins.log
   ```

3. Build console output (from Jenkins UI)

4. Docker version:
   ```bash
   docker version
   docker exec jenkins-cicd docker version
   ```

5. Webhook delivery details from GitHub (Settings → Webhooks → Recent Deliveries)

---

## ✅ Success Checklist

Once everything is working, you should see:

- [ ] Jenkins pipeline runs all stages when you click "Build Now"
- [ ] GitHub webhook shows green checkmark and 200 response
- [ ] Automatic build triggers when you push to GitHub
- [ ] Docker images are built and pushed to Docker Hub
- [ ] Email notifications received for success/failure
- [ ] Local deployment works (if enabled)
- [ ] Services accessible at:
  - Frontend: http://localhost:4200
  - API Gateway: https://localhost:8443
  - Eureka: http://localhost:8761

---

**Good luck! Follow these steps carefully and your Jenkins CI/CD pipeline will be running smoothly.** 🚀


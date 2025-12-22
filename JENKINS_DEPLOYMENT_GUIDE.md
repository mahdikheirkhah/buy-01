# Jenkins CI/CD Deployment Guide

## Overview
This Jenkins pipeline automates the build, test, and deployment of your e-commerce microservices application. It supports both **local deployment** (on the Jenkins machine) and **remote deployment** (via SSH to a remote server).

---

## 🚀 Quick Start: Local Deployment (Recommended)

If you want to deploy the application on the same machine where Jenkins is running:

### Step 1: Run the Pipeline
1. Go to your Jenkins job
2. Click **"Build with Parameters"**
3. Set the following parameters:
   - `BRANCH`: `main` (or your desired branch)
   - `RUN_TESTS`: `false` (set to true only if you have test infrastructure)
   - `RUN_SONAR`: `false` (set to true only if SonarQube is configured)
   - **`SKIP_DEPLOY`: `true`** ✅
   - **`DEPLOY_LOCALLY`: `true`** ✅

4. Click **"Build"**

### Step 2: Access Your Application
After successful deployment, access:
- **Frontend**: http://localhost:4200
- **API Gateway**: https://localhost:8443
- **Eureka Dashboard**: http://localhost:8761

---

## 🏗️ Build Only (No Deployment)

If you only want to build and publish Docker images without deploying:

### Parameters:
- `BRANCH`: `main`
- `RUN_TESTS`: `false`
- `RUN_SONAR`: `false`
- **`SKIP_DEPLOY`: `true`** ✅
- **`DEPLOY_LOCALLY`: `false`** ✅

After the build completes, you can manually deploy:

```bash
# SSH into your Jenkins machine or navigate to workspace
cd /var/jenkins_home/workspace/e-commerce-microservices-ci-cd

# Deploy with specific build number
export IMAGE_TAG=26  # Use your build number
docker compose down
docker compose pull
docker compose up -d

# Or use the stable tag
export IMAGE_TAG=stable
docker compose up -d
```

---

## 🌐 Remote Deployment via SSH (Advanced)

To deploy to a remote server, you need to configure SSH credentials first.

### Prerequisites:
1. **Remote Server Setup**:
   - Server IP: `192.168.1.100` (change in Jenkinsfile if different)
   - SSH access configured
   - Docker and Docker Compose installed on remote server
   - Directory `/opt/ecommerce` exists on remote server

2. **SSH Key Access**:
   - Generate SSH key pair if you don't have one:
     ```bash
     ssh-keygen -t rsa -b 4096 -C "jenkins@yourdomain.com"
     ```
   - Copy public key to remote server:
     ```bash
     ssh-copy-id ssh-user@192.168.1.100
     ```

### Step 1: Add SSH Credentials to Jenkins

1. Go to **Jenkins Dashboard** → **Manage Jenkins** → **Credentials**
2. Click on **(global)** domain
3. Click **Add Credentials**
4. Select **SSH Username with private key**
5. Configure:
   - **ID**: `ssh-deployment-key` (must match Jenkinsfile)
   - **Username**: `ssh-user` (your SSH user on remote server)
   - **Private Key**: Select **"Enter directly"** and paste your private key
   - **Passphrase**: (if your key has one)
6. Click **OK**

### Step 2: Configure Remote Server Details

Edit the Jenkinsfile if your server details are different:

```groovy
environment {
    // Remote SSH deployment (optional)
    SSH_CREDENTIAL_ID = 'ssh-deployment-key'
    REMOTE_HOST = '192.168.1.100'          // Your server IP
    REMOTE_USER = 'ssh-user'                // Your SSH username
    DEPLOYMENT_DIR = '/opt/ecommerce'       // Deployment directory
}
```

### Step 3: Run Remote Deployment

1. Click **"Build with Parameters"**
2. Set parameters:
   - `BRANCH`: `main`
   - `RUN_TESTS`: `false`
   - `RUN_SONAR`: `false`
   - **`SKIP_DEPLOY`: `false`** ⚠️ (enables deployment)
   - **`DEPLOY_LOCALLY`: `false`** ⚠️ (uses SSH)

3. Click **"Build"**

The pipeline will:
- Copy `docker-compose.yml` to remote server
- Pull latest Docker images on remote server
- Deploy services with zero downtime

---

## 📊 Pipeline Stages Explained

### 1. **Checkout**
- Pulls code from GitHub repository
- Uses the branch specified in parameters

### 2. **Build & Test Backend**
- Builds all backend microservices using Maven
- Runs in Docker container to ensure consistent build environment
- Skips tests by default (enable with `RUN_TESTS=true`)

### 3. **Test Backend Services** (Optional)
- Runs unit and integration tests
- Only executes when `RUN_TESTS=true`
- Requires embedded MongoDB and Kafka for tests

### 4. **SonarQube Analysis** (Optional)
- Performs code quality analysis
- Only executes when `RUN_SONAR=true`
- Requires SonarQube server configuration

### 5. **Dockerize & Publish**
- Builds Docker images for all services
- Tags images with build number (e.g., `mahdikheirkhah/user-service:26`)
- Also tags as `stable` for easy rollback
- Pushes to Docker Hub

### 6. **Deploy Locally** (Conditional)
- Executes when `SKIP_DEPLOY=true` AND `DEPLOY_LOCALLY=true`
- Deploys on Jenkins machine using `docker compose`
- No SSH required

### 7. **Deploy & Verify** (Conditional)
- Executes when `SKIP_DEPLOY=false` AND `DEPLOY_LOCALLY=false`
- Deploys to remote server via SSH
- Requires SSH credentials configured

### 8. **Local Deploy Info** (Conditional)
- Shows deployment instructions
- Executes when build-only mode is used

---

## 🔐 Required Jenkins Credentials

### 1. Docker Hub Credentials (Required)
- **ID**: `dockerhub-credentials`
- **Type**: Username with password
- **Username**: Your Docker Hub username
- **Password**: Docker Hub access token (recommended) or password

**To create:**
1. Jenkins → Manage Jenkins → Credentials
2. Add Credentials → Username with password
3. ID: `dockerhub-credentials`

### 2. SSH Credentials (Optional - for remote deployment)
- **ID**: `ssh-deployment-key`
- **Type**: SSH Username with private key
- **Username**: SSH user on remote server
- **Private Key**: Your SSH private key

---

## 🛠️ Troubleshooting

### Issue: "docker-compose: not found"
**Solution**: Your Jenkins machine uses `docker compose` (v2) instead of `docker-compose` (v1). This is already handled in the Jenkinsfile.

### Issue: "Could not find credentials entry with ID 'dockerhub-credentials'"
**Solution**: 
1. Go to Jenkins → Manage Jenkins → Credentials
2. Add Docker Hub credentials with ID: `dockerhub-credentials`

### Issue: "Could not find credentials entry with ID 'ssh-deployment-key'"
**Solution**: 
- Either add SSH credentials (see "Add SSH Credentials" section above)
- Or use local deployment: Set `SKIP_DEPLOY=true` and `DEPLOY_LOCALLY=true`

### Issue: "Docker login failed"
**Solution**:
- Verify Docker Hub credentials are correct
- Use Docker Hub access token instead of password
- Check if username has typos

### Issue: "Connection refused" during SSH deployment
**Solution**:
- Verify remote server IP address is correct
- Check if SSH port (22) is open on remote server
- Test SSH connection manually: `ssh ssh-user@192.168.1.100`

### Issue: Services not starting after deployment
**Solution**:
```bash
# Check service logs
docker compose logs -f

# Check service status
docker compose ps

# Restart services
docker compose restart

# Full restart
docker compose down && docker compose up -d
```

---

## 📧 Email Notifications

The pipeline sends email notifications on:
- ✅ **Success**: Build and deployment successful
- ❌ **Failure**: Build or deployment failed

Email is sent to: `mohammad.kheirkhah@gritlab.ax`

To change recipient, edit the `post` section in Jenkinsfile:

```groovy
to: "your-email@example.com",
```

---

## 🔄 Rollback Strategy

If a deployment fails or has issues:

### Option 1: Use Stable Tag
```bash
export IMAGE_TAG=stable
docker compose down
docker compose pull
docker compose up -d
```

### Option 2: Redeploy Previous Build
```bash
export IMAGE_TAG=25  # Previous working build number
docker compose down
docker compose pull
docker compose up -d
```

### Option 3: Jenkins Re-deployment
1. Go to Jenkins job
2. Find the previous successful build
3. Click **"Rebuild"**

---

## 🎯 Common Deployment Scenarios

### Scenario 1: First-time Setup (Local)
```
Parameters:
- SKIP_DEPLOY: true
- DEPLOY_LOCALLY: true
```

### Scenario 2: Development Build (No Deploy)
```
Parameters:
- SKIP_DEPLOY: true
- DEPLOY_LOCALLY: false
```
Then manually deploy when ready.

### Scenario 3: Production Deployment (Remote)
```
Parameters:
- SKIP_DEPLOY: false
- DEPLOY_LOCALLY: false
```
Requires SSH credentials configured.

### Scenario 4: Testing with Unit Tests
```
Parameters:
- RUN_TESTS: true
- SKIP_DEPLOY: true
- DEPLOY_LOCALLY: false
```

---

## 📝 Pipeline Parameters Reference

| Parameter | Default | Description |
|-----------|---------|-------------|
| `BRANCH` | `main` | Git branch to build |
| `RUN_TESTS` | `false` | Run backend unit tests |
| `RUN_SONAR` | `false` | Run SonarQube analysis |
| `SKIP_DEPLOY` | `true` | Skip deployment stage |
| `DEPLOY_LOCALLY` | `true` | Deploy on Jenkins machine |

### Parameter Combinations:

| SKIP_DEPLOY | DEPLOY_LOCALLY | Result |
|-------------|----------------|--------|
| `true` | `true` | ✅ Local deployment via Jenkins |
| `true` | `false` | 📦 Build only, manual deployment needed |
| `false` | `false` | 🌐 Remote SSH deployment |
| `false` | `true` | ❌ Invalid (won't deploy) |

---

## 🏭 Production Checklist

Before deploying to production:

- [ ] Enable SSL/TLS certificates for API Gateway
- [ ] Configure production database credentials
- [ ] Set up proper secrets management
- [ ] Enable monitoring (Prometheus/Grafana)
- [ ] Configure log aggregation (ELK Stack)
- [ ] Set up backup strategy for MongoDB
- [ ] Enable rate limiting on API Gateway
- [ ] Configure CORS properly for frontend
- [ ] Set up health check endpoints
- [ ] Configure resource limits in docker-compose.yml
- [ ] Enable SonarQube for code quality (`RUN_SONAR=true`)
- [ ] Enable automated tests (`RUN_TESTS=true`)

---

## 📚 Additional Resources

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Jenkins Pipeline Documentation](https://www.jenkins.io/doc/book/pipeline/)
- [Docker Hub](https://hub.docker.com/)

---

## 🆘 Getting Help

If you encounter issues:

1. Check Jenkins console output for detailed error messages
2. Review this guide's Troubleshooting section
3. Check Docker logs: `docker compose logs -f`
4. Verify credentials are correctly configured
5. Test components individually before full deployment

---

**Last Updated**: December 22, 2025  
**Pipeline Version**: 1.0  
**Maintainer**: mohammad.kheirkhah@gritlab.ax


# Deployment Guide

## Overview

Your Jenkins pipeline now supports **two deployment methods**:

---

## 1. 🏠 Local Deployment (No SSH Required)

### When to use:
- Jenkins is running on the **same machine** where you want to run the application
- You don't need to deploy to a remote server
- **Simplest option - RECOMMENDED for development**

### How it works:
✅ Build happens in Jenkins
✅ Images pushed to DockerHub
✅ Containers deployed **locally** on the Jenkins machine

### Setup:
**No SSH configuration needed!** Just make sure:
1. Jenkins has Docker installed
2. Jenkins can run `docker compose` commands

### Pipeline Parameters:
```
DEPLOY_LOCALLY = true   ← Enable automatic local deployment
SKIP_DEPLOY = true      ← Skip remote SSH deployment
```

### Manual deployment (if automatic fails):
```bash
cd /var/jenkins_home/workspace/e-commerce-microservices-ci-cd
export IMAGE_TAG=14  # Use your build number
docker compose down
docker compose pull
docker compose up -d
```

### Access your application:
- Frontend: http://localhost:4200
- API Gateway: https://localhost:8443
- Eureka: http://localhost:8761

---

## 2. 🌐 Remote Deployment (SSH Required)

### When to use:
- Jenkins is on **Machine A**
- You want to deploy to **Machine B** (different server)
- Production/staging deployment scenario

### How it works:
✅ Build happens in Jenkins
✅ Images pushed to DockerHub
✅ Jenkins connects via SSH to remote server
✅ Containers deployed on **remote server**

### SSH Setup Required:

#### Step 1: Generate SSH key on Jenkins
```bash
# Inside Jenkins container
ssh-keygen -t rsa -b 4096 -C "jenkins@deploy"
# Save to: /var/jenkins_home/.ssh/id_rsa
```

#### Step 2: Copy public key to remote server
```bash
# Copy the public key
cat /var/jenkins_home/.ssh/id_rsa.pub

# On remote server (192.168.1.100):
mkdir -p ~/.ssh
echo "PASTE_PUBLIC_KEY_HERE" >> ~/.ssh/authorized_keys
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys
```

#### Step 3: Add SSH credentials to Jenkins
1. Go to: Jenkins → Manage Jenkins → Credentials
2. Click "Add Credentials"
3. Kind: **SSH Username with private key**
4. ID: `deployment-ssh-key`
5. Username: `ssh-user`
6. Private Key: Enter directly (paste from `/var/jenkins_home/.ssh/id_rsa`)
7. Save

#### Step 4: Enable remote deployment
```
DEPLOY_LOCALLY = false   ← Disable local deployment
SKIP_DEPLOY = false      ← Enable remote SSH deployment
```

---

## Current Status

Your pipeline is currently configured for:
```
✅ Local deployment: ENABLED (DEPLOY_LOCALLY=true)
❌ Remote deployment: DISABLED (SKIP_DEPLOY=true)
```

This means:
- Build works ✅
- Images published to DockerHub ✅
- Automatic local deployment ✅
- **No SSH configuration needed** ✅

---

## Quick Start (Recommended)

1. **Run Jenkins pipeline** with default parameters
2. Build will complete and deploy locally
3. Access your app at http://localhost:4200

That's it! No SSH setup required.

---

## Troubleshooting

### Local deployment fails
```bash
# Check if docker compose is accessible from Jenkins
docker exec jenkins-cicd docker compose version

# Check workspace permissions
docker exec jenkins-cicd ls -la /var/jenkins_home/workspace/e-commerce-microservices-ci-cd

# Try manual deployment
cd /var/jenkins_home/workspace/e-commerce-microservices-ci-cd
export IMAGE_TAG=stable
docker compose up -d
```

### Remote deployment connection refused
This is **expected** if:
- SKIP_DEPLOY=true (remote deployment is disabled)
- SSH is not configured yet
- Remote server is not reachable

**Solution**: Keep using local deployment until you need remote deployment.

---

## Summary

**For Development**: Use local deployment (current setup) ✅
- No SSH needed
- Simpler setup
- Faster deployment
- DEPLOY_LOCALLY=true, SKIP_DEPLOY=true

**For Production**: Use remote deployment (requires SSH setup)
- More complex setup
- Deploys to separate server
- DEPLOY_LOCALLY=false, SKIP_DEPLOY=false


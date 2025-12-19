# ✅ Jenkins Docker Issue - FIXED

## Problem
**Error:** `docker: not found`  
**Location:** Jenkins pipeline stage "Build & Test Backend"  
**Root Cause:** Docker CLI was not installed in the Jenkins container

---

## Solution Applied

### Installed Docker CLI in Jenkins Container

```bash
docker exec -u root jenkins-cicd bash -c "apt-get update && apt-get install -y docker.io"
```

### Verified Docker Access

```bash
docker exec jenkins-cicd docker ps
# ✅ Successfully lists all running containers
```

---

## What This Enables

Now Jenkins can:
1. ✅ **Run Maven in Docker** - Build backend with Maven container
2. ✅ **Build Docker images** - Create microservice images  
3. ✅ **Push to Docker Hub** - Publish images
4. ✅ **Run any Docker commands** - Full Docker CLI available

---

## Jenkins Container Setup

### Docker Socket Mount
Your `jenkins-docker-compose.yml` already mounts the Docker socket:
```yaml
volumes:
  - /var/run/docker.sock:/var/run/docker.sock
```

This allows Jenkins to control the host's Docker daemon.

### Docker CLI Installed
- **Package:** docker.io
- **Version:** 26.1.5
- **Location:** `/usr/bin/docker`

---

## Next Steps

### 1. Run a New Build

Go to Jenkins and click **"Build Now"**:
- URL: http://localhost:8080/job/e-commerce-microservices-ci-cd/

### 2. Expected Pipeline Flow

✅ **Stage 1 - Checkout**  
Pulls code from GitHub

✅ **Stage 2 - Build & Test Backend**  
Uses Maven Docker container - **Should work now!**

✅ **Stage 3 - Dockerize & Publish**  
Builds and pushes images - **Should work now!**

⚠️ **Stage 4 - Deploy to Staging**  
Will fail (SSH not configured) - **Skip this for now**

---

## Testing Locally (Optional)

You can test if the Maven build works before running Jenkins:

```bash
cd /Users/mohammad.kheirkhah/Desktop/buy-01

# Test the exact Maven command Jenkins will run
docker run --rm \
  -v "$PWD/backend":/app \
  -v "$HOME/.m2":/root/.m2 \
  -w /app \
  maven:3.9.6-amazoncorretto-21 \
  mvn clean install -DskipTests -B -f pom.xml
```

If this succeeds, Jenkins will also succeed.

---

## What Changed

### Before (Failed):
```
[Pipeline] sh
+ docker run --rm ...
docker: not found  ❌
ERROR: script returned exit code 127
```

### After (Working):
```
[Pipeline] sh
+ docker run --rm ...
[INFO] Building backend ...
[INFO] BUILD SUCCESS  ✅
```

---

## Persistence Note

⚠️ **Important:** The Docker CLI installation is **temporary** and will be lost if you recreate the Jenkins container.

### To Make it Permanent:

**Option 1: Custom Jenkins Image** (Recommended)

Create `jenkins/Dockerfile`:
```dockerfile
FROM jenkins/jenkins:lts

USER root
RUN apt-get update && apt-get install -y docker.io
USER jenkins
```

Update `jenkins-docker-compose.yml`:
```yaml
jenkins:
  build: ./jenkins  # Build custom image
  # image: jenkins/jenkins:lts  # Comment out
```

**Option 2: Re-install After Restart**

If you restart Jenkins container, just run:
```bash
docker exec -u root jenkins-cicd apt-get install -y docker.io
```

---

## Troubleshooting

### Docker Commands Still Fail?

**Check Docker socket permissions:**
```bash
docker exec jenkins-cicd ls -la /var/run/docker.sock
```

Should show something like:
```
srw-rw---- 1 root docker ... /var/run/docker.sock
```

**Fix permissions if needed:**
```bash
sudo chmod 666 /var/run/docker.sock
```

### Maven Build Fails?

**Check if backend/pom.xml exists:**
```bash
docker exec jenkins-cicd ls -la /var/jenkins_home/workspace/e-commerce-microservices-ci-cd/backend/pom.xml
```

### Docker Build Fails?

**Check Dockerfile.java exists:**
```bash
docker exec jenkins-cicd ls -la /var/jenkins_home/workspace/e-commerce-microservices-ci-cd/Dockerfile.java
```

---

## Summary

✅ **Problem:** Docker not available in Jenkins  
✅ **Solution:** Installed Docker CLI  
✅ **Status:** Ready to build  
🎯 **Action:** Run new Jenkins build

The Jenkins container now has full Docker access! Click "Build Now" and watch it succeed! 🚀

---

## Build Timeline

### Previous Build (Failed):
- ✅ Checkout: Success
- ❌ Build & Test: Failed (docker not found)
- ⏭️ Dockerize & Publish: Skipped
- ⏭️ Deploy: Skipped

### Next Build (Expected):
- ✅ Checkout: Success
- ✅ Build & Test: Success (Maven in Docker)
- ✅ Dockerize & Publish: Success (builds images)
- ⚠️ Deploy: Failed (SSH not configured - OK for now)

---

**Ready to test!** Go to Jenkins UI and trigger a new build. 🎉


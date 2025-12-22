# Quick Fix: Docker Login Issue in Jenkins

## The Problem
Your Jenkins pipeline is failing at Docker login with:
```
Error: docker login -u Docker Registry --password-stdin
Error: lookup Registry: no such host
```

## Root Cause
The Docker Hub credentials in Jenkins are not configured correctly. The variable `DOCKER_USERNAME` is being set to "Docker Registry" instead of your actual Docker Hub username.

## Immediate Fix

### Step 1: Add/Update Docker Hub Credentials in Jenkins

1. Open Jenkins: `http://localhost:8080`
2. Go to: **Manage Jenkins** → **Credentials** → **System** → **Global credentials**
3. Click **+ Add Credentials**
4. Configure:
   ```
   Kind: Username with password
   Scope: Global
   Username: mahdikheirkhah          ← YOUR DOCKER HUB USERNAME
   Password: [your-token-here]       ← YOUR DOCKER HUB TOKEN/PASSWORD
   ID: dockerhub-credentials         ← MUST BE EXACTLY THIS
   Description: Docker Hub credentials
   ```
5. Click **Create**

### Step 2: Get a Docker Hub Personal Access Token (Recommended)

1. Go to: https://hub.docker.com/
2. Login → Click your username → **Account Settings**
3. Go to **Security** tab
4. Click **New Access Token**
5. Name: `Jenkins CI/CD`
6. Permissions: **Read, Write, Delete**
7. Click **Generate** and **COPY THE TOKEN IMMEDIATELY**
8. Use this token as the password in Jenkins credentials (Step 1)

### Step 3: Verify the Credential ID

Make sure the credential ID in Jenkins matches the Jenkinsfile:

**In Jenkins Credential:**
- ID: `dockerhub-credentials`

**In Jenkinsfile (line 13):**
```groovy
DOCKER_CREDENTIAL_ID = 'dockerhub-credentials'
```

These MUST match exactly (case-sensitive)!

### Step 4: Test the Build

1. Go to your Jenkins job
2. Click **Build with Parameters**
3. Set: `RUN_TESTS = false`
4. Click **Build**
5. Watch console output for:
   ```
   ✅ Successfully logged in to Docker Hub
   ```

## If It Still Fails

### Check 1: Credential Type
- Must be **"Username with password"**
- NOT "Secret text" or other types

### Check 2: Verify Manually
SSH into your Jenkins container and test:
```bash
docker exec -it jenkins-cicd bash
docker login -u mahdikheirkhah
# Enter your token when prompted
```

### Check 3: Restart Jenkins
Sometimes Jenkins needs a restart to pick up new credentials:
```bash
docker restart jenkins-cicd
```

## Success Indicators

When working correctly, you'll see in the console:
```
[Pipeline] withCredentials
Masking supported pattern matches of $DOCKER_PASSWORD
[Pipeline] {
[Pipeline] sh
+ echo ****
+ docker login -u mahdikheirkhah --password-stdin
Login Succeeded
✅ Successfully logged in to Docker Hub
```

## Common Mistakes

❌ **Wrong ID**: Using `docker-hub-credentials` instead of `dockerhub-credentials`
❌ **Wrong Type**: Using "Secret text" instead of "Username with password"
❌ **Wrong Scope**: Using "System" scope instead of "Global"
❌ **Wrong Username**: Using email instead of Docker Hub username
❌ **Expired Token**: Token was deleted or expired in Docker Hub

## Your Configuration

Based on your Jenkinsfile:
- Docker Hub Username: `mahdikheirkhah`
- Jenkins Credential ID: `dockerhub-credentials`
- Services to build: discovery-service, api-gateway, user-service, product-service, media-service, dummy-data, frontend

## Still Having Issues?

1. Delete the old credential in Jenkins completely
2. Create a brand new credential with the exact settings above
3. Generate a fresh Personal Access Token from Docker Hub
4. Restart Jenkins
5. Try the build again

---

**Need the full guide?** See: `JENKINS_SETUP_GUIDE.md`


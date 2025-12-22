# 🚀 QUICK START - Fix Jenkins Docker Login

## The Problem
```
Error: docker login -u Docker Registry --password-stdin
Error: lookup Registry: no such host
```

## The Solution (5 minutes)

### Step 1: Get Docker Hub Token
1. Go to https://hub.docker.com/
2. Login → Account Settings → Security
3. **New Access Token**
4. Name: `Jenkins CI/CD`
5. Permissions: `Read, Write, Delete`
6. **Generate** → **COPY IT NOW!** ⚠️

### Step 2: Add to Jenkins
1. Open Jenkins: http://localhost:8080
2. **Manage Jenkins** → **Credentials** → **System** → **Global credentials**
3. **+ Add Credentials**
4. Fill in:
   ```
   Kind:        Username with password
   Username:    mahdikheirkhah
   Password:    [paste token from step 1]
   ID:          dockerhub-credentials
   ```
5. **Create**

### Step 3: Build Again
1. Go to your job
2. **Build with Parameters**
3. **Build**
4. Look for: ✅ `Successfully logged in to Docker Hub`

## That's It! 🎉

### If it still fails:
- Check the ID is exactly: `dockerhub-credentials`
- Restart Jenkins: `docker restart jenkins-cicd`
- Try new token from Docker Hub

### Files Created for You:
- 📄 `DOCKER_LOGIN_FIX.md` - Detailed fix guide
- 📄 `JENKINS_SETUP_GUIDE.md` - Complete setup guide
- 📄 `JENKINS_CI_CD_SUMMARY.md` - Project summary

### After Success:
- Images will be pushed to: https://hub.docker.com/u/mahdikheirkhah
- Email sent to: mohammad.kheirkhah@gritlab.ax
- Services deployed locally

---
**Need help?** Read: `DOCKER_LOGIN_FIX.md`


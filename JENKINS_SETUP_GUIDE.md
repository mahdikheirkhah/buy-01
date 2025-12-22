# Jenkins Setup Guide for E-Commerce CI/CD Pipeline

## Problem: Docker Login Failing

If you see this error:
```
Error response from daemon: Get "https://Registry/v2/": dialing Registry:443
```

This means your Docker Hub credentials are not properly configured in Jenkins.

## Step-by-Step Fix

### 1. Configure Docker Hub Credentials in Jenkins

1. **Access Jenkins Dashboard**
   - Open your Jenkins URL (e.g., `http://localhost:8080`)

2. **Navigate to Credentials**
   - Click `Manage Jenkins` (left sidebar)
   - Click `Credentials`
   - Click `System` (under Stores scoped to Jenkins)
   - Click `Global credentials (unrestricted)`

3. **Add New Credentials**
   - Click `+ Add Credentials` button
   - Fill in the form:
     - **Kind**: `Username with password`
     - **Scope**: `Global`
     - **Username**: Your Docker Hub username (e.g., `mahdikheirkhah`)
     - **Password**: Your Docker Hub password OR Personal Access Token (recommended)
     - **ID**: `dockerhub-credentials` (IMPORTANT: must match Jenkinsfile)
     - **Description**: `Docker Hub credentials for CI/CD`
   - Click `Create`

### 2. Get Docker Hub Personal Access Token (Recommended)

Using a Personal Access Token is more secure than your password:

1. Log in to [Docker Hub](https://hub.docker.com/)
2. Click your username in the top right
3. Select `Account Settings`
4. Click `Security` tab
5. Click `New Access Token`
6. Give it a name (e.g., "Jenkins CI/CD")
7. Set permissions: `Read, Write, Delete`
8. Click `Generate`
9. **Copy the token immediately** (you won't see it again!)
10. Use this token as the password in Jenkins credentials

### 3. Verify Your Setup

After adding credentials, test them:

1. Go to your Jenkins job
2. Click `Build with Parameters`
3. Leave defaults and click `Build`
4. Check the console output for:
   ```
   ✅ Successfully logged in to Docker Hub
   ```

## Alternative: Manual Docker Login (For Testing)

If you want to test Docker login manually on your Jenkins server:

```bash
# SSH into your Jenkins server or access its terminal
docker login -u mahdikheirkhah
# Enter your password/token when prompted

# Test if you can push
docker pull hello-world
docker tag hello-world mahdikheirkhah/test:latest
docker push mahdikheirkhah/test:latest
```

## Troubleshooting

### Error: "No such credential ID"

- **Problem**: Jenkins can't find the credential ID
- **Solution**: Make sure the ID in Jenkins is exactly `dockerhub-credentials`
- Check in Jenkinsfile, line 13:
  ```groovy
  DOCKER_CREDENTIAL_ID = 'dockerhub-credentials'
  ```

### Error: "Invalid username or password"

- **Problem**: Wrong credentials or token expired
- **Solutions**:
  1. Verify username is correct (case-sensitive)
  2. Generate a new Personal Access Token
  3. Update the credential in Jenkins with the new token

### Error: "unauthorized: authentication required"

- **Problem**: Docker Hub rate limiting or invalid credentials
- **Solutions**:
  1. Verify you're logged in: `docker login`
  2. Check Docker Hub for rate limits
  3. Use a Personal Access Token instead of password

### Pipeline Shows "Docker Registry" as Username

- **Problem**: Credential variables not being populated
- **Solutions**:
  1. Delete the old credential and create a new one
  2. Make sure the credential type is "Username with password"
  3. Restart Jenkins: `Manage Jenkins` > `Reload Configuration`

## Current Configuration in Jenkinsfile

```groovy
environment {
    DOCKER_REPO = 'mahdikheirkhah'           // Your Docker Hub username
    DOCKER_CREDENTIAL_ID = 'dockerhub-credentials'  // Jenkins credential ID
    IMAGE_TAG = "${env.BUILD_NUMBER}"
    STABLE_TAG = 'stable'
}
```

## Security Best Practices

1. ✅ **Use Personal Access Tokens** instead of passwords
2. ✅ **Limit token permissions** to only what's needed
3. ✅ **Rotate tokens regularly** (every 6 months)
4. ✅ **Use Global credentials scope** for shared resources
5. ⚠️ **Never commit credentials** to Git

## Verification Checklist

Before running the pipeline, verify:

- [ ] Docker Hub account exists and is accessible
- [ ] Personal Access Token is generated and copied
- [ ] Jenkins credential is created with ID: `dockerhub-credentials`
- [ ] Credential username matches your Docker Hub username
- [ ] Credential password is your Personal Access Token
- [ ] You can manually login: `docker login -u mahdikheirkhah`
- [ ] Your Docker Hub repositories exist or you have permission to create them

## Next Steps After Fixing

Once credentials are configured:

1. Run a test build:
   ```
   Build with Parameters > RUN_TESTS=false > Build
   ```

2. Monitor the console output for:
   - ✅ `Successfully logged in to Docker Hub`
   - ✅ `Building discovery-service...`
   - ✅ `image built and published successfully`

3. Verify images are on Docker Hub:
   - Visit: `https://hub.docker.com/u/mahdikheirkhah`
   - You should see your services listed

## Need More Help?

If you're still having issues:

1. Check Jenkins logs: `Manage Jenkins` > `System Log`
2. Verify Docker is running: `docker ps`
3. Check Jenkins user permissions: `whoami` in Jenkins shell
4. Review this guide's troubleshooting section

## Quick Test Script

Run this in your Jenkins job to test credentials:

```bash
echo "Testing Docker Hub credentials..."
docker logout
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
if [ $? -eq 0 ]; then
    echo "✅ Credentials work!"
    docker logout
else
    echo "❌ Credentials failed!"
    exit 1
fi
```


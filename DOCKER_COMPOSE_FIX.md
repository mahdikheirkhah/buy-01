# Docker Compose Installation Fix

## Problem
The Jenkins container did not have Docker Compose installed, causing all deployment stages to fail with the error:
```
docker: 'compose' is not a docker command
```

## Solution Applied
Installed Docker Compose v2.24.5 as a Docker CLI plugin in the Jenkins container.

### Installation Command Used:
```bash
docker exec -u root jenkins-cicd bash -c "mkdir -p /usr/local/lib/docker/cli-plugins && \
  curl -SL https://github.com/docker/compose/releases/download/v2.24.5/docker-compose-linux-aarch64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose && \
  chmod +x /usr/local/lib/docker/cli-plugins/docker-compose"
```

### Verification:
```bash
docker exec jenkins-cicd docker compose version
# Output: Docker Compose version v2.24.5
```

## Changes Made

### 1. Jenkinsfile Updated
- Changed all `docker-compose` commands to `docker compose` (v2 format)
- Updated in stages:
  - Deploy Locally
  - Deploy & Verify
  - Local Deploy Info

### 2. Diagnostic Results
All checks now pass:
- ✅ Docker is installed
- ✅ Jenkins container is running
- ✅ Jenkins can access Docker daemon
- ✅ Docker Compose is available in Jenkins
- ✅ Jenkins is accessible on port 8080
- ✅ All Docker Hub images exist
- ✅ MongoDB container is running
- ✅ Kafka container is running

## Next Steps

### 1. Test the Pipeline
Go to Jenkins (http://localhost:8080) and run a build with these parameters:
- `DEPLOY_LOCALLY = true` (for local deployment)
- `SKIP_DEPLOY = true` (to skip remote SSH deployment)
- `RUN_TESTS = false` (tests require additional setup)

### 2. Expected Outcome
The pipeline should now successfully:
1. ✅ Checkout code
2. ✅ Build backend services
3. ✅ Build and publish Docker images
4. ✅ Deploy locally using Docker Compose

### 3. Access Your Application
After successful deployment:
- Frontend: http://localhost:4200
- API Gateway: https://localhost:8443
- Eureka Dashboard: http://localhost:8761
- MongoDB: localhost:27017 (admin/password)
- Kafka: localhost:9092

## GitHub Webhook Configuration

To enable automatic builds on Git push:

### 1. Configure Jenkins
1. Go to your pipeline job
2. Configure → Build Triggers
3. Check "GitHub hook trigger for GITScm polling"
4. Save

### 2. Configure GitHub Webhook
1. Go to https://github.com/mahdikheirkhah/buy-01/settings/hooks
2. Click "Add webhook"
3. Payload URL: `http://YOUR_PUBLIC_IP:8080/github-webhook/`
4. Content type: `application/json`
5. Select: "Just the push event"
6. Click "Add webhook"

**Note:** For local Jenkins, you need:
- A public IP/domain, OR
- Use ngrok: `ngrok http 8080`
- Then use the ngrok URL in webhook

### 3. Test Webhook
```bash
# Make a small change and push
git commit --allow-empty -m "test webhook"
git push
```

Check Jenkins - a build should start automatically.

## Email Notifications

Email notifications are configured in the Jenkinsfile:
- Success emails: Build details, deployment info
- Failure emails: Error details, troubleshooting tips
- Recipient: mohammad.kheirkhah@gritlab.ax

### To Test Email:
1. Go to Jenkins → Manage Jenkins → System
2. Scroll to "Extended E-mail Notification"
3. Click "Test configuration by sending test e-mail"
4. Enter your email and send

If emails don't work, check:
- SMTP server configuration
- Email credentials
- Firewall/network settings

## Manual Deployment

If you want to deploy manually without Jenkins:

```bash
# Using the latest build number
export IMAGE_TAG=<BUILD_NUMBER>  # e.g., 28
docker compose pull
docker compose up -d

# Or using stable tag
export IMAGE_TAG=stable
docker compose up -d

# Check status
docker compose ps

# View logs
docker compose logs -f

# Stop all services
docker compose down
```

## Troubleshooting

### If Docker Compose stops working after Jenkins restart:
```bash
# Reinstall Docker Compose
docker exec -u root jenkins-cicd bash -c "mkdir -p /usr/local/lib/docker/cli-plugins && \
  curl -SL https://github.com/docker/compose/releases/download/v2.24.5/docker-compose-linux-aarch64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose && \
  chmod +x /usr/local/lib/docker/cli-plugins/docker-compose"
```

### Run diagnostics anytime:
```bash
./diagnostic.sh
```

### View Jenkins logs:
```bash
docker logs jenkins-cicd -f
```

### Access Jenkins container:
```bash
docker exec -it jenkins-cicd bash
```

## Docker Compose Persistence

**Important:** The Docker Compose installation is inside the Jenkins container and will persist across container restarts. However, if you:
- Remove and recreate the Jenkins container
- Rebuild the Jenkins image

You will need to reinstall Docker Compose using the command above.

### To Make It Permanent:
Consider creating a custom Jenkins Docker image with Docker Compose pre-installed. See `JENKINS_CUSTOM_IMAGE.md` (if you want me to create this).

## Summary

✅ **FIXED:** Docker Compose is now installed and working
✅ **UPDATED:** Jenkinsfile uses correct `docker compose` v2 commands
✅ **VERIFIED:** All diagnostic checks pass
✅ **READY:** Pipeline is ready for testing

**Next Action:** Run a test build in Jenkins!


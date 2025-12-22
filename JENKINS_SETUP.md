# Jenkins Docker Setup Guide

## Overview
This guide explains how to set up Jenkins with Docker support for the e-commerce microservices CI/CD pipeline.

## Prerequisites
- Docker and Docker Compose installed on your machine
- Sufficient disk space (at least 10GB free)
- Port 8080 and 50000 available

## Quick Start

### 1. Start Jenkins with Docker Support

```bash
# From the project root directory
docker compose -f docker-compose.jenkins.yml up -d
```

This will:
- Build a custom Jenkins image with Docker CLI and Docker Compose pre-installed
- Create a container named `jenkins-cicd` (required by the Jenkinsfile)
- Mount the Docker socket for Docker-in-Docker functionality
- Create persistent volumes for Jenkins data and Maven cache

### 2. Access Jenkins

Open your browser and go to: http://localhost:8080

### 3. Get Initial Admin Password (if needed)

If this is a fresh install:

```bash
docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword
```

### 4. Configure Jenkins

1. **Install suggested plugins** (if this is a fresh install)
2. **Create admin user**
3. **Configure credentials:**
   - Go to: Manage Jenkins > Credentials > System > Global credentials
   - Add the following credentials:

#### Docker Hub Credentials
- **Kind**: Username with password
- **ID**: `dockerhub-credentials`
- **Username**: Your Docker Hub username
- **Password**: Your Docker Hub password or access token
- **Description**: Docker Hub Credentials

#### GitHub Credentials (if using private repo)
- **Kind**: Username with password
- **ID**: `github-packages-creds`
- **Username**: Your GitHub username
- **Password**: Your GitHub Personal Access Token
- **Description**: GitHub Credentials

### 6. Configure Email Notifications (Optional but Recommended)

#### Quick Setup for Gmail:

1. **Enable 2-Step Verification** on your Gmail account:
   - Go to: https://myaccount.google.com/security
   - Enable 2-Step Verification

2. **Generate App Password**:
   - Go to: https://myaccount.google.com/apppasswords
   - Select app: Mail
   - Select device: Other (Custom name) → "Jenkins"
   - Copy the 16-character password

3. **Configure in Jenkins**:
   - Go to: **Manage Jenkins** → **Configure System**
   - Scroll to **Extended E-mail Notification**:
     ```
     SMTP server: smtp.gmail.com
     SMTP Port: 587
     ☑ Use SMTP Authentication
     User Name: your-email@gmail.com
     Password: [Your 16-character App Password]
     ☑ Use TLS
     Default Recipients: your-email@gmail.com
     ```
   - Scroll to **E-mail Notification**:
     ```
     SMTP server: smtp.gmail.com
     ☑ Use SMTP Authentication
     User Name: your-email@gmail.com
     Password: [Your 16-character App Password]
     ☑ Use TLS
     SMTP Port: 587
     ```
   - Click **Test configuration by sending test e-mail**
   - Enter your email and click **Test configuration**
   - Check your inbox (and spam folder)

4. **If test email doesn't arrive**:
   - See [EMAIL_SETUP.md](EMAIL_SETUP.md) for detailed troubleshooting

### 7. Create Jenkins Pipeline Job

1. Click "New Item"
2. Enter name: `e-commerce-microservices-ci-cd`
3. Select "Pipeline"
4. Under "Pipeline":
   - Definition: Pipeline script from SCM
   - SCM: Git
   - Repository URL: `https://github.com/mahdikheirkhah/buy-01.git`
   - Credentials: Select your GitHub credentials (or leave empty for public repo)
   - Branch: `*/main`
   - Script Path: `Jenkinsfile`
5. Save

### 6. Configure Email Notifications (Optional)

1. Go to: Manage Jenkins > System
2. Find "Extended E-mail Notification"
3. Configure your SMTP settings:
   - SMTP server: `smtp.gmail.com`
   - SMTP port: `587`
   - Use TLS
   - Add credentials for your email
4. Set default recipients

## Verifying the Setup

### Check Docker Access
```bash
docker exec jenkins-cicd docker --version
docker exec jenkins-cicd docker compose version
```

You should see:
- Docker version 29.x.x or higher
- Docker Compose version v5.x.x or higher

### Check Jenkins Logs
```bash
docker logs jenkins-cicd -f
```

Look for: "Jenkins is fully up and running"

### Test a Build

1. Open your pipeline job
2. Click "Build with Parameters"
3. Use default parameters:
   - BRANCH: `main`
   - RUN_TESTS: `false` (tests require embedded MongoDB/Kafka)
   - RUN_SONAR: `false` (unless you have SonarQube configured)
   - SKIP_DEPLOY: `true`
   - DEPLOY_LOCALLY: `true`
4. Click "Build"

## Architecture

### Container Name
The container MUST be named `jenkins-cicd` because the Jenkinsfile uses:
```groovy
docker run --rm --volumes-from jenkins-cicd ...
```

### Volumes
- `jenkins_home`: Persists Jenkins configuration, jobs, and plugins
- `jenkins_m2_cache`: Caches Maven dependencies to speed up builds
- `/var/run/docker.sock`: Allows Jenkins to control the host Docker daemon

### User Permissions
Jenkins runs as `root` inside the container to have access to the Docker socket.

## Troubleshooting

### Issue: "docker: not found"
**Solution**: Your Jenkins container doesn't have Docker CLI installed.
- Rebuild the Jenkins image: `docker compose -f docker-compose.jenkins.yml build`
- Restart Jenkins: `docker compose -f docker-compose.jenkins.yml up -d`

### Issue: "permission denied while trying to connect to Docker daemon"
**Solution**: Docker socket permissions issue.
```bash
# On macOS/Linux
ls -la /var/run/docker.sock
# Should show: srw-rw---- 1 root docker

# If needed, add jenkins to docker group
docker exec -u root jenkins-cicd usermod -aG docker jenkins
docker restart jenkins-cicd
```

### Issue: "Could not find credentials entry with ID 'dockerhub-credentials'"
**Solution**: Add Docker Hub credentials in Jenkins:
1. Go to: Manage Jenkins > Credentials
2. Add "Username with password" credential
3. ID must be: `dockerhub-credentials`

### Issue: Container name is wrong
**Solution**: The container must be named `jenkins-cicd`. Check:
```bash
docker ps | grep jenkins
```

If it's named differently, update docker-compose.jenkins.yml:
```yaml
services:
  jenkins:
    container_name: jenkins-cicd  # Must be this name!
```

### Issue: Build fails at "Dockerize & Publish" stage
**Possible causes:**
1. Docker Hub credentials not configured
2. Network issues
3. Docker Hub repository doesn't exist

**Solutions:**
- Verify credentials are correct
- Create repositories on Docker Hub (or set to public)
- Check Jenkins logs for specific error

### Issue: "docker compose: command not found"
**Solution**: You need Docker Compose v2 (plugin version).
- Check: `docker compose version`
- If you have docker-compose (v1), create an alias or install v2

## Maintenance

### Stop Jenkins
```bash
docker compose -f docker-compose.jenkins.yml down
```

### Restart Jenkins
```bash
docker restart jenkins-cicd
```

### View Logs
```bash
docker logs jenkins-cicd -f
```

### Backup Jenkins Data
```bash
docker run --rm \
  --volumes-from jenkins-cicd \
  -v $(pwd)/backup:/backup \
  alpine tar czf /backup/jenkins-backup-$(date +%Y%m%d).tar.gz /var/jenkins_home
```

### Restore Jenkins Data
```bash
docker run --rm \
  --volumes-from jenkins-cicd \
  -v $(pwd)/backup:/backup \
  alpine tar xzf /backup/jenkins-backup-YYYYMMDD.tar.gz -C /
```

### Clean Up Everything (Start Fresh)
```bash
# WARNING: This will delete all Jenkins data!
docker compose -f docker-compose.jenkins.yml down -v
docker volume rm jenkins_home jenkins_m2_cache
docker rmi jenkins-with-docker:lts
```

## Build Parameters Explained

- **BRANCH**: Git branch to build (default: `main`)
- **RUN_TESTS**: Run unit tests (requires test containers, default: `false`)
- **RUN_SONAR**: Run SonarQube analysis (requires SonarQube server, default: `false`)
- **SKIP_DEPLOY**: Skip deployment stage (default: `true`)
- **DEPLOY_LOCALLY**: Deploy on Jenkins machine without SSH (default: `true`)

## Next Steps

1. Set up webhook on GitHub for automatic builds
2. Configure SonarQube for code quality analysis
3. Set up proper test containers for running tests
4. Configure remote deployment with SSH keys
5. Set up backup automation

## References

- [Jenkins Docker Documentation](https://www.jenkins.io/doc/book/installing/docker/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Jenkins Pipeline Syntax](https://www.jenkins.io/doc/book/pipeline/syntax/)


# Deployment Strategy & Rollback Mechanism

**Last Updated:** January 6, 2026  
**Status:** ✅ Automated Rollback Implemented

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Deployment Flows](#deployment-flows)
3. [Rollback Mechanism](#rollback-mechanism)
4. [Local Deployment](#local-deployment)
5. [Remote Deployment](#remote-deployment)
6. [Recovery Procedures](#recovery-procedures)
7. [Testing Rollback](#testing-rollback)

---

## Overview

The Buy-01 platform implements **automated rollback on deployment failure** to ensure service availability and data integrity. When a deployment fails health checks or container startup, the system automatically:

1. **Detects the failure**
2. **Restores the previous version**
3. **Verifies the rollback**
4. **Logs all events**
5. **Sends notifications**

---

## Deployment Flows

### Deployment Lifecycle

```
┌─────────────────────────────────────────────────────────────┐
│                    Build Pipeline                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. Checkout → 2. Build → 3. Test → 4. SonarQube          │
│                                                              │
│  5. Docker Build & Push                                    │
│                                                              │
│  ↓                                                           │
│                                                              │
│  6. DEPLOY LOCALLY (or DEPLOY REMOTE)                      │
│     ├─ Step 1: Backup Current State 📦                     │
│     ├─ Step 2: Clean & Pull Images 🧹                      │
│     ├─ Step 3: Start Containers 🚀                         │
│     ├─ Step 4: Health Check ✅                             │
│     └─ [PASS] → Success ✅ [FAIL] → Rollback 🔄            │
│                                                              │
│  7. Post-Deployment Verification                           │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## Rollback Mechanism

### Automatic Rollback Process

When a deployment fails, the Jenkinsfile automatically executes this sequence:

#### **Phase 1: Failure Detection**

```groovy
try {
    // Deploy new version
    docker compose up -d
    sleep 30

    // Health checks
    curl http://localhost:8080/api/health

} catch (Exception e) {
    // Failure detected → initiate rollback
    echo "🔄 Initiating automatic rollback..."
}
```

#### **Phase 2: Backup & Restore**

```bash
# Backup Directory Structure
.backup/
├── deployment-{BUILD_NUMBER}-{TIMESTAMP}/
│   ├── docker-compose.yml          # Previous config
│   ├── previous-tag.txt            # Previous IMAGE_TAG
│   ├── containers-before.log       # Running containers
│   ├── images-before.log           # Available images
│   └── rollback-report.txt         # Rollback details
└── latest-backup-path.txt          # Quick reference
```

#### **Phase 3: Container Restoration**

```bash
# 1. Stop failed deployment
docker compose down --remove-orphans

# 2. Restore previous configuration
cp .backup/deployment-{TS}/docker-compose.yml docker-compose.yml

# 3. Restore environment
export IMAGE_TAG=$(cat .backup/deployment-{TS}/previous-tag.txt)

# 4. Start previous version
docker compose up -d --remove-orphans
```

#### **Phase 4: Verification**

```bash
# 1. Wait for services
sleep 20

# 2. Check status
docker compose ps

# 3. Generate report
{
    echo "ROLLBACK REPORT"
    echo "Failed Image Tag: {NEW_TAG}"
    echo "Rolled Back To: {PREVIOUS_TAG}"
    echo "Timestamp: $(date)"
} > .backup/deployment-{TS}/rollback-report.txt
```

#### **Phase 5: Notification**

- Email sent to `mohammad.kheirkhah@gritlab.ax`
- Subject: `❌ Build FAILED with ROLLBACK: Buy-01-Pipeline #{BUILD_NUMBER}`
- Includes backup location and instructions

---

## Local Deployment

### Configuration

**Parameters:**

- `DEPLOY_LOCALLY = true` (default)
- `SKIP_DEPLOY = false` to trigger deployment

### Deployment Steps

```groovy
stage('🚀 Deploy Locally') {
    when {
        allOf {
            expression { params.DEPLOY_LOCALLY == true }
            expression { params.SKIP_DEPLOY == false }
        }
    }
}
```

### What Gets Backed Up

```
Before deployment:
✅ docker-compose.yml
✅ .env (if exists)
✅ Running container list
✅ Available images list
✅ Previous IMAGE_TAG
```

### Health Checks

```bash
# Critical services verified:
✅ API Gateway (http://localhost:8080/api/health)
✅ Eureka Discovery (http://localhost:8761/actuator/health)
✅ Frontend (http://localhost:4200)

# Threshold: 2/3 services must be healthy
```

### Rollback Triggers

```
Rollback executes when:
❌ Docker pull fails
❌ Container startup fails
❌ Services don't respond
❌ Health checks timeout
❌ Exceptions in deployment
```

---

## Remote Deployment

### Configuration

**Parameters:**

- `DEPLOY_LOCALLY = false`
- `SKIP_DEPLOY = false` to trigger deployment

**SSH Credentials:**

```groovy
SSH_CREDENTIAL_ID = 'ssh-deployment-key'
REMOTE_HOST = '192.168.1.100'
REMOTE_USER = 'ssh-user'
DEPLOYMENT_DIR = '/opt/ecommerce'
```

### Deployment Steps

```groovy
stage('🚀 Deploy Remote') {
    when {
        allOf {
            expression { params.SKIP_DEPLOY == false }
            expression { params.DEPLOY_LOCALLY == false }
        }
    }
}
```

### Remote Backup & Restore

```bash
# On remote server:
${DEPLOYMENT_DIR}/.backup/
├── deployment-{TIMESTAMP}/
│   ├── docker-compose.yml
│   └── .env.previous
```

### Remote Health Check

```bash
# After deployment:
curl -sf http://localhost:8080/api/health
# Must return 200 or rollback triggers
```

### Remote Rollback

If deployment fails:

```bash
1. SSH to remote host
2. Restore previous docker-compose.yml
3. Restore previous .env
4. Execute: docker compose up -d --remove-orphans
5. Wait 20 seconds
6. Verify services
7. Save rollback report
```

---

## Recovery Procedures

### Scenario 1: Automatic Rollback Successful

**Symptoms:**

- Build fails
- Jenkins output shows: `🔄 Initiating automatic rollback...`
- Email received: `❌ Build FAILED with ROLLBACK`

**Resolution:**

```bash
# 1. Check backup directory
ls -la .backup/deployment-*/

# 2. View rollback report
cat .backup/deployment-*/rollback-report.txt

# 3. Identify root cause
# - Check build logs for errors
# - Review code changes
# - Fix issues

# 4. Retry build
# Click "Build Now" in Jenkins
```

### Scenario 2: Automatic Rollback Failed

**Symptoms:**

- Email: `❌ CRITICAL: Rollback also failed!`
- Manual intervention required
- Services may be down

**Manual Recovery Steps:**

```bash
# 1. SSH to server
ssh -i ${SSH_KEY} ${REMOTE_USER}@${REMOTE_HOST}

# 2. Check status
docker ps
docker compose ps

# 3. Find latest backup
ls -lt .backup/deployment-*/ | head -1

# 4. Manual restore
BACKUP_DIR=".backup/deployment-{latest}/"
cp "$BACKUP_DIR/docker-compose.yml" .
cp "$BACKUP_DIR/.env.previous" .env

# 5. Start services
docker compose down --remove-orphans
docker compose up -d

# 6. Verify
sleep 30
docker compose ps
curl http://localhost:8080/api/health
```

### Scenario 3: Complete System Failure

**Symptoms:**

- No backup directory found
- Services won't start
- Manual recovery impossible

**Emergency Recovery:**

```bash
# 1. Stop all containers
docker compose down
docker ps -a | grep -v CONTAINER | awk '{print $1}' | xargs docker rm -f

# 2. Pull last known good images
docker pull mahdikheirkhah/api-gateway:stable
docker pull mahdikheirkhah/user-service:stable
docker pull mahdikheirkhah/product-service:stable
# ... etc

# 3. Update docker-compose.yml with stable tags
sed -i 's/IMAGE_TAG=.*/IMAGE_TAG=stable/' .env

# 4. Start services
docker compose up -d

# 5. Verify
docker compose ps
curl http://localhost:8080/api/health
```

---

## Testing Rollback

### Test Case 1: Simulate Deployment Failure

```bash
# 1. Modify docker-compose.yml to cause failure
# Edit service to use wrong image or port

# 2. Trigger build
Jenkins → Buy-01-Pipeline → Build with Parameters
  SKIP_DEPLOY = false
  DEPLOY_LOCALLY = true

# 3. Observe rollback
# Should see:
# ✅ "Backing up current deployment state"
# ❌ "Local deployment failed"
# 🔄 "Initiating automatic rollback"
# ✅ "Rollback COMPLETED"

# 4. Verify previous version running
docker compose ps
docker images | grep mahdikheirkhah
```

### Test Case 2: Health Check Failure

```bash
# 1. Add failing health check endpoint
# Modify QUICK_TEST_GUIDE to check non-existent endpoint

# 2. Trigger build
# Build will:
# - Deploy new version
# - Run health checks
# - Detect failure (0/3 passing)
# - Trigger rollback

# 3. Verify
curl http://localhost:8080/api/health  # Should work
cat .backup/*/rollback-report.txt       # Review report
```

### Test Case 3: Container Startup Failure

```bash
# 1. Set impossible resource limits in docker-compose
# memory: 1m (too low)
# cpus: 0.001 (too low)

# 2. Trigger build
# Containers will fail to start
# Rollback will restore previous version

# 3. Verify
docker compose ps              # All services running
docker logs api-gateway | head # No errors
```

---

## Backup & Cleanup

### Automatic Cleanup

The Jenkins pipeline includes cleanup that:

- Keeps last 30 builds
- Keeps last 10 artifacts
- Removes workspace after each build

### Manual Cleanup

```bash
# View backup sizes
du -sh .backup/deployment-*/

# Remove old backups (keep recent 5)
ls -dt .backup/deployment-*/ | tail -n +6 | xargs rm -rf

# Archive old backups
tar -czf backups-archive-$(date +%Y%m%d).tar.gz .backup/
rm -rf .backup/
mkdir -p .backup
```

---

## Monitoring & Alerts

### Email Notifications

**Success:**

```
✅ Build SUCCESS: Buy-01-Pipeline #{BUILD_NUMBER}
- Image: {IMAGE_TAG}
- Duration: {TIME}
```

**Failure with Rollback:**

```
❌ Build FAILED with ROLLBACK: Buy-01-Pipeline #{BUILD_NUMBER}
- Failed Image: {NEW_TAG}
- Rolled Back To: {PREVIOUS_TAG}
- Backup Location: .backup/
```

**Critical Failure:**

```
❌ Build FAILED - Manual Intervention Required
- Rollback Failed
- Services may be down
- Check manual recovery procedures
```

### Log Locations

```
Jenkins Logs:
- Console Output: http://localhost:8080/job/Buy-01-Pipeline/{BUILD_NUMBER}/console

Local Deployment Logs:
- Backup: .backup/deployment-{TS}/
- Reports: .backup/deployment-{TS}/rollback-report.txt
- Docker: docker logs {service-name}

Remote Deployment Logs:
- SSH to ${REMOTE_HOST}:${DEPLOYMENT_DIR}/.backup/
```

---

## Configuration Reference

### Jenkinsfile Settings

```groovy
// Deployment Parameters
SKIP_DEPLOY = false                 // false to deploy
DEPLOY_LOCALLY = true               // true for local, false for remote

// Remote Settings (if DEPLOY_LOCALLY = false)
SSH_CREDENTIAL_ID = 'ssh-deployment-key'
REMOTE_HOST = '192.168.1.100'
REMOTE_USER = 'ssh-user'
DEPLOYMENT_DIR = '/opt/ecommerce'

// Docker Settings
DOCKER_REPO = 'mahdikheirkhah'
IMAGE_TAG = "${env.BUILD_NUMBER}"
STABLE_TAG = 'stable'
```

### Docker Compose Configuration

```yaml
version: "3.8"

services:
  api-gateway:
    image: ${DOCKER_REPO}/api-gateway:${IMAGE_TAG}
    # ... other services
```

### Environment Variables

```bash
# Set before deployment
export IMAGE_TAG=123                           # Build number
export DOCKER_REPO=mahdikheirkhah             # Docker repo
export DEPLOYMENT_DIR=/opt/ecommerce          # Remote dir
```

---

## Troubleshooting

### Issue: Rollback not executing

**Check:**

```bash
# 1. Verify backup directory
ls -la .backup/

# 2. Check Jenkinsfile syntax
grep "🔄 Rolling back" Jenkinsfile

# 3. Review Jenkins logs
http://localhost:8080/job/Buy-01-Pipeline/{BUILD}/logs
```

### Issue: Services not starting after rollback

**Check:**

```bash
# 1. View Docker errors
docker logs {service-name}

# 2. Check volume mounts
docker inspect {container-id} | grep -A 10 Mounts

# 3. Verify image availability
docker images | grep mahdikheirkhah
```

### Issue: Backup directory growing too large

**Solution:**

```bash
# 1. Remove backups older than 7 days
find .backup -type d -mtime +7 -exec rm -rf {} \;

# 2. Archive old backups
tar -czf backups-old.tar.gz .backup/
rm -rf .backup/*

# 3. Update cleanup script
# Add to crontab: 0 0 * * * cd /path && find .backup -type d -mtime +7 -exec rm -rf {} \;
```

---

## Best Practices

✅ **DO:**

- Always test deployments in local environment first
- Review backup reports before deleting them
- Keep stable tags updated
- Test rollback procedures periodically
- Monitor deployment notifications
- Document any manual interventions

❌ **DON'T:**

- Deploy during critical business hours without warning
- Delete .backup directory without archiving
- Modify docker-compose.yml without testing
- Ignore deployment failure notifications
- Mix local and remote deployments in same build
- Use hardcoded image tags

---

## Quick Reference

### Test Deployment with Rollback

```bash
# 1. Navigate to project
cd /Users/mohammad.kheirkhah/Desktop/buy-01

# 2. Trigger Jenkins build
curl -X POST http://localhost:8080/job/Buy-01-Pipeline/buildWithParameters \
  -u admin:admin \
  -d "DEPLOY_LOCALLY=true" \
  -d "SKIP_DEPLOY=false"

# 3. Monitor build
open http://localhost:8080/job/Buy-01-Pipeline/

# 4. Check backup (after build)
ls -la .backup/
cat .backup/deployment-*/rollback-report.txt
```

### View Rollback History

```bash
# List all rollbacks
ls -lt .backup/deployment-*/ | head -10

# View specific rollback report
cat .backup/deployment-{TS}/rollback-report.txt

# Check rollback logs
docker compose logs --tail=100
```

### Check Current Deployment

```bash
# Running version
docker compose ps
docker images | grep mahdikheirkhah

# Service health
curl http://localhost:8080/api/health
curl http://localhost:8761/actuator/health
curl http://localhost:4200
```

---

## Summary

| Feature              | Local | Remote |
| -------------------- | ----- | ------ |
| Backup Before Deploy | ✅    | ✅     |
| Health Checks        | ✅    | ✅     |
| Auto Rollback        | ✅    | ✅     |
| Rollback Reports     | ✅    | ✅     |
| Email Notifications  | ✅    | ✅     |
| Manual Restore       | ✅    | ✅     |

**Status: PRODUCTION READY** ✅

---

**Last Reviewed:** January 6, 2026  
**Next Review:** January 20, 2026  
**Owner:** DevOps Team

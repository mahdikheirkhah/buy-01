# Jenkins Pipeline - Quick Reference Card

## 🎯 Common Build Scenarios

### 1️⃣ Local Deployment (Recommended for Development)
```
✅ SKIP_DEPLOY: true
✅ DEPLOY_LOCALLY: true
```
**Result**: Builds, publishes, and deploys to Jenkins machine  
**Access**: http://localhost:4200

---

### 2️⃣ Build & Publish Only
```
✅ SKIP_DEPLOY: true
⬜ DEPLOY_LOCALLY: false
```
**Result**: Builds and publishes Docker images  
**Manual Deploy**:
```bash
export IMAGE_TAG=26
docker compose up -d
```

---

### 3️⃣ Remote SSH Deployment
```
⚠️ SKIP_DEPLOY: false
⚠️ DEPLOY_LOCALLY: false
```
**Result**: Deploys to remote server via SSH  
**Requires**: SSH credentials configured in Jenkins

---

## 🔑 Required Credentials

### Docker Hub (Required)
- **Location**: Jenkins → Manage Jenkins → Credentials
- **Type**: Username with password
- **ID**: `dockerhub-credentials`
- **Username**: Your Docker Hub username
- **Password**: Docker Hub token or password

### SSH Credentials (Optional)
- **Type**: SSH Username with private key
- **ID**: `ssh-deployment-key`
- **Username**: SSH user on remote server
- **Private Key**: Your SSH private key

---

## 🚀 Quick Commands

### Check Services
```bash
docker compose ps
```

### View Logs
```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f user-service
```

### Restart Services
```bash
docker compose restart
```

### Full Restart
```bash
docker compose down
docker compose up -d
```

### Rollback to Stable
```bash
export IMAGE_TAG=stable
docker compose down
docker compose pull
docker compose up -d
```

---

## 🌐 Access Points

- **Frontend**: http://localhost:4200
- **API Gateway**: https://localhost:8443
- **Eureka**: http://localhost:8761
- **MongoDB**: mongodb://localhost:27017

---

## 📧 Email Notifications

**Recipient**: mohammad.kheirkhah@gritlab.ax

- ✅ Success: Build and deployment successful
- ❌ Failure: Build or deployment failed (with console link)

---

## 🐛 Common Issues

### "docker-compose not found"
✅ Fixed - using `docker compose` (v2)

### "SSH credentials not found"
**Solution**: Use local deployment instead:
- Set `SKIP_DEPLOY=true`
- Set `DEPLOY_LOCALLY=true`

### "Docker login failed"
**Solution**: Check Docker Hub credentials:
- ID must be: `dockerhub-credentials`
- Use access token instead of password

### Services not starting
```bash
docker compose logs -f
docker compose ps
docker compose restart
```

---

## 📚 Full Documentation

- **README.md** - Project overview
- **JENKINS_DEPLOYMENT_GUIDE.md** - Complete guide
- **TODO.md** - Project roadmap
- **ISSUE_RESOLUTION.md** - Recent fixes

---

## 🎯 Next Steps (from TODO.md)

### This Week:
1. ✅ Fix deployment logic - DONE
2. ⏳ Fix test infrastructure
3. ⏳ Set up SonarQube

### This Month:
4. ⏳ Security hardening
5. ⏳ Monitoring setup (Prometheus/Grafana)

---

**Quick Help**: See JENKINS_DEPLOYMENT_GUIDE.md  
**Version**: 1.0  
**Last Updated**: December 22, 2025


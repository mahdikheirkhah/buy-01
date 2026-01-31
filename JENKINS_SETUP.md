# 🔄 Jenkins CI/CD Setup Guide

## ⚠️ IMPORTANT: Correct Startup Order

**ALWAYS run the core project BEFORE Jenkins!**

```bash
# Step 1: Start the core project (microservices, database, etc.)
docker-compose up -d

# Wait 30-60 seconds for services to be healthy

# Step 2: Verify core services are running
docker ps | grep -E "(api-gateway|user-service|mongo|kafka|sonarqube)"

# Step 3: Start Jenkins (in a separate terminal)
docker-compose -f docker-compose.jenkins.yml up -d

# Step 4: Access Jenkins
# URL: http://localhost:8080
```

## ❌ What NOT To Do

**DON'T start Jenkins first!**

```bash
# ❌ WRONG - This will fail
docker-compose -f docker-compose.jenkins.yml up -d
```

**Why?** Jenkins needs the `buy-01_BACKEND` network which is created by the core project (`docker-compose.yml`). If you start Jenkins first, this network won't exist.

## ✅ Correct Workflow

### Option A: Core Project Only (for learning/development)

```bash
cd /Users/mohammad.kheirkhah/Desktop/buy-01

# Start core project
make all

# Access at:
# - Frontend: https://localhost:4200
# - Eureka: http://localhost:8761
# - SonarQube: http://localhost:9000
```

### Option B: Core Project + Jenkins (for CI/CD testing)

```bash
cd /Users/mohammad.kheirkhah/Desktop/buy-01

# Terminal 1: Start core project
docker-compose up -d

# Wait for services to start...
sleep 60

# Terminal 2: Start Jenkins
docker-compose -f docker-compose.jenkins.yml up -d

# Access at:
# - Frontend: https://localhost:4200
# - Jenkins: http://localhost:8080
# - SonarQube: http://localhost:9000
```

### Option C: Use the Setup Script (Recommended)

```bash
./setup.sh --clean --jenkins

# This script:
# 1. Cleans Docker (optional)
# 2. Starts core project
# 3. Waits for services to be healthy
# 4. Starts Jenkins
```

## 🐛 Troubleshooting

### Problem: "Network buy-01_BACKEND does not exist"

**Cause**: Jenkins started before core project

**Solution**:

```bash
# Stop Jenkins
docker-compose -f docker-compose.jenkins.yml down

# Ensure core project is running
docker-compose up -d

# Wait 30 seconds
sleep 30

# Start Jenkins
docker-compose -f docker-compose.jenkins.yml up -d
```

### Problem: "Container buy-01 exited (48)"

**Cause**: MongoDB port conflict (27017 already in use)

**Solution**:

```bash
# Check what's using port 27017
lsof -i :27017

# Kill the process
kill -9 <PID>

# Or find and stop conflicting containers
docker ps | grep mongo
docker stop <container-name>

# Restart core project
docker-compose down
docker-compose up -d
```

### Problem: Services won't connect to Jenkins

**Cause**: Network misconfiguration

**Solution**:

```bash
# Check if buy-01_BACKEND network exists
docker network ls | grep buy-01

# If it doesn't exist, restart core project
docker-compose down
docker-compose up -d

# Verify network
docker network inspect buy-01_BACKEND

# Check Jenkins is connected
docker inspect jenkins-cicd | grep -A 20 NetworkSettings
```

## 📊 Monitoring Jenkins

```bash
# View Jenkins logs
docker logs -f jenkins-cicd

# Check Jenkins health
curl http://localhost:8080/login

# Access Jenkins container shell
docker exec -it jenkins-cicd bash

# Check Jenkins memory usage
docker stats jenkins-cicd
```

## 🔑 Jenkins Initial Setup

### First Time Access

1. **Get Initial Admin Password**:

```bash
docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword
```

2. **Access Jenkins**:

   - URL: `http://localhost:8080`
   - Username: `admin`
   - Password: [paste the password from above]

3. **Install Recommended Plugins**:
   - Jenkins will prompt you to install plugins
   - Click "Install suggested plugins"
   - Wait for plugins to install (2-5 minutes)

### Configure Credentials

#### 1. Docker Hub Credentials

```
Jenkins > Manage Jenkins > Manage Credentials > System > Global credentials > Add Credentials

Kind: Username with password
Username: [your-docker-username]
Password: [your-docker-token or password]
ID: dockerhub-credentials
```

#### 2. GitHub Credentials (Optional)

```
Kind: Username with password
Username: [your-github-username]
Password: [your-github-personal-access-token]
ID: github-credentials
```

**How to get GitHub Personal Access Token:**

1. Go to GitHub: Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Click "Generate new token (classic)"
3. Select scopes: `repo`, `admin:repo_hook` (for webhooks)
4. Copy the token (you won't see it again!)

#### 3. SonarQube Token (Optional)

```
Kind: Secret text
Secret: [your-sonarqube-token]
ID: sonarqube-token
```

### Create Pipeline Job

1. **New Item** → Enter name: `Buy-01-Pipeline` → Select **Pipeline**

2. **Definition**: Pipeline script from SCM

3. **SCM**: Git

   - Repository URL: `https://github.com/mahdikheirkhah/buy-01.git`
   - Credentials: Select `github-credentials` (if using private repo)
   - Branch: `*/main`
   - Script path: `Jenkinsfile`

4. **Build Triggers** (Optional - for automatic builds):

   - Check **"GitHub hook trigger for GITScm polling"**

5. **Save** and test with **Build Now**

### Configure GitHub Webhook (For Automatic Builds)

If you want Jenkins to automatically build when you push code to GitHub:

#### Option 1: Using ngrok (for local development)

1. **Start Jenkins with ngrok:**

   ```bash
   ./setup.sh --jenkins --ngrok
   ```

2. **Get your webhook URL:**

   - The setup script will display: `https://[random-name].ngrok-free.app/github-webhook/`
   - Or check ngrok dashboard: http://localhost:4040

3. **Add webhook to GitHub:**

   - Go to your repository: Settings → Webhooks → Add webhook
   - **Payload URL**: `https://[your-ngrok-url].ngrok-free.app/github-webhook/`
   - **Content type**: `application/json`
   - **Which events**: Select "Just the push event"
   - Click **Add webhook**

4. **Test it:**
   - Make a commit and push to GitHub
   - Jenkins should automatically start a build
   - Check Jenkins dashboard: http://localhost:8080

**Example webhook URL:**

```
https://alida-ungravitational-overstudiously.ngrok-free.app/github-webhook/
```

**⚠️ Note**: ngrok URLs change every time you restart ngrok, so you'll need to update the webhook URL in GitHub each time.

#### Option 2: Using public Jenkins server

If you have a publicly accessible Jenkins server:

1. **Webhook URL**: `https://your-jenkins-domain.com/github-webhook/`
2. Follow steps 3-4 from Option 1 above

## 📝 Useful Commands

```bash
# Start/Stop Services
docker-compose up -d                          # Start core
docker-compose -f docker-compose.jenkins.yml up -d    # Start Jenkins
docker-compose down                           # Stop core
docker-compose -f docker-compose.jenkins.yml down     # Stop Jenkins

# View Status
docker ps                                     # List containers
docker-compose ps                             # Services in core project
docker-compose -f docker-compose.jenkins.yml ps      # Jenkins status

# View Logs
docker-compose logs -f                        # Core project logs
docker logs -f jenkins-cicd                   # Jenkins logs

# Full Cleanup (Warning: Removes all data!)
docker system prune -a --volumes --force
docker-compose up -d                          # Restart everything
docker-compose -f docker-compose.jenkins.yml up -d
```

## 🎯 Common Issues & Solutions

| Issue                                   | Cause                       | Solution                                      |
| --------------------------------------- | --------------------------- | --------------------------------------------- |
| Network error                           | Jenkins started before core | `docker-compose up -d` first                  |
| MongoDB won't start                     | Port 27017 in use           | `lsof -i :27017` and kill conflicting process |
| Jenkins can't connect to services       | Network not available       | Restart core project                          |
| Jenkins won't start                     | Docker socket permission    | Run as root (already configured)              |
| Pipeline fails with "service not found" | Typo in service name        | Check service names in docker-compose.yml     |

## 🔄 Typical Jenkins Workflow

```bash
# 1. Start core project
docker-compose up -d
sleep 60

# 2. Start Jenkins
docker-compose -f docker-compose.jenkins.yml up -d
sleep 30

# 3. Get Jenkins password
docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword

# 4. Access http://localhost:8080
# Login with admin + password from above

# 5. Create pipeline job (use Jenkinsfile from repo)

# 6. Configure credentials (Docker Hub, GitHub, SonarQube)

# 7. Build the pipeline
# Jenkins will:
#   - Checkout code
#   - Build services
#   - Run tests
#   - Run SonarQube analysis
#   - Build Docker images
#   - Deploy locally or remotely
```

## 📚 Next Steps

1. **Get Jenkins running** following this guide
2. **Configure credentials** for Docker Hub and GitHub
3. **Create pipeline job** pointing to `Jenkinsfile`
4. **Test the pipeline** with "Build Now"
5. **Monitor builds** and check logs for issues

## ⚠️ Remember

- **Always** start core project first: `docker-compose up -d`
- **Wait** 30-60 seconds for services to be healthy
- **Then** start Jenkins: `docker-compose -f docker-compose.jenkins.yml up -d`
- **Check logs** if things don't work: `docker-compose logs -f`

---

**Need Help?** Check the troubleshooting section or view logs:

```bash
docker-compose logs -f api-gateway
docker logs -f jenkins-cicd
```

# 🚀 Local Development & Testing Guide

## What You Have Working NOW

✅ **Docker Compose setup** - Can run all services locally  
✅ **Jenkins in Docker** - Running at http://localhost:8080  
✅ **All microservices** - Built and ready  
✅ **Frontend** - Angular application  
✅ **Git repository** - Clean and pushed to GitHub  

---

## 🎯 What You Can Do Right Now (No Remote Server Needed)

### Option 1: Run Everything Locally with Docker Compose

This is the **simplest** way to test your application:

```bash
cd /Users/mohammad.kheirkhah/Desktop/buy-01

# Set the image tag
export IMAGE_TAG=latest

# Start all services
docker compose up -d

# Check status
docker compose ps

# View logs
docker compose logs -f

# Access the application
open https://localhost:4200
```

**Benefits:**
- ✅ No Jenkins needed
- ✅ No remote server needed
- ✅ Test everything locally
- ✅ Fast development cycle

---

### Option 2: Use Jenkins for Local CI/CD

Jenkins can build and test your code automatically:

```bash
# Jenkins is already running at:
# http://localhost:8080

# Get admin password:
docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword
```

**Configure Jenkins for LOCAL deployment:**
1. Create a new pipeline job
2. Point it to your GitHub repo
3. **Skip the SSH deployment stage** (comment it out in Jenkinsfile)
4. Let Jenkins build Docker images
5. Manually run `docker compose up` after Jenkins builds

---

### Option 3: Manual Build & Deploy Script

Use your `deploy.sh` script **without** the deployment stage:

```bash
cd /Users/mohammad.kheirkhah/Desktop/buy-01

# Set environment
export DOCKER_USERNAME=mahdikheirkhah
export DOCKER_PASSWORD=your_token_here
export IMAGE_TAG=v1.0.0
export SKIP_BUILD=false  # Build images

# Build and push images (stops before deployment)
bash deploy.sh
```

Then start services locally:
```bash
docker compose pull
docker compose up -d
```

---

## 🛠️ Simplified Jenkinsfile (Local Only)

Want to use Jenkins without remote deployment? Here's a simplified version:

```groovy
pipeline {
    agent any
    
    environment {
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        DOCKER_CREDENTIAL_ID = 'dockerhub-creds'
        DOCKER_REPO = 'mahdikheirkhah'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build & Test') {
            steps {
                script {
                    echo 'Building microservices...'
                    sh './mvnw clean install -DskipTests'
                }
            }
        }
        
        stage('Docker Build & Push') {
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: env.DOCKER_CREDENTIAL_ID,
                        passwordVariable: 'DOCKER_PASSWORD',
                        usernameVariable: 'DOCKER_USERNAME'
                    )]) {
                        sh "echo \$DOCKER_PASSWORD | docker login -u \$DOCKER_USERNAME --password-stdin"
                    }
                    
                    def services = ['discovery-service', 'api-gateway', 'user-service', 'product-service', 'media-service', 'dummy-data']
                    
                    for (service in services) {
                        sh """
                            docker build -t ${env.DOCKER_REPO}/${service}:${env.IMAGE_TAG} \
                                -f Dockerfile.java --build-arg SERVICE_NAME=${service} .
                            docker push ${env.DOCKER_REPO}/${service}:${env.IMAGE_TAG}
                        """
                    }
                    
                    sh """
                        docker build -t ${env.DOCKER_REPO}/frontend:${env.IMAGE_TAG} \
                            -f frontend/Dockerfile frontend/
                        docker push ${env.DOCKER_REPO}/frontend:${env.IMAGE_TAG}
                    """
                }
            }
        }
        
        // DEPLOYMENT STAGE REMOVED - Deploy manually with docker compose
    }
    
    post {
        always {
            echo "Build complete! Run 'docker compose up -d' to start services."
        }
    }
}
```

---

## 📋 Quick Start Commands

### Start Everything:
```bash
export IMAGE_TAG=latest
docker compose up -d
```

### Stop Everything:
```bash
docker compose down
```

### View Logs:
```bash
docker compose logs -f discovery-service
docker compose logs -f api-gateway
docker compose logs -f frontend
```

### Rebuild a Service:
```bash
docker compose build discovery-service
docker compose up -d discovery-service
```

### Access Services:
- Frontend: https://localhost:4200
- Discovery (Eureka): http://localhost:8761
- API Gateway: https://localhost:8443

---

## 🎓 What You've Accomplished

✅ **Microservices Architecture** - Discovery, Gateway, 3 services  
✅ **Containerization** - All services Dockerized  
✅ **CI/CD Pipeline** - Jenkinsfile ready  
✅ **Docker Compose** - Full orchestration  
✅ **Git Workflow** - Clean repository  
✅ **SSL Configuration** - HTTPS enabled  
✅ **MongoDB Integration** - Database ready  
✅ **Kafka Integration** - Message queue ready  
✅ **Frontend** - Angular application  

---

## 🔮 Future: When You Want Remote Deployment

When you're ready to deploy to a production server:

1. **Get a VPS** (DigitalOcean $4/month or AWS free tier)
2. **Enable SSH** on the VPS
3. **Update Jenkinsfile** with VPS IP
4. **Add SSH credentials** to Jenkins
5. **Run the full pipeline**

But for now, **everything works locally!** 🎉

---

## 📚 Documentation Files (For Reference)

These files are here if you need them later:
- `SERVER_ANALYSIS.md` - Analysis of 213.204.48.87
- `SSH_TROUBLESHOOTING.md` - SSH setup guide
- `HOW_TO_ENABLE_SSH.md` - Step-by-step SSH
- `JENKINS_SETUP.md` - Jenkins configuration
- `JENKINS_CONTAINER_ACCESS.md` - Jenkins commands
- `DEPLOYMENT_BLOCKED.md` - Deployment issues

**You don't need these right now.** Focus on local development!

---

## ✨ Next Steps (Recommended)

1. ✅ **Test locally:**
   ```bash
   docker compose up -d
   open https://localhost:4200
   ```

2. ✅ **Make changes to your code**

3. ✅ **Rebuild and test:**
   ```bash
   docker compose build
   docker compose up -d
   ```

4. ✅ **Push to GitHub:**
   ```bash
   git add .
   git commit -m "your changes"
   git push
   ```

5. 🔄 **When ready for production:** Set up a VPS and enable SSH

---

**Focus:** Build amazing features locally, deploy to cloud when you're ready! 🚀


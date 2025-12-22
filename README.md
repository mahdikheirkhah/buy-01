# E-Commerce Microservices Platform - Jenkins CI/CD

## 🎯 Project Overview

This is a fully-featured e-commerce platform built with microservices architecture, featuring automated CI/CD pipeline using Jenkins, Docker containerization, and cloud-ready deployment.

### Technology Stack

**Backend:**
- Java 21 with Spring Boot 3.2.8
- Spring Cloud (Eureka, Gateway)
- MongoDB (Database)
- Apache Kafka (Message Broker)
- Maven 3.9.6

**Frontend:**
- Angular 18
- TypeScript
- Material Design

**DevOps:**
- Jenkins (CI/CD Automation)
- Docker & Docker Compose
- GitHub (Version Control)
- Docker Hub (Container Registry)

**Optional Tools:**
- SonarQube (Code Quality Analysis)
- JUnit (Testing)

---

## 🏗️ Architecture

### Microservices:
1. **Discovery Service** (Port 8761) - Eureka Server for service discovery
2. **API Gateway** (Port 8443) - HTTPS entry point with routing and security
3. **User Service** (Port 8081) - User management and authentication
4. **Product Service** (Port 8082) - Product catalog management
5. **Media Service** (Port 8083) - Image/file upload and management
6. **Dummy Data Service** - Populates test data on startup
7. **Frontend** (Port 4200) - Angular SPA

### Infrastructure:
- **MongoDB** (Port 27017) - NoSQL database
- **Kafka** (Port 9092) - Message broker for async communication
- **Zookeeper** (Port 2181) - Kafka coordination service

---

## 🚀 Quick Start

### Prerequisites
- Docker Desktop or Docker Engine
- Jenkins (running in Docker or standalone)
- GitHub account
- Docker Hub account

### 1. Clone Repository
```bash
git clone https://github.com/mahdikheirkhah/buy-01.git
cd buy-01
```

### 2. Configure Jenkins Credentials

#### Docker Hub Credentials:
1. Jenkins → Manage Jenkins → Credentials
2. Add Credentials → Username with password
3. **ID**: `dockerhub-credentials`
4. **Username**: Your Docker Hub username
5. **Password**: Docker Hub access token

#### SSH Credentials (Optional - for remote deployment):
1. Add Credentials → SSH Username with private key
2. **ID**: `ssh-deployment-key`
3. **Username**: SSH user on remote server
4. **Private Key**: Your SSH private key

### 3. Create Jenkins Pipeline Job
1. Jenkins Dashboard → New Item
2. Enter name: `e-commerce-microservices-ci-cd`
3. Select **Pipeline**
4. Pipeline Definition: **Pipeline script from SCM**
5. SCM: **Git**
6. Repository URL: `https://github.com/mahdikheirkhah/buy-01.git`
7. Branch: `*/main`
8. Script Path: `Jenkinsfile`
9. Save

### 4. Run the Pipeline

**For Local Deployment (Recommended):**
1. Click "Build with Parameters"
2. Set:
   - `SKIP_DEPLOY`: **true**
   - `DEPLOY_LOCALLY`: **true**
3. Click "Build"

**For Build Only (No Deployment):**
1. Click "Build with Parameters"
2. Set:
   - `SKIP_DEPLOY`: **true**
   - `DEPLOY_LOCALLY`: **false**
3. Click "Build"

### 5. Access Application
After successful deployment:
- Frontend: http://localhost:4200
- API Gateway: https://localhost:8443
- Eureka Dashboard: http://localhost:8761

---

## 📚 Documentation

- **[JENKINS_DEPLOYMENT_GUIDE.md](./JENKINS_DEPLOYMENT_GUIDE.md)** - Complete deployment guide
- **[TODO.md](./TODO.md)** - Project roadmap and pending tasks
- **[Jenkinsfile](./Jenkinsfile)** - Pipeline configuration
- **[docker-compose.yml](./docker-compose.yml)** - Container orchestration

---

## 🎯 Jenkins Pipeline Features

### ✅ Implemented:
- ✅ Automated build for all microservices
- ✅ Docker image creation and publishing
- ✅ Multi-tag strategy (build number + stable)
- ✅ Local deployment via Jenkins
- ✅ Remote SSH deployment support
- ✅ Email notifications (success/failure)
- ✅ Parameterized builds
- ✅ Build artifact management
- ✅ Zero-downtime deployment
- ✅ Health checks for services
- ✅ Rollback capability

### 🔄 Optional Features:
- Automated testing (enable with `RUN_TESTS=true`)
- SonarQube integration (enable with `RUN_SONAR=true`)

---

## 🛠️ Pipeline Stages

1. **Checkout** - Clone code from GitHub
2. **Build & Test Backend** - Maven build all services
3. **Test Backend Services** (Optional) - Run unit/integration tests
4. **SonarQube Analysis** (Optional) - Code quality scanning
5. **Dockerize & Publish** - Build and push Docker images
6. **Deploy Locally** (Optional) - Deploy on Jenkins machine
7. **Deploy & Verify** (Optional) - Deploy to remote server via SSH
8. **Local Deploy Info** - Show deployment instructions

---

## 📦 Docker Images

All images are published to Docker Hub: `mahdikheirkhah/*`

### Services:
- `mahdikheirkhah/discovery-service:latest`
- `mahdikheirkhah/api-gateway:latest`
- `mahdikheirkhah/user-service:latest`
- `mahdikheirkhah/product-service:latest`
- `mahdikheirkhah/media-service:latest`
- `mahdikheirkhah/dummy-data:latest`
- `mahdikheirkhah/frontend:latest`

### Tags:
- **Build Number** (e.g., `26`) - Specific build version
- **stable** - Last successful deployment

---

## 🔐 Security Features

- ✅ HTTPS/TLS support on API Gateway
- ✅ MongoDB authentication
- ✅ Jenkins credentials management
- ✅ Docker secrets (planned)
- ✅ CORS configuration
- ✅ JWT authentication (in progress)
- ✅ Rate limiting (planned)

---

## 🧪 Testing

### Unit Tests:
```bash
# In each service directory
mvn test
```

### Integration Tests:
```bash
# Run with Jenkins pipeline
Build Parameters → RUN_TESTS: true
```

### Manual Testing:
Access Swagger UI at: `https://localhost:8443/swagger-ui.html`

---

## 📊 Monitoring & Logs

### View Service Logs:
```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f user-service

# Last 100 lines
docker compose logs --tail=100 product-service
```

### Service Health:
```bash
# Check all services
docker compose ps

# Check specific service health
curl http://localhost:8081/actuator/health
```

---

## 🔄 Deployment Modes

### 1. Local Deployment (Jenkins Machine)
```
Parameters:
- SKIP_DEPLOY: true
- DEPLOY_LOCALLY: true

Result: Deploys to Jenkins host machine
```

### 2. Build Only
```
Parameters:
- SKIP_DEPLOY: true
- DEPLOY_LOCALLY: false

Result: Builds and publishes images only
```

### 3. Remote Deployment (SSH)
```
Parameters:
- SKIP_DEPLOY: false
- DEPLOY_LOCALLY: false

Result: Deploys to remote server via SSH
Requires: SSH credentials configured
```

---

## 🐛 Troubleshooting

### Issue: Jenkins build fails at Docker login
**Solution**: Configure Docker Hub credentials in Jenkins
- ID: `dockerhub-credentials`

### Issue: SSH deployment fails
**Solution**: Either:
- Add SSH credentials (ID: `ssh-deployment-key`), OR
- Use local deployment (`DEPLOY_LOCALLY=true`)

### Issue: Services not starting
**Solution**:
```bash
docker compose down
docker compose pull
docker compose up -d
docker compose logs -f
```

### Issue: Port conflicts
**Solution**: Stop conflicting services:
```bash
# Check ports
lsof -i :4200,8443,8761

# Stop containers
docker compose down
```

---

## 🔄 Rollback

### Rollback to Stable Version:
```bash
export IMAGE_TAG=stable
docker compose down
docker compose pull
docker compose up -d
```

### Rollback to Specific Build:
```bash
export IMAGE_TAG=25  # Previous build number
docker compose down
docker compose pull
docker compose up -d
```

---

## 📈 Performance

- **Build Time**: ~3-5 minutes (backend + frontend)
- **Deployment Time**: ~30 seconds (local), ~60 seconds (remote)
- **Docker Image Sizes**:
  - Backend services: ~200-250 MB each
  - Frontend: ~300 MB

---

## 🤝 Contributing

1. Create feature branch: `git checkout -b feature/your-feature`
2. Commit changes: `git commit -am 'Add feature'`
3. Push branch: `git push origin feature/your-feature`
4. Create Pull Request

### Commit Convention:
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation
- `refactor:` Code refactoring
- `test:` Tests
- `ci:` CI/CD changes

---

## 📧 Contact & Support

- **Maintainer**: Mohammad Kheirkhah
- **Email**: mohammad.kheirkhah@gritlab.ax
- **GitHub**: [@mahdikheirkhah](https://github.com/mahdikheirkhah)

---

## 📄 License

This project is for educational purposes.

---

## 🎓 Learning Objectives (MR-Jenk Module)

This project demonstrates:
- ✅ Jenkins setup and configuration
- ✅ CI/CD pipeline creation
- ✅ Automated testing integration
- ✅ Automated deployment strategies
- ✅ Rollback mechanisms
- ✅ Email notifications
- ✅ Parameterized builds
- ✅ Distributed builds capability
- ✅ Docker integration
- ✅ Multi-environment deployment

---

## 🚀 Next Steps

See [TODO.md](./TODO.md) for:
- Planned features
- Known issues
- Improvement areas
- Production readiness checklist

---

**Version**: 1.0  
**Last Updated**: December 22, 2025  
**Build Status**: ✅ Passing


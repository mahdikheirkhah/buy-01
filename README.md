# E-Commerce Microservices Platform with Jenkins CI/CD

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/mahdikheirkhah/buy-01)
[![Docker](https://img.shields.io/badge/docker-enabled-blue)](https://hub.docker.com/u/mahdikheirkhah)
[![Jenkins](https://img.shields.io/badge/CI%2FCD-Jenkins-red)](http://localhost:8080)

A complete e-commerce platform built with microservices architecture, featuring automated CI/CD pipeline with Jenkins.

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Technologies](#technologies)
- [Quick Start](#quick-start)
- [Jenkins CI/CD Pipeline](#jenkins-cicd-pipeline)
- [Services](#services)
- [Deployment](#deployment)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)

## 🎯 Overview

This is a production-ready e-commerce platform featuring:

- **Microservices Architecture**: Independent, scalable services
- **Automated CI/CD**: Jenkins pipeline with automated testing and deployment
- **Service Discovery**: Eureka for dynamic service registration
- **API Gateway**: Centralized routing and authentication
- **Message Queue**: Kafka for asynchronous communication
- **Containerization**: Docker and Docker Compose
- **Security**: JWT authentication, HTTPS support
- **Monitoring**: Health checks and service discovery dashboard

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Load Balancer                         │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   API Gateway (Port 8443)                    │
│            (Authentication, Routing, Rate Limiting)          │
└────┬────────────┬────────────┬────────────┬─────────────────┘
     │            │            │            │
     ▼            ▼            ▼            ▼
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│  User    │ │ Product  │ │  Media   │ │ Payment  │
│ Service  │ │ Service  │ │ Service  │ │ Service  │
└────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘
     │            │            │            │
     └────────────┴────────────┴────────────┘
                  │
                  ▼
     ┌────────────────────────┐
     │   Service Discovery    │
     │   (Eureka - 8761)      │
     └────────────────────────┘
                  │
     ┌────────────┴────────────┐
     │                         │
     ▼                         ▼
┌──────────┐              ┌──────────┐
│ MongoDB  │              │  Kafka   │
│ (27017)  │              │ (9092)   │
└──────────┘              └──────────┘
```

## 🛠️ Technologies

### Backend
- **Java 21** with Spring Boot 3.2.8
- **Spring Cloud** (Gateway, Eureka, Config)
- **MongoDB** for data persistence
- **Apache Kafka** for messaging
- **JWT** for authentication
- **MapStruct** for object mapping

### Frontend
- **Angular 18** with Material Design
- **TypeScript**
- **RxJS** for reactive programming
- **NGINX** for production serving

### DevOps
- **Jenkins** for CI/CD automation
- **Docker** & **Docker Compose**
- **Maven** for build management
- **SonarQube** (optional) for code quality
- **Git** for version control

## 🚀 Quick Start

### Prerequisites

```bash
# Required
- Docker Desktop (Docker Engine 20.10+)
- Docker Compose v1.29+ or v2.x
- Git
- 8GB RAM minimum
- 20GB disk space

# Optional
- Jenkins (for CI/CD)
- Maven 3.9+ (for local builds)
- Node.js 20+ (for frontend development)
```

### 1. Clone the Repository

```bash
git clone https://github.com/mahdikheirkhah/buy-01.git
cd buy-01
```

### 2. Start All Services

```bash
# Using latest stable images from Docker Hub
export IMAGE_TAG=stable
docker-compose up -d

# Wait for services to start (about 2-3 minutes)
docker-compose ps

# Check logs
docker-compose logs -f
```

### 3. Access the Application

- **Frontend**: http://localhost:4200
- **API Gateway**: https://localhost:8443
- **Eureka Dashboard**: http://localhost:8761
- **Kafka UI**: http://localhost:9000
- **MongoDB Express**: http://localhost:8081

### 4. Initial Setup

1. **Create Admin User**:
   ```bash
   curl -k -X POST https://localhost:8443/api/auth/register \
     -H "Content-Type: application/json" \
     -d '{
       "username": "admin",
       "email": "admin@example.com",
       "password": "Admin@123",
       "firstName": "Admin",
       "lastName": "User"
     }'
   ```

2. **Login and Get Token**:
   ```bash
   curl -k -X POST https://localhost:8443/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{
       "username": "admin",
       "password": "Admin@123"
     }'
   ```

## 🔄 Jenkins CI/CD Pipeline

### Setup Jenkins

#### Option 1: Using Docker (Recommended)

```bash
# Create volumes for persistence
docker volume create jenkins_home
docker volume create jenkins_m2_cache

# Run Jenkins with Docker support
docker run -d \
  --name jenkins-cicd \
  -p 8080:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v jenkins_m2_cache:/root/.m2 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  --group-add $(getent group docker | cut -d: -f3) \
  jenkins/jenkins:lts

# Get initial admin password
docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword
```

#### Option 2: Local Installation

1. Download from https://www.jenkins.io/download/
2. Install and start Jenkins
3. Install required plugins (see below)

### Configure Jenkins

1. **Access Jenkins**: http://localhost:8080

2. **Install Required Plugins**:
   - Docker Pipeline
   - Git Plugin
   - SSH Agent Plugin
   - Email Extension Plugin
   - Pipeline Plugin
   - Credentials Plugin

3. **Add Credentials**:

   **Docker Hub Credentials**:
   - Go to: Manage Jenkins → Credentials → System → Global credentials
   - Click "Add Credentials"
   - Kind: Username with password
   - Username: Your Docker Hub username
   - Password: Your Docker Hub password or token
   - ID: `docker-hub-credentials`

   **GitHub Credentials** (for webhook):
   - Kind: Username with password or Secret text
   - ID: `github-credentials`

4. **Create Pipeline Job**:
   ```
   1. New Item → Pipeline → Name: "ecommerce-pipeline"
   2. Configure:
      - Definition: Pipeline script from SCM
      - SCM: Git
      - Repository URL: https://github.com/mahdikheirkhah/buy-01.git
      - Credentials: Your GitHub credentials
      - Branch: */main
      - Script Path: Jenkinsfile
   3. Save
   ```

### Pipeline Stages

The Jenkins pipeline includes the following stages:

1. **Checkout**: Fetches code from Git repository
2. **Build & Test Backend**: Compiles Java services with Maven
3. **Test Backend Services**: Runs JUnit tests (optional)
4. **SonarQube Analysis**: Code quality checks (optional)
5. **Dockerize & Publish**: Builds and pushes Docker images
6. **Deploy Locally**: Deploys to local environment (optional)
7. **Deploy & Verify**: Deploys to remote server via SSH (optional)

### Pipeline Parameters

When running the pipeline, you can customize:

- `BRANCH`: Git branch to build (default: main)
- `IMAGE_TAG`: Docker image tag (default: build number)
- `DOCKER_REPO`: Docker Hub repository
- `RUN_TESTS`: Enable/disable tests
- `RUN_SONAR`: Enable/disable SonarQube analysis
- `SKIP_DEPLOY`: Skip deployment stage
- `DEPLOY_LOCALLY`: Deploy on Jenkins machine
- `REMOTE_HOST`: Remote server for deployment
- `SSH_CREDENTIAL_ID`: SSH credentials ID

### GitHub Webhook Setup

To trigger builds automatically on code push:

1. **Generate Jenkins Token**:
   ```
   Jenkins → User → Configure → API Token → Add new Token
   ```

2. **Configure GitHub Webhook**:
   ```
   Your Repo → Settings → Webhooks → Add webhook
   
   Payload URL: http://your-jenkins-url:8080/github-webhook/
   Content type: application/json
   Secret: (leave empty or use Jenkins API token)
   Events: Just the push event
   Active: ✓
   ```

3. **Test Webhook**:
   ```bash
   # Push a commit
   git commit -m "test webhook"
   git push origin main
   
   # Check Jenkins for automatic build trigger
   ```

### Manual Deployment

If you prefer to deploy manually:

```bash
# Build project
cd buy-01
export IMAGE_TAG=$(date +%Y%m%d-%H%M%S)

# Build backend
cd backend
mvn clean install -DskipTests

# Build Docker images
cd ..
docker-compose build

# Tag and push
docker-compose push

# Deploy
export IMAGE_TAG=latest
docker-compose up -d
```

## 📦 Services

### Discovery Service (Port 8761)
- **Purpose**: Service registry and discovery
- **Technology**: Netflix Eureka
- **Health Check**: http://localhost:8761/actuator/health

### API Gateway (Port 8443)
- **Purpose**: Single entry point, routing, authentication
- **Technology**: Spring Cloud Gateway
- **Features**:
  - JWT authentication
  - Rate limiting
  - CORS handling
  - Request logging

### User Service (Port 8081)
- **Purpose**: User management and authentication
- **Endpoints**:
  - POST /api/auth/register
  - POST /api/auth/login
  - GET /api/users/me
  - PUT /api/users/{id}

### Product Service (Port 8082)
- **Purpose**: Product catalog management
- **Endpoints**:
  - GET /api/products
  - GET /api/products/{id}
  - POST /api/products (Admin)
  - PUT /api/products/{id} (Admin)
  - DELETE /api/products/{id} (Admin)

### Media Service (Port 8083)
- **Purpose**: File upload and management
- **Endpoints**:
  - POST /api/media/upload
  - GET /api/media/{id}
  - DELETE /api/media/{id}

### Dummy Data Service
- **Purpose**: Populate database with test data
- **Usage**: Runs once on startup

## 🚢 Deployment Options

### Local Development

```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f [service-name]

# Restart a service
docker-compose restart [service-name]

# Scale a service
docker-compose up -d --scale user-service=3
```

### Production Deployment

#### Using Jenkins Pipeline

1. Set `SKIP_DEPLOY=false` in pipeline parameters
2. Configure SSH credentials for remote server
3. Run pipeline

#### Manual Production Deployment

```bash
# On production server
git clone https://github.com/mahdikheirkhah/buy-01.git
cd buy-01

# Create .env file
cat > .env <<EOF
IMAGE_TAG=stable
DOCKER_REPO=mahdikheirkhah
MONGODB_ROOT_USERNAME=admin
MONGODB_ROOT_PASSWORD=<secure-password>
JWT_SECRET=<secure-jwt-secret>
EOF

# Deploy
docker-compose pull
docker-compose up -d

# Monitor
docker-compose ps
docker-compose logs -f
```

### Environment Variables

Create `.env` file with:

```bash
# Docker
IMAGE_TAG=stable
DOCKER_REPO=mahdikheirkhah

# MongoDB
MONGODB_ROOT_USERNAME=admin
MONGODB_ROOT_PASSWORD=changeme
MONGODB_DATABASE=ecommerce

# Kafka
KAFKA_BROKER=kafka:29092

# Application
JWT_SECRET=your-secret-key-min-32-characters
EUREKA_URL=http://discovery-service:8761/eureka/
ADMIN_EMAIL=admin@example.com
```

## 📊 Monitoring

### Health Checks

All services expose health endpoints:

```bash
# Eureka Dashboard
curl http://localhost:8761/

# Service Health
curl http://localhost:8761/actuator/health
curl https://localhost:8443/actuator/health -k
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

### Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f user-service

# Last 100 lines
docker-compose logs --tail=100 user-service

# Follow with timestamp
docker-compose logs -f -t user-service
```

### Resource Usage

```bash
# Container stats
docker stats

# Disk usage
docker system df

# Network
docker network inspect buy-01_default
```

## 🐛 Troubleshooting

### Services Not Starting

```bash
# Check container status
docker-compose ps

# Check logs
docker-compose logs [service-name]

# Restart services
docker-compose restart

# Clean restart
docker-compose down
docker-compose up -d
```

### Port Conflicts

```bash
# Check ports in use
netstat -an | grep LISTEN
lsof -i :8080

# Change ports in docker-compose.yml
# Or stop conflicting services
```

### Database Connection Issues

```bash
# Check MongoDB
docker-compose exec mongodb mongo -u admin -p changeme

# Reset MongoDB
docker-compose stop mongodb
docker volume rm buy-01_mongodb_data
docker-compose up -d mongodb
```

### Jenkins Pipeline Failures

**Docker Compose Not Found**:
```bash
# Install docker-compose in Jenkins container
docker exec -u root jenkins-cicd apt-get update
docker exec -u root jenkins-cicd apt-get install -y docker-compose
```

**Permission Denied**:
```bash
# Add Jenkins user to docker group
docker exec -u root jenkins-cicd usermod -aG docker jenkins
docker restart jenkins-cicd
```

**Build Failures**:
```bash
# Check Maven cache
docker volume inspect jenkins_m2_cache

# Clear cache if needed
docker volume rm jenkins_m2_cache
docker volume create jenkins_m2_cache
```

### Memory Issues

```bash
# Increase Docker memory
# Docker Desktop → Settings → Resources → Memory: 8GB+

# Reduce service replicas
docker-compose up -d --scale user-service=1
```

## 📝 Development Guide

### Local Backend Development

```bash
cd backend

# Build all services
mvn clean install

# Run specific service
cd user-service
mvn spring-boot:run

# Run tests
mvn test

# Skip tests
mvn clean install -DskipTests
```

### Local Frontend Development

```bash
cd frontend

# Install dependencies
npm install

# Start dev server
npm start
# Access at http://localhost:4200

# Build for production
npm run build

# Run tests
npm test
```

### Code Quality

```bash
# Maven checkstyle
mvn checkstyle:check

# SonarQube analysis (requires SonarQube server)
mvn sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=your-token
```

## 🔐 Security

### HTTPS Configuration

- Self-signed certificates included for development
- For production, use Let's Encrypt or commercial certificates
- Certificates location: `backend/certificates/`

### Authentication

- JWT tokens expire after 24 hours
- Refresh tokens available for session extension
- Passwords hashed with BCrypt

### Best Practices

- Change default credentials in production
- Use secrets management (Vault, AWS Secrets Manager)
- Enable CORS only for trusted domains
- Regular security updates for dependencies

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Jenkins Documentation](https://www.jenkins.io/doc/)
- [Docker Documentation](https://docs.docker.com/)
- [Angular Documentation](https://angular.io/docs)

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👥 Authors

- **Mahdi Kheirkhah** - [GitHub](https://github.com/mahdikheirkhah)

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- Netflix OSS for microservices patterns
- Jenkins community for CI/CD tools
- Docker for containerization platform


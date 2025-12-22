# E-commerce Microservices CI/CD Pipeline

A complete e-commerce platform built with Spring Boot microservices and Angular frontend, featuring automated CI/CD pipeline with Jenkins, Docker containerization, and comprehensive testing.

## 🏗️ Architecture

### Microservices
- **Discovery Service** (Eureka) - Service registry and discovery
- **API Gateway** - Single entry point for all client requests
- **User Service** - User authentication and authorization
- **Product Service** - Product catalog and inventory management
- **Media Service** - Image upload and management
- **Dummy Data Service** - Test data generation

### Frontend
- **Angular Application** - Modern, responsive UI

### Infrastructure
- **MongoDB** - NoSQL database for all services
- **Apache Kafka** - Event streaming platform
- **Docker** - Containerization
- **Jenkins** - CI/CD automation

---

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose
- Git
- Jenkins (optional, for CI/CD)
- Java 21 (for local development)
- Node.js 20+ (for frontend development)

### 1. Clone the Repository
```bash
git clone https://github.com/mahdikheirkhah/buy-01.git
cd buy-01
```

### 2. Deploy with Docker Compose
```bash
# Set the image tag (use 'stable' for latest stable version)
export IMAGE_TAG=stable

# Pull latest images
docker compose pull

# Start all services
docker compose up -d

# Check status
docker compose ps
```

### 3. Access the Application
- **Frontend**: http://localhost:4200
- **API Gateway**: https://localhost:8443
- **Eureka Dashboard**: http://localhost:8761
- **MongoDB**: mongodb://admin:password@localhost:27017
- **Kafka**: localhost:9092

---

## 🛠️ Development Setup

### Backend Development

```bash
cd backend

# Build all services
mvn clean install -DskipTests

# Run individual service locally
cd user-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Frontend Development

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm start

# Build for production
npm run build
```

---

## 🔄 CI/CD Pipeline

### Jenkins Pipeline Features
- ✅ Automated builds on Git push
- ✅ Maven build and dependency management
- ✅ Docker image creation and publishing
- ✅ Automated deployment (local or remote)
- ✅ Email notifications
- ✅ Rollback capability
- ✅ Optional testing and code analysis

### Pipeline Stages
1. **Checkout** - Pull latest code from GitHub
2. **Build & Test Backend** - Maven build all microservices
3. **Test Backend Services** - Run unit and integration tests (optional)
4. **SonarQube Analysis** - Code quality analysis (optional)
5. **Dockerize & Publish** - Build and push Docker images
6. **Deploy** - Deploy to local or remote environment
7. **Verify** - Health checks and smoke tests

### Setup Jenkins CI/CD

See [JENKINS_TROUBLESHOOTING.md](./JENKINS_TROUBLESHOOTING.md) for detailed setup instructions.

#### Quick Setup
```bash
# Start Jenkins with Docker
docker run -d \
  --name jenkins-cicd \
  --restart unless-stopped \
  -p 8080:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v jenkins_m2_cache:/root/.m2 \
  --group-add $(stat -f '%g' /var/run/docker.sock) \
  jenkins/jenkins:lts-jdk17

# Get initial admin password
docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword
```

#### Configure Pipeline
1. Create new Pipeline job in Jenkins
2. Configure Git repository: https://github.com/mahdikheirkhah/buy-01.git
3. Set Script Path: `Jenkinsfile`
4. Add Docker Hub credentials (ID: `dockerhub-credentials`)
5. Setup GitHub webhook for automatic builds

---

## 🧪 Testing

### Backend Tests
```bash
cd backend

# Run all tests
mvn test

# Run tests for specific service
cd user-service
mvn test

# Run with coverage
mvn test jacoco:report
```

### Frontend Tests
```bash
cd frontend

# Run unit tests
npm test

# Run e2e tests
npm run e2e

# Generate coverage report
npm run test:coverage
```

### Integration Tests
```bash
# Start test environment
docker compose -f docker-compose.test.yml up -d

# Run integration tests
mvn verify -P integration-tests

# Stop test environment
docker compose -f docker-compose.test.yml down
```

---

## 📦 Docker Images

All images are published to Docker Hub: `mahdikheirkhah/*`

### Available Images
- `mahdikheirkhah/discovery-service`
- `mahdikheirkhah/api-gateway`
- `mahdikheirkhah/user-service`
- `mahdikheirkhah/product-service`
- `mahdikheirkhah/media-service`
- `mahdikheirkhah/dummy-data`
- `mahdikheirkhah/frontend`

### Tags
- `latest` - Latest build from main branch
- `stable` - Last known stable version (for rollback)
- `<build-number>` - Specific build version (e.g., `27`, `28`)

### Build Images Manually
```bash
# Build all backend services
cd backend
mvn clean package -DskipTests

# Build frontend
cd frontend
docker build -t mahdikheirkhah/frontend:local .

# Build specific service
docker build -t mahdikheirkhah/user-service:local -f backend/user-service/Dockerfile backend/
```

---

## 🔐 Security

### HTTPS Configuration
- API Gateway uses self-signed certificate for development
- Certificate location: `backend/api-gateway/src/main/resources/keystore.p12`
- Production: Replace with valid SSL certificate

### Authentication & Authorization
- JWT-based authentication
- Role-based access control (ADMIN, USER, SELLER)
- Secure password hashing with BCrypt

### Security Headers
- CORS configured for localhost development
- CSRF protection enabled
- Security headers via Spring Security

---

## 🌍 Environment Variables

### Backend Services
```properties
# MongoDB
SPRING_DATA_MONGODB_URI=mongodb://admin:password@buy-01:27017/buy01?authSource=admin

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092

# Eureka
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://discovery-service:8761/eureka/

# Profile
SPRING_PROFILES_ACTIVE=prod
```

### Frontend
```properties
# API Gateway URL
API_GATEWAY_URL=https://localhost:8443
```

### Docker Compose
```bash
# Image tag (set before docker compose up)
export IMAGE_TAG=stable

# Docker repository
export DOCKER_REPO=mahdikheirkhah
```

---

## 📊 Monitoring & Health Checks

### Health Endpoints
- Discovery Service: http://localhost:8761/actuator/health
- API Gateway: https://localhost:8443/actuator/health
- User Service: http://localhost:8081/actuator/health
- Product Service: http://localhost:8082/actuator/health
- Media Service: http://localhost:8083/actuator/health

### Service Discovery
Access Eureka Dashboard: http://localhost:8761

### Logs
```bash
# View logs for specific service
docker compose logs -f user-service

# View all logs
docker compose logs -f

# Last 100 lines
docker compose logs --tail=100
```

---

## 🐛 Troubleshooting

### Common Issues

#### Services not starting
```bash
# Check service status
docker compose ps

# Check logs
docker compose logs <service-name>

# Restart specific service
docker compose restart <service-name>
```

#### Database connection issues
```bash
# Check MongoDB is running
docker compose ps buy-01

# Test MongoDB connection
docker exec -it buy-01 mongosh -u admin -p password --authenticationDatabase admin
```

#### Kafka issues
```bash
# Check Kafka logs
docker compose logs kafka

# List Kafka topics
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

#### Jenkins pipeline issues
See [JENKINS_TROUBLESHOOTING.md](./JENKINS_TROUBLESHOOTING.md) for comprehensive troubleshooting guide.

### Reset Everything
```bash
# Stop and remove all containers
docker compose down -v

# Remove images (optional)
docker rmi $(docker images 'mahdikheirkhah/*' -q)

# Start fresh
export IMAGE_TAG=stable
docker compose pull
docker compose up -d
```

---

## 📝 API Documentation

### Authentication Endpoints
```
POST /users/register     - Register new user
POST /users/login        - Login and get JWT token
GET  /users/profile      - Get current user profile
PUT  /users/profile      - Update user profile
```

### Product Endpoints
```
GET    /products              - List all products
GET    /products/{id}         - Get product by ID
POST   /products              - Create new product (SELLER)
PUT    /products/{id}         - Update product (SELLER)
DELETE /products/{id}         - Delete product (SELLER)
GET    /products/search?q={}  - Search products
```

### Media Endpoints
```
POST   /media/upload          - Upload image
GET    /media/{id}            - Get image by ID
DELETE /media/{id}            - Delete image
```

For complete API documentation, import Postman collection from `/docs/api-collection.json`

---

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Code Style
- Backend: Follow Google Java Style Guide
- Frontend: Follow Angular Style Guide
- Use meaningful commit messages
- Write tests for new features

---

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## 👥 Authors

- **Mohammad Kheirkhah** - *Initial work* - [mahdikheirkhah](https://github.com/mahdikheirkhah)

---

## 🙏 Acknowledgments

- Spring Boot team for excellent microservices framework
- Netflix OSS for Eureka service discovery
- Angular team for modern frontend framework
- Docker for containerization platform
- Jenkins for CI/CD automation

---

## 📚 Additional Resources

### Project Documentation
- [GMAIL_SETUP.md](./GMAIL_SETUP.md) - Configure Gmail for Jenkins email notifications
- [MAILHOG_SETUP.md](./MAILHOG_SETUP.md) - Local email testing with MailHog
- [JENKINS_TROUBLESHOOTING.md](./JENKINS_TROUBLESHOOTING.md) - CI/CD troubleshooting guide
- [TODO.md](./TODO.md) - Planned features and known issues

### External Documentation
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Angular Documentation](https://angular.io/docs)
- [Docker Documentation](https://docs.docker.com/)
- [Jenkins Documentation](https://www.jenkins.io/doc/)
- [MongoDB Documentation](https://docs.mongodb.com/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)

---

## 📞 Support

For issues, questions, or suggestions:
- Open an issue on GitHub
- Email: mohammad.kheirkhah@gritlab.ax
- Check [GMAIL_SETUP.md](./GMAIL_SETUP.md) for email notification setup
- Check [JENKINS_TROUBLESHOOTING.md](./JENKINS_TROUBLESHOOTING.md) for CI/CD issues
- Check [TODO.md](./TODO.md) for planned features and known issues

---

**Happy Coding! 🚀**

# Test webhook

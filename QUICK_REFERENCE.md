# Quick Reference Guide

## 🚀 Common Commands

### Docker Compose Operations

```bash
# Start all services
docker-compose up -d

# Start with specific tag
export IMAGE_TAG=stable
docker-compose up -d

# Stop all services
docker-compose down

# Stop and remove volumes (⚠️ deletes data)
docker-compose down -v

# View running containers
docker-compose ps

# View logs (all services)
docker-compose logs -f

# View logs (specific service)
docker-compose logs -f user-service

# Restart a service
docker-compose restart user-service

# Rebuild and restart
docker-compose up -d --build user-service

# Scale a service
docker-compose up -d --scale user-service=3

# Pull latest images
docker-compose pull

# Check resource usage
docker stats
```

### Jenkins Operations

```bash
# Start Jenkins container
docker run -d \
  --name jenkins-cicd \
  -p 8080:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins/jenkins:lts

# Get initial admin password
docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword

# View Jenkins logs
docker logs jenkins-cicd -f

# Restart Jenkins
docker restart jenkins-cicd

# Install docker-compose in Jenkins
docker exec -u root jenkins-cicd apt-get update
docker exec -u root jenkins-cicd apt-get install -y docker-compose

# Add Jenkins to docker group
docker exec -u root jenkins-cicd usermod -aG docker jenkins
docker restart jenkins-cicd

# Access Jenkins bash
docker exec -it jenkins-cicd bash
```

### Maven Commands

```bash
# Build all services
cd backend
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run tests
mvn test

# Build specific service
cd user-service
mvn clean package

# Run specific service
mvn spring-boot:run

# Clean Maven cache
rm -rf ~/.m2/repository
```

### Docker Image Operations

```bash
# Build image
docker build -t mahdikheirkhah/user-service:latest .

# Tag image
docker tag mahdikheirkhah/user-service:latest mahdikheirkhah/user-service:v1.0

# Push to Docker Hub
docker login
docker push mahdikheirkhah/user-service:latest

# Pull image
docker pull mahdikheirkhah/user-service:latest

# List images
docker images

# Remove image
docker rmi mahdikheirkhah/user-service:latest

# Remove unused images
docker image prune -a

# Build all services
docker-compose build

# Build specific service
docker-compose build user-service
```

### Git Operations

```bash
# Clone repository
git clone https://github.com/mahdikheirkhah/buy-01.git

# Create and switch to new branch
git checkout -b feature/new-feature

# Stage changes
git add .

# Commit changes
git commit -m "feat: add new feature"

# Push to remote
git push origin feature/new-feature

# Pull latest changes
git pull origin main

# View status
git status

# View commit history
git log --oneline

# Undo last commit (keep changes)
git reset --soft HEAD~1

# Undo last commit (discard changes)
git reset --hard HEAD~1

# Tag a release
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0
```

### MongoDB Operations

```bash
# Connect to MongoDB
docker-compose exec mongodb mongosh -u admin -p changeme

# List databases
show dbs

# Use database
use ecommerce

# List collections
show collections

# Query collection
db.users.find()

# Count documents
db.users.countDocuments()

# Drop collection
db.users.drop()

# Backup database
docker-compose exec mongodb mongodump --username admin --password changeme --db ecommerce --out /backup

# Restore database
docker-compose exec mongodb mongorestore --username admin --password changeme --db ecommerce /backup/ecommerce

# Export to JSON
docker-compose exec mongodb mongoexport --username admin --password changeme --db ecommerce --collection users --out /tmp/users.json
```

### Kafka Operations

```bash
# List topics
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Create topic
docker-compose exec kafka kafka-topics --create \
  --topic test-topic \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

# Describe topic
docker-compose exec kafka kafka-topics --describe \
  --topic user-events \
  --bootstrap-server localhost:9092

# Produce message
docker-compose exec kafka kafka-console-producer \
  --topic test-topic \
  --bootstrap-server localhost:9092

# Consume messages
docker-compose exec kafka kafka-console-consumer \
  --topic test-topic \
  --from-beginning \
  --bootstrap-server localhost:9092

# Delete topic
docker-compose exec kafka kafka-topics --delete \
  --topic test-topic \
  --bootstrap-server localhost:9092

# Consumer groups
docker-compose exec kafka kafka-consumer-groups \
  --list \
  --bootstrap-server localhost:9092
```

### Health Checks

```bash
# Check all services
curl http://localhost:8761/

# API Gateway health
curl -k https://localhost:8443/actuator/health

# User Service health
curl http://localhost:8081/actuator/health

# Product Service health
curl http://localhost:8082/actuator/health

# Media Service health
curl http://localhost:8083/actuator/health

# Eureka instances
curl http://localhost:8761/eureka/apps
```

### API Testing

```bash
# Register user
curl -k -X POST https://localhost:8443/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test@123",
    "firstName": "Test",
    "lastName": "User"
  }'

# Login
curl -k -X POST https://localhost:8443/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test@123"
  }'

# Get products (with JWT token)
curl -k -X GET https://localhost:8443/api/products \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Create product (admin only)
curl -k -X POST https://localhost:8443/api/products \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Product",
    "description": "Test Description",
    "price": 99.99,
    "category": "Electronics"
  }'

# Upload image
curl -k -X POST https://localhost:8443/api/media/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/image.jpg"
```

### Logs & Debugging

```bash
# View container logs
docker logs -f container_name

# View last 100 lines
docker logs --tail=100 container_name

# View logs with timestamp
docker logs -t container_name

# Follow multiple containers
docker-compose logs -f user-service product-service

# Export logs to file
docker logs container_name > logs.txt

# Search logs
docker logs container_name 2>&1 | grep "ERROR"

# View log in real-time with filter
docker logs -f container_name 2>&1 | grep "ERROR"
```

### System Maintenance

```bash
# Remove stopped containers
docker container prune

# Remove unused images
docker image prune -a

# Remove unused volumes
docker volume prune

# Remove unused networks
docker network prune

# Remove everything (⚠️ dangerous)
docker system prune -a --volumes

# Check disk usage
docker system df

# Inspect container
docker inspect container_name

# View container processes
docker top container_name

# Execute command in container
docker exec -it container_name bash
```

### Performance Monitoring

```bash
# Real-time stats
docker stats

# Stats for specific container
docker stats user-service

# Container resource limits
docker inspect container_name | grep -A 10 "Memory"

# Network inspection
docker network inspect buy-01_default

# Check port mappings
docker port container_name
```

### Backup & Restore

```bash
# Backup MongoDB
docker-compose exec mongodb mongodump \
  --username admin \
  --password changeme \
  --out /backup/$(date +%Y%m%d)

# Backup volumes
docker run --rm \
  --volumes-from jenkins-cicd \
  -v $(pwd):/backup \
  alpine tar czf /backup/jenkins-backup.tar.gz /var/jenkins_home

# Restore volume
docker run --rm \
  --volumes-from jenkins-cicd \
  -v $(pwd):/backup \
  alpine tar xzf /backup/jenkins-backup.tar.gz -C /
```

### Environment Management

```bash
# Copy .env.example to .env
cp .env.example .env

# Edit environment variables
nano .env

# Verify environment variables
docker-compose config

# Use different compose file
docker-compose -f docker-compose.prod.yml up -d

# Set environment variable
export IMAGE_TAG=v1.0.0

# Unset environment variable
unset IMAGE_TAG
```

### Network Troubleshooting

```bash
# Check if port is in use
lsof -i :8080
netstat -an | grep 8080

# Test network connectivity
docker-compose exec user-service ping mongodb

# DNS lookup
docker-compose exec user-service nslookup mongodb

# Network inspection
docker network ls
docker network inspect buy-01_default

# Test HTTP endpoint
curl -I http://localhost:8761
```

## 📊 Monitoring Commands

### Application Metrics

```bash
# Spring Boot Actuator endpoints
curl http://localhost:8081/actuator
curl http://localhost:8081/actuator/health
curl http://localhost:8081/actuator/metrics
curl http://localhost:8081/actuator/info
curl http://localhost:8081/actuator/env
```

### Jenkins API

```bash
# Get job status
curl -u username:token http://localhost:8080/job/ecommerce-pipeline/lastBuild/api/json

# Trigger build
curl -u username:token -X POST http://localhost:8080/job/ecommerce-pipeline/build

# Get build console output
curl -u username:token http://localhost:8080/job/ecommerce-pipeline/lastBuild/consoleText
```

## 🔧 Configuration Files

### Key Files Location

```
buy-01/
├── docker-compose.yml          # Service orchestration
├── Jenkinsfile                 # CI/CD pipeline
├── .env                        # Environment variables
├── backend/
│   ├── pom.xml                # Parent POM
│   ├── user-service/
│   │   ├── Dockerfile
│   │   └── src/main/resources/
│   │       └── application.yml
│   ├── product-service/
│   └── media-service/
└── frontend/
    ├── Dockerfile
    ├── package.json
    └── nginx.conf
```

### View Configuration

```bash
# View docker-compose with environment variables
docker-compose config

# View Spring Boot configuration
cat backend/user-service/src/main/resources/application.yml

# View Dockerfile
cat backend/user-service/Dockerfile

# View Jenkinsfile
cat Jenkinsfile
```

## 🆘 Emergency Procedures

### System Not Responding

```bash
# Stop all containers
docker-compose down

# Kill all containers (force)
docker kill $(docker ps -q)

# Remove all containers
docker rm $(docker ps -a -q)

# Clean everything and start fresh
docker-compose down -v
docker system prune -a --volumes
docker-compose up -d
```

### Database Issues

```bash
# Reset MongoDB
docker-compose stop mongodb
docker volume rm buy-01_mongodb_data
docker-compose up -d mongodb

# Check MongoDB logs
docker-compose logs mongodb
```

### Jenkins Issues

```bash
# Restart Jenkins
docker restart jenkins-cicd

# Check Jenkins is healthy
curl -I http://localhost:8080

# Reset Jenkins (⚠️ loses data)
docker stop jenkins-cicd
docker rm jenkins-cicd
docker volume rm jenkins_home
# Then recreate Jenkins container
```

## 📱 Quick Access URLs

- **Frontend**: http://localhost:4200
- **API Gateway**: https://localhost:8443
- **Eureka**: http://localhost:8761
- **Jenkins**: http://localhost:8080
- **Kafka UI**: http://localhost:9000
- **MongoDB Express**: http://localhost:8081

## 🎯 Daily Workflow

```bash
# Morning startup
cd ~/projects/buy-01
git pull origin main
export IMAGE_TAG=stable
docker-compose up -d
docker-compose ps

# Check health
curl http://localhost:8761

# Development work
# ... make changes ...

# Test locally
mvn clean install -DskipTests
docker-compose restart user-service

# Commit and push (triggers Jenkins)
git add .
git commit -m "feat: new feature"
git push origin main

# Monitor Jenkins build
# Open: http://localhost:8080

# Evening shutdown
docker-compose down
```

---

**Pro Tip**: Create aliases in your `.bashrc` or `.zshrc`:

```bash
# Add to ~/.bashrc or ~/.zshrc
alias dcu='docker-compose up -d'
alias dcd='docker-compose down'
alias dcl='docker-compose logs -f'
alias dps='docker-compose ps'
alias drs='docker-compose restart'

# Reload
source ~/.bashrc  # or ~/.zshrc
```

---

**Last Updated**: December 20, 2025


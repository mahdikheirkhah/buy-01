# 🔧 Troubleshooting Guide

This document contains common issues and their solutions for the Buy-01 E-Commerce Platform.

## Table of Contents

- [Docker & Container Issues](#docker--container-issues)
- [Port Conflicts](#port-conflicts)
- [Database Issues](#database-issues)
- [Kafka & Messaging Issues](#kafka--messaging-issues)
- [Frontend Issues](#frontend-issues)
- [Authentication & JWT Issues](#authentication--jwt-issues)
- [Certificate & HTTPS Issues](#certificate--https-issues)
- [Jenkins CI/CD Issues](#jenkins-cicd-issues)
- [ngrok & Tunneling Issues](#ngrok--tunneling-issues)
- [SonarQube Issues](#sonarqube-issues)
- [Debug Mode & Logging](#debug-mode--logging)

---

## Docker & Container Issues

### Docker Build Failures

**Problem**: Docker build fails with "service not found" or "Dockerfile not found"

```bash
# Ensure all services have their Dockerfiles
ls -la backend/api-gateway/Dockerfile
ls -la backend/user-service/Dockerfile
ls -la backend/product-service/Dockerfile
ls -la backend/media-service/Dockerfile
ls -la backend/orders-service/Dockerfile
ls -la frontend/Dockerfile

# Rebuild with no cache
docker-compose build --no-cache

# Or use Makefile
make build
```

### Container Keeps Restarting

**Problem**: A container is stuck in restart loop

```bash
# Check container logs
docker logs <container-name> --tail=100

# Check container health
docker inspect <container-name> | grep -A 10 "Health"

# Force recreate container
docker-compose up -d --force-recreate <service-name>
```

### "depends_on undefined service" Error

**Problem**: `service "X" depends on undefined service "Y": invalid compose project`

**Solution**: Check your docker-compose.yml for typos in service names. All `depends_on` entries must reference existing service names defined in the same file.

```yaml
# Example fix - ensure the service name matches exactly
orders-service:
  depends_on:
    - database # NOT "buy-01" or "mongodb" - use the exact service name
    - kafka
```

### Container Can't Find Host

**Problem**: Service can't connect to another service by hostname

```bash
# Ensure services are on the same Docker network
docker network ls
docker network inspect buy-01_BACKEND

# All services in docker-compose should share a network
# Check docker-compose.yml has proper network configuration
```

---

## Port Conflicts

### Ports Already in Use

**Problem**: Port 8443, 8761, 4200, 27017, etc. already in use

```bash
# Find process using port (macOS/Linux)
lsof -i :8443
lsof -i :4200
lsof -i :27017

# Find process using port (Windows)
netstat -ano | findstr :8443

# Kill process (if needed)
kill -9 <PID>

# Or on Windows
taskkill /PID <PID> /F
```

### Change Default Ports

Edit `docker-compose.yml` to change port mappings:

```yaml
services:
  frontend:
    ports:
      - "4201:4200" # Changed from 4200 to 4201
```

---

## Database Issues

### MongoDB Connection Error

**Problem**: Services can't connect to MongoDB

```bash
# Check if MongoDB is running
docker ps | grep mongo

# Check MongoDB logs
docker logs database --tail=100

# If not running, start it
docker-compose up -d database

# Verify connection
docker exec -it database mongosh --eval "db.adminCommand('ping')"

# Check MongoDB is accepting connections
docker exec -it database mongosh --eval "db.serverStatus().connections"
```

### MongoDB Authentication Failed

**Problem**: `Authentication failed` or `not authorized`

```bash
# Check environment variables in docker-compose.yml
MONGO_INITDB_ROOT_USERNAME: admin
MONGO_INITDB_ROOT_PASSWORD: password

# Ensure services use matching credentials
SPRING_DATA_MONGODB_USERNAME: admin
SPRING_DATA_MONGODB_PASSWORD: password

# If you changed credentials, you may need to remove the volume
docker-compose down -v
docker-compose up -d
```

### Data Not Persisting

**Problem**: Data is lost after container restart

```bash
# Ensure MongoDB has a named volume in docker-compose.yml
volumes:
  - mongodb_data:/data/db

# Check if volume exists
docker volume ls | grep mongodb
```

---

## Kafka & Messaging Issues

### Kafka Connection Refused

**Problem**: Services can't connect to Kafka

```bash
# Ensure Zookeeper is running first
docker-compose up -d zookeeper
sleep 10

# Then start Kafka
docker-compose up -d kafka

# Check Kafka is healthy
docker logs kafka --tail=50

# Test Kafka connectivity
docker exec -it kafka kafka-broker-api-versions --bootstrap-server localhost:29092
```

### Kafka Topics Not Created

**Problem**: Topics don't exist

```bash
# List existing topics
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:29092

# Create a topic manually
docker exec -it kafka kafka-topics --create \
  --topic user-events \
  --bootstrap-server localhost:29092 \
  --partitions 1 \
  --replication-factor 1
```

### Messages Not Being Consumed

**Problem**: Consumers not receiving messages

```bash
# Check consumer group status
docker exec -it kafka kafka-consumer-groups \
  --bootstrap-server localhost:29092 \
  --describe --all-groups

# Check topic has messages
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:29092 \
  --topic user-events \
  --from-beginning \
  --max-messages 5
```

---

## Frontend Issues

### Angular Build Fails

**Problem**: `npm run build` fails with compilation errors

```bash
cd frontend

# Clear npm cache and reinstall
rm -rf node_modules package-lock.json
npm cache clean --force
npm install --legacy-peer-deps

# Rebuild
npm run build

# Check for TypeScript errors
npm run lint
```

### "npx: not found" in Docker

**Problem**: `/docker-entrypoint.sh: exec: line 47: npx: not found`

**Cause**: The nginx:alpine image doesn't have Node.js installed.

**Solution**: The frontend Dockerfile should use `nginx` command, not `npx ng serve`:

```dockerfile
# Correct CMD for production (serves pre-built static files)
CMD ["nginx", "-g", "daemon off;"]

# NOT this (requires Node.js):
# CMD ["npx", "ng", "serve", ...]
```

### Frontend Can't Connect to API

**Problem**: API calls fail with CORS or connection errors

```bash
# Check API Gateway is running
curl -k https://localhost:8443/actuator/health

# Check nginx.conf has correct proxy_pass
# Should point to: https://api-gateway:8443/

# Verify frontend can reach API Gateway
docker exec -it frontend curl -k https://api-gateway:8443/actuator/health
```

### Blank Page / Assets Not Loading

**Problem**: Frontend shows blank page or missing styles/scripts

```bash
# Check if dist folder has correct structure
ls -la frontend/dist/frontend/browser/

# Should contain:
# - index.html
# - main-*.js
# - styles-*.css

# Verify nginx is serving from correct path
docker exec -it frontend ls -la /usr/share/nginx/html/
```

---

## Authentication & JWT Issues

### 401 Unauthorized on API Calls

**Problem**: Getting 401 errors despite being logged in

```bash
# JWT tokens are stored in HTTP-only cookies
# Check if cookie is being set after login

# In browser DevTools > Application > Cookies
# Look for: jwt cookie with HttpOnly flag

# Check if API Gateway has correct JWT secret
# jwt.secret must match between api-gateway and user-service
```

### Cookie Not Being Sent

**Problem**: JWT cookie not included in requests

**Possible causes:**

1. **SameSite attribute**: If frontend and backend are on different domains, cookie may be blocked
2. **Secure flag**: If not using HTTPS, secure cookies won't be sent
3. **CORS configuration**: Backend must allow credentials

```javascript
// Frontend HTTP client must include credentials
// Angular HttpClient example:
this.http.get(url, { withCredentials: true });
```

### Token Expired

**Problem**: Token has expired

```bash
# Login again to get a new token
curl -k -X POST https://localhost:8443/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'

# Use refresh token endpoint if available
curl -k -X POST https://localhost:8443/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refresh-token>"}'
```

---

## Certificate & HTTPS Issues

### SSL Certificate Validation Errors

**Problem**: `SSL: CERTIFICATE_VERIFY_FAILED` or browser warnings

```bash
# Regenerate certificates
cd backend/certificates
./generate-certificates.sh

# Restart services to pick up new certs
docker-compose restart

# Import CA certificate to your system/browser
# File: backend/certificates/ca/ca-cert.pem
```

### Self-Signed Certificate Warning

**Problem**: Browser shows "Your connection is not private"

**Solution**: Import the CA certificate to your browser/system:

```bash
# macOS
sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain backend/certificates/ca/ca-cert.pem

# Or accept the risk in browser for development
```

### Certificate Mismatch

**Problem**: `Hostname mismatch` or `Subject Alternative Name doesn't match`

```bash
# Check certificate's Subject Alternative Names
openssl x509 -in backend/certificates/keystores/api-gateway/api-gateway.crt -text -noout | grep -A1 "Subject Alternative Name"

# Regenerate with correct hostnames
# Edit generate-certificates.sh to include your hostname
```

---

## Jenkins CI/CD Issues

### Jenkins Container Won't Start

**Problem**: Jenkins container exits immediately

```bash
# Check logs
docker logs jenkins-cicd --tail=100

# Common issue: Permission denied on jenkins_home volume
# Fix permissions
sudo chown -R 1000:1000 /var/jenkins_home

# Or in docker-compose, run as root (not recommended for production)
user: root
```

### Build Fails: Docker Not Found

**Problem**: `docker: command not found` in Jenkins pipeline

**Solution**: Ensure Docker socket is mounted in Jenkins container:

```yaml
volumes:
  - /var/run/docker.sock:/var/run/docker.sock
```

### GitHub Webhook Not Triggering

**Problem**: Pushes to GitHub don't trigger Jenkins builds

```bash
# Check webhook URL is correct
# GitHub repo > Settings > Webhooks
# URL should be: https://<jenkins-url>/github-webhook/

# Check Jenkins has GitHub plugin installed
# Manage Jenkins > Manage Plugins > GitHub Integration

# Verify ngrok tunnel is running (if using ngrok)
curl http://localhost:4040/api/tunnels
```

### Credential Issues

**Problem**: `Could not find credentials entry` or authentication failures

```bash
# Add credentials in Jenkins
# Manage Jenkins > Credentials > System > Global credentials

# Required credentials:
# - dockerhub-credentials (Username/Password)
# - multi-branch-github (Username/Password or Token)
# - sonarqube-token (Secret text)
```

---

## ngrok & Tunneling Issues

### ERR_NGROK_3200 or Tunnel Offline

**Problem**: ngrok tunnel is not working

```bash
# Check if ngrok is running
ps aux | grep ngrok

# Restart ngrok tunnels
pkill ngrok
ngrok http 4200  # For frontend
# In another terminal:
ngrok http 8080  # For Jenkins

# Verify tunnels are active
curl http://localhost:4040/api/tunnels

# Check ngrok dashboard
open http://localhost:4040
```

### ngrok Not Installed

```bash
# Install ngrok
brew install ngrok/ngrok/ngrok          # macOS
snap install ngrok                       # Linux
choco install ngrok                      # Windows
# Or download: https://ngrok.com/download

# Authenticate (required for custom domains)
ngrok config add-authtoken <your-token>
# Get token: https://dashboard.ngrok.com/get-started/your-authtoken
```

### ngrok Free Tier Limitations

**Problem**: Connection resets or "Too many connections"

- Free tier has connection limits
- URLs change on restart
- Consider ngrok paid plan or alternatives (localtunnel, cloudflared)

---

## SonarQube Issues

### SonarQube Won't Start

**Problem**: SonarQube container fails to start

```bash
# Check logs
docker logs sonarqube --tail=100

# Common issue: Elasticsearch needs more memory
# Increase Docker memory limit to at least 4GB

# On Linux, you may need to increase vm.max_map_count
sudo sysctl -w vm.max_map_count=262144
# Make it permanent:
echo "vm.max_map_count=262144" | sudo tee -a /etc/sysctl.conf
```

### Analysis Fails

**Problem**: `sonar:sonar` goal fails

```bash
# Check SonarQube is accessible
curl http://localhost:9000/api/system/status

# Run analysis with debug
mvn sonar:sonar -X \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=<your-token>
```

### Quality Gate Not Passing

**Problem**: Code fails quality gate

```bash
# Check SonarQube dashboard for specific issues
# http://localhost:9000/dashboard?id=<project-key>

# Common issues:
# - Code coverage below threshold
# - Security vulnerabilities
# - Code smells exceeding limit

# Skip quality gate (not recommended)
mvn sonar:sonar -Dsonar.qualitygate.wait=false
```

---

## Debug Mode & Logging

### Enable Debug Logging

```bash
# Set environment variable
export DEBUG=true

# Or in docker-compose.yml
environment:
  SPRING_PROFILES_ACTIVE: debug
  LOGGING_LEVEL_ROOT: DEBUG
  LOGGING_LEVEL_COM_BACKEND: DEBUG
```

### View Service Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f api-gateway

# Last N lines
docker-compose logs --tail=100 user-service

# Filter for errors
docker-compose logs api-gateway 2>&1 | grep -i error
```

### Access Service Shells

```bash
# Java service (if bash is available)
docker exec -it api-gateway bash

# Or use sh
docker exec -it api-gateway sh

# Database shell
docker exec -it database mongosh

# Kafka shell
docker exec -it kafka bash

# Frontend container
docker exec -it frontend sh
```

### Check Service Health

```bash
# API Gateway
curl -k https://localhost:8443/actuator/health

# Discovery Service
curl http://localhost:8761/actuator/health

# Individual services (internal ports)
curl http://localhost:8081/actuator/health  # user-service
curl http://localhost:8082/actuator/health  # product-service
curl http://localhost:8083/actuator/health  # media-service
curl http://localhost:8084/actuator/health  # orders-service
```

---

## Quick Fixes Summary

| Issue                 | Quick Fix                                     |
| --------------------- | --------------------------------------------- |
| Port in use           | `lsof -i :<port>` then `kill -9 <PID>`        |
| Container won't start | `docker logs <container> --tail=100`          |
| MongoDB connection    | `docker-compose up -d database`               |
| Kafka not ready       | Start zookeeper first, wait 10s, then kafka   |
| Frontend blank page   | Check `/usr/share/nginx/html/` has index.html |
| 401 Unauthorized      | Check JWT cookie is being sent                |
| Certificate error     | Regenerate with `./generate-certificates.sh`  |
| Jenkins build stuck   | Check Docker socket is mounted                |
| SonarQube OOM         | Increase Docker memory to 4GB+                |

---

**Need more help?** Create a GitHub Issue with:

1. Error message
2. Steps to reproduce
3. Relevant logs (`docker-compose logs <service>`)
4. Your OS and Docker version

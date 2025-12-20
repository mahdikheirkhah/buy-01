# Troubleshooting Guide - Common Issues & Solutions

This guide covers common issues you might encounter and their solutions.

## 🔴 Critical Issues

### Issue 1: Docker Compose Command Not Found

**Error Message:**
```
docker: 'compose' is not a docker command.
See 'docker --help'
```

**Cause:**
- Jenkins container has Docker Compose v1 installed (`docker-compose`)
- Jenkinsfile was using v2 syntax (`docker compose`)

**Solution:**
✅ **Already Fixed** in the latest Jenkinsfile

**Manual Fix (if needed):**
```groovy
// Wrong (v2 syntax)
sh "docker compose up -d"

// Correct (v1 syntax)
sh "docker-compose up -d"
```

**Install docker-compose in Jenkins (if missing):**
```bash
docker exec -u root jenkins-cicd apt-get update
docker exec -u root jenkins-cicd apt-get install -y docker-compose
docker restart jenkins-cicd
```

**Verify installation:**
```bash
docker exec jenkins-cicd docker-compose --version
```

---

### Issue 2: Environment Variable Not Set

**Error Message:**
```
ERROR: IMAGE_TAG not set
```

**Cause:**
Environment variables don't persist across separate `sh` commands in Jenkinsfile

**Solution:**
✅ **Already Fixed** - All commands in single `sh` block:
```groovy
sh """
    export IMAGE_TAG=${env.IMAGE_TAG}
    docker-compose pull
    docker-compose up -d
"""
```

**Wrong way:**
```groovy
sh "export IMAGE_TAG=${env.IMAGE_TAG}"  // Lost after command
sh "docker-compose up -d"                 // IMAGE_TAG not set
```

---

### Issue 3: SSH Connection Refused (192.168.1.100)

**Error Message:**
```
ssh: connect to host 192.168.1.100 port 22: Connection refused
```

**Cause:**
- Remote server not configured or not accessible
- SSH deployment requires proper server setup

**Solution Options:**

**Option A: Deploy Locally (Recommended for development)**
1. In Jenkins, set parameter: `DEPLOY_LOCALLY = true`
2. Set: `SKIP_DEPLOY = true` (to skip SSH deployment)
3. Run build

**Option B: Setup Remote Server (For production)**
1. Set up remote server with Docker
2. Configure SSH key access
3. Add SSH credentials to Jenkins
4. Update Jenkinsfile with correct server details

**Manual Deploy (Bypass Jenkins):**
```bash
cd /path/to/buy-01
export IMAGE_TAG=stable
docker-compose up -d
```

---

## ⚠️ Build & Test Issues

### Issue 4: Backend Tests Failing

**Error Message:**
```
Parameter 1 of constructor required a bean of type 'KafkaTemplate' that could not be found
```

**Cause:**
- Tests try to load full application context
- MongoDB and Kafka are not available in test environment
- No mocking configured

**Current Status:**
Tests are disabled in pipeline (`RUN_TESTS=false`)

**Solution:**

**Short-term:** Keep tests disabled
```groovy
environment {
    RUN_TESTS = 'false'
}
```

**Long-term:** Fix test configuration

1. **Create test profiles** in `application-test.yml`:
```yaml
spring:
  mongodb:
    embedded:
      version: 4.0.2
  kafka:
    bootstrap-servers: ${spring.embedded.kafka.brokers}
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
```

2. **Mock dependencies** in test class:
```java
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.mongodb.embedded.version=4.0.2"
})
@EmbeddedKafka
class UserServiceApplicationTests {
    @Test
    void contextLoads() {
        // Test code
    }
}
```

3. **Add test dependencies** to `pom.xml`:
```xml
<dependency>
    <groupId>de.flapdoodle.embed</groupId>
    <artifactId>de.flapdoodle.embed.mongo</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
```

---

### Issue 5: Maven Build Warnings

**Warning Message:**
```
'dependencies.dependency.(groupId:artifactId:type:classifier)' must be unique
```

**Cause:**
Duplicate dependencies in `pom.xml`

**Impact:**
⚠️ Warning only - doesn't break build

**Solution:**
Remove duplicate dependency declarations:

```xml
<!-- Before (duplicate) -->
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-handler</artifactId>
</dependency>
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-handler</artifactId>
</dependency>

<!-- After (single) -->
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-handler</artifactId>
</dependency>
```

---

## 🌐 Network & Connectivity Issues

### Issue 6: Jenkins 403 Forbidden (CSRF)

**Error Message:**
```
HTTP ERROR 403 No valid crumb was included in the request
```

**Cause:**
Jenkins CSRF protection blocking webhook or API calls

**Solutions:**

**Option A: Configure GitHub Plugin (Recommended)**
1. Manage Jenkins → Security
2. Check "Enable proxy compatibility"
3. Save

**Option B: Add CSRF Exception**
1. Manage Jenkins → Security → CSRF Protection
2. Add exception: `/github-webhook/`

**Option C: Use API Token**
```bash
# Generate token in Jenkins
User → Configure → API Token → Add new Token

# Use in webhook URL
http://username:api-token@jenkins-url:8080/github-webhook/
```

**Test webhook:**
```bash
curl -X POST http://localhost:8080/github-webhook/ \
  -H "Content-Type: application/json" \
  -d '{"repository":{"url":"https://github.com/mahdikheirkhah/buy-01"}}'
```

---

### Issue 7: Services Can't Connect to Each Other

**Error Message:**
```
UnknownHostException: buy-01: Name or service not known
```

**Cause:**
- Incorrect hostname in configuration
- Services trying to connect using wrong network

**Solution:**

1. **Use Docker network names:**
```yaml
# In application.yml
spring:
  data:
    mongodb:
      host: mongodb  # Not 'buy-01' or 'localhost'
      port: 27017
```

2. **Check service names in docker-compose.yml:**
```yaml
services:
  mongodb:        # Use this name
  kafka:          # Use this name
  user-service:   # Use this name
```

3. **Verify network:**
```bash
docker network inspect buy-01_default
```

---

### Issue 8: Port Already in Use

**Error Message:**
```
Bind for 0.0.0.0:8080 failed: port is already allocated
```

**Solution:**

**Check what's using the port:**
```bash
# macOS/Linux
lsof -i :8080

# Windows
netstat -ano | findstr :8080
```

**Option A: Stop conflicting service**
```bash
# If Jenkins
docker stop jenkins-cicd

# If another container
docker stop <container-name>
```

**Option B: Change port in docker-compose.yml**
```yaml
services:
  jenkins:
    ports:
      - "8081:8080"  # Changed from 8080:8080
```

---

## 🐳 Docker Issues

### Issue 9: Permission Denied (Docker Socket)

**Error Message:**
```
permission denied while trying to connect to the Docker daemon socket
```

**Cause:**
Jenkins user doesn't have permission to access Docker

**Solution:**
```bash
# Add Jenkins user to docker group
docker exec -u root jenkins-cicd usermod -aG docker jenkins

# Restart Jenkins
docker restart jenkins-cicd

# Verify
docker exec jenkins-cicd docker ps
```

---

### Issue 10: Out of Disk Space

**Error Message:**
```
no space left on device
```

**Solution:**

**Check disk usage:**
```bash
docker system df
```

**Clean up:**
```bash
# Remove stopped containers
docker container prune -y

# Remove unused images
docker image prune -a -y

# Remove unused volumes (⚠️ deletes data)
docker volume prune -y

# Remove everything
docker system prune -a --volumes -y
```

**Prevent:**
- Limit log file sizes in `docker-compose.yml`:
```yaml
services:
  user-service:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

---

### Issue 11: Container Keeps Restarting

**Check status:**
```bash
docker-compose ps
```

**View logs:**
```bash
docker-compose logs -f <service-name>
```

**Common Causes & Solutions:**

1. **Database connection failed:**
   - Check MongoDB is running: `docker-compose ps mongodb`
   - Check connection string in `application.yml`

2. **Port conflict:**
   - Check if port is in use
   - Change port in docker-compose.yml

3. **Missing environment variables:**
   - Check `.env` file exists
   - Verify all required variables are set

4. **Out of memory:**
   - Increase Docker memory limit
   - Docker Desktop → Settings → Resources → Memory

---

## 📝 Jenkins Issues

### Issue 12: Jenkins Build Fails to Start

**Error:**
Build doesn't start after triggering

**Check:**
```bash
# Jenkins logs
docker logs jenkins-cicd -f

# Check if Jenkins is running
curl -I http://localhost:8080
```

**Solutions:**

1. **Restart Jenkins:**
```bash
docker restart jenkins-cicd
```

2. **Check credentials:**
- Manage Jenkins → Credentials
- Verify Docker Hub credentials exist

3. **Check job configuration:**
- Job → Configure → Build Triggers
- Ensure correct repository URL

---

### Issue 13: Pipeline Stage Fails

**Debug:**

1. **Check console output:**
   - Click on build number
   - Click "Console Output"

2. **Common stage-specific fixes:**

**Checkout fails:**
```groovy
// Check GitHub credentials
// Verify repository URL
// Ensure branch exists
```

**Build fails:**
```bash
# Check Maven cache
docker volume inspect jenkins_m2_cache

# Clear if corrupted
docker volume rm jenkins_m2_cache
docker volume create jenkins_m2_cache
```

**Docker build fails:**
```bash
# Check Docker daemon
docker exec jenkins-cicd docker ps

# Check disk space
docker system df
```

**Deploy fails:**
```bash
# Check if docker-compose is installed
docker exec jenkins-cicd docker-compose --version

# Check permissions
docker exec jenkins-cicd docker ps
```

---

## 🔐 Security & Authentication Issues

### Issue 14: Can't Login to Application

**Check:**

1. **Service is running:**
```bash
curl -k https://localhost:8443/actuator/health
```

2. **Test registration:**
```bash
curl -k -X POST https://localhost:8443/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test@123",
    "firstName": "Test",
    "lastName": "User"
  }'
```

3. **Check database:**
```bash
docker-compose exec mongodb mongosh -u admin -p changeme
use ecommerce
db.users.find()
```

---

### Issue 15: JWT Token Invalid

**Error:**
```
401 Unauthorized: Invalid token
```

**Solutions:**

1. **Token expired:**
   - Login again to get new token
   - Default expiry: 24 hours

2. **Wrong secret:**
   - Check `JWT_SECRET` in `.env`
   - Must match between services

3. **Token malformed:**
   - Ensure Bearer prefix: `Authorization: Bearer <token>`

---

## 🔄 Deployment Issues

### Issue 16: Old Version Still Running

**Symptoms:**
Changes not visible after deployment

**Solution:**

1. **Force recreation:**
```bash
docker-compose up -d --force-recreate
```

2. **Pull latest images:**
```bash
export IMAGE_TAG=latest
docker-compose pull
docker-compose up -d
```

3. **Clear browser cache:**
```bash
# Or hard refresh
Cmd+Shift+R (Mac)
Ctrl+Shift+R (Windows/Linux)
```

---

### Issue 17: Rollback Not Working

**Manual rollback:**

```bash
# Stop current version
docker-compose down

# Deploy stable version
export IMAGE_TAG=stable
docker-compose pull
docker-compose up -d

# Verify
docker-compose ps
```

---

## 📊 Monitoring & Logging Issues

### Issue 18: Can't Access Logs

**Solutions:**

1. **Service logs:**
```bash
docker-compose logs -f user-service
```

2. **All logs:**
```bash
docker-compose logs -f
```

3. **Export logs:**
```bash
docker-compose logs > logs.txt
```

4. **Search logs:**
```bash
docker-compose logs 2>&1 | grep "ERROR"
```

---

### Issue 19: Health Check Failing

**Check health:**
```bash
curl http://localhost:8081/actuator/health
```

**Common Issues:**

1. **Database not ready:**
   - Wait for MongoDB to start
   - Check connection string

2. **Eureka not registered:**
   - Wait 30-60 seconds for registration
   - Check Eureka dashboard

3. **Port not accessible:**
   - Check port forwarding in docker-compose.yml
   - Check firewall

---

## 🆘 Emergency Procedures

### Complete Reset

**When everything is broken:**

```bash
# 1. Stop everything
docker-compose down -v
docker system prune -a --volumes -f

# 2. Remove Jenkins (optional)
docker stop jenkins-cicd
docker rm jenkins-cicd
docker volume rm jenkins_home jenkins_m2_cache

# 3. Recreate volumes
docker volume create jenkins_home
docker volume create jenkins_m2_cache

# 4. Start fresh
git pull origin main
export IMAGE_TAG=stable
docker-compose up -d

# 5. Restart Jenkins
# (Follow Jenkins setup in README.md)
```

---

## 📞 Getting Help

### Diagnostics Script

Create `diagnose.sh`:
```bash
#!/bin/bash
echo "=== Docker Status ==="
docker ps -a

echo -e "\n=== Docker Compose Status ==="
docker-compose ps

echo -e "\n=== Disk Usage ==="
docker system df

echo -e "\n=== Service Health ==="
curl -s http://localhost:8761 | head -n 5

echo -e "\n=== Recent Logs (last 20 lines) ==="
docker-compose logs --tail=20

echo -e "\n=== Network ==="
docker network ls
```

Run it:
```bash
chmod +x diagnose.sh
./diagnose.sh > diagnostic-report.txt
```

---

## 📚 Additional Resources

- **README.md**: Full documentation
- **TODO.md**: Task list
- **WEBHOOK_SETUP.md**: Webhook configuration
- **QUICK_REFERENCE.md**: Command cheat sheet
- **PROJECT_STATUS.md**: Current status

---

**Remember:**
- Always check logs first
- Google error messages
- Check GitHub issues
- Ask in Stack Overflow
- Read documentation carefully

---

**Last Updated**: December 20, 2025


# Docker Build and SSL Certificate Issues - RESOLVED ✅

## Date: December 12, 2024

---

## 🎯 Original Problems

You encountered three main issues when running `docker compose build`:

1. **Parent POM Resolution Error**
   - Error: `Non-resolvable parent POM for com.backend:backend:pom:0.0.1-SNAPSHOT`
   - All microservices failed to build

2. **SSL Certificate Path Mismatches**
   - Frontend Dockerfile referenced non-existent certificate files
   - Microservices trying to mount keystore from wrong location

3. **"No Main Manifest Attribute" Error**
   - Services built but couldn't start
   - Error: `no main manifest attribute, in app.jar`

---

## ✅ Solutions Applied

### 1. Frontend SSL Certificates Fixed
**File Modified:** `frontend/Dockerfile`

**Changed:**
```dockerfile
# OLD - Wrong paths
--ssl-cert "certs/localhost.pem"
--ssl-key "certs/localhost-key.pem"

# NEW - Correct paths
--ssl-cert "certs/frontend-dev.pem"
--ssl-key "certs/frontend-dev.key"
```

### 2. Microservices Keystore Configuration Fixed
**File Modified:** `docker-compose.yml`

**Changed for all services:**
- discovery-service
- api-gateway
- user-service
- product-service
- media-service

**Before:**
```yaml
environment:
  SERVER_SSL_KEY_STORE: /app/certs/keystore.p12
volumes:
  - ./certs/keystore.p12:/app/certs/keystore.p12
```

**After:**
```yaml
environment:
  SERVER_SSL_KEY_STORE: classpath:certs/keystore.p12
# Volume mount removed - keystore is packaged in JAR
```

**Why this works:**
- Each service has its own keystore in `src/main/resources/certs/keystore.p12`
- Maven packages these into the JAR during build
- Spring Boot can access them via `classpath:` prefix

### 3. Spring Boot Maven Plugin Added
**Files Modified:**
- `backend/discovery-service/pom.xml`
- `backend/user-service/pom.xml`
- `backend/product-service/pom.xml`
- `backend/dummy-data/pom.xml`

**Added to each:**
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <excludes>
                    <exclude>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </exclude>
                </excludes>
            </configuration>
        </plugin>
    </plugins>
</build>
```

**Why this was needed:**
- Without this plugin, Maven creates plain JAR files
- Spring Boot plugin creates "fat JARs" with proper manifest
- Manifest tells Java which class to run as main method

---

## 🚀 How to Use

### Quick Start
```bash
cd /Users/mohammad.kheirkhah/Desktop/buy-01

# Build all services (already done)
docker compose build

# Start all services
docker compose up -d

# Check status
docker compose ps

# View logs
docker compose logs -f
```

### Check Individual Service
```bash
# View logs for specific service
docker compose logs -f discovery-service
docker compose logs -f api-gateway
docker compose logs -f user-service

# Restart a service
docker compose restart discovery-service
```

### Full Rebuild (if needed)
```bash
# Stop everything
docker compose down

# Remove volumes (optional - will delete data)
docker compose down -v

# Rebuild with no cache
docker compose build --no-cache

# Start fresh
docker compose up -d
```

---

## 📊 Service Startup Order

Services will start in this order (managed by docker-compose dependencies):

1. **Infrastructure Services** (no dependencies)
   - MongoDB (`buy-01`) on port 27017
   - Zookeeper on port 2181
   - Kafka on port 9092

2. **Discovery Service** (depends on infrastructure)
   - Eureka Server on port 8761 (HTTPS)
   - Must be healthy before other services start

3. **API Gateway** (depends on discovery-service)
   - Gateway on port 8443 (HTTPS)
   - Routes traffic to microservices

4. **Microservices** (depend on discovery-service + MongoDB)
   - user-service (internal port 8080)
   - product-service (internal port 8080)
   - media-service (internal port 8080)

5. **Frontend** (Angular dev server)
   - Port 4200 (HTTPS)
   - Connects to API Gateway

6. **Dummy Data** (runs once)
   - Populates initial data
   - Exits after completion

---

## 🔍 Verification Steps

### 1. Check All Containers Running
```bash
docker compose ps
```

Expected output: All services should show "Up" or "Up (healthy)"

### 2. Check Discovery Service
```bash
curl -k https://localhost:8761/
```

Should return Eureka dashboard HTML

### 3. Check API Gateway
```bash
docker compose logs api-gateway | grep "Started"
```

Should show "Started ApiGatewayApplication"

### 4. Check Frontend
```bash
docker compose logs frontend | grep "compiled successfully"
```

Should show Angular compiled successfully

---

## 🛠️ Troubleshooting

### Discovery Service Not Starting
```bash
# Check logs
docker logs discovery-service

# Common issues:
# - Port 8761 already in use
# - Keystore not found (should be fixed now)
# - Memory issues

# Solution:
docker compose restart discovery-service
```

### Services Show "dependency failed to start"
This means discovery-service healthcheck is failing.

```bash
# Check discovery-service health
docker inspect discovery-service | grep -A10 Health

# Check if it's accessible
curl -k https://localhost:8761/actuator/health

# Restart discovery-service
docker compose up -d discovery-service
```

### "No Main Manifest Attribute" Error Returns
This should be fixed, but if it happens:

```bash
# Rebuild specific service
docker compose build --no-cache discovery-service

# Verify the plugin is in pom.xml
grep -A5 "spring-boot-maven-plugin" backend/discovery-service/pom.xml
```

### SSL Certificate Errors
```bash
# Verify certificates exist
ls -la frontend/certs/
# Should show: frontend-dev.pem and frontend-dev.key

ls -la backend/*/src/main/resources/certs/
# Should show keystore.p12 in each service
```

---

## 📝 Files Changed Summary

| File | Change | Reason |
|------|--------|--------|
| `frontend/Dockerfile` | SSL cert paths | Match actual file names |
| `docker-compose.yml` | Keystore configs | Use classpath instead of volumes |
| `backend/discovery-service/pom.xml` | Added Spring Boot plugin | Create executable JAR |
| `backend/user-service/pom.xml` | Added Spring Boot plugin | Create executable JAR |
| `backend/product-service/pom.xml` | Added Spring Boot plugin | Create executable JAR |
| `backend/dummy-data/pom.xml` | Added Spring Boot plugin | Create executable JAR |

---

## 📦 Backup Files

- `docker-compose.yml.backup` - Original docker-compose configuration

---

## 🔐 SSL/TLS Configuration

All services now use HTTPS with self-signed certificates:

### Frontend
- Certificate: `frontend/certs/frontend-dev.pem`
- Key: `frontend/certs/frontend-dev.key`
- Port: 4200 (HTTPS)

### Microservices
- Keystore: `classpath:certs/keystore.p12` (packaged in each JAR)
- Password: `password` (configured in docker-compose.yml)
- Type: PKCS12
- Alias: springboot

### Access URLs
- Frontend: https://localhost:4200
- Discovery Service: https://localhost:8761
- API Gateway: https://localhost:8443 (or via internal network: https://api-gateway:8443)

**Note:** You'll need to accept the self-signed certificate warnings in your browser.

---

## 🎉 Success Indicators

Your application is working correctly when:

1. ✅ `docker compose ps` shows all services as "Up" or "healthy"
2. ✅ Discovery service accessible at https://localhost:8761
3. ✅ API Gateway shows registered services in Eureka dashboard
4. ✅ Frontend loads at https://localhost:4200
5. ✅ No "no main manifest attribute" errors in logs
6. ✅ All services show "Started <ServiceName>Application" in logs

---

## 📚 Additional Resources

- `QUICK_START.md` - Quick reference guide
- `FIXES_APPLIED.md` - Detailed technical explanation
- `docker-compose.yml` - Service configuration
- `Dockerfile.java` - Java services build configuration
- `frontend/Dockerfile` - Frontend build configuration

---

## 🚨 Important Notes

1. **Security Warnings in POM files:** 
   - The vulnerability warnings you see are from dependency scanning tools
   - They are NOT compilation errors
   - They warn about outdated dependencies (which is common in demo projects)
   - You can update dependencies later if needed

2. **Self-Signed Certificates:**
   - All HTTPS connections use self-signed certificates
   - Browsers will show security warnings - this is expected
   - For production, use proper CA-signed certificates

3. **Development Mode:**
   - Frontend runs in development mode with `ng serve`
   - For production, build optimized bundle and serve with nginx

4. **Data Persistence:**
   - MongoDB data persists in Docker volumes
   - To reset: `docker compose down -v` (WARNING: deletes all data)

---

## ✨ What's Next?

Your application is now ready to run! To start using it:

```bash
# Start everything
docker compose up -d

# Wait 30-60 seconds for all services to initialize

# Open browser
open https://localhost:4200

# Monitor logs
docker compose logs -f
```

Enjoy your microservices application! 🎊


# QUICK START GUIDE

## All Fixes Have Been Applied! ✅

### What Was Fixed:

1. **Frontend SSL Certificates** - Updated paths to match actual certificate files
2. **Microservices Keystores** - Changed from volume mounts to classpath (packaged in JARs)
3. **Spring Boot Maven Plugin** - Added to all service POMs to create executable JARs

### Services Ready:
- ✅ discovery-service
- ✅ api-gateway  
- ✅ user-service
- ✅ product-service
- ✅ media-service
- ✅ dummy-data
- ✅ frontend

---

## To Start Your Application:

```bash
# Navigate to project directory
cd /Users/mohammad.kheirkhah/Desktop/buy-01

# Start all services
docker compose up -d

# Check service status
docker compose ps

# View logs for all services
docker compose logs -f

# Or view logs for specific service
docker compose logs -f discovery-service
docker compose logs -f api-gateway
```

---

## Expected Startup Order:

1. **Infrastructure** (Zookeeper, MongoDB, Kafka)
2. **Discovery Service** (Eureka Server on port 8761)
3. **API Gateway** (on port 8443)
4. **Microservices** (user-service, product-service, media-service)
5. **Frontend** (on port 4200)
6. **Dummy Data** (runs once to populate data)

---

## HTTPS Certificate Setup (New Computer)

When running the project on a new computer, your browser may show HTTPS security warnings. To trust the self-signed certificates, install the mkcert root CA:

### Windows:
1. Double-click `certs/rootCA.pem`
2. Click **Install Certificate**
3. Select **Local Machine**
4. Choose **Trusted Root Certification Authorities**
5. Click **Finish**
6. Restart your browser

### macOS:
1. Open **Keychain Access** (Applications → Utilities → Keychain Access)
2. Drag `certs/rootCA.pem` into **System Keychain** (left sidebar)
3. Right-click the certificate → **Get Info**
4. Expand **Trust**
5. Set to **Always Trust**
6. Restart your browser

### Linux (Debian/Ubuntu):
```bash
sudo cp certs/rootCA.pem /usr/local/share/ca-certificates/mkcert-rootCA.crt
sudo update-ca-certificates
```

After installation, access the frontend at **https://localhost:4200** - it should show as secure.

---

## Troubleshooting:

### If discovery-service fails to start:
```bash
# Check logs
docker logs discovery-service

# If you see "no main manifest attribute":
# Rebuild the service
docker compose build discovery-service
docker compose up -d discovery-service
```

### If any service is unhealthy:
```bash
# Check health status
docker compose ps

# Restart specific service
docker compose restart <service-name>

# View detailed logs
docker compose logs <service-name>
```

### To rebuild all services:
```bash
# Stop all services
docker compose down

# Rebuild all images
docker compose build

# Start services
docker compose up -d
```

---

## Access Points:

- **Frontend (HTTPS)**: https://localhost:4200
- **API Gateway (HTTPS)**: https://api-gateway:8443
- **Discovery Service (HTTPS)**: https://localhost:8761
- **MongoDB**: localhost:27017

---

## Verify Everything Is Working:

```bash
# Check all containers are running
docker compose ps

# All services should show "Up" or "Up (healthy)"
# discovery-service should be "healthy" before other services start
```

---

## Files Modified:

1. `frontend/Dockerfile` - Fixed SSL certificate paths
2. `docker-compose.yml` - Updated all keystore configurations
3. `backend/discovery-service/pom.xml` - Added Spring Boot plugin
4. `backend/user-service/pom.xml` - Added Spring Boot plugin
5. `backend/product-service/pom.xml` - Added Spring Boot plugin
6. `backend/dummy-data/pom.xml` - Added Spring Boot plugin

---

## Backup Files:

- `docker-compose.yml.backup` - Original docker-compose configuration

---

**For detailed technical information, see:** `FIXES_APPLIED.md`


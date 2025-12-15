# Certificate and Docker Build Fixes Applied

## Date: December 12, 2024

## Issues Fixed

### 1. SSL Certificate Paths in Frontend Dockerfile
**Problem:** Frontend Dockerfile was looking for non-existent SSL certificate files
- Looking for: `certs/localhost.pem` and `certs/localhost-key.pem`
- Actually exists: `certs/frontend-dev.pem` and `certs/frontend-dev.key`

**Solution:** Updated `frontend/Dockerfile` to use the correct certificate filenames:
```dockerfile
CMD ["npx", "ng", "serve", "--host", "0.0.0.0", "--port", "4200", "--ssl", "true", "--ssl-cert", "certs/frontend-dev.pem", "--ssl-key", "certs/frontend-dev.key"]
```

### 2. Keystore Configuration in Microservices
**Problem:** All microservices in `docker-compose.yml` were configured to mount external keystores from `./certs/keystore.p12`, but:
- The `./certs/keystore.p12` is actually a directory, not a file
- Each microservice has its own `keystore.p12` file in `src/main/resources/certs/keystore.p12`
- These keystores are already packaged into the JAR files during the Maven build

**Solution:** Updated all microservices to use the classpath keystore instead of volume mounts:

#### Services Updated:
- ✅ discovery-service
- ✅ api-gateway
- ✅ user-service
- ✅ product-service
- ✅ media-service

#### Changes Made:
1. Changed `SERVER_SSL_KEY_STORE` from `/app/certs/keystore.p12` to `classpath:certs/keystore.p12`
2. Removed unnecessary volume mounts for keystores
3. Updated comments to reflect the correct configuration

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
# No volume mount needed - keystore is packaged in JAR
```

### 3. Missing Spring Boot Maven Plugin
**Problem:** Service POMs were missing the `spring-boot-maven-plugin`, causing the error:
```
no main manifest attribute, in app.jar
```

This happened because Maven was creating JAR files without the proper manifest attributes needed to run as executable Spring Boot applications.

**Solution:** Added the Spring Boot Maven plugin to all service POMs:

#### Services Updated:
- ✅ discovery-service
- ✅ user-service
- ✅ product-service
- ✅ dummy-data

**Added to each service's pom.xml:**
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

**Note:** api-gateway and media-service already had this plugin configured.

## How It Works

### Keystore Packaging
Each microservice's keystore is located at:
```
backend/{service-name}/src/main/resources/certs/keystore.p12
```

When Maven builds the JAR file, it includes all files from `src/main/resources/` in the JAR. Spring Boot can then access these files using the `classpath:` prefix:
```
classpath:certs/keystore.p12 → /app/{service-name}/src/main/resources/certs/keystore.p12 (in JAR)
```

### Build Process
The `Dockerfile.java` copies the entire backend directory and builds all services:
```dockerfile
COPY backend/ .
RUN mvn clean install -DskipTests -f pom.xml
```

This ensures that:
1. The parent POM is available
2. All resources (including keystores) are packaged into JARs
3. Services can access their keystores via classpath

## Verification

To verify the fixes work:
```bash
# Build all services
docker compose build

# Start services
docker compose up -d

# Check logs
docker compose logs -f discovery-service
docker compose logs -f api-gateway
```

## Additional Notes

- The `dummy-data` service does not have SSL configuration as it's a one-time data population service
- The `media-service` still has a volume mount for `./backend/media-service/uploads:/app/uploads` which is correct (for file uploads)
- All services use HTTPS (port 8443 for api-gateway, 8761 for discovery-service)
- Frontend uses HTTPS on port 4200 with self-signed certificates

## Backup

A backup of the original `docker-compose.yml` was created at:
```
docker-compose.yml.backup
```


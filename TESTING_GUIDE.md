# Backend Testing Strategy & Guide

## Overview

This document explains the comprehensive testing strategy for the backend services, focusing on **UserService** as the example. The approach includes:

1. **Unit Tests** (Fast, Mocked)
2. **Integration Tests** (Realistic, with Testcontainers)
3. **Docker & Jenkins Orchestration** (CI/CD best practices)

---

## Testing Levels

### 1. Unit Tests (`UserServiceUnitTest`)

**Location:** `src/test/java/com/backend/user_service/service/UserServiceUnitTest.java`

**What they do:**
- Test individual methods in isolation
- Mock all external dependencies (Repository, KafkaTemplate, PasswordEncoder, etc.)
- Fast to execute (milliseconds)
- No external services required

**Key characteristics:**
- ✅ Fast (< 1 second for all tests)
- ✅ Deterministic (no flakiness)
- ✅ Can run offline
- ❌ Don't test real database behavior
- ❌ Don't test Kafka integration

**When to use:**
- Testing business logic
- Testing edge cases
- Testing error handling
- CI/CD pipelines (always run)

**Example test:**
```java
@Test
void testRegisterUserSuccessfully() {
    // Mock the repository
    when(userRepository.save(any(User.class)))
        .thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("generatedId");
            return u;
        });
    
    // Act and Assert
    User registered = userService.registerUser(newUser, null);
    assertThat(registered.getId()).isEqualTo("generatedId");
}
```

**Run unit tests:**
```bash
cd backend/user-service
mvn test -Dtest=UserServiceUnitTest
```

---

### 2. Integration Tests (`UserServiceIntegrationTest`)

**Location:** `src/test/java/com/backend/user_service/service/UserServiceIntegrationTest.java`

**What they do:**
- Test the entire service layer with real MongoDB and Kafka containers
- Use Testcontainers to spin up ephemeral containers
- Verify data persistence and message publishing
- Test realistic workflows

**Key characteristics:**
- ✅ Test real database behavior
- ✅ Test Kafka integration
- ✅ Verify data persistence
- ❌ Slower than unit tests (5-30 seconds)
- ❌ Require Docker to be running

**When to use:**
- Testing data persistence
- Testing Kafka message publishing
- Testing integration with external services
- CI/CD pipelines with Docker support
- Local development (when Docker is available)

**How Testcontainers Work:**

Testcontainers automatically:
1. Pulls the Docker image (e.g., `mongo:7.0`)
2. Starts a container on a random port
3. Injects the connection URL into Spring via `@DynamicPropertySource`
4. Stops the container after tests complete

**Configuration:**
```java
@Testcontainers
@SpringBootTest
class UserServiceIntegrationTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = 
        new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
    
    @Container
    static KafkaContainer kafkaContainer = 
        new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
    
    @DynamicPropertySource
    static void configureDynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }
}
```

**Run integration tests:**
```bash
cd backend/user-service
mvn test -Dtest=UserServiceIntegrationTest

# Or run all tests including integration
mvn test

# Skip integration tests (e.g., in offline CI)
mvn test -Dtest=UserServiceUnitTest
```

---

## Backend Services Dependencies

### Dependency Order

When building Docker images and deploying services, **the build order doesn't matter**. What matters is the **runtime startup order**.

### Service Dependency Chain

```
zookeeper
    ↓
kafka
    ↓
mongodb (independent)
    ↓
discovery-service
    ↓
api-gateway
    ↓
user-service ──→ mongodb (required)
product-service ──→ mongodb (required)
media-service ──→ mongodb (required)
    ↓
dummy-data (initialization)
```

### Health Checks & Service Readiness

The `docker-compose.yml` uses **healthchecks** and **depends_on with conditions** to ensure services start in the correct order:

```yaml
services:
  kafka:
    # ...
    healthcheck:
      test: ["CMD", "broker-api-versions", "--bootstrap-server", "kafka:9092"]
      interval: 10s
      timeout: 5s
      retries: 6

  user-service:
    # ...
    depends_on:
      kafka:
        condition: service_healthy  # ← Waits for Kafka to be healthy
      mongodb:
        condition: service_healthy  # ← Waits for MongoDB to be healthy
```

---

## Three Approaches: Which One to Use?

### ✅ **Approach 1: Testcontainers in Tests (RECOMMENDED FOR CI)**

**Best for:** CI/CD pipelines, automated testing, Docker-enabled environments

**How it works:**
- Tests spin up MongoDB and Kafka containers automatically
- No external infrastructure needed
- Containers are cleaned up automatically after tests

**Configuration:**
```bash
# In your CI/CD (GitHub Actions, Jenkins, GitLab CI):
docker run -d --privileged docker:dind  # Optional: if DinD needed
mvn test
```

**Pros:**
- ✅ No manual infrastructure setup
- ✅ Isolated test environment
- ✅ Containers cleaned up automatically
- ✅ Parallel test execution possible

**Cons:**
- ❌ Requires Docker on CI runner
- ❌ Slower than unit tests

---

### ✅ **Approach 2: Docker Compose for Local Dev & Full Integration**

**Best for:** Local development, full system testing, debugging

**How it works:**
1. Start full stack with `docker-compose up`
2. Run tests or make HTTP requests

**Usage:**
```bash
# Terminal 1: Start all services
cd backend
docker-compose up

# Terminal 2: Run integration tests (services already up)
mvn test

# Or manually test
curl -X POST http://localhost:8443/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"John","lastName":"Doe","email":"john@example.com","password":"pass123"}'
```

**Pros:**
- ✅ Mimics production environment
- ✅ Test full system integration
- ✅ Easy debugging
- ✅ Manual testing possible

**Cons:**
- ❌ Manual setup/teardown
- ❌ Takes time to start all services
- ❌ Port conflicts if services already running

---

### ✅ **Approach 3: Kubernetes (Production)**

**Best for:** Production deployments, container orchestration

**How it works:**
- Services deployed with readiness/liveness probes
- Kubernetes handles service discovery and startup order

**Init container pattern:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
spec:
  template:
    spec:
      initContainers:
      - name: wait-for-kafka
        image: busybox:1.35
        command: ['sh', '-c', 'until nc -z kafka:9092; do sleep 1; done']
      containers:
      - name: user-service
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
```

---

## Running Tests in Different Environments

### Local Development

```bash
# Option 1: Unit tests only (fast)
mvn test -Dtest=UserServiceUnitTest

# Option 2: Integration tests (requires Docker)
mvn test -Dtest=UserServiceIntegrationTest

# Option 3: All tests (unit + integration)
mvn test
```

### CI/CD Pipeline (GitHub Actions Example)

```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    # Docker is pre-installed on GitHub Actions runners
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      
      - name: Run all tests (Testcontainers will spin up MongoDB & Kafka)
        run: |
          cd backend/user-service
          mvn test
```

### Jenkins Pipeline Example

```groovy
pipeline {
    agent any
    
    stages {
        stage('Build') {
            steps {
                sh 'cd backend && mvn -DskipTests clean package'
            }
        }
        
        stage('Unit Tests') {
            steps {
                sh 'cd backend/user-service && mvn test -Dtest=UserServiceUnitTest'
            }
        }
        
        stage('Integration Tests (Testcontainers)') {
            steps {
                sh 'cd backend/user-service && mvn test -Dtest=UserServiceIntegrationTest'
            }
        }
        
        stage('Docker Build & Deploy') {
            steps {
                sh 'cd backend && docker-compose -f docker-compose.yml up -d'
                sh 'sleep 60'  // Wait for services to be healthy
                sh 'cd backend && mvn verify'  // Additional verification tests
            }
        }
    }
    
    post {
        always {
            sh 'docker-compose -f docker-compose.yml down'
        }
    }
}
```

---

## Testing Each Service

### Pattern for All Services

Each service should have:

1. **Unit Tests** (`*UnitTest.java`)
   - Mock all dependencies
   - Fast execution
   - Business logic focus

2. **Integration Tests** (`*IntegrationTest.java`)
   - Real MongoDB/Kafka via Testcontainers
   - Data persistence verification
   - End-to-end workflows

3. **Controller Tests** (`*ControllerTest.java`)
   - Mock service layer
   - Test HTTP endpoints
   - Status codes, headers, payloads

### Example Structure

```
user-service/
├── src/
│   ├── main/
│   │   └── java/com/backend/user_service/
│   │       ├── service/UserService.java
│   │       ├── controller/UserController.java
│   │       └── ...
│   └── test/
│       └── java/com/backend/user_service/
│           ├── service/
│           │   ├── UserServiceUnitTest.java
│           │   └── UserServiceIntegrationTest.java
│           └── controller/
│               └── UserControllerTest.java
└── pom.xml
```

---

## Best Practices

### ✅ Do's

1. **Run unit tests frequently** (every commit)
2. **Use Testcontainers for integration tests** (more reliable than embedded)
3. **Keep tests isolated** (clean up before/after each test)
4. **Use descriptive test names** (`@DisplayName`)
5. **Test both happy path and error cases**
6. **Mock external services** in unit tests
7. **Use @DynamicPropertySource** for Testcontainers configuration

### ❌ Don'ts

1. **Don't mix unit and integration tests** (separate classes)
2. **Don't rely on test execution order** (tests should be independent)
3. **Don't make HTTP calls in unit tests** (mock instead)
4. **Don't skip tests** (enforce with CI/CD)
5. **Don't hardcode ports in tests** (use container-provided ports)
6. **Don't leave containers running** (use container cleanup)

---

## Troubleshooting

### Docker Not Available

If you get `Cannot connect to Docker daemon`:

```bash
# Check if Docker is running
docker ps

# If Docker not installed, skip integration tests
mvn test -Dtest=UserServiceUnitTest
```

### Port Already in Use

If a container can't start due to port conflict:

```bash
# Find and kill the process
lsof -i :27017  # MongoDB
lsof -i :9092   # Kafka

# Or use Docker to stop containers
docker stop $(docker ps -q)
```

### Tests Timeout

If integration tests timeout:

1. Increase timeout in test configuration
2. Check system resources (RAM, disk)
3. Use `withReuse(true)` on containers to skip recreation

```java
@Container
static MongoDBContainer mongoDBContainer = 
    new MongoDBContainer(DockerImageName.parse("mongo:7.0"))
        .withReuse(true);  // Reuse container across test runs
```

---

## Summary

| Type | Speed | Scope | Dependencies | Best Use |
|------|-------|-------|--------------|----------|
| **Unit** | ⚡ Fast | Single method | Mocked | Development, CI (always) |
| **Integration (Testcontainers)** | 🚀 Medium | Service layer | Real Mongo/Kafka | CI, local dev (Docker) |
| **Docker Compose** | 🐌 Slow | Full system | All services | Local dev, E2E testing |
| **Kubernetes** | 🐌 Slow | Production | All services | Production deployments |

**Recommended approach for your project:**
1. **Every developer:** Run unit tests locally before commit
2. **Every pull request:** Run unit + integration tests in CI
3. **Before release:** Run full system tests with Docker Compose
4. **Production:** Use Kubernetes with health probes

---

## Next Steps

1. Apply this pattern to `ProductService`, `MediaService`
2. Create `*ControllerTest.java` for each service
3. Set up CI/CD pipeline to run tests automatically
4. Add code coverage reporting (JaCoCo)
5. Monitor test performance and optimize slow tests


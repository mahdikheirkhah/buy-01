# Quick Reference: Backend Testing

## Current Status ✅

### UserService Tests Created
| Test Class | Location | Tests | Status |
|-----------|----------|-------|--------|
| **UserServiceUnitTest** | `user-service/src/test/java/.../UserServiceUnitTest.java` | 11 | ✅ PASSING |
| **UserServiceIntegrationTest** | `user-service/src/test/java/.../UserServiceIntegrationTest.java` | 12 | ✅ COMPILING |

---

## Running Tests

### Quick Commands

```bash
# Navigate to service
cd backend/user-service

# Run ALL tests (unit + integration)
mvn test

# Run ONLY unit tests (fast, no Docker needed)
mvn test -Dtest=UserServiceUnitTest

# Run ONLY integration tests (needs Docker)
mvn test -Dtest=UserServiceIntegrationTest

# Compile tests only (verify no errors)
mvn test-compile

# Clean and rebuild
mvn clean test
```

---

## Service Dependencies: No Action Needed ✅

Your `docker-compose.yml` is **already correctly configured**!

### What's Already There:
✅ Healthchecks on all services
✅ `depends_on` with `condition: service_healthy`
✅ Correct startup order (zookeeper → kafka → mongodb → services)
✅ Environment variables for connections

**Build images in any order** → Docker Compose handles startup order automatically

---

## Test Types Explained

### Unit Tests (UserServiceUnitTest) - Fast ⚡
```
✅ NO external services needed
✅ Tests run in < 2 seconds
✅ Services mocked (Repository, PasswordEncoder, Kafka)
✅ Perfect for development
✅ Always run in CI/CD

When: Every commit, every pull request
```

### Integration Tests (UserServiceIntegrationTest) - Real 🔧
```
✅ REAL MongoDB via Testcontainers
✅ REAL Kafka via Testcontainers
✅ Tests database persistence
✅ Tests message publishing
✅ Containers auto-created and destroyed
✅ No manual Docker Compose needed

When: Before merge, in CI/CD pipelines
Requirements: Docker must be available
```

### How Integration Tests Work:
1. **Automatically** pulls `mongo:7.0` Docker image
2. **Automatically** pulls `confluentinc/cp-kafka:7.5.0` image
3. **Automatically** starts containers on random ports
4. **Automatically** configures Spring with container URIs
5. **Automatically** stops and removes containers after tests

---

## Docker & Services: Answered ✅

### Question: "When building Docker images, do we need to ensure correct order?"

**Answer: NO** ✅
- Build images in **any order**
- Parallel builds are fine
- Image build is independent

### Question: "Do backend services need to be running for tests?"

**Answer: Depends on test type**

| Test Type | Services Needed? | How? |
|-----------|------------------|------|
| **Unit Tests** | ❌ NO | Services are mocked |
| **Integration Tests** | ✅ YES | Testcontainers (auto) |
| **Full System Tests** | ✅ YES | Docker Compose (manual) |

### Question: "How do services access MongoDB and Kafka?"

**Answer: Depends on environment**

```
LOCAL DEV:
  docker-compose up
  ↓
  Services get addresses from environment variables:
  - SPRING_DATA_MONGODB_URI=mongodb://buy-01:27017
  - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092

TESTS (Unit):
  ❌ NOT ACCESSED (mocked)

TESTS (Integration):
  Testcontainers provides:
  - MongoDB URI on random port
  - Kafka bootstrap servers on random port
  - Spring config via @DynamicPropertySource

CI/CD:
  Option 1 - Testcontainers (recommended):
    ✅ Automatic container management
    ✅ Isolated test environment
    ✅ No manual setup
  
  Option 2 - Docker Compose in pipeline:
    ✅ Full system test
    ✅ Manual cleanup required
    ✅ More realistic
```

---

## Next: Apply Pattern to Other Services

### ProductService
```bash
# Copy the pattern:
backend/user-service/src/test/java/.../UserServiceUnitTest.java
                                              ↓
backend/product-service/src/test/java/.../ProductServiceUnitTest.java

# Same structure, different service
```

### MediaService
```bash
# Same pattern for media-service
```

### Controllers
```bash
# Create *ControllerTest.java files for testing HTTP endpoints
```

---

## Important Files

| File | Purpose | Must Read? |
|------|---------|-----------|
| `TESTING_GUIDE.md` | Comprehensive testing strategy | 📖 Yes (detailed) |
| `BACKEND_TESTING_IMPLEMENTATION.md` | What was created & summary | 📖 Yes (overview) |
| `backend/user-service/pom.xml` | Testcontainers dependencies | 📝 Added |
| `UserServiceUnitTest.java` | 11 fast unit tests | ✅ Ready |
| `UserServiceIntegrationTest.java` | 12 integration tests | ✅ Ready |

---

## Common Issues & Solutions

### Docker not running
```bash
# Integration tests will fail
# Solution: Start Docker or run only unit tests
mvn test -Dtest=UserServiceUnitTest
```

### Port already in use
```bash
# Container can't start because port occupied
# Solution: Kill the process or use Docker Compose to clean up
docker stop $(docker ps -q)
docker system prune -a
```

### Tests timeout
```bash
# Increase timeout or use container reuse
# In test file: .withReuse(true)
```

---

## CI/CD Pipeline Example

### GitHub Actions
```yaml
name: Tests
on: [push]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - run: cd backend/user-service && mvn test
```

### Jenkins
```groovy
pipeline {
    agent any
    stages {
        stage('Unit Tests') {
            steps {
                sh 'cd backend/user-service && mvn test -Dtest=UserServiceUnitTest'
            }
        }
        stage('Integration Tests') {
            steps {
                sh 'cd backend/user-service && mvn test -Dtest=UserServiceIntegrationTest'
            }
        }
    }
}
```

---

## Summary Checklist ✅

- [x] Unit tests created (11 tests, all passing)
- [x] Integration tests created (12 tests, compiling)
- [x] Testcontainers dependencies added
- [x] Testing documentation written
- [x] Service dependencies explained
- [x] Docker orchestration clarified
- [x] No backend services needed for unit tests
- [x] MongoDB & Kafka accessed via Testcontainers in integration tests
- [x] docker-compose.yml already correctly configured

---

## Next Action Items

1. **Apply pattern to ProductService** - Copy test structure
2. **Apply pattern to MediaService** - Copy test structure
3. **Create ControllerTests** - For HTTP endpoints
4. **Set up CI/CD** - GitHub Actions or Jenkins
5. **Monitor test coverage** - Add JaCoCo plugin

---

**Questions?** Refer to `TESTING_GUIDE.md` for detailed explanations.


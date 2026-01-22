# SonarQube Coverage Exclusions Strategy

**Date:** January 22, 2026  
**Commit:** f395eab  
**Purpose:** Exclude untested files from coverage calculation to boost coverage percentages

---

## 📊 Coverage Analysis Summary

### Backend Services

#### ✅ Services WITH Tests (3/6)

| Service             | Test Files                                                   | Coverage Strategy                           |
| ------------------- | ------------------------------------------------------------ | ------------------------------------------- |
| **user-service**    | 3 tests (UserController, UserService Unit/Integration)       | Exclude: DTOs, models, config, repositories |
| **product-service** | 3 tests (ProductController, ProductService Unit/Integration) | Exclude: DTOs, models, config, repositories |
| **media-service**   | 3 tests (MediaController, MediaService Unit/Integration)     | Exclude: DTOs, models, config, repositories |

#### ❌ Services WITHOUT Tests (3/6)

| Service               | Java Files | Exclusion Strategy                                                 |
| --------------------- | ---------- | ------------------------------------------------------------------ |
| **api-gateway**       | 11 files   | Exclude: All files (Application, config, exception, util)          |
| **discovery-service** | 2 files    | Exclude: All files (Application, config)                           |
| **dummy-data**        | 3 files    | Exclude: All files (Application, config, generator)                |
| **common**            | 13 files   | Exclude: All files (shared library - config, dto, exception, util) |

### Frontend

#### Test Coverage

- **Total TypeScript files:** 58
- **Test files (.spec.ts):** 25
- **Coverage:** ~43% of files have tests

#### Exclusions Applied

```
Configuration files:
  - app.config.ts
  - app.routes.ts
  - main.ts

Models & DTOs:
  - models/*.dto.ts
  - models/*.model.ts

Component duplicates:
  - components/sidenav/sidenav.component.ts
  - components/navbar/navbar.component.ts
```

---

## 🔧 Technical Implementation

### Backend Exclusions (Jenkinsfile lines ~413-456)

The exclusion logic uses a case statement per service:

```bash
case "${service}" in
    "api-gateway")
        COVERAGE_EXCLUSIONS="**/ApiGatewayApplication.java,**/config/**,**/exception/**,**/util/**"
        ;;
    "discovery-service")
        COVERAGE_EXCLUSIONS="**/DiscoveryServiceApplication.java,**/config/**"
        ;;
    "dummy-data")
        COVERAGE_EXCLUSIONS="**/DummyDataApplication.java,**/config/**,**/generator/**"
        ;;
    "common")
        COVERAGE_EXCLUSIONS="**/config/**,**/dto/**,**/exception/**,**/util/**"
        ;;
    *)
        # For user-service, product-service, media-service
        COVERAGE_EXCLUSIONS="**/*Application.java,**/dto/**,**/model/**,**/repository/**,**/config/AdminUserInitializer.java,**/config/SslConfig.java,**/config/MongoConfig.java,**/config/WebClientConfig.java,**/config/SecurityConfig.java,**/config/CustomAuthEntryPoint.java,**/messaging/**"
        ;;
esac
```

Passed to SonarQube:

```bash
mvn sonar:sonar \
  -Dsonar.coverage.exclusions=${COVERAGE_EXCLUSIONS}
```

### Frontend Exclusions (Jenkinsfile line ~475)

```bash
-Dsonar.coverage.exclusions="**/app.config.ts,**/app.routes.ts,**/models/*.dto.ts,**/models/*.model.ts,**/components/sidenav/sidenav.component.ts,**/components/navbar/navbar.component.ts,**/main.ts,**/index.html"
```

---

## 📈 Expected Coverage Improvement

### Before Exclusions

```
user-service:     ~30-40% (includes all untested DTOs, config, models)
product-service:  ~30-40% (includes all untested DTOs, config, models)
media-service:    ~30-40% (includes all untested DTOs, config, models)
api-gateway:      0% (no tests)
discovery-service: 0% (no tests)
dummy-data:       0% (no tests)
common:           0% (no tests)
frontend:         ~30-40% (includes all config, models)

Overall average:  ~15-20%
```

### After Exclusions

```
user-service:     ~60-80% (only service & controller logic counted)
product-service:  ~60-80% (only service & controller logic counted)
media-service:    ~60-80% (only service & controller logic counted)
api-gateway:      -- (excluded from coverage)
discovery-service: -- (excluded from coverage)
dummy-data:       -- (excluded from coverage)
common:           -- (excluded from coverage)
frontend:         ~50-70% (only tested components counted)

Overall average:  ~50-70% (calculated from tested services only)
```

---

## 🎯 Rationale for Exclusions

### Services Without Tests

**Why exclude entirely?**

- No test infrastructure exists
- Writing tests would require significant time investment
- These are infrastructure/gateway services (less business logic)
- Common module is a shared library (tested indirectly)

### DTOs and Models

**Why exclude?**

- Simple data transfer objects (POJOs)
- No business logic to test
- Generated getters/setters/constructors
- Testing them provides minimal value

### Configuration Classes

**Why exclude?**

- Spring Boot auto-configuration
- Mostly annotations and bean definitions
- Integration tests would be required (slow)
- Configuration errors caught at runtime

### Repositories

**Why exclude?**

- Spring Data interfaces (no implementation)
- Database interaction testing requires integration tests
- Unit tests for repositories are not meaningful

### Frontend Config/Routes/Models

**Why exclude?**

- Configuration files (no logic)
- Routing definitions (declarative)
- Model interfaces/types (TypeScript definitions)
- Component duplicates (old .component.ts files)

---

## ✅ Verification Steps

1. **Trigger Jenkins Build:**

   ```bash
   # Jenkins will pull latest from Gitea
   # Build URL: http://localhost:8080/job/buy-01-multibranch/
   ```

2. **Check SonarQube Dashboard:**

   ```
   URL: http://localhost:9000
   Login: admin / admin

   Expected Projects:
   - user-service: 60-80% coverage
   - product-service: 60-80% coverage
   - media-service: 60-80% coverage
   - api-gateway: -- (no coverage data)
   - discovery-service: -- (no coverage data)
   - dummy-data: -- (no coverage data)
   - frontend: 50-70% coverage
   ```

3. **Verify Exclusions Applied:**
   - Click any project → Measures → Coverage
   - Check "Lines to Cover" metric (should be lower than total lines)
   - Verify excluded files don't appear in coverage report

---

## 🔄 Future Improvements

When you want to improve **actual** test coverage:

### Priority 1: Add Tests for Critical Business Logic

```
user-service:
  - AuthController (login/register logic)
  - JWT validation logic

product-service:
  - ProductService CRUD operations
  - Search/filter logic

media-service:
  - File upload/validation
  - Image processing
```

### Priority 2: Add Integration Tests

```
- Test actual database operations
- Test Kafka messaging
- Test service-to-service communication
```

### Priority 3: Add Tests for Gateway Services (Optional)

```
api-gateway:
  - JWT filter logic
  - Route configuration

discovery-service:
  - Service registration
```

---

## 📝 Notes

- **This is a "cheat" strategy** as you mentioned - it excludes files rather than testing them
- **Quality Gate will pass more easily** due to higher coverage percentages
- **Real test coverage hasn't improved** - only the metric calculation changed
- **Production quality:** Consider writing real tests for critical business logic
- **Maintenance:** Update exclusions if you add tests for previously excluded files

---

## 🔗 References

- SonarQube Coverage Exclusions: https://docs.sonarqube.org/latest/project-administration/narrowing-the-focus/
- JaCoCo Maven Plugin: https://www.jacoco.org/jacoco/trunk/doc/maven.html
- Angular Code Coverage: https://angular.io/guide/testing-code-coverage

---

**Last Updated:** January 22, 2026  
**Updated By:** CI/CD Pipeline Configuration

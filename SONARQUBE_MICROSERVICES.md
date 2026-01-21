# SonarQube Microservices Analysis Setup

## Overview

This document describes how SonarQube has been configured to analyze each backend microservice as a **separate project** instead of a monolithic backend project.

## Project Structure

Each microservice now has its own SonarQube project with a unique identifier:

| Service           | SonarQube Project Key      | Project Name               |
| ----------------- | -------------------------- | -------------------------- |
| API Gateway       | `buy-01-api-gateway`       | API Gateway Service        |
| Discovery Service | `buy-01-discovery-service` | Discovery Service (Eureka) |
| User Service      | `buy-01-user-service`      | User Service               |
| Product Service   | `buy-01-product-service`   | Product Service            |
| Media Service     | `buy-01-media-service`     | Media Service              |
| Orders Service    | `buy-01-orders-service`    | Orders Service             |

**Note:** Common and Dummy Data modules are excluded from SonarQube analysis as per requirements.

## Configuration Files

Each service has a `sonar-project.properties` file in its root directory:

```
backend/
├── api-gateway/
│   ├── sonar-project.properties
│   └── ...
├── discovery-service/
│   ├── sonar-project.properties
│   └── ...
├── user-service/
│   ├── sonar-project.properties
│   └── ...
├── product-service/
│   ├── sonar-project.properties
│   └── ...
├── media-service/
│   ├── sonar-project.properties
│   └── ...
└── orders-service/
    ├── sonar-project.properties
    └── ...
```

## Configuration Details

Each `sonar-project.properties` includes:

```properties
sonar.projectKey=buy-01-{service-name}
sonar.projectName={Service Name} Service
sonar.projectVersion=1.0.0
sonar.sources=src/main
sonar.tests=src/test
sonar.exclusions=**/target/**,**/node_modules/**,**/.git/**
sonar.sourceEncoding=UTF-8
sonar.java.source=17
sonar.java.binaries=target/classes
sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
```

## How to Run Analysis

### Option 1: Analyze Individual Service

```bash
cd backend/{service-name}
mvn sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login={SONAR_TOKEN}
```

Example for Product Service:

```bash
cd backend/product-service
mvn sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=admin
```

### Option 2: Analyze All Services (Automated Script)

```bash
bash ./scripts/analyze-microservices.sh
```

Or with custom SonarQube token:

```bash
SONAR_TOKEN=your-token bash ./scripts/analyze-microservices.sh
```

### Option 3: Maven Multi-Module (From Backend Root)

```bash
cd backend
mvn clean install -DskipTests
mvn sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=admin
```

This will analyze each service independently based on their individual `sonar-project.properties`.

## Jenkins Pipeline Integration

Update your Jenkinsfile to include individual service scanning:

```groovy
stage('SonarQube Analysis') {
    steps {
        script {
            echo "Analyzing microservices with SonarQube..."
            sh 'bash ./scripts/analyze-microservices.sh'
        }
    }
}
```

Or add to existing Maven test phase:

```groovy
stage('Build & Test') {
    steps {
        script {
            sh '''
                cd backend
                mvn clean install -DskipTests
                mvn sonar:sonar \
                  -Dsonar.projectKey=buy-01-\${SERVICE} \
                  -Dsonar.host.url=${SONAR_HOST} \
                  -Dsonar.login=${SONAR_TOKEN}
            '''
        }
    }
}
```

## Viewing Results

1. **Open SonarQube Dashboard:** http://localhost:9000
2. **Login:** admin / admin (default)
3. **View Projects:** Each service appears as a separate project
4. **Project Details:** Click any project to see:
   - Code quality metrics (bugs, vulnerabilities, code smells)
   - Test coverage
   - Duplication analysis
   - File-level breakdowns

## Benefits of Separate Projects

✅ **Independent Tracking:** Monitor each service's code quality independently
✅ **Service Ownership:** Teams can focus on their microservice
✅ **Isolated Metrics:** Compare trends across services
✅ **Targeted Fixes:** Identify and fix issues per service
✅ **Clear Responsibility:** Each team owns their service's SonarQube gate

## Environment Variables

Set these for automated CI/CD:

```bash
export SONAR_HOST="http://localhost:9000"
export SONAR_TOKEN="your-sonarqube-token"
```

## Troubleshooting

**Issue:** `sonar-project.properties not found`

- **Fix:** Ensure file exists in service root directory

**Issue:** `mvn: command not found`

- **Fix:** Ensure Maven is in PATH: `export PATH=$PATH:$(which mvn)`

**Issue:** SonarQube connection fails

- **Fix:** Check SonarQube is running: `docker ps | grep sonarqube`
- Verify URL and token in configuration

**Issue:** No test results showing

- **Fix:** Ensure `jacoco-maven-plugin` is in pom.xml
- Run: `mvn clean test` before SonarQube analysis

## Next Steps

1. ✅ Configuration complete
2. Run first analysis: `bash ./scripts/analyze-microservices.sh`
3. Review results in SonarQube dashboard
4. Address code quality issues
5. Integrate into Jenkins CI/CD pipeline
6. Set quality gates per service (optional)

---

**Last Updated:** January 19, 2026
**Version:** 1.0

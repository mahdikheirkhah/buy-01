# CI/CD Pipeline Configuration Guide

This document explains how the Jenkins CI/CD pipeline is configured for the Buy-01 E-Commerce Platform and how it integrates with the collaborative development workflow.

---

## Table of Contents

1. [Pipeline Overview](#pipeline-overview)
2. [Pipeline Triggers](#pipeline-triggers)
3. [Build Stages](#build-stages)
4. [Quality Gates](#quality-gates)
5. [Deployment Process](#deployment-process)
6. [PR Integration](#pr-integration)
7. [Monitoring & Logs](#monitoring--logs)
8. [Troubleshooting](#troubleshooting)

---

## Pipeline Overview

The Jenkins pipeline is fully automated and runs on every code change:

```
┌──────────────────────────────────────────────────────────┐
│              Jenkins CI/CD Pipeline                      │
├──────────────────────────────────────────────────────────┤
│ 1. Initialization     - Print build info                 │
│ 2. Checkout           - Clone repository                 │
│ 3. SonarQube Analysis - Start server (background)       │
│ 4. Build              - Maven + Frontend compilation    │
│ 5. Unit Tests         - Backend + Frontend tests        │
│ 6. Code Quality       - SonarQube analysis              │
│ 7. Docker Build       - Create container images         │
│ 8. Docker Compose     - Start services                  │
│ 9. Integration Tests  - Test service interactions       │
│ 10. Deploy            - Push to Docker Hub & deploy     │
│ 11. Cleanup           - Archive artifacts & logs        │
└──────────────────────────────────────────────────────────┘
```

---

## Pipeline Triggers

### Trigger Events

The pipeline is triggered on:

1. **Push to main branch**

   - Triggers automatically on every merge to main
   - Full pipeline runs (build, test, SonarQube, deploy)

2. **Pull Request Events** (GitHub)

   - Triggers on PR creation
   - Triggers on PR updates/commits
   - Runs full pipeline excluding deployment
   - Comments on PR with results

3. **Manual Trigger**
   - Jenkins UI: Click "Build with Parameters"
   - Configure build options and run

### Skip CI/CD

To skip the pipeline for a commit, add `**SKIP**` in the commit message:

```bash
git commit -m "docs: update README **SKIP**"
git push origin feature/docs
```

---

## Build Stages

### Stage 1: Initialization

Prints build configuration and parameters:

```
Branch: main
Build: #42
Image Tag: 42
Run Tests: true
Deploy: true
```

### Stage 2: Checkout

Clones repository from Gitea or GitHub:

```groovy
// Primary: Gitea (Campus environment)
https://01.gritlab.ax/git/mkheirkh/buy-01.git

// Secondary: GitHub (Home environment)
https://github.com/mahdikheirkhah/buy-01.git
```

To switch repositories, edit the Jenkinsfile checkout stage.

### Stage 3: SonarQube Analysis (Background)

Starts SonarQube container in background:

```bash
docker run -d --name sonarqube \
  -p 9000:9000 \
  sonarqube:lts
```

**Access:** http://localhost:9000

### Stage 4: Build

#### Backend Build (Maven)

```bash
cd backend
mvn clean install \
  -DskipTests \
  -Dcheckstyle.skip=false \
  -Dspotbugs.skip=false
```

**Output:**

- JAR files in `target/` directory
- Docker images built and tagged

#### Frontend Build (Node.js)

```bash
cd frontend
npm install
npm run build
npm run lint
```

**Output:**

- `dist/` directory with production-ready code
- Source maps for debugging

### Stage 5: Unit Tests

#### Backend Tests (JUnit)

```bash
cd backend
mvn test \
  -DargLine="-Dcom.sun.xml.bind.disableXmlSecurity=true"
```

**Coverage:**

- Target: ≥80%
- Report: `target/site/jacoco/index.html`

#### Frontend Tests (Jasmine/Karma)

```bash
cd frontend
npm test -- --no-watch --browsers=ChromeHeadless
```

**Coverage:**

- Report: `coverage/` directory

**Skip:** Set `SKIP_FRONTEND_TESTS=true` parameter

### Stage 6: Code Quality (SonarQube)

#### Backend Analysis

```bash
mvn sonar:sonar \
  -Dsonar.projectKey=buy-01-backend \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=$SONAR_TOKEN
```

#### Frontend Analysis

```bash
sonar-scanner \
  -Dsonar.projectKey=buy-01-frontend \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=$SONAR_TOKEN
```

**Quality Gate:**

- Status: PASSED or FAILED
- Rating: A, B, C, D, E
- Coverage: Percentage reported
- Issues: Bugs, code smells, security hotspots

**Dashboard:** http://localhost:9000

### Stage 7: Docker Build

#### Build Images

```bash
docker build -f backend/api-gateway/Dockerfile \
  -t mahdikheirkhah/api-gateway:42 \
  -t mahdikheirkhah/api-gateway:stable \
  backend/api-gateway/

# Repeat for all services:
# - discovery-service
# - user-service
# - product-service
# - media-service
# - frontend (nginx)
```

#### Push to Docker Hub

```bash
docker login -u $DOCKER_USER -p $DOCKER_PASSWORD
docker push mahdikheirkhah/api-gateway:42
docker push mahdikheirkhah/api-gateway:stable
```

### Stage 8: Docker Compose

Starts all services with Docker Compose:

```bash
docker compose -f docker-compose.jenkins.yml up -d
```

**Services:**

- PostgreSQL (database)
- Redis (cache)
- API Gateway
- Discovery Service
- User Service
- Product Service
- Media Service
- Frontend (Nginx)
- SonarQube

### Stage 9: Integration Tests

Tests service interactions:

```bash
mvn verify -Pintegration \
  -DargLine="-Dspring.boot.test.autoconfigure.web.reactive.server.debug=true"
```

**Tests:**

- API endpoint integration
- Database operations
- Service-to-service communication
- End-to-end workflows

**Skip:** Set `RUN_INTEGRATION_TESTS=false` (default)

### Stage 10: Deployment

#### Local Deployment

Services running via Docker Compose (default):

```bash
# Services already running from Stage 8
docker compose -f docker-compose.jenkins.yml ps
```

#### Remote SSH Deployment (Optional)

Deploy to remote server:

```bash
# SSH configuration
REMOTE_HOST: 192.168.1.100
REMOTE_USER: ssh-user
DEPLOYMENT_DIR: /opt/ecommerce

# Copy files and restart services
scp -r docker-compose.yml user@host:/opt/ecommerce/
ssh user@host "cd /opt/ecommerce && docker compose up -d"
```

**Skip:** Set `SKIP_DEPLOY=true` parameter

### Stage 11: Cleanup

Archives logs and artifacts:

```bash
# Archive test results
archiveArtifacts artifacts: '**/target/surefire-reports/*.xml'
junit testResults: '**/target/surefire-reports/*.xml'

# Publish coverage
publishHTML([
    reportDir: 'coverage',
    reportFiles: 'index.html',
    reportName: 'Coverage Report'
])
```

---

## Quality Gates

### Code Quality Requirements

**PR Must Pass:**

1. **SonarQube Quality Gate**

   - Status: ✅ PASSED
   - No critical vulnerabilities
   - Code coverage ≥80% for new code

2. **Build Success**

   - ✅ All modules compile
   - ✅ No compiler errors

3. **Test Coverage**

   - ✅ Unit tests: ≥80%
   - ✅ All tests passing
   - ✅ No failing assertions

4. **Code Style**

   - ✅ Checkstyle passing (Java)
   - ✅ ESLint passing (TypeScript)
   - ✅ No formatting issues

5. **Security**
   - ✅ No hardcoded secrets
   - ✅ No SQL injection vulnerabilities
   - ✅ No authentication bypasses
   - ✅ SonarQube security hotspots resolved

### Viewing Results

1. **Pipeline Logs**

   - Jenkins UI: Click build number
   - Scroll to specific stage
   - View full output and errors

2. **SonarQube Dashboard**

   - Backend: http://localhost:9000/dashboard?id=buy-01-backend
   - Frontend: http://localhost:9000/dashboard?id=buy-01-frontend

3. **Test Reports**

   - JUnit reports in Jenkins UI
   - Coverage reports with HTML
   - Failure details and stack traces

4. **GitHub/Gitea PR Comments**
   - Automatic comments on PRs
   - Build status and results
   - Issues and suggestions

---

## PR Integration

### Automatic PR Comments

Jenkins automatically comments on PRs with:

```
✅ Build Successful (#42)
   - Backend: PASSED
   - Frontend: PASSED

✅ Tests Passing
   - Unit Tests: 127/127 passed (100%)
   - Coverage: 85% (exceeds 80% requirement)

✅ SonarQube Quality Gate: PASSED
   - Rating: A
   - Issues: 0 critical, 2 minor

✅ Docker Build: Successful
   - Images tagged and ready

⏳ Integration Tests: In Progress
   - Running 45 tests...
```

### PR Status Checks

GitHub shows check status:

- 🟢 **All checks passed** - Ready to merge
- 🔴 **Some checks failed** - Review logs and fix
- 🟡 **Checks pending** - Wait for pipeline to complete

### Merge Requirements

Before merging:

- ✅ All checks passing (green)
- ✅ 2+ code reviews approved
- ✅ SonarQube quality gate PASSED
- ✅ No merge conflicts

---

## Monitoring & Logs

### Jenkins Dashboard

**Location:** http://localhost:8080

**View:**

1. Click build number (e.g., #42)
2. View full logs or by stage
3. Click stage name for specific output
4. Search logs for errors

### Real-time Monitoring

```bash
# Watch pipeline progress
watch curl -s http://localhost:8080/api/json | jq '.lastBuild.result'

# Stream logs (if pipeline is running)
ssh jenkins-server tail -f /var/log/jenkins/jenkins.log
```

### Common Log Locations

```
Docker Compose logs:  docker compose logs -f
Maven build logs:     target/build.log
Test reports:         target/surefire-reports/
Coverage reports:     coverage/index.html
SonarQube logs:       sonarqube-container logs
```

### Alert/Notification Configuration

**Email Notifications:**

- On PR merge failure
- On deployment issue
- On SonarQube quality gate failure

**Slack Notifications (Optional):**

- Configure in Jenkins > Manage Jenkins > Configure
- Post build status to Slack channel

---

## Troubleshooting

### Build Failures

| Error               | Cause                 | Solution                         |
| ------------------- | --------------------- | -------------------------------- |
| `BUILD FAILURE`     | Compilation error     | Check Java syntax, imports       |
| `COMPILATION ERROR` | Missing dependency    | Run `mvn dependency:resolve`     |
| `TEST FAILURE`      | Test assertion failed | Debug test locally, fix code     |
| `TIMEOUT`           | Build took >2 hours   | Optimize build or extend timeout |

### Test Failures

```bash
# Debug locally
mvn test -Dtest=YourTestClass#testMethod -X

# Run specific test
npm test -- --include='**/component.spec.ts'

# View detailed error
tail -100 target/surefire-reports/TestClass.txt
```

### SonarQube Issues

**Quality Gate Failed:**

1. Go to http://localhost:9000
2. Click "Branches" tab
3. Review issues in each category
4. Fix code locally
5. Push to PR branch

**Server Not Ready:**

```bash
# Wait for SonarQube startup (30-60 seconds)
docker logs sonarqube -f

# Verify running
curl http://localhost:9000
```

### Docker Build Issues

```bash
# View Docker build log
docker build -f Dockerfile . 2>&1 | tail -100

# Check image size
docker images | grep mahdikheirkhah

# Remove unused images
docker image prune -a
```

### Docker Compose Failures

```bash
# View container logs
docker compose logs -f api-gateway

# Check container status
docker compose ps

# Restart services
docker compose restart

# Full reset
docker compose down -v
docker compose up -d
```

---

## Configuration Files

### Jenkinsfile

**Location:** `/Jenkinsfile`

**Key Configuration:**

- Triggers
- Build parameters
- Environment variables
- Pipeline stages
- Deployment configuration

### docker-compose.jenkins.yml

**Location:** `/docker-compose.jenkins.yml`

**Services:**

- PostgreSQL
- Redis
- All microservices
- Frontend
- SonarQube (optional)

### pom.xml (Backend)

**Configuration:**

- Maven plugins
- Dependencies
- Build profiles
- SonarQube plugin

### package.json (Frontend)

**Configuration:**

- Dependencies
- Build scripts
- Test configuration
- ESLint rules

---

## Best Practices

### For Pipeline Success

✅ **Do**

- Run tests locally before pushing
- Check build logs for warnings
- Keep commits focused and small
- Add meaningful commit messages
- Update documentation with code changes

❌ **Don't**

- Push untested code
- Ignore pipeline failures
- Skip quality gates
- Commit secrets or sensitive data
- Push directly to main (always use PR)

### Optimizing Pipeline Speed

1. **Cache Maven dependencies**

   ```bash
   docker run -v ~/.m2:/root/.m2 maven:3.9.6 ...
   ```

2. **Parallel testing**

   ```bash
   mvn test -T 1C  # 1 thread per CPU core
   ```

3. **Skip slow tests in PR**

   - Set `RUN_INTEGRATION_TESTS=false`
   - Run integration tests only on main

4. **Use lightweight containers**
   - Alpine Linux base images
   - Minimal dependencies

---

## Summary

The CI/CD pipeline ensures:

✅ **Automatic validation** on every code change  
✅ **Quality gates** prevent poor code  
✅ **Quick feedback** on pull requests  
✅ **Continuous deployment** to production  
✅ **Audit trail** of all changes

**Result:** Fast, reliable, and secure delivery of features!

🚀 **Happy Building!**

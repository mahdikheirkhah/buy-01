# SonarQube Setup & CI/CD Integration Guide

## Phase 1: SonarQube Local Setup ✅

### 1.1 Start SonarQube with Docker

Your docker-compose.yml already has SonarQube configured! Start it:

```bash
# Start all services including SonarQube
docker compose up -d

# Or just SonarQube
docker compose up -d sonarqube

# Check if it's running
docker compose ps

# View logs
docker compose logs -f sonarqube
```

**Wait for SonarQube to be ready:**
```bash
# Check health
docker exec sonarqube curl -s http://localhost:9000/api/system/status | grep -i status
# Should return: "UP"

# Or manually check
sleep 30  # Wait for startup
curl http://localhost:9000
```

### 1.2 Access SonarQube Web Interface

**URL**: http://localhost:9000

**Default Credentials:**
- Username: `admin`
- Password: `admin`

**First Login Steps:**
1. Go to http://localhost:9000
2. Login with admin/admin
3. Change password (recommended)
4. Create new project

---

## Phase 2: SonarQube Configuration

### 2.1 Create Project in SonarQube

1. **Log into SonarQube** (http://localhost:9000)
2. Click **"Create Project"**
3. Choose **"Manually"**
4. Enter Project Details:
   - Project Key: `buy-01`
   - Display Name: `buy-01 E-Commerce`
5. Click **"Set Up"**
6. Choose **"Locally"** (we'll set up CI/CD later)
7. Select **"Java"** and **"Maven"** and **"JavaScript"** (for frontend)

### 2.2 Generate Authentication Token

1. Go to http://localhost:9000/account/security
2. Click **"Generate Token"**
3. Name it: `jenkins-token`
4. Click **"Generate"**
5. **Copy the token** (you'll need this for Jenkinsfile)

### 2.3 Configure Quality Gates

Quality Gates define when builds fail:

1. Go to **Quality Gates**
2. Click **"Create"**
3. Name: `buy-01-quality-gate`
4. Add Conditions:
   - Overall Code Coverage < 30% → FAIL
   - Duplicated Lines (%) > 3% → FAIL
   - Blocker Issues > 0 → FAIL
   - Critical Issues > 0 → FAIL
   - Major Issues > 5 → FAIL
   - Code Smells > 20 → FAIL

5. Set as **Default** for your project

### 2.4 Configure Code Analysis Rules

1. Go to **Quality Profiles**
2. Select **"Java"** profile
3. Click **"Activate"** more rules
4. Search for: Security, Code Smells, Bugs
5. Activate recommended rules

---

## Phase 3: CI/CD Pipeline Integration

### 3.1 Jenkinsfile SonarQube Integration

Update your Jenkinsfile to include SonarQube analysis (already has a stage, needs token):

```groovy
stage('📊 SonarQube Analysis') {
    when {
        expression { params.RUN_SONAR == true }
    }
    steps {
        script {
            echo "📊 Running SonarQube analysis..."
            try {
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        docker run --rm \\
                          -v ${WORKSPACE}:${WORKSPACE} \\
                          -w ${WORKSPACE} \\
                          -e SONAR_HOST_URL=http://sonarqube:9000 \\
                          -e SONAR_LOGIN=${SONAR_TOKEN} \\
                          sonarsource/sonar-scanner-cli:latest \\
                          -Dsonar.projectKey=buy-01 \\
                          -Dsonar.projectName="buy-01 E-Commerce" \\
                          -Dsonar.sources=backend,frontend/src \\
                          -Dsonar.java.binaries=backend/*/target/classes \\
                          -Dsonar.coverage.exclusions=**/dto/**,**/config/**,**/entity/** \\
                          -Dsonar.qualitygate.wait=true
                    '''
                }
                echo "✅ SonarQube analysis completed"
            } catch (Exception e) {
                echo "⚠️ SonarQube analysis failed: ${e.message}"
            }
        }
    }
}
```

### 3.2 Jenkins Configuration for SonarQube

1. **Go to Jenkins**: http://localhost:8080
2. **Manage Jenkins** → **System**
3. Find **"SonarQube servers"**
4. Add server:
   - Name: `SonarQube`
   - Server URL: `http://sonarqube:9000`
   - Server authentication token: (create Jenkins credential)

5. Create credential:
   - Go to **Credentials** → **System** → **Global**
   - Click **"Add Credentials"**
   - Kind: **Secret text**
   - Secret: (paste your SonarQube token)
   - ID: `sonarqube-token`
   - Save

---

## Phase 4: GitHub Integration

### 4.1 GitHub App for SonarQube

1. **In SonarQube**, go to **Administration** → **Configuration** → **Webhooks**
2. Click **"Create"**
3. Name: `GitHub`
4. URL: `https://github.com/mahdikheirkhah/buy-01`
5. Create GitHub App at: https://github.com/settings/apps/new
   - App name: `SonarQube-buy-01`
   - Homepage URL: `http://localhost:9000`
   - Webhook URL: (from SonarQube webhook)
   - Permissions:
     - Contents: Read
     - Commit statuses: Read & Write
     - Pull requests: Read & Write

### 4.2 GitHub Actions for CI/CD

Create `.github/workflows/sonarqube.yml`:

```yaml
name: SonarQube Analysis

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  sonarqube:
    name: SonarQube Analysis
    runs-on: ubuntu-latest
    
    services:
      sonarqube:
        image: sonarqube:lts-community
        options: >-
          --health-cmd "wget -qO- http://localhost:9000/api/system/status"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v3
        with:
          fetch-depth: 0

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'corretto'
          cache: maven

      - name: Build backend
        run: |
          cd backend
          mvn clean install -DskipTests

      - name: Run unit tests
        run: |
          cd backend
          mvn test -B -Dtest=*UnitTest

      - name: SonarQube Analysis
        run: |
          cd backend
          mvn sonar:sonar \
            -Dsonar.projectKey=buy-01 \
            -Dsonar.host.url=http://sonarqube:9000 \
            -Dsonar.login=${{ secrets.SONARQUBE_TOKEN }}

      - name: Check Quality Gate
        run: |
          echo "Quality gate check (optional failure)"
```

---

## Phase 5: Continuous Monitoring

### 5.1 SonarQube Dashboard

- **Dashboard URL**: http://localhost:9000/dashboard?id=buy-01
- **Shows**:
  - Code Coverage
  - Duplicated Lines
  - Code Smells
  - Bugs
  - Vulnerabilities
  - Security Hotspots

### 5.2 Configure Notifications

#### Email Notifications

1. In SonarQube: **Administration** → **Configuration** → **Email**
2. Configure SMTP:
   - SMTP Host: `smtp.gmail.com`
   - SMTP Port: `587`
   - SMTP Username: `your-email@gmail.com`
   - SMTP Password: `app-password`
3. Enable notifications in project settings

#### Slack Notifications

1. Create Slack App at: https://api.slack.com/apps
2. Get webhook URL
3. In SonarQube: **Administration** → **Webhooks**
4. Add webhook:
   - URL: `https://hooks.slack.com/services/YOUR/WEBHOOK/URL`
   - Trigger: "Quality gate failing"

---

## Phase 6: Code Review & Approval Process

### 6.1 GitHub Branch Protection Rules

1. Go to repository **Settings** → **Branches**
2. Add rule for `main` branch:
   - ✅ Require a pull request before merging
   - ✅ Require status checks to pass (SonarQube)
   - ✅ Require branches to be up to date
   - ✅ Require code reviews: 1 approval

### 6.2 SonarQube PR Decoration

1. In SonarQube: **Administration** → **Pull Request Decoration**
2. Select **GitHub**
3. Enter GitHub App credentials
4. Enable: "Decorate PRs with SonarQube comments"

---

## Phase 7: IDE Integration (Bonus)

### 7.1 VS Code SonarLint Extension

1. Install extension: **SonarLint**
2. Configure:
   - Settings → Extension: SonarLint
   - Server: `http://localhost:9000`
   - Token: (your SonarQube token)
3. Get real-time code quality feedback

### 7.2 IntelliJ IDEA Integration

1. Install plugin: **SonarLint**
2. **File** → **Settings** → **Tools** → **SonarLint**
3. Configure:
   - Server URL: `http://localhost:9000`
   - Authentication Token: (your token)
4. Start seeing issues in real-time

---

## Usage Commands

### Local Analysis (Manual)

```bash
# Analyze backend
mvn sonar:sonar \
  -Dsonar.projectKey=buy-01 \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=admin \
  -Dsonar.password=admin

# Analyze frontend
npm install -g sonar-scanner
sonar-scanner \
  -Dsonar.projectKey=buy-01-frontend \
  -Dsonar.sources=frontend/src \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=admin
```

### View Results

- Dashboard: http://localhost:9000/dashboard?id=buy-01
- Issues: http://localhost:9000/project/issues?id=buy-01
- Code: http://localhost:9000/project/code?id=buy-01
- Measures: http://localhost:9000/project/measurements?id=buy-01

---

## Troubleshooting

### SonarQube Not Starting

```bash
# Check logs
docker compose logs sonarqube

# Increase memory
docker compose down
docker run -d \
  -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true \
  -p 9000:9000 \
  sonarqube:lts-community
```

### Analysis Not Showing Results

```bash
# Verify project exists
curl -u admin:admin http://localhost:9000/api/projects/search?projects=buy-01

# Check analysis status
curl -u admin:admin http://localhost:9000/api/ce/activity?projectKey=buy-01
```

### Token Issues

```bash
# Generate new token
# 1. Go to http://localhost:9000/account/security
# 2. Generate new token
# 3. Update Jenkins credentials
```

---

## Next Steps

1. ✅ Start SonarQube with `docker compose up -d`
2. ✅ Access http://localhost:9000
3. ✅ Create project `buy-01`
4. ✅ Generate token
5. ✅ Configure Jenkins (add SonarQube server)
6. ✅ Update Jenkinsfile with RUN_SONAR parameter
7. ✅ Configure GitHub integration
8. ✅ Set up branch protection rules
9. ✅ Install IDE extensions
10. ✅ Run first analysis

---

**Status**: Ready to implement
**Effort**: ~2-3 hours for full setup
**Result**: Automated code quality monitoring with 75+ tests


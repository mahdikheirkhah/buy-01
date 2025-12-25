# SonarQube Implementation - Step-by-Step Guide

## Overview

This guide will walk you through implementing SonarQube for your buy-01 e-commerce microservices project. The complete setup includes:

- ✅ Local SonarQube instance (Docker)
- ✅ Backend & Frontend analysis
- ✅ Jenkins CI/CD integration
- ✅ GitHub Actions workflow
- ✅ Quality gates & rules
- ✅ IDE integrations
- ✅ Slack/Email notifications
- ✅ Code review process

**Estimated Time**: 2-3 hours

---

## Phase 1: Start SonarQube (15 minutes)

### Step 1: Start SonarQube Container

```bash
# Navigate to project root
cd /C/Users/mahdi/Desktop/buy-01

# Start SonarQube (already in docker-compose.yml)
docker compose up -d sonarqube

# Wait for startup (30-60 seconds)
# Monitor logs
docker compose logs -f sonarqube
```

### Step 2: Verify SonarQube is Running

```bash
# Check status
docker compose ps | grep sonarqube

# Check health
curl http://localhost:9000

# Should return 200 OK and the SonarQube interface
```

### Step 3: Access Web Interface

**URL**: http://localhost:9000

**Default Credentials**:
- Username: `admin`
- Password: `admin`

**You should see**:
- SonarQube dashboard
- Login prompt
- Welcome page

---

## Phase 2: Configure SonarQube (30 minutes)

### Step 1: Change Admin Password

1. Click on your profile (top right)
2. Select "My Account"
3. Go to "Security" tab
4. Change password to something secure
5. Save

### Step 2: Create Projects

#### Create Backend Project

1. Click "Create Project"
2. Choose "Manually"
3. Fill in:
   - **Project Key**: `buy-01-backend`
   - **Display Name**: `buy-01 Backend Microservices`
4. Click "Setup"
5. Choose "Locally"
6. Select **Maven** as build system
7. Copy the command (you'll use this in Jenkinsfile)

#### Create Frontend Project

1. Repeat above, but:
   - **Project Key**: `buy-01-frontend`
   - **Display Name**: `buy-01 Frontend`
   - Select **JavaScript** as language

### Step 3: Generate Authentication Token

1. Go to http://localhost:9000/account/security
2. Under "Generate Tokens" section:
   - Name: `jenkins-token`
   - Click "Generate"
3. **Copy the token** (long string)
4. Save it somewhere safe (you'll need it for Jenkins)

### Step 4: Create Quality Gate

1. Go to **Quality Gates** (left sidebar)
2. Click **"Create"**
3. Name: `buy-01-quality-gate`
4. Click **"Create"**
5. Add Conditions:

| Metric | Condition | Value |
|--------|-----------|-------|
| Coverage | is less than | 30 % |
| Duplicated Lines (%) | is greater than | 3 % |
| Blocker Issues | is greater than | 0 |
| Critical Issues | is greater than | 0 |
| Code Smells | is greater than | 20 |
| Major Issues | is greater than | 5 |
| Security Hotspots Reviewed | is less than | 50 % |

6. Click "Set as Default" button

### Step 5: Configure Code Analysis Rules

1. Go to **Quality Profiles** (left sidebar)
2. Find **"Sonar Way"** for Java
3. Click on it
4. Click **"Activate More"**
5. Filter by **"Type: Vulnerability"**
6. Activate all security rules
7. Repeat for:
   - Bug
   - Code Smell
   - Security Hotspot

---

## Phase 3: Jenkins Configuration (30 minutes)

### Step 1: Add SonarQube Credential to Jenkins

1. Go to Jenkins: http://localhost:8080
2. Click **"Manage Jenkins"**
3. Click **"Credentials"**
4. Click **"System"** → **"Global credentials"**
5. Click **"Add Credentials"**
6. Fill in:
   - **Kind**: "Secret text"
   - **Secret**: (paste your SonarQube token)
   - **ID**: `sonarqube-token`
   - **Description**: "SonarQube API Token"
7. Click **"Create"**

### Step 2: Update Jenkinsfile

The Jenkinsfile has already been updated! The SonarQube Analysis stage now:
- Uses the credential you just created
- Analyzes both backend and frontend
- Waits for results
- Provides helpful error messages

### Step 3: Test SonarQube Integration

1. Go to your Jenkins job
2. Click **"Build with Parameters"**
3. Set:
   - `RUN_TESTS`: true
   - `RUN_SONAR`: true ✅ (THIS IS NEW!)
   - `SKIP_DEPLOY`: true
   - `DEPLOY_LOCALLY`: false
4. Click **"Build"**
5. Monitor the console output
6. Look for "Backend analysis completed" and "Frontend analysis completed"
7. Check results at http://localhost:9000

---

## Phase 4: GitHub Integration (45 minutes)

### Step 1: Create GitHub Actions Workflow

The workflow file has already been created at `.github/workflows/sonarqube.yml`!

It includes:
- Build backend & frontend
- Run tests
- Run SonarQube analysis
- Check quality gates
- Upload coverage reports
- Comment on PRs
- Notify Slack (optional)

### Step 2: Add GitHub Secrets

1. Go to GitHub repo → **Settings**
2. Click **"Secrets and variables"** → **"Actions"**
3. Add secrets:

| Secret | Value |
|--------|-------|
| `SONARQUBE_TOKEN` | (your SonarQube token) |
| `SONARQUBE_HOST` | `http://localhost:9000` |
| `SLACK_WEBHOOK` | (optional, if using Slack) |

### Step 3: Configure Branch Protection

1. Go to **Settings** → **Branches**
2. Click **"Add rule"** for `main` branch
3. Enable:
   - ✅ Require a pull request before merging
   - ✅ Require status checks to pass (SonarQube)
   - ✅ Require code reviews: 1 approval
   - ✅ Dismiss stale PR approvals

### Step 4: Test GitHub Workflow

1. Create a new branch: `git checkout -b feature/test-sonarqube`
2. Make a small change to a file
3. Commit and push: `git push origin feature/test-sonarqube`
4. Create a Pull Request
5. Watch the workflow run automatically
6. SonarQube will analyze and comment on the PR
7. Merge only if:
   - All checks pass ✅
   - Quality gate passes ✅
   - Code review approved ✅

---

## Phase 5: IDE Integration (15 minutes)

### Option A: VS Code

1. **Install Extension**: "SonarLint"
2. **Open Settings**: `Ctrl+,` (or `Cmd+,`)
3. Search: "sonarlint"
4. Configure:
   - **Server URL**: `http://localhost:9000`
   - **Token**: (your SonarQube token)
5. **Create .vscode/settings.json** in project:

```json
{
  "sonarlint.connectedMode.project": "buy-01-backend",
  "sonarlint.showDescriptionInHovers": true,
  "sonarlint.showAnalysisProgress": true
}
```

6. **Open a Java file** → See issues highlighted in real-time!

### Option B: IntelliJ IDEA

1. **Install Plugin**: "SonarLint"
2. Go to **Settings** → **Tools** → **SonarLint**
3. Click **"Add Connection"**:
   - **Type**: SonarQube
   - **Server URL**: `http://localhost:9000`
   - **Token**: (your SonarQube token)
4. Click **"Project Binding"**
5. Select **"buy-01-backend"** project
6. **Start editing** → See real-time feedback!

---

## Phase 6: Email/Slack Notifications (Optional, 15 minutes)

### Email Notifications

1. In SonarQube: **Administration** → **Configuration** → **Email**
2. Configure SMTP:
   - **SMTP Host**: `smtp.gmail.com`
   - **SMTP Port**: `587`
   - **SMTP Username**: `your-email@gmail.com`
   - **SMTP Password**: (create App Password in Gmail settings)
3. Enable notifications in user preferences

### Slack Notifications

1. Create Slack App at: https://api.slack.com/apps/new
2. Get **Webhook URL** from "Incoming Webhooks"
3. In SonarQube: **Administration** → **Webhooks**
4. Add webhook:
   - **Name**: Slack
   - **URL**: (paste your Slack webhook URL)
   - **Events**: "Quality gate failing"

---

## Phase 7: First Analysis (10 minutes)

### Run Manual Analysis

```bash
# Backend
cd backend
mvn sonar:sonar \
  -Dsonar.projectKey=buy-01-backend \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=your-token-here

# Frontend
cd frontend
sonar-scanner \
  -Dsonar.projectKey=buy-01-frontend \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=your-token-here
```

### Review Results

1. Go to http://localhost:9000/dashboard?id=buy-01-backend
2. Check:
   - **Code Coverage**: % of lines tested
   - **Duplicated Lines**: Code duplication
   - **Code Smells**: Maintainability issues
   - **Bugs**: Potential bugs
   - **Vulnerabilities**: Security issues
3. Click on each metric to drill down
4. Check **Issues** tab for detailed list

---

## Phase 8: Code Quality Improvements (Ongoing)

### Address Issues

1. For each critical/blocker issue in SonarQube:
   - Click on the issue
   - Read the description
   - Click "See rule" for explanation
   - Fix the code in your IDE (SonarLint will help!)
   - Commit and push

### Create Pull Request

1. Push your fixes to a branch
2. Create PR
3. SonarQube will analyze
4. Approve the review
5. Merge when quality gate passes

---

## Testing Checklist

### SonarQube Running
- [ ] Can access http://localhost:9000
- [ ] Default login works (admin/admin)
- [ ] Projects created (backend & frontend)
- [ ] Token generated

### Jenkins Integration
- [ ] Credential `sonarqube-token` created
- [ ] Jenkinsfile updated
- [ ] Build runs with `RUN_SONAR=true`
- [ ] Analysis completes successfully

### GitHub Integration
- [ ] Workflow file exists at `.github/workflows/sonarqube.yml`
- [ ] Secrets added (SONARQUBE_TOKEN, etc.)
- [ ] PR triggers workflow automatically
- [ ] Quality gate status appears in PR

### IDE Integration
- [ ] SonarLint installed in VS Code/IntelliJ
- [ ] Connected to SonarQube server
- [ ] Real-time feedback appears
- [ ] Issues highlighted in files

### Quality Gate
- [ ] Quality gate created
- [ ] Rules configured
- [ ] Default set for projects
- [ ] Build fails if gate fails

---

## Command Reference

```bash
# Start SonarQube
docker compose up -d sonarqube

# Check SonarQube status
curl http://localhost:9000/api/system/status

# View logs
docker compose logs -f sonarqube

# Stop SonarQube
docker compose down

# Access SonarQube
http://localhost:9000

# Jenkins builds
http://localhost:8080 (with RUN_SONAR=true)

# GitHub Actions
https://github.com/mahdikheirkhah/buy-01/actions
```

---

## Troubleshooting

### SonarQube Won't Start
```bash
docker compose logs sonarqube
# Check for "bootstrap" errors
# May need more memory
```

### Analysis Not Appearing
```bash
# Check if project exists
curl -u admin:admin http://localhost:9000/api/projects/search

# Check analysis status
curl -u admin:admin http://localhost:9000/api/ce/activity
```

### Token Expired
```bash
# Generate new token at http://localhost:9000/account/security
# Update Jenkins credential
# Update GitHub secrets
```

---

## Next Steps

1. ✅ Follow all 8 phases above
2. ✅ Test each integration
3. ✅ Fix any issues found by SonarQube
4. ✅ Merge PR with clean quality gate
5. ✅ Monitor dashboard regularly

---

## Audit Checklist

- [ ] SonarQube web interface accessible
- [ ] Projects configured (backend & frontend)
- [ ] Jenkins integration working
- [ ] GitHub Actions workflow running
- [ ] Quality gates defined and enforcing
- [ ] PR comments showing analysis results
- [ ] IDE extensions showing real-time feedback
- [ ] Email/Slack notifications configured (optional)
- [ ] Code review process enforced

---

**Status**: Ready to implement
**Difficulty**: Medium
**Time**: 2-3 hours
**Result**: Automated code quality monitoring with automated quality gates



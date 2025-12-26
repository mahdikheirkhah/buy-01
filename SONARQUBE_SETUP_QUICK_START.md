# Quick Start: SonarQube Setup for Project Audit

## 🚀 Step-by-Step Setup (15 minutes)

### 1. Start SonarQube
```bash
cd /Users/mohammad.kheirkhah/Desktop/buy-01
docker compose up -d sonarqube
```

**Verify it's running:**
```bash
# Wait 30-60 seconds for SonarQube to start
curl http://localhost:9000

# Expected response: HTML page loads without error
```

### 2. Access SonarQube Web Interface
- **URL:** http://localhost:9000
- **Username:** admin
- **Password:** admin
- **Action on first login:** Change password to secure value

### 3. Create Authentication Token for Jenkins

1. Click your profile icon (top-right) → Account
2. Click "Security" tab
3. Generate Tokens section → Create token
4. Name: `jenkins-sonarqube-token`
5. Copy the token value

### 4. Create Projects in SonarQube

#### Backend Project:
1. Administration → Projects → Create Project
2. **Key:** `buy-01-backend`
3. **Name:** `Buy-01 Backend`
4. **Main Branch:** `main`

#### Frontend Project:
1. Create new project
2. **Key:** `buy-01-frontend`
3. **Name:** `Buy-01 Frontend`
4. **Main Branch:** `main`

### 5. Configure Jenkins Credentials

1. Jenkins → Manage Jenkins → Credentials
2. Add credentials → Secret text
   - **Secret:** (paste token from step 3)
   - **ID:** `sonarqube-token`

### 6. Run Your First Analysis

Push code and trigger Jenkins build with SonarQube enabled:

```bash
# Option 1: Manual build in Jenkins UI
# Select "Build with Parameters" → RUN_SONAR=true

# Option 2: Push code (if webhook configured)
git add .
git commit -m "Enable SonarQube analysis"
git push origin main
```

### 7. View Results

Once analysis completes (5-10 minutes):
- Backend: http://localhost:9000/dashboard?id=buy-01-backend
- Frontend: http://localhost:9000/dashboard?id=buy-01-frontend

---

## ✅ Audit Requirements Checklist

### FUNCTIONAL REQUIREMENTS

#### ✅ 1. SonarQube Web Interface Accessibility
- [ ] SonarQube running on http://localhost:9000
- [ ] Can login successfully
- [ ] Dashboard displays properly
- [ ] Projects are visible

**Verification:**
```bash
curl -u admin:password http://localhost:9000/api/system/status
```

#### ✅ 2. GitHub Integration
- [ ] Created GitHub token with repo:status and public_repo scopes
- [ ] Configured in SonarQube Administration
- [ ] Webhook configured on GitHub repository
- [ ] PR comments show analysis results

**GitHub Setup:**
```
Settings → Developer settings → Personal access tokens
Scope required: repo:status, public_repo
```

#### ✅ 3. Docker Configuration
- [ ] SonarQube service in docker-compose.yml
- [ ] Volume persistence configured
- [ ] Network connectivity verified
- [ ] Health checks passing

**Verification:**
```bash
docker ps | grep sonarqube
docker logs sonarqube | tail -20
```

#### ✅ 4. CI/CD Automation
- [ ] Jenkins pipeline configured
- [ ] SonarQube stage in Jenkinsfile
- [ ] Analysis runs on every push (with RUN_SONAR=true)
- [ ] Quality gate configured
- [ ] Build fails on quality gate breach

**Current Configuration:**
- Jenkinsfile has SonarQube Analysis stage
- Runs both backend (Maven) and frontend (sonar-scanner)
- Quality gate: PASSED/FAILED

#### ✅ 5. Code Review Process
- [ ] Pull request template created (.github/pull_request_template.md)
- [ ] Branch protection enabled on main
- [ ] Requires at least 1 review
- [ ] Status checks must pass
- [ ] GitHub Actions runs on every PR
- [ ] Merging blocked if checks fail

**Setup Branch Protection:**
```
Repository Settings → Branches → Add Rule
- Branch name: main
- Require pull request reviews: ✓
- Require status checks to pass: ✓
- Require branches to be up to date: ✓
```

---

### COMPREHENSION REQUIREMENTS

#### 📚 1. SonarQube Setup & Integration
**Questions to Answer:**

Q: What are the steps to set up SonarQube in this project?
```
A: 
1. Run 'docker compose up -d sonarqube'
2. Access http://localhost:9000
3. Login and change password
4. Create projects: buy-01-backend and buy-01-frontend
5. Generate Jenkins token
6. Configure Jenkins credentials
7. Run analysis via Jenkinsfile stage
```

Q: How is SonarQube integrated with GitHub and CI/CD?
```
A:
- GitHub webhook triggers analysis on push
- Jenkins pipeline runs SonarQube Maven plugin
- Results sent to SonarQube server
- Quality gate evaluated
- PR comments show findings
- Build fails if quality gate broken
```

#### 📚 2. SonarQube Functionality
**Questions to Answer:**

Q: How does SonarQube function in this project?
```
A:
- Analyzes Java code (6 backend microservices)
- Analyzes TypeScript code (Angular frontend)
- Tracks metrics: coverage, bugs, vulnerabilities
- Enforces quality standards
- Provides security hotspot analysis
- Identifies code duplicates
- Measures code complexity
```

Q: What code quality metrics are tracked?
```
A:
- Lines of Code (LOC)
- Code Coverage %
- Bug Count
- Vulnerability Count
- Code Smell Count
- Duplication %
- Cyclomatic Complexity
- Cognitive Complexity
```

---

### SECURITY REQUIREMENTS

#### 🔒 1. Permissions & Access Control
- [ ] Admin user configured (admin/password)
- [ ] Jenkins service account created
- [ ] Developer read-only access configured
- [ ] Token rotation policy documented
- [ ] No hardcoded credentials in code

**Verify Users:**
```
SonarQube → Administration → Security → Users
- admin (system administrator)
- jenkins (CI/CD integration)
- developers (read-only access)
```

#### 🔒 2. Rules Configuration
- [ ] Security rules enabled (SQL injection, XSS, etc.)
- [ ] Reliability rules enabled (null checks, resource leaks)
- [ ] Maintainability rules enabled (complexity, duplication)
- [ ] Coverage threshold set (minimum 80%)
- [ ] Quality gate enforced

**Configured Rules:**
```
Backend (Java):
- Security: SQL Injection, XXE, CSRF
- Reliability: Null pointers, Resource leaks
- Maintainability: Complexity limits, Comments

Frontend (TypeScript):
- Security: eval(), XSS, Crypto
- Reliability: Unused variables, Exception handling
- Maintainability: Complexity, Dead code
```

#### 🔒 3. Code Quality Improvements
- [ ] SonarQube issues addressed
- [ ] Fixes committed to repository
- [ ] Quality trend improving
- [ ] Security hotspots resolved
- [ ] Code coverage increasing

**Monitor Progress:**
```
Dashboard → Dashboard?id=buy-01-backend
Dashboard → Dashboard?id=buy-01-frontend

Check:
✓ Quality Gate Status
✓ Issues trend (decreasing)
✓ Code Coverage %
✓ Reliability Rating
```

---

### BONUS FEATURES

#### 📧 Email Notifications
**Setup Instructions:**

1. SonarQube → Administration → Configuration → Email
2. Configure SMTP:
   ```yaml
   SMTP Server: smtp.gmail.com
   SMTP Port: 587
   Sender: your-email@gmail.com
   ```
3. User Profile → Notifications → Enable alerts

#### 💬 Slack Integration
**Setup Instructions:**

1. Create Slack Workspace Webhook:
   - Slack Workspace Settings → Apps & Integrations
   - Create Incoming Webhook for #notifications channel

2. SonarQube → Administration → Configuration → Slack
3. Add webhook URL
4. Enable notifications

#### 💻 IDE Integration

**VS Code:**
```bash
# Install extension
code --install-extension SonarSource.sonarlint-vscode

# Configure in .vscode/settings.json
{
  "sonarlint.connectedMode.serverUrl": "http://localhost:9000",
  "sonarlint.connectedMode.token": "your-token"
}
```

**IntelliJ IDEA:**
```
Settings → Plugins → Search "SonarLint" → Install
Settings → SonarLint → Configure Server
```

---

## 🔍 Audit Verification Steps

### Step 1: Verify Web Interface
```bash
# Test 1: Check URL accessibility
curl http://localhost:9000

# Test 2: Check API status
curl -u admin:password http://localhost:9000/api/system/status
```

✅ Expected: Response includes "UP" status

### Step 2: Verify GitHub Integration
```bash
# In GitHub repository:
# 1. Go to Pull Requests
# 2. Create test PR
# 3. Verify SonarQube analysis status appears
```

✅ Expected: Status check shows "SonarQube" with status

### Step 3: Verify CI/CD Pipeline
```bash
# In Jenkins:
# 1. Navigate to pipeline job
# 2. Select "Build with Parameters"
# 3. Set RUN_SONAR=true
# 4. Click Build
# 5. Wait for analysis to complete
```

✅ Expected: SonarQube Analysis stage completes successfully

### Step 4: Verify Quality Gate
```bash
# Check quality gate status
curl -u admin:password "http://localhost:9000/api/qualitygates/project_status?projectKey=buy-01-backend"
```

✅ Expected: Response includes "status":"PASSED" or "status":"FAILED"

### Step 5: Verify Code Review Process
```bash
# In GitHub:
# 1. Create branch
# 2. Make change
# 3. Push and create PR
# 4. Verify:
#    - GitHub Actions runs
#    - Branch protection prevents merge
#    - PR template appears
```

✅ Expected: PR shows analysis results and review required

---

## 🆘 Troubleshooting

### SonarQube Won't Start
```bash
# Check logs
docker logs sonarqube | tail -30

# Check disk space
docker exec sonarqube df -h

# Check memory
docker stats sonarqube
```

### Analysis Fails
```bash
# Check Jenkins logs
docker logs jenkins-server | grep -i sonar

# Test connectivity
curl -v http://localhost:9000/api/system/status

# Verify token
echo $SONAR_TOKEN
```

### GitHub Integration Not Working
```bash
# In SonarQube:
# Administration → ALM Integrations → GitHub
# Verify token and repository configuration

# In GitHub:
# Settings → Webhooks → Verify webhook URL
# Check recent deliveries for errors
```

---

## 📝 Audit Submission Checklist

Before submitting for audit, verify:

- [ ] SONARQUBE_AUDIT_GUIDE.md created and documented
- [ ] SonarQube running and accessible
- [ ] Projects created: buy-01-backend, buy-01-frontend
- [ ] Jenkins pipeline configured with SonarQube stage
- [ ] GitHub integration working (PR analysis comments)
- [ ] Branch protection enabled on main
- [ ] Pull request template created
- [ ] Quality gate configured and enforced
- [ ] Code review process documented
- [ ] IDE extensions installed (VS Code/IntelliJ)
- [ ] Email/Slack notifications configured
- [ ] All audit requirements documented with evidence

---

## 📞 Support

For issues or questions:
1. Check SONARQUBE_AUDIT_GUIDE.md for detailed information
2. Review troubleshooting section above
3. Check Docker logs: `docker logs sonarqube`
4. Check Jenkins logs for pipeline issues

---

**Last Updated:** December 26, 2025
**Project:** buy-01 E-Commerce Platform

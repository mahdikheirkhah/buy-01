# SonarQube Troubleshooting & Quick Reference

## SonarQube Not Starting

### Problem: Container won't start
```bash
# Check if port 9000 is already in use
lsof -i :9000  # macOS/Linux
netstat -ano | findstr :9000  # Windows

# If port in use, either stop the service or use different port
docker compose down
```

### Problem: SonarQube is slow to start
```bash
# SonarQube may take 30-60 seconds to fully start
# Check logs
docker compose logs -f sonarqube

# Wait for "SonarQube is up" message
# Don't give up after 30 seconds!
```

### Problem: Elasticsearch error in logs
```bash
# If you see "bootstrap" errors:
# SonarQube needs more memory

# Solution: Increase Docker memory
# Edit docker-compose.yml and add:
# environment:
#   SONAR_ES_BOOTSTRAP_CHECKS_DISABLE: 'true'
# (This is already in your config)

# Restart
docker compose restart sonarqube
```

---

## Jenkins Integration Issues

### Problem: SonarQube Analysis stage not running

**Check 1**: Parameter enabled?
```groovy
// In Jenkinsfile, the stage has:
when {
    expression { params.RUN_SONAR == true }
}
// Make sure you set RUN_SONAR=true when building
```

**Check 2**: Credential exists?
```bash
# In Jenkins:
# Manage Jenkins → Credentials → System → Global
# Should have "sonarqube-token" credential
```

**Check 3**: Token is valid?
```bash
# Test token
curl -u admin:your-token http://localhost:9000/api/user_tokens/search
# Should return success
```

### Problem: "Connection refused" error

**Likely cause**: SonarQube not accessible from Jenkins

**Solution**:
```bash
# If Jenkins in Docker, use: sonarqube:9000 (service name)
# If Jenkins on host, use: http://localhost:9000

# Test from Jenkins container
docker exec jenkins curl http://sonarqube:9000
# Should return 200 OK
```

### Problem: Analysis times out

**Likely cause**: Large codebase, slow network, or insufficient memory

**Solution**:
```bash
# Increase timeout in Jenkinsfile
// Add to stage:
timeout(time: 30, unit: 'MINUTES')

# Or run analysis with more memory
-Xmx2G flag
```

---

## GitHub Actions Issues

### Problem: Workflow not triggering

**Check 1**: Workflow file exists?
```bash
git ls-files | grep sonarqube.yml
# Should show: .github/workflows/sonarqube.yml
```

**Check 2**: Workflow syntax valid?
```bash
# GitHub will show error in Actions tab
# Fix YAML formatting (indentation, colons, etc.)
```

**Check 3**: Secret configured?
```bash
# In GitHub repo:
# Settings → Secrets and variables → Actions
# Should have: SONARQUBE_TOKEN
```

### Problem: "Failed to find SonarQube"

**Likely cause**: SONARQUBE_HOST secret missing or wrong URL

**Solution**:
```bash
# Add secret in GitHub:
SONARQUBE_HOST=http://localhost:9000
# Or use default in workflow if not set
```

### Problem: Quality gate check fails silently

**Solution**: Check SonarQube project key
```bash
# In workflow, check:
-Dsonar.projectKey=buy-01-backend  # Must match SonarQube
```

---

## SonarQube Analysis Issues

### Problem: "Project not found"

**Solution 1**: Check project key
```bash
# Project keys are case-sensitive
# Verify in SonarQube dashboard
# Should be: buy-01-backend, buy-01-frontend
```

**Solution 2**: Create project if missing
```bash
# In SonarQube:
# Create Project → Manually
# Enter correct project key
```

### Problem: "No issues found" but analysis completes

This is usually correct behavior!

```bash
# If code is clean:
# - No bugs detected ✅
# - No vulnerabilities ✅
# - Quality gate passes ✅

# Verify analysis ran:
curl -u admin:token http://localhost:9000/api/ce/activity?projectKey=buy-01-backend
# Should show recent analysis
```

### Problem: Coverage metrics show 0%

**Likely cause**: Test reports not provided to SonarQube

**Solution 1**: Run tests with coverage
```bash
# Backend (includes JaCoCo)
mvn test jacoco:report

# Frontend
npm test -- --code-coverage
```

**Solution 2**: Tell SonarQube where reports are
```groovy
// In Jenkinsfile, add:
-Dsonar.coverage.jacoco.xmlReportPaths=backend/*/target/site/jacoco/jacoco.xml
-Dsonar.javascript.lcov.reportPaths=frontend/coverage/lcov.info
```

---

## PR Comment Issues

### Problem: SonarQube not commenting on PR

**Check 1**: GitHub integration configured
```bash
# In SonarQube:
# Administration → Configuration → Pull Request Decoration
# Select: GitHub
```

**Check 2**: Webhook configured
```bash
# In SonarQube:
# Administration → Webhooks
# Should have: GitHub webhook
```

**Check 3**: PR branch has analysis
```bash
# Make sure workflow ran on the branch
# Check Actions tab in GitHub
```

---

## Quality Gate Issues

### Problem: Quality gate always passes

**Likely cause**: No quality gate assigned to project

**Solution**:
```bash
# In SonarQube:
# Quality Gates → select your gate
# Projects → assign to project
# Or set as default
```

### Problem: Quality gate too strict

**Solution**: Adjust thresholds
```bash
# In SonarQube:
# Quality Gates → edit → update conditions
# Increase thresholds (more lenient)
# Or remove conditions not needed
```

### Problem: Can't see quality gate status in PR

**Likely cause**: Status check not configured

**Solution**:
```bash
# In GitHub:
# Settings → Branches → Branch protection
# Check: "Require status checks to pass"
# Make sure SonarQube check is selected
```

---

## IDE Integration Issues

### Problem: SonarLint not connecting to SonarQube

**VS Code**:
```json
{
  "sonarlint.connectedMode.servers": [
    {
      "connectionId": "sonarqube-local",
      "serverUrl": "http://localhost:9000",
      "token": "your-token-here"
    }
  ]
}
```

**IntelliJ**:
1. Settings → Tools → SonarLint → Server Connections
2. Add: http://localhost:9000 with token
3. Re-open project

### Problem: Real-time analysis not working

**Check**:
```bash
# Try simple test:
1. Add unused variable: int x = 5;
2. Should see red squiggly line immediately
3. Hover to see issue details
```

**If not showing**:
1. Restart IDE
2. Reopen file
3. Check SonarLint output panel for errors

---

## Performance Issues

### Problem: Analysis takes too long

**Solutions**:
```bash
# 1. Skip tests (if slow)
-DskipTests

# 2. Skip coverage (faster but no metrics)
-Dsonar.coverage.exclusions=**/*

# 3. Increase memory
-Xmx4G

# 4. Exclude files
-Dsonar.exclusions=**/node_modules/**,**/target/**
```

### Problem: SonarQube UI is slow

**Solutions**:
```bash
# 1. Increase Docker memory: -m 4g
# 2. Clear browser cache
# 3. Restart SonarQube: docker compose restart sonarqube
# 4. Check available disk space
```

---

## Security Issues

### Problem: Token not working

**Solution 1**: Check token validity
```bash
curl -u admin:token http://localhost:9000/api/user_tokens/search
```

**Solution 2**: Regenerate token
```bash
# In SonarQube:
# Account → Security → Tokens
# Click trash to delete old token
# Click "Generate" for new token
# Copy and update Jenkins/GitHub
```

### Problem: Unauthorized when accessing dashboard

**Solution**: Check login
```bash
# Default: admin/admin
# If changed, use new credentials
# Or reset in SonarQube UI
```

---

## Docker Issues

### Problem: "Cannot connect to Docker daemon"

```bash
# Make sure Docker is running
docker ps

# If not running, start Docker Desktop
# Then retry: docker compose up -d sonarqube
```

### Problem: Port conflicts

```bash
# Check what's using port 9000
lsof -i :9000  # macOS/Linux
netstat -ano | findstr :9000  # Windows

# Kill process or use different port in docker-compose.yml
```

---

## Quick Commands Reference

```bash
# Start/Stop SonarQube
docker compose up -d sonarqube
docker compose down

# Check status
docker compose ps
curl http://localhost:9000

# View logs
docker compose logs -f sonarqube

# Access SonarQube
http://localhost:9000

# Default credentials
# Username: admin
# Password: admin

# Run analysis manually
mvn sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.login=token

# Check quality gate
curl http://localhost:9000/api/qualitygates/list
curl http://localhost:9000/api/qualitygates/project_status?projectKey=buy-01-backend
```

---

## Getting Help

### Check logs in order:
1. SonarQube logs: `docker compose logs sonarqube`
2. Jenkins console: Build → Console Output
3. GitHub Actions: Actions → Workflow run
4. Browser developer tools: F12 → Console

### Common error patterns:
- "Connection refused" → SonarQube not running
- "Unauthorized" → Token invalid
- "Not found" → Project key wrong
- "Timeout" → Network or memory issue

### Where to find more info:
- SonarQube Docs: https://docs.sonarqube.org
- GitHub Actions: https://docs.github.com/en/actions
- Jenkins Help: http://localhost:8080/help
- Project docs: SONARQUBE_IMPLEMENTATION_STEPS.md

---

## Still having issues?

1. Check all prerequisites are running:
   - Docker running ✅
   - SonarQube container running ✅
   - Jenkins running ✅
   - Network connectivity ✅

2. Verify configuration:
   - Token exists and valid ✅
   - Project keys match ✅
   - Credentials added ✅
   - Secrets configured ✅

3. Try basic test:
   - Manual analysis: `mvn sonar:sonar` ✅
   - Check dashboard: http://localhost:9000 ✅

4. Review documentation:
   - SONARQUBE_SETUP_GUIDE.md
   - SONARQUBE_IMPLEMENTATION_STEPS.md
   - SONARQUBE_CONFIGURATION.md

5. Restart and try again:
   - `docker compose restart sonarqube`
   - Clear browser cache
   - Restart IDE
   - Retry build



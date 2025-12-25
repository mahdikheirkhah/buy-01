# IntelliJ IDEA - SonarQube Integration Guide

## Quick Setup (5 minutes)

### Step 1: Install SonarLint Plugin

1. Open IntelliJ IDEA
2. Go to **File** → **Settings** (or **Preferences** on macOS)
3. Search for **"Plugins"**
4. Click **"Marketplace"**
5. Search for **"SonarLint"**
6. Click **"Install"**
7. Restart IntelliJ

### Step 2: Configure SonarQube Server Connection

1. Go to **File** → **Settings** → **Tools** → **SonarLint**
2. Click **"Project Binding"** tab
3. Click **"Configure Server Connections"** (or go to Servers tab first)
4. Click **"+"** to add new server
5. Fill in:
   - **Name**: `SonarQube Local`
   - **Server URL**: `http://localhost:9000`
   - **Authentication Type**: `Token`
   - **Token**: (paste your SonarQube token from http://localhost:9000/account/security)
6. Click **"Test Connection"** (should show "Connection successful")
7. Click **"OK"**

### Step 3: Bind Project to SonarQube

1. Still in **Settings** → **Tools** → **SonarLint**
2. Click **"Project Binding"** tab
3. Enable: **"Bind project to SonarQube"**
4. Select:
   - **Server connection**: `SonarQube Local`
   - **Project key**: `buy-01-backend`
5. Click **"OK"**

### Step 4: Enable Real-Time Analysis

1. Go to **Settings** → **Tools** → **SonarLint** → **General**
2. Check: **"Enable real-time code analysis"**
3. Check: **"Show analysis on-the-fly"**
4. Click **"OK"**

---

## Verification

### Test Real-Time Feedback

1. Open any Java file in the backend
2. Add an unused variable:
   ```java
   public void testMethod() {
       int unusedVar = 5;  // Should show warning
       System.out.println("Test");
   }
   ```
3. You should see:
   - 🔴 Red squiggly line under `unusedVar`
   - Hover to see issue details
   - Issue type shown (e.g., "Local variable is not used")

### Check SonarLint Tool Window

1. Go to **View** → **Tool Windows** → **SonarLint**
2. You should see:
   - Current file analysis results
   - Issues list
   - Issue details
   - Links to SonarQube

---

## IDE Integration Features

### Real-Time Analysis ✅
- Issues highlighted as you type
- Hover for issue details
- Quick-fix suggestions

### SonarLint Panel
- View all issues in current file
- Filter by severity
- Navigate to issues
- Open in SonarQube

### Issue Information
- Rule name and description
- Severity level
- Issue category
- Link to full rule explanation

### Quick Actions
- Suppress issue
- Open in SonarQube
- View similar issues
- Configure rule

---

## Using Run Configurations

### Run SonarQube Analysis

The project includes 3 pre-configured run configurations:

1. **"SonarQube Analysis - Backend"**
   - Runs: `mvn sonar:sonar`
   - Analyzes: backend microservices
   - Result: Published to SonarQube

2. **"Backend Unit Tests"**
   - Runs: `mvn test -B -Dtest=*UnitTest`
   - Executes: 43 unit tests
   - Time: ~5 minutes

3. **"Backend Integration Tests"**
   - Runs: `mvn test -B -Dtest=*IntegrationTest`
   - Executes: 32 integration tests
   - Time: ~10 minutes
   - Requires: Docker running

### Run a Configuration

1. Click on run configuration dropdown (top-right)
2. Select desired configuration
3. Click green **"Run"** button
4. Monitor in **Run** tool window

---

## Code Inspection Profile

The project includes a **"SonarQube Rules"** inspection profile with 50+ rules:

### Apply Profile

1. Go to **Settings** → **Editor** → **Inspections**
2. Click gear icon (top-right)
3. Select **"Import Profile"**
4. Choose `.idea/inspectionProfiles/SonarQube_Rules.xml`
5. Inspect code: **Code** → **Run Inspection by Name**

### Inspection Severity Levels

- 🔴 **ERROR**: Must fix before commit
- 🟠 **WARNING**: Should fix before merge
- 🟡 **WEAK WARNING**: Consider fixing

---

## Best Practices

### Before Committing Code

1. ✅ Check SonarLint panel for issues
2. ✅ Fix all errors and warnings
3. ✅ Run unit tests
4. ✅ Run SonarQube analysis via run configuration

### Code Review Process

1. ✅ Write code
2. ✅ Fix SonarLint issues (real-time)
3. ✅ Run tests (unit + integration)
4. ✅ Run SonarQube analysis
5. ✅ Create PR
6. ✅ GitHub Actions runs analysis
7. ✅ Quality gate must pass
8. ✅ Get code review approval
9. ✅ Merge

### Quality Standards

Target metrics:
- ✅ Code Coverage: ≥ 30%
- ✅ Code Smells: < 20
- ✅ Duplicated Code: < 3%
- ✅ Bugs: 0
- ✅ Vulnerabilities: 0

---

## Troubleshooting

### SonarLint not showing issues

**Solution 1**: Restart IntelliJ
- File → Invalidate Caches → Invalidate and Restart

**Solution 2**: Check server connection
- Settings → Tools → SonarLint
- Click "Test Connection"
- Should show "Connection successful"

**Solution 3**: Verify project binding
- Settings → Tools → SonarLint → Project Binding
- Check "Bind project to SonarQube" is enabled
- Verify project key is correct

### Token expired

**Fix**:
1. Generate new token in SonarQube
   - http://localhost:9000/account/security
2. Update in IntelliJ
   - Settings → Tools → SonarLint → Servers
   - Edit connection → update token

### Can't connect to SonarQube

**Check**:
1. Is SonarQube running?
   - `docker compose ps | grep sonarqube`
2. Is it accessible?
   - `curl http://localhost:9000`
3. Is firewall blocking?
   - Check port 9000

---

## Keyboard Shortcuts

| Action | Shortcut |
|--------|----------|
| Run SonarQube Analysis | Alt + Shift + Q (Windows/Linux) |
| Show SonarLint Tool | Alt + 9 |
| Run Inspections | Ctrl + Alt + I (Windows/Linux) |
| Go to Next Issue | F2 |
| Go to Previous Issue | Shift + F2 |

---

## Tips & Tricks

### 1. View Full Rule Details
- Click issue in SonarLint panel
- Click "Open in SonarQube"
- Read full rule explanation

### 2. Suppress Issues (when necessary)
- Right-click issue
- Select "Suppress issue"
- Choose scope (file/method)

### 3. Filter Issues by Severity
- SonarLint panel
- Click filter icon
- Show only errors/warnings

### 4. Navigate to Problematic Code
- Double-click issue in panel
- Opens file and highlights code

### 5. Track Progress
- Run configuration periodically
- Dashboard: http://localhost:9000
- Watch metrics improve over time

---

## Integration with Other Tools

### Git Integration
- Pre-commit hook can run SonarLint
- Prevent committing with issues

### Maven Integration
- Included in backend pom.xml
- Run: `mvn sonar:sonar`
- Via run configuration

### GitHub Integration
- Workflow analyzes on every PR
- Quality gate enforces standards
- PR comments with issues

---

## File Locations

```
.idea/
├── sonarlint.xml                    ← SonarLint config
├── runConfigurations.xml            ← Run configurations
└── inspectionProfiles/
    └── SonarQube_Rules.xml          ← Inspection profile
```

---

## Quick Reference

### Essential URLs
- SonarQube Dashboard: http://localhost:9000
- Account: http://localhost:9000/account
- Security Tokens: http://localhost:9000/account/security

### Essential Commands
```bash
# Generate new token
# Go to http://localhost:9000/account/security

# Check SonarQube status
curl http://localhost:9000/api/system/status

# Run analysis from terminal
mvn sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.login=TOKEN
```

### File Structure
- Backend code: `backend/*/src/main/java`
- Tests: `backend/*/src/test/java`
- Configuration: `.idea/`

---

## Support Resources

- **SonarLint Plugin**: https://www.sonarlint.org/intellij
- **SonarQube Docs**: https://docs.sonarqube.org
- **IntelliJ Help**: https://www.jetbrains.com/help/idea
- **Project Docs**: See SONARQUBE_*.md files

---

## Next Steps

1. ✅ Install SonarLint plugin
2. ✅ Configure server connection
3. ✅ Bind project to SonarQube
4. ✅ Enable real-time analysis
5. ✅ Test with sample code
6. ✅ Run configurations
7. ✅ Start fixing issues!

---

**Date**: December 25, 2025
**Status**: ✅ Ready for IntelliJ Integration
**Version**: 1.0


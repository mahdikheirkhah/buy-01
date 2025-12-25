# IntelliJ IDEA - SonarQube Quick Reference Card

## 🚀 5-Minute Setup

```
1. Install SonarLint
   File → Settings → Plugins → Marketplace → SonarLint → Install

2. Configure Server
   Settings → Tools → SonarLint → Servers
   Add: http://localhost:9000 + Token

3. Bind Project
   Settings → Tools → SonarLint → Project Binding
   Project Key: buy-01-backend

4. Enable Real-Time
   Settings → Tools → SonarLint → General
   ✅ Enable real-time code analysis

5. Test It!
   Open Java file → Add unused variable → See red squiggly!
```

---

## 🎯 Essential Tasks

### View Code Issues
- **SonarLint Panel**: View → Tool Windows → SonarLint
- **Inspection**: Code → Run Inspection by Name
- **Current File**: Hover over issues

### Run Analysis
- **SonarQube Analysis**: Run menu → Select config → Run
- **Unit Tests**: Run menu → Backend Unit Tests
- **Integration Tests**: Run menu → Backend Integration Tests

### Go to Issue
- **Next**: F2
- **Previous**: Shift + F2
- **List**: Click in SonarLint panel

### Configure
- **SonarLint**: Settings → Tools → SonarLint
- **Inspections**: Settings → Editor → Inspections
- **Profiles**: Settings → Editor → Inspections → Manage

---

## ⌨️ Keyboard Shortcuts

| Action | Windows/Linux | macOS |
|--------|---------------|-------|
| Run SonarQube | Alt + Shift + Q | Cmd + Shift + Q |
| SonarLint Panel | Alt + 9 | Cmd + 9 |
| Inspect Code | Ctrl + Alt + I | Cmd + Opt + I |
| Next Issue | F2 | F2 |
| Prev Issue | Shift + F2 | Shift + F2 |
| Settings | Ctrl + Alt + S | Cmd + , |

---

## 🔧 Included Run Configurations

### 1. SonarQube Analysis - Backend
```bash
mvn sonar:sonar
  -Dsonar.projectKey=buy-01-backend
  -Dsonar.host.url=http://localhost:9000
  -Dsonar.login=admin
```
**Use**: Analyze all backend code and upload to SonarQube

### 2. Backend Unit Tests
```bash
mvn test -B -Dtest=*UnitTest
```
**Use**: Run 43 unit tests (5 min)

### 3. Backend Integration Tests
```bash
mvn test -B -Dtest=*IntegrationTest
```
**Use**: Run 32 integration tests (10 min, requires Docker)

---

## 📊 Included Inspection Profile

**50+ Rules** aligned with SonarQube quality gates:

- Code Smells (20+)
- Bugs (15+)
- Vulnerabilities (5+)
- Security issues (5+)
- Best practices (10+)

**Severity Levels**:
- 🔴 ERROR - Must fix
- 🟠 WARNING - Should fix
- 🟡 WEAK WARNING - Consider fixing

---

## 📋 Pre-configured Settings

### .idea/sonarlint.xml
✅ Server: http://localhost:9000
✅ Project: buy-01-backend
✅ Binding: Enabled
✅ Token: Required (add manually)

### .idea/runConfigurations.xml
✅ 3 Maven configurations
✅ Pre-configured commands
✅ Ready to use

### .idea/inspectionProfiles/SonarQube_Rules.xml
✅ 50+ rules
✅ Applied to project
✅ Severity levels defined

---

## ✅ Verification Checklist

- [ ] SonarLint installed
- [ ] Server connection: http://localhost:9000
- [ ] Project binding: buy-01-backend
- [ ] Real-time analysis: Enabled
- [ ] Open Java file: Issues highlighted
- [ ] Run configurations: Visible in dropdown
- [ ] Inspection profile: "SonarQube Rules" applied
- [ ] SonarLint panel: View → Tool Windows

---

## 🔍 Viewing Issues

### In Code
```java
public void test() {
    int unused = 5;  // 🔴 Red squiggly - unused variable
    System.out.println("test");
}
```

### Hover for Details
- Issue name
- Rule description
- Severity level
- Link to rule

### In SonarLint Panel
- List of all issues
- Click to navigate
- Filter by severity
- Open in SonarQube

---

## 🐛 Troubleshooting Quick Fix

| Problem | Solution |
|---------|----------|
| No issues shown | Restart → File → Invalidate Caches |
| Can't connect | Check http://localhost:9000 |
| Token expired | Generate new in SonarQube |
| Project not found | Verify project key |
| Run config missing | Import .idea/runConfigurations.xml |

---

## 📈 Workflow Example

```
1. Open Java file
   ↓
2. See red squiggly lines (real-time)
   ↓
3. Click issue → Read rule
   ↓
4. Fix code
   ↓
5. Issue disappears (auto re-analyze)
   ↓
6. Run Unit Tests (Alt + Shift + Q)
   ↓
7. All green? Commit!
```

---

## 🎯 Best Practices

✅ Fix issues as you code (real-time feedback)
✅ Run tests before committing
✅ Run SonarQube analysis before PR
✅ Check SonarLint panel regularly
✅ Read rule descriptions for learning
✅ Use keyboard shortcuts for speed

❌ Ignore warnings
❌ Suppress issues unnecessarily
❌ Skip testing
❌ Commit with red issues

---

## 📚 Documentation

- **Setup**: INTELLIJ_SONARQUBE_SETUP.md (detailed)
- **Files Added**: INTELLIJ_FILES_ADDED.md (summary)
- **General**: SONARQUBE_CONFIGURATION.md (all IDEs)
- **Troubleshooting**: SONARQUBE_TROUBLESHOOTING.md

---

## 🚀 First Run

1. Start SonarQube
   ```bash
   docker compose up -d sonarqube
   ```

2. Open Project in IntelliJ

3. Install SonarLint
   - (Auto-suggested or manual install)

4. Configure (5 min)
   - Follow 5-Minute Setup above

5. Test
   - Open Java file
   - Add unused variable
   - See red line!

6. Start Analyzing
   - Real-time feedback enabled
   - Use run configurations
   - Enjoy code quality!

---

**Status**: ✅ Ready to Use
**Setup Time**: 5 minutes
**Difficulty**: Easy

Begin with: INTELLIJ_SONARQUBE_SETUP.md



# SonarQube IDE Setup Guide

Complete setup guide for connecting VS Code and IntelliJ IDEA to local SonarQube instance.

## Prerequisites

- ✅ SonarQube running at `http://localhost:9000`
- ✅ SonarQube Token: `sqa_98d53844dcf675266c1b023f1c2cf4dce7f01af4`
- ✅ Java 17 installed
- ✅ Node.js 20+ installed

---

## VS Code Setup

### 1. Install Extension

1. Open **Extensions** (Cmd + Shift + X)
2. Search: **`SonarQube for Visual Studio Code`** by SonarSource
3. Click **Install**

### 2. Load Environment Variables

Before opening VS Code, load the environment variables:

```bash
source /Users/mohammad.kheirkhah/Desktop/buy-01/.env
```

Or add to `~/.zshrc` for persistent setup:

```bash
echo 'export SONARQUBE_TOKEN=sqa_98d53844dcf675266c1b023f1c2cf4dce7f01af4' >> ~/.zshrc
source ~/.zshrc
```

### 3. Verify Connection

1. **Cmd + Shift + P** → `SonarQube for Visual Studio Code: Show SonarQube Output`
2. Should see: ✅ `Connected to SonarQube at http://localhost:9000`
3. Open any Java file - issues should appear highlighted

### 4. Test Each Module

Open files from each backend module:
- `backend/common/src/main/java/...`
- `backend/api-gateway/src/main/java/...`
- `backend/user-service/src/main/java/...`
- `backend/product-service/src/main/java/...`
- `backend/media-service/src/main/java/...`
- `backend/discovery-service/src/main/java/...`
- `frontend/src/app/...`

Each should show issues from the respective SonarQube project.

---

## IntelliJ IDEA Setup

### 1. Install Plugin

1. **IntelliJ IDEA → Preferences** (or **Settings** on Linux/Windows)
2. **Plugins** section
3. Search: `SonarLint`
4. Install **SonarLint** by SonarSource
5. Restart IDE

### 2. Add SonarQube Connection

1. **Tools → SonarLint → SonarLint Settings**
2. Click **+** to add new server
3. **Name**: `SonarQube Local`
4. **URL**: `http://localhost:9000`
5. **Token**: `sqa_98d53844dcf675266c1b023f1c2cf4dce7f01af4`
6. Click **Test Connection** → ✅ Should succeed
7. Click **Save**

### 3. Bind Project

1. **Tools → SonarLint → SonarLint Settings**
2. Select the project binding:
   - **Server**: `SonarQube Local`
   - **Project**: `buy-01-backend`
3. Click **OK**

### 4. Analyze Files

1. **Tools → SonarLint → Analyze All Files in Project**
2. Or: Right-click file → **SonarLint → Analyze This File**
3. Issues appear in **SonarLint tool window**

### 5. Verify Issues Appear

Open:
- `backend/api-gateway/src/main/java/com/backend/api_gateway/config/GatewayConfig.java`

Should show the same bugs as in SonarQube dashboard.

---

## Troubleshooting

### VS Code: No Issues Showing

1. **Check token loaded**: `echo $SONARQUBE_TOKEN`
2. **Reload VS Code**: Cmd + Q, then reopen
3. **View output**: Cmd + Shift + P → `SonarQube: Show Output`
4. Look for: `Connected to SonarQube` and `Analyzing with project key`

### IntelliJ: Connection Failed

1. **Verify SonarQube running**: `curl http://localhost:9000/api/system/status`
2. **Check token in settings** is exact
3. **Test connection** in plugin settings
4. Restart IntelliJ

### Issues Not Matching Dashboard

- Ensure you're looking at the **correct project key** (e.g., `buy-01-backend:api-gateway`)
- Wait 30 seconds for analysis to complete
- Both tools may analyze slightly differently (VS Code may be stricter)

---

## Configuration Files

- **VS Code**: `.vscode/settings.json`
  - Server: `http://localhost:9000`
  - Token: Uses `${SONARQUBE_TOKEN}` environment variable
  - Projects: All 7 modules configured

- **IntelliJ IDEA**: `.idea/sonarlint.xml`
  - Server: `http://localhost:9000`
  - Token: Stored in IDE (not in git)
  - Bound to: `buy-01-backend` project

- **Environment**: `.env`
  - Contains `SONARQUBE_TOKEN` (not committed)
  - Must be sourced before opening VS Code

---

## Running Analysis Manually

### VS Code
```bash
source ~/.env
code /Users/mohammad.kheirkhah/Desktop/buy-01
```

### IntelliJ IDEA
```bash
open -a "IntelliJ IDEA" /Users/mohammad.kheirkhah/Desktop/buy-01
```

---

## Next Steps

1. ✅ Test both IDEs can connect
2. ✅ Verify issues appear for all modules
3. ⏳ Configure GitHub webhooks (optional)
4. ⏳ Set quality gates in SonarQube (optional)

---

For questions, check SonarQube documentation: https://docs.sonarqube.org/

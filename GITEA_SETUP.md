# Gitea Integration with Jenkins

This guide explains how to configure Jenkins to work with Gitea, especially when Gitea is restricted to campus WiFi.

---

## 🔒 Understanding the Limitation

**Gitea Restriction**: Only accessible from campus WiFi
**Problem**: Gitea can't send webhooks to external URLs (like ngrok)
**Solution**: Use **SCM Polling** instead of webhooks

---

## 📝 Step 1: Create Gitea Personal Access Token (PAT)

### 1.1 Generate Token in Gitea

1. **Log in to Gitea**: https://your-gitea-server.com
2. **Go to Settings**: Click your profile → Settings
3. **Access Tokens**: Applications → Manage Access Tokens
4. **Generate New Token**:
   - **Token Name**: `jenkins-buy-01`
   - **Select Scopes**:
     - ✅ `repo` (Full repository access)
     - ✅ `read:org` (Read organization data)
     - ✅ `write:repository` (Push to repositories)
   - Click **Generate Token**
5. **Copy the token** immediately (you won't see it again!)

Example token format: `607c29ff043ff1402673853aa16eb8f87bf51501`

---

## 🔧 Step 2: Configure Jenkins Credentials

### 2.1 Add Gitea Credentials

1. **Open Jenkins**: http://localhost:8080
2. **Manage Jenkins** → **Manage Credentials**
3. **System** → **Global credentials** → **Add Credentials**

**Configure:**

```
Kind: Username with password
Username: [your-gitea-username]
Password: [paste your Gitea PAT token]
ID: gitea-credentials
Description: Gitea Access Token for Buy-01
```

Click **Create**

### 2.2 Add Docker Hub Credentials (if not done)

Same process as above:

```
Kind: Username with password
Username: [dockerhub-username]
Password: [dockerhub-password or token]
ID: dockerhub-credentials
Description: Docker Hub for pushing images
```

---

## 🚀 Step 3: Create Jenkins Pipeline Job

### 3.1 Create New Pipeline

1. **Jenkins Dashboard** → **New Item**
2. **Item name**: `Buy-01-Pipeline`
3. **Type**: **Pipeline**
4. Click **OK**

### 3.2 Configure Source Code Management

**In the pipeline configuration:**

1. **General** → Check **"Discard old builds"**

   - Days to keep builds: `7`
   - Max # of builds to keep: `10`

2. **Build Triggers** → Check **"Poll SCM"**

   ```
   Schedule: H/5 * * * *
   ```

   _(This polls Gitea every 5 minutes for changes)_

3. **Pipeline** section:

   - **Definition**: Pipeline script from SCM
   - **SCM**: Git
   - **Repository URL**:
     ```
     https://your-gitea-server.com/your-username/buy-01.git
     ```
   - **Credentials**: Select `gitea-credentials`
   - **Branch Specifier**: `*/main` (or `*/master` if that's your default)
   - **Script Path**: `Jenkinsfile`

4. Click **Save**

---

## 🔄 Step 4: Understanding Polling vs Webhooks

### Webhook Approach (GitHub - works with ngrok)

```
Code Push → GitHub sends webhook → Jenkins builds immediately
```

**Pros**: Instant builds
**Cons**: Requires external access

### Polling Approach (Gitea on campus WiFi)

```
Jenkins checks Gitea every 5 minutes → If changes detected → Build starts
```

**Pros**: Works behind firewall
**Cons**: 5-minute delay (configurable)

### Polling Schedule Options

```groovy
H/5 * * * *     // Every 5 minutes
H/10 * * * *    // Every 10 minutes
H/15 * * * *    // Every 15 minutes
H * * * *       // Every hour
```

---

## 🌐 Step 5: Access Jenkins When Off-Campus

### Option 1: Using ngrok (Jenkins UI only)

When **on campus WiFi**:

```bash
./setup.sh --jenkins --ngrok
```

This gives you external access to Jenkins UI:

```
https://xxx.ngrok-free.dev
```

**Note**: Gitea webhooks still won't work, but you can:

- Access Jenkins dashboard from anywhere
- Manually trigger builds
- Monitor build status remotely

### Option 2: VPN to Campus Network

If your campus provides VPN:

1. Connect to campus VPN
2. Access Gitea normally
3. Access Jenkins at http://localhost:8080

---

## 📊 Step 6: Test the Integration

### 6.1 Manual Build Test

1. **Jenkins Dashboard** → **Buy-01-Pipeline**
2. Click **"Build Now"**
3. Watch the build progress
4. Check console output for any errors

### 6.2 Automatic Build Test (Polling)

1. Make a change to your code:

   ```bash
   echo "# Test polling" >> README.md
   git add README.md
   git commit -m "test: verify Jenkins polling"
   git push
   ```

2. **Wait up to 5 minutes** (based on your poll schedule)
3. Jenkins should automatically detect the change and start a build
4. Check **"Git Polling Log"** in Jenkins to see poll activity

---

## 🔑 Step 7: Update Jenkinsfile for Gitea

Your Jenkinsfile should work as-is, but verify these sections:

### 7.1 Check Git URL in Jenkinsfile

If you have hardcoded Git URLs, update them:

```groovy
// Change from:
git url: 'https://github.com/mahdikheirkhah/buy-01.git'

// To:
git url: 'https://your-gitea-server.com/your-username/buy-01.git',
    credentialsId: 'gitea-credentials'
```

### 7.2 Verify Jenkinsfile Uses Credentials

The Jenkinsfile should use credentials for Git operations:

```groovy
checkout([
    $class: 'GitSCM',
    branches: [[name: '*/main']],
    userRemoteConfigs: [[
        url: 'https://your-gitea-server.com/your-username/buy-01.git',
        credentialsId: 'gitea-credentials'
    ]]
])
```

---

## 🐛 Troubleshooting

### Issue 1: Jenkins Can't Access Gitea

**Error**: `Failed to connect to repository`

**Solution**:

1. Verify you're on campus WiFi
2. Test Gitea access from terminal:
   ```bash
   git clone https://your-gitea-server.com/your-username/buy-01.git
   ```
3. Ensure credentials are correct in Jenkins

### Issue 2: Polling Not Working

**Check Poll Log**:

1. Jenkins job → **Git Polling Log**
2. Look for error messages

**Common fixes**:

- Verify repository URL is correct
- Check credentials are properly set
- Ensure Jenkins can reach Gitea (on campus network)

### Issue 3: SSL Certificate Errors

If Gitea uses self-signed certificates:

**Option A**: Add to Jenkins Java trust store (recommended)

**Option B**: Disable SSL verification (not recommended):

```groovy
git url: 'https://your-gitea-server.com/repo.git',
    credentialsId: 'gitea-credentials',
    branch: 'main',
    changelog: true,
    poll: true,
    gitTool: 'Default',
    extensions: [
        [$class: 'CloneOption', noTags: false, shallow: false, depth: 0]
    ]
```

---

## ⚙️ Alternative: Gitea Actions (Optional)

Gitea also supports **Gitea Actions** (similar to GitHub Actions):

1. Create `.gitea/workflows/build.yml` in your repo
2. Configure action to trigger Jenkins via API
3. This provides immediate builds (no 5-minute delay)

**Example Gitea Action**:

```yaml
name: Trigger Jenkins
on: [push]
jobs:
  trigger:
    runs-on: ubuntu-latest
    steps:
      - name: Trigger Jenkins Job
        run: |
          curl -X POST \
            http://jenkins-url/job/Buy-01-Pipeline/build \
            --user jenkins:api-token
```

---

## 📋 Quick Reference

### Gitea vs GitHub Comparison

| Feature           | GitHub   | Gitea (Campus WiFi)    |
| ----------------- | -------- | ---------------------- |
| PAT Token         | ✅ Yes   | ✅ Yes                 |
| Webhooks          | ✅ Works | ❌ Blocked by firewall |
| Polling           | ✅ Yes   | ✅ Yes (recommended)   |
| API Access        | ✅ Yes   | ✅ Yes (on campus)     |
| ngrok Integration | ✅ Full  | ⚠️ UI only             |

### Common Commands

```bash
# Test Gitea access
curl https://your-gitea-server.com

# Clone repo with PAT token
git clone https://username:PAT_TOKEN@gitea-server.com/repo.git

# Check Jenkins poll log
# Jenkins → Job → Git Polling Log

# Manual Jenkins build trigger
# Jenkins → Job → Build Now
```

---

## 🎯 Recommended Setup for Campus Gitea

**Best configuration**:

1. ✅ Use SCM Polling (H/5 \* \* \* \*)
2. ✅ Set up Gitea PAT token
3. ✅ Work on campus WiFi for development
4. ✅ Use ngrok for remote Jenkins UI access
5. ✅ Accept 5-minute build delay (better than no automation!)

**When off-campus**:

- Connect via VPN if available
- Or wait until back on campus to see builds
- Use ngrok to check Jenkins status remotely

---

## ✅ Next Steps

1. Generate Gitea PAT token
2. Add credentials to Jenkins
3. Configure pipeline with polling
4. Test manual build
5. Test automatic build (make a commit and wait 5 minutes)
6. Monitor "Git Polling Log" to verify polling works

---

**Questions?** Check the main [JENKINS_SETUP.md](JENKINS_SETUP.md) for general Jenkins configuration.

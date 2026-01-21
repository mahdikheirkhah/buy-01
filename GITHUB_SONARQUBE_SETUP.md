# SonarQube GitHub Actions Setup Guide

## Problem

Your GitHub Actions workflow can't connect to `localhost:9000` because:

- GitHub Actions runs on **remote Ubuntu servers**
- Your local SonarQube at `localhost:9000` is **not accessible** from there
- The `SONAR_HOST_URL` secret points to localhost, which only exists on your machine

## Solutions

### Option 1: Use SonarCloud (Recommended ✅)

**Pros:**

- ✅ Free for public repositories
- ✅ No infrastructure to manage
- ✅ Seamless GitHub integration
- ✅ Automatic PR decorations
- ✅ Always available

**Cons:**

- ⚠️ Code is uploaded to cloud (privacy concern for private projects)
- ⚠️ Limited for private repos on free tier

**Setup Steps:**

1. **Create SonarCloud Account:**
   - Go to https://sonarcloud.io/
   - Sign in with GitHub account
   - Import your repository

2. **Get Organization Key:**
   - Click your profile → My Organizations
   - Copy your organization key (e.g., `your-org-key`)

3. **Generate Token:**
   - My Account → Security → Generate Token
   - Copy the token

4. **Add GitHub Secrets:**

   ```bash
   # Go to: Settings → Secrets and variables → Actions → New repository secret

   Name: SONAR_TOKEN
   Value: [paste your SonarCloud token]
   ```

5. **Update Workflow (already done):**
   - The `.github/workflows/sonarqube.yml` already has SonarCloud configured
   - Just replace `YOUR_SONAR_ORG` with your organization key:

   ```yaml
   -Dsonar.organization=your-actual-org-key
   ```

6. **Test:**
   - Push a commit
   - Check Actions tab for results
   - View analysis at https://sonarcloud.io/

---

### Option 2: Run SonarQube as GitHub Actions Service

**Pros:**

- ✅ Code stays in CI environment (more private)
- ✅ Free and unlimited
- ✅ Full control

**Cons:**

- ⚠️ Slower (starts SonarQube on each run)
- ⚠️ Takes ~2-3 minutes for SonarQube to start
- ⚠️ Analysis data not persisted between runs

**Setup Steps:**

1. **Uncomment SonarQube Service:**

In `.github/workflows/sonarqube.yml`, uncomment these lines:

```yaml
services:
  # ... existing services ...

  sonarqube:
    image: sonarqube:lts-community
    ports:
      - 9000:9000
    env:
      SONAR_ES_BOOTSTRAP_CHECKS_DISABLE: "true"
    options: >-
      --health-cmd="wget -qO- http://localhost:9000/api/system/status | grep -q UP || exit 1"
      --health-interval=30s
      --health-timeout=10s
      --health-retries=5
```

2. **Comment Out SonarCloud Step:**

Replace the SonarCloud action with Maven analysis:

```yaml
- name: Wait for SonarQube
  run: |
    echo "Waiting for SonarQube to be ready..."
    timeout 180 bash -c 'until curl -s http://localhost:9000/api/system/status | grep -q "UP"; do sleep 5; done'
    echo "✅ SonarQube is ready"

- name: SonarQube Analysis (Local)
  run: |
    cd backend
    mvn sonar:sonar \
      -Dsonar.projectKey=buy-01-backend \
      -Dsonar.host.url=http://localhost:9000 \
      -Dsonar.login=admin \
      -Dsonar.password=admin \
      -Dsonar.qualitygate.wait=true
```

3. **No Secrets Needed:**
   - Uses default admin/admin credentials
   - Data is ephemeral (lost after workflow completes)

---

### Option 3: Self-Hosted SonarQube on Public Server

**Pros:**

- ✅ Full control and persistence
- ✅ Private and unlimited
- ✅ Data persists between runs

**Cons:**

- ⚠️ Requires public server/VPS
- ⚠️ Costs money for hosting
- ⚠️ Security configuration needed

**Setup Steps:**

1. **Deploy SonarQube to Cloud Provider:**
   - AWS EC2, DigitalOcean, Azure VM, etc.
   - Use Docker: `docker run -d -p 9000:9000 sonarqube:lts-community`
   - Or install native: https://docs.sonarqube.org/latest/setup/install-server/

2. **Configure Firewall:**
   - Open port 9000
   - Restrict access to GitHub Actions IPs (optional)

3. **Get Server URL:**
   - Example: `http://your-server-ip:9000`
   - Or use domain: `https://sonar.yourdomain.com`

4. **Generate Token in SonarQube:**
   - Login → My Account → Security → Generate Token

5. **Add GitHub Secrets:**

   ```bash
   Name: SONAR_TOKEN
   Value: [your token]

   Name: SONAR_HOST_URL
   Value: http://your-server-ip:9000
   ```

6. **Update Workflow:**
   ```yaml
   - name: SonarQube Analysis
     env:
       SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
       SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
     run: |
       cd backend
       mvn sonar:sonar \
         -Dsonar.projectKey=buy-01-backend \
         -Dsonar.host.url=$SONAR_HOST_URL \
         -Dsonar.login=$SONAR_TOKEN \
         -Dsonar.qualitygate.wait=true
   ```

---

## Comparison

| Feature         | SonarCloud    | GitHub Service        | Self-Hosted |
| --------------- | ------------- | --------------------- | ----------- |
| **Cost**        | Free (public) | Free                  | $5-50/mo    |
| **Setup**       | Easy          | Medium                | Hard        |
| **Speed**       | Fast          | Slow (~3 min startup) | Fast        |
| **Privacy**     | Cloud         | Ephemeral CI          | Private     |
| **Persistence** | ✅ Yes        | ❌ No                 | ✅ Yes      |
| **Maintenance** | ☁️ Managed    | ☁️ Managed            | 🔧 Self     |

---

## Recommendation

**For this project:** Use **SonarCloud** (Option 1)

**Why:**

- ✅ Easiest to set up
- ✅ Best GitHub integration
- ✅ Free for public repos
- ✅ No infrastructure management
- ✅ PR decorations and comments
- ✅ Historical data and trends

**When to use others:**

- **Option 2** (Service): Testing/learning, no persistence needed
- **Option 3** (Self-hosted): Private project with budget, need full control

---

## Quick Start (SonarCloud)

```bash
# 1. Go to SonarCloud
open https://sonarcloud.io/

# 2. Sign in with GitHub, import repo

# 3. Get your org key from URL
# Example: https://sonarcloud.io/organizations/YOUR-ORG-KEY

# 4. Generate token: My Account → Security → Generate

# 5. Add GitHub secret
gh secret set SONAR_TOKEN --body "YOUR_TOKEN_HERE"

# 6. Update workflow - replace YOUR_SONAR_ORG with your key

# 7. Push and watch!
git add .
git commit -m "feat: add SonarCloud integration"
git push
```

---

## Troubleshooting

### "Can not be reached" error

- ❌ Using localhost in secrets
- ✅ Use SonarCloud or self-hosted public URL

### "Unauthorized" error

- ❌ Wrong token
- ✅ Regenerate token and update secret

### "Quality gate timeout"

- ❌ Too many issues found
- ✅ Increase timeout or fix issues first

### SonarQube service won't start

- ❌ Not enough memory (needs ~2GB)
- ✅ Use SonarCloud instead

---

**Last Updated:** January 21, 2026
**Recommended Option:** SonarCloud (Option 1)

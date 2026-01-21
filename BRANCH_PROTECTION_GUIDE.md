# Branch Protection Setup Guide

## Overview

This guide explains how to protect your `main` branch by requiring:

1. ✅ **SonarQube quality gate to pass**
2. ✅ **2 approvals from reviewers**
3. ✅ **All status checks to pass**

---

## Step 1: Configure GitHub Branch Protection Rules

### 1.1 Navigate to Repository Settings

1. Go to your GitHub repository: `https://github.com/YOUR_USERNAME/buy-01`
2. Click on **Settings** (top-right menu)
3. In the left sidebar, click **Branches**
4. Under "Branch protection rules", click **Add rule** or **Add branch protection rule**

### 1.2 Configure Protection Rules

**Branch name pattern:**

```
main
```

**Enable the following options:**

#### ✅ Require a pull request before merging

- Check this box
- **Require approvals:** Set to `2`
- Check **Dismiss stale pull request approvals when new commits are pushed**
- Check **Require review from Code Owners** (optional, if you have CODEOWNERS file)

#### ✅ Require status checks to pass before merging

- Check this box
- Check **Require branches to be up to date before merging**
- In the search box, search and add:
  - `Build & Test` (from your GitHub Actions workflow)
  - `SonarQube Quality Gate` (you'll add this next)

#### ✅ Require conversation resolution before merging

- Check this box (ensures all PR comments are resolved)

#### ✅ Require linear history (optional)

- Check this to prevent merge commits

#### ✅ Include administrators

- Check this to apply rules to repository admins too

**Then click:** `Create` or `Save changes`

---

## Step 2: Add SonarQube Status Check to GitHub Actions

Update your GitHub Actions workflow to report SonarQube status.

### 2.1 Update `.github/workflows/sonarqube.yml`

Add SonarQube analysis step that reports to GitHub:

```yaml
# Add after "Run Backend Unit Tests" step
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
      -Dsonar.qualitygate.wait=true \
      -Dsonar.qualitygate.timeout=300
    echo "✅ SonarQube analysis completed"

- name: SonarQube Quality Gate Check
  run: |
    echo "Checking quality gate status..."
    # The previous step will fail if quality gate fails
    echo "✅ Quality gate passed"
```

### 2.2 Add GitHub Secrets

1. Go to **Settings** → **Secrets and variables** → **Actions**
2. Click **New repository secret**
3. Add:
   - **Name:** `SONAR_TOKEN`
   - **Value:** Your SonarQube token (generate from SonarQube → My Account → Security → Generate Token)
4. Add another secret:
   - **Name:** `SONAR_HOST_URL`
   - **Value:** `http://YOUR_SONAR_SERVER:9000` (or your SonarQube URL)

---

## Step 3: Configure SonarQube Quality Gate

### 3.1 Access SonarQube

1. Open SonarQube: `http://localhost:9000`
2. Login as admin (default: admin/admin)

### 3.2 Create/Configure Quality Gate

1. Go to **Quality Gates** (top menu)
2. Click **Create** or edit existing gate
3. Set conditions:
   - **Bugs:** 0 on New Code
   - **Vulnerabilities:** 0 on New Code
   - **Code Smells:** A rating on New Code
   - **Coverage:** ≥ 80% on New Code
   - **Duplicated Lines:** ≤ 3% on New Code
   - **Security Hotspots Reviewed:** 100% on New Code

4. Click **Set as Default** if you want this for all projects

### 3.3 Link Projects to Quality Gate

1. Go to **Projects**
2. Select your project (e.g., `buy-01-backend`)
3. Go to **Project Settings** → **Quality Gate**
4. Select your quality gate
5. Save

---

## Step 4: Test the Protection

### 4.1 Create a Test PR

```bash
# Create a feature branch
git checkout -b test-branch-protection

# Make a small change
echo "# Test" >> README.md

# Commit and push
git add README.md
git commit -m "test: branch protection"
git push origin test-branch-protection
```

### 4.2 Open Pull Request

1. Go to GitHub and open a PR from `test-branch-protection` to `main`
2. You should see:
   - ✅ `Build & Test` check running
   - ✅ `SonarQube Quality Gate` check running
   - ⏳ Awaiting 2 reviewers

### 4.3 Verify Protection Works

**Try to merge without approvals:**

- Click **Merge pull request**
- You should see: "Merging is blocked" with message about required approvals

**Check status checks:**

- Wait for checks to complete
- If SonarQube fails, you cannot merge
- If Build & Test fails, you cannot merge

**Request reviews:**

- Click **Reviewers** on the right
- Request reviews from 2 team members
- After 2 approvals and green checks, merge button becomes active

---

## Step 5: Workflow for Team Members

### For PR Authors:

1. **Create feature branch:**

   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make changes and commit:**

   ```bash
   git add .
   git commit -m "feat: your feature description"
   git push origin feature/your-feature-name
   ```

3. **Open PR on GitHub:**
   - Go to repository → Pull requests → New pull request
   - Fill out the PR template
   - Request reviews from 2+ team members

4. **Wait for checks:**
   - ✅ Build & Test must pass
   - ✅ SonarQube Quality Gate must pass
   - ✅ Need 2 approvals

5. **Address feedback:**
   - Respond to review comments
   - Make changes if needed
   - Push updates (this resets stale approvals if configured)

6. **Merge when ready:**
   - All checks green
   - 2 approvals received
   - All conversations resolved
   - Click **Merge pull request**

### For Reviewers:

1. **Review the PR:**
   - Check code quality
   - Verify tests are added/updated
   - Check SonarQube report
   - Test locally if needed

2. **Provide feedback:**
   - Comment on specific lines
   - Request changes if issues found
   - Approve if looks good

3. **Approve:**
   - Click **Review changes**
   - Select **Approve**
   - Submit review

---

## Step 6: Additional Protection (Optional)

### 6.1 Add CODEOWNERS File

Create `.github/CODEOWNERS`:

```
# Global owners
* @team-lead @tech-lead

# Backend services
/backend/ @backend-team @team-lead

# Frontend
/frontend/ @frontend-team @team-lead

# Infrastructure
/docker-compose.yml @devops-team
/Jenkinsfile @devops-team
/.github/ @devops-team
```

### 6.2 Enable More Checks

In branch protection settings:

- ✅ **Require deployments to succeed before merging** (if you have staging)
- ✅ **Lock branch** (prevent all pushes, PRs only)
- ✅ **Do not allow bypassing the above settings** (strict mode)

---

## Troubleshooting

### Issue: Can't see status checks in branch protection

**Solution:**

1. Run the GitHub Actions workflow at least once
2. Wait for it to complete
3. Then the check name appears in branch protection search
4. Refresh the branch protection page

### Issue: SonarQube check always fails

**Solution:**

1. Check SonarQube is running: `docker ps | grep sonarqube`
2. Verify `SONAR_TOKEN` secret is correct
3. Check SonarQube project exists and quality gate is configured
4. Review GitHub Actions logs for specific error

### Issue: Need to bypass protection in emergency

**Solution:**

1. Go to Settings → Branches
2. Edit the branch protection rule
3. Temporarily uncheck **Include administrators**
4. Merge with admin privileges
5. Re-enable protection immediately after

### Issue: Quality gate too strict, can't merge anything

**Solution:**

1. Go to SonarQube → Quality Gates
2. Relax conditions temporarily (e.g., Coverage 60% instead of 80%)
3. Create a plan to improve code quality incrementally
4. Increase thresholds gradually

---

## Summary Checklist

- [ ] Branch protection enabled for `main`
- [ ] Require 2 approvals before merge
- [ ] Require status checks to pass
- [ ] GitHub Actions workflow updated with SonarQube
- [ ] SonarQube secrets added to GitHub
- [ ] Quality gate configured in SonarQube
- [ ] Tested with a PR
- [ ] Team members notified of new workflow
- [ ] CODEOWNERS file created (optional)
- [ ] Documentation updated

---

## Quick Reference Commands

```bash
# Check current branch
git branch

# Create feature branch
git checkout -b feature/my-feature

# Push feature branch
git push origin feature/my-feature

# Update branch with main
git fetch origin
git rebase origin/main

# Delete local branch after merge
git branch -d feature/my-feature

# Delete remote branch after merge
git push origin --delete feature/my-feature
```

---

**Last Updated:** January 21, 2026  
**Version:** 1.0

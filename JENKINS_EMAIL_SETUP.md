# 📧 Jenkins Email Configuration Guide

## 🎯 Quick Fix Summary

**Problem:** Pipeline fails with `sshagent` error and no emails are sent.

**Solution:** 
1. ✅ Jenkinsfile fixed - replaced `sshagent` with `withCredentials`
2. ✅ Added missing `agent` and `parameters` declarations
3. 📧 Need to configure Jenkins Email Extension plugin

---

## 🔧 Jenkins Email Configuration (Required)

### Step 1: Install Email Extension Plugin

1. Go to Jenkins Dashboard → **Manage Jenkins** → **Plugins**
2. Click **Available plugins**
3. Search for **"Email Extension Plugin"**
4. Install it (restart if needed)

### Step 2: Configure Email Settings

1. Go to **Manage Jenkins** → **System**
2. Scroll to **Extended E-mail Notification** section
3. Configure:

```yaml
SMTP Server:        smtp.gmail.com  (for Gmail)
SMTP Port:          587
Use SMTP Authentication: ✓ Checked
Username:           your-email@gmail.com
Password:           your-app-password (see below)
Use SSL:            ✓ Checked
Default Recipients: mohammad.kheirkhah@gritlab.ax
```

### Step 3: Gmail App Password Setup

⚠️ **Don't use your regular Gmail password!**

1. Go to: https://myaccount.google.com/apppasswords
2. Create a new app password for "Jenkins"
3. Copy the 16-character password
4. Use this in Jenkins SMTP settings

### Step 4: Test Email Configuration

1. In Jenkins, scroll to bottom of **Extended E-mail Notification**
2. Click **Test configuration by sending test e-mail**
3. Enter: `mohammad.kheirkhah@gritlab.ax`
4. Click **Test configuration**

---

## 🚀 Alternative: Use Mailtrap for Testing

If Gmail is blocked or you want to test safely:

### Mailtrap Setup (Recommended for Testing)

1. Sign up: https://mailtrap.io (free)
2. Get your SMTP credentials from dashboard
3. Configure Jenkins:

```yaml
SMTP Server:    sandbox.smtp.mailtrap.io
SMTP Port:      2525
Username:       [from mailtrap]
Password:       [from mailtrap]
Use SSL:        ✗ Unchecked
Use TLS:        ✓ Checked
```

---

## ✅ Verify Setup

### Method 1: Trigger a Build

```bash
# Push a commit to trigger webhook
git commit --allow-empty -m "test: trigger Jenkins build"
git push
```

**Expected emails:**
- ✅ Success email if build passes
- ❌ Failure email if build fails

### Method 2: Manual Test

1. Go to your Jenkins job
2. Click **"Build with Parameters"**
3. Set:
   - `SKIP_DEPLOY = true`
   - `DEPLOY_LOCALLY = false` 
4. Click **Build**

---

## 🐛 Troubleshooting

### No Email Received?

**Check 1: Email plugin installed?**
```bash
# Check if plugin exists
docker exec jenkins-cicd ls /var/jenkins_home/plugins/ | grep email
```

**Check 2: SMTP configured?**
- Go to: Manage Jenkins → System
- Verify **Extended E-mail Notification** section is filled

**Check 3: Test connection**
- Use "Test configuration" button in Jenkins
- Check for error messages

**Check 4: Check Jenkins logs**
```bash
docker logs jenkins-cicd | grep -i "email\|mail"
```

### Gmail "Less Secure Apps" Error?

Gmail blocks regular passwords. **Solution:**

1. Enable 2-Factor Authentication
2. Generate App Password (see Step 3 above)
3. Use App Password in Jenkins

### Still No Emails?

**Option 1: Use Mailtrap** (safest for testing)
- No Gmail restrictions
- View all emails in web dashboard
- Perfect for development

**Option 2: Use Office 365**
```yaml
SMTP Server:    smtp.office365.com
SMTP Port:      587
Use TLS:        ✓ Checked
```

**Option 3: Use SendGrid** (free tier)
```yaml
SMTP Server:    smtp.sendgrid.net
SMTP Port:      587
Username:       apikey
Password:       [your-sendgrid-api-key]
```

---

## 📋 Current Jenkinsfile Emails

### Success Email
- **Trigger:** Build completes successfully
- **Recipient:** mohammad.kheirkhah@gritlab.ax
- **Contains:** 
  - Build number
  - Docker image tags
  - Deployment URLs

### Failure Email
- **Trigger:** Build fails at any stage
- **Recipient:** mohammad.kheirkhah@gritlab.ax
- **Contains:**
  - Error details
  - Last 50 lines of console output
  - Link to full console

---

## 🔄 Quick Reference

| What | Command/Action |
|------|----------------|
| Install plugin | Dashboard → Plugins → Email Extension |
| Configure SMTP | Manage Jenkins → System → Extended E-mail |
| Test email | Use "Test configuration" button |
| Check logs | `docker logs jenkins-cicd \| grep email` |
| Gmail app password | https://myaccount.google.com/apppasswords |
| Mailtrap (testing) | https://mailtrap.io |

---

## 🎯 What Was Fixed in Jenkinsfile

### Before (Broken):
```groovy
pipeline{
    // Missing: agent declaration
    // Missing: parameters
    // Missing: environment variables
    
    stages {
        // ...
        sshagent(credentials: [...]) {  // ❌ Plugin not installed
```

### After (Fixed):
```groovy
pipeline {
    agent any  // ✅ Added
    
    parameters {  // ✅ Added
        string(name: 'BRANCH', defaultValue: 'main', ...)
        // ...
    }
    
    environment {  // ✅ Added
        DOCKER_REPO = 'mahdikheirkhah'
        // ...
    }
    
    stages {
        // ...
        withCredentials([...]) {  // ✅ Fixed - no plugin needed
```

---

## 🚀 Next Steps

1. **Configure Jenkins email** (follow Step 1-4 above)
2. **Test the configuration** (send test email)
3. **Trigger a build** (commit & push)
4. **Check your email** 📧

---

## 📞 Need Help?

**Problem:** Email configuration not working

**Quick Solution:**
```bash
# 1. Use Mailtrap for testing (easiest)
# 2. Visit: https://mailtrap.io
# 3. Copy SMTP settings to Jenkins
# 4. No Gmail hassles!
```

**Full Jenkins logs:**
```bash
docker logs jenkins-cicd 2>&1 | tail -100
```

---

**Your Jenkins pipeline is now fixed! Configure email and you're ready to go! 🎉**


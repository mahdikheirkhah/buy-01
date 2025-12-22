# 📧 Email Not Working? Quick Fix Guide

## Problem
Jenkins shows "Sending email to: your-email" but you're not receiving emails.

## Solution (5 minutes)

### Step 1: Generate Gmail App Password

1. Go to: https://myaccount.google.com/security
2. Enable **2-Step Verification** (if not already enabled)
3. Go to: https://myaccount.google.com/apppasswords
4. Create App Password:
   - App: **Mail**
   - Device: **Other (Custom name)** → type "Jenkins"
5. Click **Generate**
6. **COPY the 16-character password** (you won't see it again!)

### Step 2: Configure Jenkins

1. Open Jenkins: http://localhost:8080
2. Go to: **Manage Jenkins** → **Configure System**
3. Scroll to **Extended E-mail Notification**:
   - SMTP server: `smtp.gmail.com`
   - SMTP Port: `587`
   - ☑ Use SMTP Authentication
   - User Name: `your-email@gmail.com`
   - Password: `[paste the 16-character App Password]`
   - ☑ Use TLS
   - Default Recipients: `your-email@gmail.com`

4. Scroll to **E-mail Notification**:
   - SMTP server: `smtp.gmail.com`
   - ☑ Use SMTP Authentication
   - User Name: `your-email@gmail.com`
   - Password: `[paste the 16-character App Password]`
   - ☑ Use TLS
   - SMTP Port: `587`

5. **TEST IT**:
   - Click "Test configuration by sending test e-mail"
   - Enter your email
   - Click "Test configuration"
   - Check your **inbox and spam folder**

### Step 3: If Test Fails

Run diagnostic:
```bash
./check-email-config.sh
```

Common issues:
- ❌ Wrong password → Use App Password, not Gmail password
- ❌ Port blocked → Check firewall
- ❌ Plugin missing → Install "Email Extension Plugin"
- ❌ Spam folder → Check it!

### Step 4: Test with a Pipeline

Create a new Pipeline job in Jenkins:

1. Click **New Item**
2. Name: `test-email`
3. Select **Pipeline**
4. In Pipeline script, paste content from: `test-email-pipeline.groovy`
5. Click **Build Now**
6. Check your email!

## Still Not Working?

### Check Jenkins Logs
```bash
docker logs jenkins-cicd | grep -i "mail\|smtp"
```

### Enable Debug Logging
1. **Manage Jenkins** → **System Log**
2. **Add new log recorder**
3. Name: `Email Debug`
4. Add logger: `hudson.tasks.Mailer`, Level: `ALL`
5. Add logger: `javax.mail`, Level: `ALL`
6. Save and check logs after next build

### Alternative Email Providers

#### SendGrid (Free)
```
SMTP server: smtp.sendgrid.net
Port: 587
Username: apikey
Password: [Your SendGrid API Key]
Use TLS: Yes
```

#### Outlook/Office 365
```
SMTP server: smtp.office365.com
Port: 587
Use TLS: Yes
```

## Reverse Proxy Warning Fix

To fix the "reverse proxy broken" warning:

1. **Manage Jenkins** → **Configure System**
2. Find **Jenkins Location**
3. Jenkins URL: `http://localhost:8080/`
4. Click **Save**

Or suppress the warning:
- Uncheck "Enable proxy compatibility"

## Complete Documentation

See [EMAIL_SETUP.md](EMAIL_SETUP.md) for comprehensive troubleshooting.

## Quick Checklist

- [ ] 2-Step Verification enabled
- [ ] App Password generated
- [ ] Email Extension Plugin installed
- [ ] SMTP configured in Jenkins
- [ ] Test email sent successfully
- [ ] Checked spam folder
- [ ] Email address correct in Jenkinsfile
- [ ] No firewall blocking port 587

## Need More Help?

1. Check [EMAIL_SETUP.md](EMAIL_SETUP.md) - Detailed guide
2. Check [JENKINS_SETUP.md](JENKINS_SETUP.md) - Jenkins setup
3. Run `./check-email-config.sh` - Diagnostic tool
4. Check Jenkins System Log for errors

---

**Pro Tip**: If email never works, use these alternatives:
- Slack notifications
- Discord webhooks
- Microsoft Teams
- Jenkins Blue Ocean UI (better visual feedback)


# Quick Fix: Gmail Email Notifications Not Working

## Problem
You configured Gmail SMTP in Jenkins (Manage Jenkins → System), test emails work, but you're not receiving build notification emails.

## Root Cause
The Jenkinsfile was sending emails to `test@example.com` instead of your actual email address.

## ✅ Solution Applied

I've updated your Jenkinsfile to send emails to the correct address: `mohammad.kheirkhah@gritlab.ax`

## 🚀 Next Steps (Do This Now!)

### Step 1: Verify Your Gmail Settings in Jenkins

1. **Open Jenkins**: http://localhost:8080
2. **Go to**: Manage Jenkins → System
3. **Find**: "Extended E-mail Notification" section
4. **Verify these settings are correct**:

```
SMTP server: smtp.gmail.com
SMTP Port: 587
✅ Use SMTP Authentication: CHECKED
Username: mohammad.kheirkhah@gritlab.ax
Password: [Your 16-character App Password]
✅ Use TLS: CHECKED
❌ Use SSL: UNCHECKED (since we're using TLS)
```

### Step 2: Generate Gmail App Password (if you haven't already)

1. Go to: https://myaccount.google.com/apppasswords
2. Make sure 2-Step Verification is enabled first
3. Create new App Password:
   - App: Mail
   - Device: Other (Jenkins)
4. Copy the 16-character password (e.g., `abcd efgh ijkl mnop`)
5. Use this password in Jenkins, NOT your regular Gmail password

### Step 3: Send Test Email from Jenkins

1. Still in Jenkins → System
2. Scroll to "E-mail Notification" section
3. Click "Test configuration by sending test e-mail"
4. Enter: `mohammad.kheirkhah@gritlab.ax`
5. Click "Test configuration"
6. **Check your Gmail inbox** (and spam folder!)

### Step 4: Commit and Push the Updated Jenkinsfile

The Jenkinsfile has been updated, now commit and push it:

```bash
cd /Users/mohammad.kheirkhah/Desktop/buy-01

# Check what was changed
git diff Jenkinsfile

# Stage the changes
git add Jenkinsfile

# Commit
git commit -m "fix: update email recipients to mohammad.kheirkhah@gritlab.ax"

# Push to trigger webhook
git push origin main
```

### Step 5: Trigger a Build and Check Email

1. The push will automatically trigger a build via webhook
2. Wait for the build to complete (watch in Jenkins)
3. **Check your Gmail inbox** for the build notification
4. **If not in inbox, check spam folder**

## 📧 What Changed in Jenkinsfile

**Before:**
```groovy
to: "test@example.com",
```

**After:**
```groovy
to: "mohammad.kheirkhah@gritlab.ax",
```

This change was made in 4 places:
- Success notification (HTML email)
- Success notification (plain text fallback)
- Failure notification (HTML email)
- Failure notification (plain text fallback)

## 🔍 Troubleshooting

### "Test email sent successfully" but still no emails?

**Check these:**

1. **Spam Folder** - Gmail might be filtering it
   - If you find it in spam, mark as "Not Spam"
   - Add Jenkins sender to your contacts

2. **Gmail Filters** - Check if you have filters blocking it
   - Go to Gmail → Settings → Filters and Blocked Addresses

3. **App Password** - Make sure you're using the App Password
   - NOT your regular Gmail password
   - It should be 16 characters without spaces

4. **Username** - Must be full email address
   - Correct: `mohammad.kheirkhah@gritlab.ax`
   - Wrong: `mohammad.kheirkhah` or `mohammad`

5. **Port and Security**
   - Port 587 with TLS (recommended)
   - OR Port 465 with SSL
   - NOT both SSL and TLS checked at the same time

### Authentication Failed?

1. Regenerate App Password: https://myaccount.google.com/apppasswords
2. Copy it carefully (remove spaces if needed)
3. Update in Jenkins

### Connection Timeout?

1. Check firewall settings
2. Try alternative port:
   - If using 587, try 465 with SSL
   - If using 465, try 587 with TLS

## 📋 Complete Gmail SMTP Settings

For your reference, here are the correct settings:

```
SMTP Server: smtp.gmail.com
Port: 587
Security: TLS (not SSL)
Authentication: Required
Username: mohammad.kheirkhah@gritlab.ax
Password: [Your 16-character Gmail App Password]
```

## 📚 Full Documentation

For detailed setup instructions, see:
- **[GMAIL_SETUP.md](./GMAIL_SETUP.md)** - Complete Gmail configuration guide
- **[MAILHOG_SETUP.md](./MAILHOG_SETUP.md)** - Alternative local testing setup

## ✅ Expected Email Behavior

Once configured correctly, you'll receive emails for:

**✅ Successful Builds:**
- Subject: "✅ Build SUCCESS: [Job Name] #[Number]"
- Contains: Build info, deployed services URLs, Docker image tags

**❌ Failed Builds:**
- Subject: "❌ Build FAILED: [Job Name] #[Number]"
- Contains: Build info, error links, troubleshooting tips

## 🎯 Quick Test Right Now

1. **Verify Jenkins SMTP settings** (see Step 1 above)
2. **Send test email from Jenkins** (see Step 3 above)
3. **If test works:** Commit and push the updated Jenkinsfile
4. **Check your email after build completes**

## Need Help?

If emails still don't work after following these steps:

1. Check Jenkins logs:
   ```bash
   docker logs jenkins-cicd | grep -i mail
   ```

2. Check your Gmail "Less secure app access" settings (though App Passwords should work regardless)

3. Try sending from a different Gmail account to rule out account-specific issues

4. Open an issue on GitHub with:
   - Jenkins SMTP configuration (screenshot without password)
   - Console output of failed email attempt
   - Error messages from Jenkins logs

---

**Last Updated**: December 22, 2025

**Status**: ✅ Jenkinsfile updated, ready to test after you configure Gmail SMTP in Jenkins


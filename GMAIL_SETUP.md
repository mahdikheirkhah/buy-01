# Gmail Setup for Jenkins Email Notifications

This guide will help you configure Jenkins to send real email notifications using Gmail.

## Prerequisites

- Gmail account (e.g., mohammad.kheirkhah@gritlab.ax)
- 2-Step Verification enabled on your Google account (required for App Passwords)
- Jenkins with Email Extension Plugin installed

## Step-by-Step Setup

### 1. Generate Gmail App Password

Since Google requires 2-Step Verification for App Passwords, follow these steps:

1. **Enable 2-Step Verification** (if not already enabled):
   - Go to: https://myaccount.google.com/security
   - Click "2-Step Verification"
   - Follow the setup wizard

2. **Create an App Password**:
   - Go to: https://myaccount.google.com/apppasswords
   - If you don't see this option, make sure 2-Step Verification is enabled
   - Select app: **Mail**
   - Select device: **Other (Custom name)**
   - Name it: **Jenkins CI/CD**
   - Click **Generate**
   - **IMPORTANT**: Copy the 16-character password (e.g., `abcd efgh ijkl mnop`)
   - You won't be able to see it again!

### 2. Configure Jenkins Extended E-mail Notification

1. **Open Jenkins**: http://localhost:8080

2. **Navigate to**: Manage Jenkins → System

3. **Find**: "Extended E-mail Notification" section (scroll down)

4. **Configure these settings**:

   | Setting | Value |
   |---------|-------|
   | **SMTP server** | `smtp.gmail.com` |
   | **SMTP Port** | `587` (for TLS) or `465` (for SSL) |
   | **Default Recipients** | `mohammad.kheirkhah@gritlab.ax` |
   | **Default Subject** | `$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS!` |
   | **Default Content** | `Check console output at $BUILD_URL to view the results.` |

5. **Click "Advanced" button** (next to SMTP settings)

6. **Configure Advanced Settings**:

   | Setting | Value |
   |---------|-------|
   | ✅ **Use SMTP Authentication** | Checked |
   | **User Name** | Your full Gmail address (e.g., `mohammad.kheirkhah@gritlab.ax`) |
   | **Password** | The 16-character App Password you generated |
   | **Use SSL** | ✅ Checked if using port 465 |
   | **Use TLS** | ✅ Checked if using port 587 (recommended) |
   | **SMTP port** | `587` (TLS) or `465` (SSL) |
   | **Reply-To Address** | `mohammad.kheirkhah@gritlab.ax` (optional) |
   | **Charset** | `UTF-8` |

7. **IMPORTANT**: Save your App Password in Jenkins Credentials (recommended):
   - Instead of entering the password directly, store it securely:
   - Go to: Manage Jenkins → Credentials → System → Global credentials
   - Click "Add Credentials"
   - Kind: **Username with password**
   - Username: `mohammad.kheirkhah@gritlab.ax`
   - Password: Your 16-character App Password
   - ID: `gmail-smtp-credentials` (you'll reference this)
   - Description: `Gmail SMTP for Jenkins notifications`
   - Click "Create"

### 3. Configure Standard E-mail Notification

1. **Scroll down to**: "E-mail Notification" section

2. **Configure these settings**:

   | Setting | Value |
   |---------|-------|
   | **SMTP server** | `smtp.gmail.com` |
   | **Default user e-mail suffix** | `@gritlab.ax` |
   | **SMTP Port** | `587` |

3. **Click "Advanced"**

4. **Configure Advanced Settings**:

   | Setting | Value |
   |---------|-------|
   | ✅ **Use SMTP Authentication** | Checked |
   | **User Name** | `mohammad.kheirkhah@gritlab.ax` |
   | **Password** | Your 16-character App Password |
   | **Use SSL** | ❌ Unchecked (we're using TLS) |
   | **Use TLS** | ✅ Checked |
   | **SMTP Port** | `587` |

### 4. Test the Configuration

1. **Still in the "E-mail Notification" section**
2. **Find**: "Test configuration by sending test e-mail"
3. **Enter**: `mohammad.kheirkhah@gritlab.ax`
4. **Click**: "Test configuration"
5. **Expected result**: "Email was successfully sent"

6. **Check your Gmail inbox**:
   - You should receive a test email within 1-2 minutes
   - **Check spam folder** if you don't see it in inbox
   - If it's in spam, mark it as "Not Spam" to ensure future emails arrive

### 5. Save Configuration

1. **Scroll to the bottom of the page**
2. **Click "Save"**

## Troubleshooting

### Issue 1: "Email was successfully sent" but no email received

**Possible causes:**
- Email is in spam folder (check spam!)
- Wrong email address
- Gmail is blocking the emails

**Solutions:**
1. Check spam folder first
2. Verify email address is correct: `mohammad.kheirkhah@gritlab.ax`
3. Check Gmail settings:
   - Go to: Gmail → Settings → Filters and Blocked Addresses
   - Make sure Jenkins emails aren't blocked
4. Try sending another test email

### Issue 2: "Authentication failed"

**Possible causes:**
- Using regular Gmail password instead of App Password
- Wrong username format
- App Password copied incorrectly

**Solutions:**
1. Generate a new App Password: https://myaccount.google.com/apppasswords
2. Copy it carefully (remove spaces: `abcd efgh ijkl mnop` → `abcdefghijklmnop`)
3. Use your full email address as username: `mohammad.kheirkhah@gritlab.ax`
4. Make sure 2-Step Verification is enabled

### Issue 3: "Connection timed out" or "Could not connect to SMTP host"

**Possible causes:**
- Wrong SMTP server or port
- Firewall blocking outgoing connections
- Jenkins running in Docker without network access

**Solutions:**
1. Verify SMTP settings:
   - Server: `smtp.gmail.com`
   - Port: `587` (TLS) or `465` (SSL)
2. If Jenkins is in Docker, make sure it can reach the internet:
   ```bash
   docker exec jenkins-cicd curl -I https://smtp.gmail.com:587
   ```
3. Check firewall settings on your Mac

### Issue 4: Emails sent but going to spam

**Solutions:**
1. Mark the first email as "Not Spam" in Gmail
2. Add Jenkins email to your contacts
3. Create a Gmail filter:
   - From: contains your email address
   - Never send it to Spam

### Issue 5: "No valid crumb" or permission errors

**This is NOT an email issue** - it's a Jenkins CSRF protection issue.

**Solutions:**
1. Make sure you're logged into Jenkins
2. Refresh the Jenkins page
3. Try configuring email again

## Testing with Your Pipeline

After configuring Gmail, test it with your Jenkins pipeline:

1. **Commit and push a change** to trigger the webhook:
   ```bash
   git add .
   git commit -m "test: trigger email notification"
   git push
   ```

2. **Or manually trigger a build**:
   - Go to your Jenkins job
   - Click "Build with Parameters"
   - Click "Build"

3. **Wait for build to complete** (success or failure)

4. **Check your Gmail** for the build notification

## Current Configuration in Jenkinsfile

Your Jenkinsfile is now configured to send emails to: `mohammad.kheirkhah@gritlab.ax`

The pipeline sends two types of emails:
- ✅ **Success notifications**: When build completes successfully
- ❌ **Failure notifications**: When build fails

Both email types include:
- Build number and job name
- Build status and duration
- Links to build details
- Service URLs (for successful builds)
- Error troubleshooting tips (for failed builds)

## Gmail App Password Security Tips

1. **Never share your App Password** - treat it like your regular password
2. **Store it in Jenkins Credentials** - don't hardcode it in Jenkinsfile
3. **Revoke unused App Passwords**:
   - Go to: https://myaccount.google.com/apppasswords
   - Remove any old/unused passwords
4. **Use a dedicated email account** for CI/CD (optional but recommended)

## Switching Back to MailHog (for testing)

If you want to switch back to MailHog for local testing:

1. Change SMTP settings in Jenkins:
   - SMTP server: `host.docker.internal`
   - SMTP Port: `1025`
   - Uncheck "Use SMTP Authentication"
   - Uncheck "Use SSL" and "Use TLS"

2. Start MailHog:
   ```bash
   docker compose up -d mailhog
   ```

3. View emails at: http://localhost:8025

## Quick Reference

| Service | SMTP Server | Port | Security | Username | Password |
|---------|-------------|------|----------|----------|----------|
| **Gmail (Recommended)** | smtp.gmail.com | 587 | TLS | Full email | App Password |
| Gmail (Alternative) | smtp.gmail.com | 465 | SSL | Full email | App Password |
| MailHog (Testing) | host.docker.internal | 1025 | None | - | - |

## Summary Checklist

Before emails will work, make sure:

- ✅ 2-Step Verification enabled on Gmail
- ✅ App Password generated (16 characters)
- ✅ Jenkins SMTP configured with `smtp.gmail.com:587`
- ✅ SMTP Authentication enabled
- ✅ Username is full email address
- ✅ Password is App Password (not regular password)
- ✅ TLS is enabled
- ✅ Configuration saved in Jenkins
- ✅ Test email sent successfully
- ✅ Jenkinsfile updated with correct email address: `mohammad.kheirkhah@gritlab.ax`
- ✅ Changes committed and pushed to Git

---

## Common Gmail SMTP Settings Summary

```
SMTP Server: smtp.gmail.com
SMTP Port: 587 (TLS) or 465 (SSL)
Security: TLS or SSL (TLS recommended)
Authentication: Required
Username: mohammad.kheirkhah@gritlab.ax
Password: [Your 16-character App Password]
```

---

**Need Help?**
- Google App Passwords: https://myaccount.google.com/apppasswords
- Jenkins Email Extension Plugin: https://plugins.jenkins.io/email-ext/
- Gmail SMTP Help: https://support.google.com/mail/answer/7126229

**Last Updated**: December 22, 2025


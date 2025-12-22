# MailHog Setup for Jenkins Email Testing

This guide will help you set up MailHog to test email notifications from Jenkins without sending real emails.

## What is MailHog?

MailHog is an email testing tool that captures emails sent from your applications and displays them in a web interface. It's perfect for testing Jenkins email notifications locally.

## Quick Setup

### 1. Start MailHog with Docker Compose

MailHog is already configured in your `docker-compose.yml`. Simply start it:

```bash
cd /Users/mohammad.kheirkhah/Desktop/buy-01
export IMAGE_TAG=stable
docker compose up -d mailhog
```

This will start MailHog with:
- **SMTP server** on port `1025` (for Jenkins to send emails)
- **Web UI** on port `8025` (for you to view captured emails)

### 2. Configure Jenkins to Use MailHog

#### Option A: Using Jenkins UI (Recommended)

1. **Open Jenkins**: http://localhost:8080
2. **Go to**: Manage Jenkins → System
3. **Scroll to**: "Extended E-mail Notification" section
4. **Configure these settings**:
   - **SMTP server**: `host.docker.internal`
   - **SMTP Port**: `1025`
   - **Default Recipients**: `test@example.com`
   - **Default Subject**: `$PROJECT_NAME - Build # $BUILD_NUMBER - $BUILD_STATUS!`
   - **Default Content**: `${FILE,path="email-templates/email-html.template"}`
   
5. **Scroll to**: "E-mail Notification" section (below Extended E-mail)
6. **Configure these settings**:
   - **SMTP server**: `host.docker.internal`
   - **SMTP Port**: `1025`
   - **Default user e-mail suffix**: `@example.com`
   - **Advanced Settings**: Click "Advanced"
     - ✅ Check: "Use SMTP Authentication" (then uncheck it - MailHog doesn't need auth)
     - ❌ Uncheck: "Use SSL" and "Use TLS"
   
7. **Test Configuration**:
   - Click "Test configuration by sending test e-mail"
   - Enter: `test@example.com`
   - Click "Test configuration"

8. **Save** your changes

#### Option B: Using Configuration as Code

If you're using Jenkins Configuration as Code (JCasC), add this to your configuration:

```yaml
unclassified:
  mailer:
    smtpHost: "host.docker.internal"
    smtpPort: "1025"
    useSsl: false
    
  email-ext:
    defaultContentType: "text/html"
    smtpServer: "host.docker.internal"
    smtpPort: 1025
    useSsl: false
```

### 3. View Captured Emails

1. **Open MailHog Web UI**: http://localhost:8025
2. Run a Jenkins build to trigger email notifications
3. Check MailHog - you should see the email appear immediately!

## Troubleshooting

### Problem: Jenkins can't connect to MailHog

**Solution**: Make sure you're using `host.docker.internal` as the SMTP server, not `localhost` or `mailhog`.

Jenkins is running inside a Docker container, so:
- ❌ `localhost` → Points to Jenkins container itself
- ❌ `mailhog` → Only works if both are on same Docker network
- ✅ `host.docker.internal` → Points to your Mac's localhost (where MailHog is accessible)

### Problem: No emails appearing in MailHog

1. **Check if MailHog is running**:
   ```bash
   docker compose ps mailhog
   ```

2. **Check Jenkins logs for email errors**:
   ```bash
   docker logs jenkins-cicd | grep -i mail
   ```

3. **Verify SMTP settings in Jenkins**:
   - Go to: Manage Jenkins → System
   - Look for "Extended E-mail Notification"
   - Make sure SMTP server is `host.docker.internal` and port is `1025`

4. **Test email from Jenkins console**:
   - Go to: Manage Jenkins → System
   - Scroll to "E-mail Notification"
   - Click "Test configuration by sending test e-mail"
   - Enter `test@example.com`
   - Check MailHog UI

### Problem: Emails work but are not HTML formatted

Make sure you're using `emailext` plugin, not the basic `mail` step:

```groovy
// ✅ Good - HTML emails
emailext (
    subject: "Build Status",
    body: "<h1>Build Success!</h1>",
    to: "test@example.com",
    mimeType: 'text/html'
)

// ❌ Limited - Plain text only
mail to: 'test@example.com',
     subject: "Build Status",
     body: "Build completed"
```

## Current Configuration

Your Jenkinsfile is already configured to send emails to `test@example.com` for testing. This means:

1. All Jenkins build notifications will go to `test@example.com`
2. MailHog will capture these emails
3. You can view them at http://localhost:8025

## Switching to Real Email (Gmail)

Once you've tested with MailHog and want to switch to real Gmail notifications:

### 1. Update Jenkins SMTP Settings:
- **SMTP server**: `smtp.gmail.com`
- **SMTP Port**: `465` (SSL) or `587` (TLS)
- **Use SSL/TLS**: ✅ Enabled
- **Username**: `your-email@gmail.com`
- **Password**: Use an [App Password](https://myaccount.google.com/apppasswords), not your regular Gmail password

### 2. Update Jenkinsfile:
Change `to: "test@example.com"` to `to: "your-email@gmail.com"`

### 3. Create Gmail App Password:
1. Go to: https://myaccount.google.com/apppasswords
2. Select "Mail" and "Other (Custom name)"
3. Name it: "Jenkins"
4. Click "Generate"
5. Copy the 16-character password
6. Use this in Jenkins SMTP authentication

## Quick Commands

```bash
# Start MailHog
docker compose up -d mailhog

# View MailHog logs
docker logs mailhog -f

# Stop MailHog
docker compose stop mailhog

# Restart MailHog
docker compose restart mailhog

# Remove MailHog
docker compose down mailhog

# Open MailHog Web UI
open http://localhost:8025
```

## Testing Email Notifications

1. **Trigger a build**: Click "Build with Parameters" in Jenkins
2. **Wait for build to complete**: Watch the build progress
3. **Check MailHog**: Open http://localhost:8025
4. **View the email**: Click on the email in MailHog to see the full content

## Features of MailHog

- 📧 **Captures all SMTP emails** - No emails are actually sent
- 🌐 **Web UI** - View emails in your browser
- 🔍 **Search** - Find emails by sender, recipient, or subject
- 📱 **Responsive** - Works on mobile devices
- 🗑️ **Delete emails** - Clear the mailbox when needed
- 💾 **Download emails** - Save emails for later inspection

## Summary

✅ **MailHog is perfect for**:
- Testing Jenkins email notifications locally
- Development and debugging
- Avoiding spam to real email addresses
- Seeing exactly what emails will look like

❌ **Don't use MailHog for**:
- Production deployments
- Sending real notifications to team members
- Long-term email storage

---

**Need Help?**
- MailHog Web UI: http://localhost:8025
- MailHog Documentation: https://github.com/mailhog/MailHog
- Jenkins Extended Email Plugin: https://plugins.jenkins.io/email-ext/


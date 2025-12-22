# Gmail Setup for Jenkins Email Notifications

## Overview
This guide will help you configure Jenkins to send email notifications using your Gmail account.

## Prerequisites
- Gmail account
- Gmail App Password (2FA must be enabled)

---

## Step 1: Generate Gmail App Password

1. **Enable 2-Factor Authentication**
   - Go to: https://myaccount.google.com/security
   - Click on "2-Step Verification"
   - Follow the steps to enable it

2. **Create App Password**
   - Go to: https://myaccount.google.com/apppasswords
   - Select "Mail" as the app
   - Select "Other (Custom name)" as the device
   - Enter "Jenkins CI/CD" as the name
   - Click "Generate"
   - **Copy the 16-character password** (you won't see it again!)

---

## Step 2: Configure Jenkins Extended E-mail Notification

### Method 1: Via Jenkins UI (Recommended)

1. **Go to Jenkins Dashboard**
   - Navigate to: `Manage Jenkins` → `System`

2. **Scroll to "Extended E-mail Notification" Section**

3. **Configure SMTP Settings:**
   ```
   SMTP server: smtp.gmail.com
   SMTP port: 465
   ```

4. **Click "Advanced..."**

5. **Configure Advanced Settings:**
   - ☑ Use SMTP Authentication
   - User Name: `your-email@gmail.com`
   - Password: `your-16-char-app-password`
   - ☑ Use SSL
   - SMTP port: 465
   - Charset: UTF-8

6. **Set Default Recipients:**
   - Default user E-mail suffix: `@gmail.com`
   - Default Recipients: `mohammad.kheirkhah@gritlab.ax`

7. **Click "Apply" and "Save"**

---

### Method 2: Via Jenkins Credentials (More Secure)

1. **Create Credential**
   - Go to: `Manage Jenkins` → `Credentials` → `System` → `Global credentials`
   - Click "Add Credentials"
   - Kind: `Username with password`
   - Username: `your-email@gmail.com`
   - Password: `your-16-char-app-password`
   - ID: `gmail-smtp-credentials`
   - Description: `Gmail SMTP for Jenkins`
   - Click "OK"

2. **Configure Extended E-mail Notification**
   - Go to: `Manage Jenkins` → `System`
   - Scroll to "Extended E-mail Notification"
   - SMTP server: `smtp.gmail.com`
   - Click "Advanced..."
   - ☑ Use SMTP Authentication
   - Credentials: Select `gmail-smtp-credentials`
   - ☑ Use SSL
   - SMTP port: 465

---

## Step 3: Configure Standard E-mail Notification (Fallback)

1. **Scroll to "E-mail Notification" Section** (below Extended E-mail)

2. **Configure:**
   ```
   SMTP server: smtp.gmail.com
   ```

3. **Click "Advanced..."**

4. **Configure:**
   - ☑ Use SMTP Authentication
   - User Name: `your-email@gmail.com`
   - Password: `your-16-char-app-password`
   - ☑ Use SSL
   - SMTP Port: 465
   - Charset: UTF-8
   - Reply-To Address: `your-email@gmail.com`

---

## Step 4: Test Email Configuration

1. **Test via Jenkins UI**
   - In the "E-mail Notification" section
   - Check "Test configuration by sending test e-mail"
   - Enter your email: `mohammad.kheirkhah@gritlab.ax`
   - Click "Test configuration"
   - You should see: "Email was successfully sent"

2. **Check your inbox** (and spam folder!)

---

## Step 5: Verify Jenkinsfile Configuration

Your Jenkinsfile should have email notifications configured in the `post` section:

```groovy
post {
    success {
        echo "✅ Pipeline completed successfully!"
        script {
            emailext (
                subject: "✅ Build SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """
                    <h2 style="color: green;">Build Successful!</h2>
                    <p><strong>Job:</strong> ${env.JOB_NAME}</p>
                    ...
                """,
                to: "mohammad.kheirkhah@gritlab.ax",
                mimeType: 'text/html'
            )
        }
    }
    
    failure {
        echo "❌ Pipeline failed!"
        script {
            emailext (
                subject: "❌ Build FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: """
                    <h2 style="color: red;">Build Failed!</h2>
                    ...
                """,
                to: "mohammad.kheirkhah@gritlab.ax",
                mimeType: 'text/html'
            )
        }
    }
}
```

---

## Troubleshooting

### Issue: "Email was not sent" or "Connection timed out"

**Solution 1: Check Gmail Settings**
- Ensure 2FA is enabled
- Verify App Password is correct (no spaces)
- Make sure "Less secure app access" is OFF (use App Password instead)

**Solution 2: Check Port and SSL**
- Use port `465` with SSL
- OR use port `587` with TLS (Start TLS)

**Solution 3: Check Firewall**
- Ensure Jenkins can access `smtp.gmail.com:465`
- Test from Jenkins container:
  ```bash
  docker exec jenkins-cicd nc -zv smtp.gmail.com 465
  ```

### Issue: Email sent but not received

**Check:**
1. Spam/Junk folder
2. Email address is correct
3. Gmail "Sent" folder (check if email was actually sent)
4. Check Jenkins console output for "Email sent successfully"

### Issue: "Authentication failed"

**Solutions:**
1. Regenerate App Password
2. Make sure you copied the entire 16-character password
3. Try removing spaces if you copied them
4. Verify username is the full email address

---

## Alternative: Using Port 587 with TLS

If port 465 doesn't work, try:

```
SMTP server: smtp.gmail.com
SMTP port: 587
☑ Use SMTP Authentication
☐ Use SSL (unchecked)
☑ Use TLS (if available)
```

---

## Testing from Command Line

Test SMTP connection from Jenkins container:

```bash
# Access Jenkins container
docker exec -it jenkins-cicd bash

# Install testing tools
apt-get update && apt-get install -y telnet

# Test connection
telnet smtp.gmail.com 465
# Or for TLS:
telnet smtp.gmail.com 587
```

---

## Common Gmail SMTP Settings Summary

| Setting | Value |
|---------|-------|
| SMTP Server | smtp.gmail.com |
| SSL Port | 465 |
| TLS Port | 587 |
| Authentication | Yes (required) |
| Username | your-email@gmail.com |
| Password | 16-char App Password |

---

## Security Best Practices

1. ✅ Use App Passwords (not your main Gmail password)
2. ✅ Store credentials in Jenkins Credentials Manager
3. ✅ Enable 2FA on your Gmail account
4. ✅ Regularly rotate App Passwords
5. ✅ Use different App Passwords for different services
6. ❌ Never commit passwords to Git

---

## Next Steps

After email is working:

1. Test by triggering a Jenkins build
2. Check spam folder for first email
3. Mark as "Not Spam" if needed
4. Add Jenkins email to contacts
5. Create email filters/labels for better organization

---

## Support

If you still have issues:

1. Check Jenkins system logs:
   ```bash
   docker logs jenkins-cicd | grep -i mail
   ```

2. Enable debug logging:
   - Go to: `Manage Jenkins` → `System Log`
   - Add new logger: `javax.mail` with level `FINEST`

3. Review Jenkins job console output for detailed error messages

---

**Last Updated:** December 22, 2025


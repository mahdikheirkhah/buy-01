# Jenkins Email Configuration Guide

## Issue: Emails Not Being Received

Even though Jenkins shows "Sending email to: your-email@domain.com", you're not receiving emails.

## Common Causes & Solutions

### 1. Gmail SMTP Configuration Issues

#### Step 1: Enable 2-Step Verification
1. Go to your Google Account: https://myaccount.google.com
2. Navigate to **Security** → **2-Step Verification**
3. Enable it if not already enabled

#### Step 2: Generate App Password
1. Go to: https://myaccount.google.com/apppasswords
2. Select app: **Mail**
3. Select device: **Other (Custom name)**
4. Enter name: `Jenkins CI/CD`
5. Click **Generate**
6. Copy the 16-character password (save it - you won't see it again!)

#### Step 3: Configure Jenkins Email Settings

1. Go to **Jenkins** → **Manage Jenkins** → **Configure System**

2. Find **Extended E-mail Notification** section:
   ```
   SMTP server: smtp.gmail.com
   SMTP Port: 587
   ☑ Use SMTP Authentication
   User Name: your-email@gmail.com
   Password: [Your 16-character App Password]
   ☑ Use TLS
   
   Default Recipients: your-email@gmail.com
   Default Content Type: HTML (text/html)
   ```

3. Find **E-mail Notification** section (standard mailer):
   ```
   SMTP server: smtp.gmail.com
   ☑ Use SMTP Authentication
   User Name: your-email@gmail.com
   Password: [Your 16-character App Password]
   ☑ Use TLS
   SMTP Port: 587
   
   Test configuration by sending test e-mail
   Test e-mail recipient: your-email@gmail.com
   ```

4. Click **Test configuration** button to verify

### 2. Check Email Configuration in Jenkinsfile

Your Jenkinsfile uses `emailext` which requires the **Email Extension Plugin**.

Verify plugin is installed:
1. Go to **Manage Jenkins** → **Plugins**
2. Search for "Email Extension Plugin"
3. Install if not present
4. Restart Jenkins

### 3. Alternative: Use Standard Mail

If `emailext` doesn't work, you can use the simpler `mail` step.

Replace the `emailext` blocks in your Jenkinsfile with:

```groovy
post {
    success {
        echo "✅ Pipeline completed successfully!"
        
        script {
            try {
                mail to: 'mohammad.kheirkhah@gritlab.ax',
                     subject: "✅ Build SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                     body: """
Build Successful!

Job: ${env.JOB_NAME}
Build Number: ${env.BUILD_NUMBER}
Branch: ${params.BRANCH}
Image Tag: ${env.IMAGE_TAG}
Duration: ${currentBuild.durationString}

Build URL: ${env.BUILD_URL}

Deployed Services:
- Frontend: http://localhost:4200
- API Gateway: https://localhost:8443
- Eureka: http://localhost:8761

All Docker images published with tag: ${env.IMAGE_TAG}
                     """
            } catch (Exception e) {
                echo "Failed to send email: ${e.message}"
            }
        }
    }
    
    failure {
        echo "❌ Pipeline failed!"
        
        script {
            try {
                mail to: 'mohammad.kheirkhah@gritlab.ax',
                     subject: "❌ Build FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                     body: """
Build Failed!

Job: ${env.JOB_NAME}
Build Number: ${env.BUILD_NUMBER}
Branch: ${params.BRANCH}
Status: ${currentBuild.result}
Duration: ${currentBuild.durationString}

Build URL: ${env.BUILD_URL}
Console: ${env.BUILD_URL}console

Please check Jenkins for details.
                     """
            } catch (Exception e) {
                echo "Failed to send email: ${e.message}"
            }
        }
    }
}
```

### 4. Check Jenkins System Log

1. Go to **Manage Jenkins** → **System Log**
2. Click **Add new log recorder**
3. Name: `Email Debug`
4. Add logger: `hudson.tasks.Mailer`
5. Log level: `ALL`
6. Add logger: `jenkins.plugins.mailer`
7. Log level: `ALL`
8. Save

Now run a build and check this log for email-related errors.

### 5. Common Gmail Issues

#### Issue: "Username and Password not accepted"
- **Solution**: Use App Password, not your regular Gmail password

#### Issue: "Authentication failed"
- **Solution**: 
  - Verify 2-Step Verification is enabled
  - Generate a new App Password
  - Make sure there are no spaces in the App Password

#### Issue: "Connection timeout"
- **Solution**: 
  - Check firewall settings
  - Try port 465 with SSL instead of 587 with TLS
  - Verify Jenkins container can access the internet

#### Issue: Email goes to Spam
- **Solution**: 
  - Check your Spam/Junk folder
  - Add jenkins@yourserver to contacts
  - Mark as "Not Spam"

### 6. Test Email from Jenkins Container

SSH into Jenkins container and test SMTP:

```bash
docker exec -it jenkins-cicd bash

# Install telnet
apt-get update && apt-get install -y telnet

# Test connection to Gmail SMTP
telnet smtp.gmail.com 587

# You should see: 220 smtp.google.com ESMTP
# If connection fails, there's a network issue
```

### 7. Alternative Email Providers

If Gmail doesn't work, try these:

#### Outlook/Office 365
```
SMTP server: smtp.office365.com
Port: 587
Use TLS: Yes
```

#### SendGrid (Free tier available)
```
SMTP server: smtp.sendgrid.net
Port: 587
Username: apikey
Password: [Your SendGrid API Key]
Use TLS: Yes
```

#### Mailgun
```
SMTP server: smtp.mailgun.org
Port: 587
Username: [Your Mailgun username]
Password: [Your Mailgun password]
Use TLS: Yes
```

### 8. Debugging Steps

1. **Check Jenkins logs**:
   ```bash
   docker logs jenkins-cicd | grep -i mail
   ```

2. **Verify email plugin is working**:
   - Go to a previous build
   - Click "Email" or "Console Output"
   - Look for email-related errors

3. **Test with a simple job**:
   Create a new pipeline with just:
   ```groovy
   pipeline {
       agent any
       stages {
           stage('Test') {
               steps {
                   echo 'Testing email'
               }
           }
       }
       post {
           always {
               mail to: 'your-email@gmail.com',
                    subject: 'Test Email from Jenkins',
                    body: 'This is a test email'
           }
       }
   }
   ```

4. **Check email is not being blocked**:
   - Check spam folder
   - Check if Gmail is blocking Jenkins emails
   - Try sending to a different email address

## Reverse Proxy Warning Fix

The "reverse proxy broken" warning can be fixed:

1. Go to **Manage Jenkins** → **Configure System**
2. Find **Jenkins URL** section
3. Set to: `http://localhost:8080/`
4. Click **Save**

Or if you want to suppress the warning:
1. Go to **Manage Jenkins** → **Configure System**
2. Find **Jenkins Location**
3. Uncheck **Enable proxy compatibility**

## Quick Test Checklist

- [ ] 2-Step Verification enabled on Gmail
- [ ] App Password generated and saved
- [ ] Email Extension Plugin installed in Jenkins
- [ ] SMTP settings configured in Jenkins System
- [ ] Test email sent successfully from Jenkins System config
- [ ] Check spam folder for test email
- [ ] Email address spelled correctly in Jenkinsfile
- [ ] Jenkins container can access the internet
- [ ] No firewall blocking port 587
- [ ] Jenkins System Log checked for errors

## Still Not Working?

If emails still don't work after trying all above:

1. **Use webhook notifications instead**:
   - Slack
   - Discord
   - Microsoft Teams
   - Telegram

2. **Use Jenkins built-in notifications**:
   - RSS feed (available at `/rssLatest`)
   - Browser notifications
   - Blue Ocean UI has better visual feedback

3. **Check with IT/Network team**:
   - Corporate firewall may block SMTP
   - VPN might interfere
   - Network policies may prevent external email

## Need More Help?

Check Jenkins logs:
```bash
docker logs jenkins-cicd -f | grep -i "mail\|smtp\|email"
```

Enable debug mode in Jenkins:
1. Manage Jenkins → Script Console
2. Run:
   ```groovy
   import java.util.logging.Logger
   import java.util.logging.Level
   
   def logger = Logger.getLogger("hudson.tasks.Mailer")
   logger.setLevel(Level.ALL)
   
   def logger2 = Logger.getLogger("javax.mail")
   logger2.setLevel(Level.ALL)
   ```
3. Run a build and check System Log


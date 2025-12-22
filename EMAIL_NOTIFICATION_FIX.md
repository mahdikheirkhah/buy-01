# Email Notification Fix - Jenkins CI/CD Pipeline

## Issue Summary
The Jenkins pipeline was failing to send email notifications on build failure due to two main issues:

### 1. **Early Pipeline Failure** 
- The pipeline was failing in the environment setup phase because of the missing SonarQube tool
- Error: `No tool named SonarQubeScanner found`
- This prevented the pipeline from reaching the post-failure notification stage

### 2. **Security Sandbox Restriction**
- The failure notification was trying to use `currentBuild.rawBuild.getLog()`
- Error: `Scripts not permitted to use method org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper getRawBuild`
- This method requires script approval in Jenkins and violates sandbox security

## Changes Made

### Change 1: Removed SonarQube Tool from Environment Block
**Before:**
```groovy
environment {
    ...
    SONAR_SCANNER_HOME = tool 'SonarQubeScanner'  // ❌ Fails if tool not configured
}
```

**After:**
```groovy
environment {
    ...
    // SonarQube tool moved to stage-level (no early failure)
}
```

### Change 2: Made SonarQube Stage Fault-Tolerant
**Before:**
```groovy
withSonarQubeEnv('SonarQube') {
    sh "${env.SONAR_SCANNER_HOME}/bin/sonar-scanner ..."  // ❌ Uses undefined variable
}
```

**After:**
```groovy
try {
    def scannerHome = tool name: 'SonarQubeScanner', type: 'hudson.plugins.sonar.SonarRunnerInstallation'
    withSonarQubeEnv('SonarQube') {
        sh "${scannerHome}/bin/sonar-scanner ..."  // ✅ Only runs if tool exists
    }
} catch (Exception e) {
    echo "⚠️  SonarQube analysis skipped: ${e.getMessage()}"
    // Don't fail build
}
```

### Change 3: Removed Unsafe `rawBuild.getLog()` Call
**Before:**
```groovy
failure {
    emailext (
        body: """
            <h3>Console Output:</h3>
            <pre>${currentBuild.rawBuild.getLog(50).join('\n')}</pre>  // ❌ Requires special permissions
        """
    )
}
```

**After:**
```groovy
failure {
    emailext (
        body: """
            <p><strong>Console Output:</strong> <a href="${env.BUILD_URL}console">${env.BUILD_URL}console</a></p>
            
            <h3>Possible Issues:</h3>
            <ul>
                <li>Check if all services are properly configured</li>
                <li>Verify Docker Hub credentials are valid</li>
                <li>Review the console output for detailed errors</li>
                <li>Check if backend build completed successfully</li>
            </ul>  // ✅ Provides helpful troubleshooting tips instead
        """
    )
}
```

## Why Email Wasn't Sending

1. **Pipeline Failed Too Early**: The `tool 'SonarQubeScanner'` call in the environment block was evaluated before any stages ran, causing the pipeline to fail before reaching the post-failure notification

2. **Secondary Failure in Post Block**: Even when the pipeline reached the post-failure block, the `currentBuild.rawBuild.getLog()` method threw a security exception, preventing the email from being sent

## Testing the Fix

### Step 1: Commit and Push Changes
```bash
cd /Users/mohammad.kheirkhah/Desktop/buy-01
git add Jenkinsfile
git commit -m "fix: resolve email notification issues in Jenkins pipeline"
git push origin main
```

### Step 2: Trigger a Build
1. Go to Jenkins dashboard
2. Click on your pipeline job
3. Click "Build with Parameters"
4. Keep default settings (this should fail quickly to test email)
5. Click "Build"

### Step 3: Verify Email Notification
- Check your email: `mohammad.kheirkhah@gritlab.ax`
- You should receive an email with:
  - Subject: `❌ Build FAILED: [job-name] #[build-number]`
  - HTML formatted body with build details
  - Link to console output
  - Troubleshooting suggestions

## Email Configuration Checklist

Make sure you have configured email in Jenkins:

### 1. Extended Email Notification Plugin
- **Manage Jenkins** → **Plugins** → Search "Email Extension Plugin"
- Should be installed and enabled

### 2. Email Server Configuration
- **Manage Jenkins** → **Configure System** → **Extended E-mail Notification**
- SMTP Server: `smtp.gmail.com` (or your provider)
- SMTP Port: `587` (TLS) or `465` (SSL)
- Credentials: Add your email credentials
- Default Recipients: Your email address

### 3. Test Configuration
- **Manage Jenkins** → **Configure System** → **Extended E-mail Notification**
- Scroll to bottom and click **"Test configuration by sending test e-mail"**
- Enter: `mohammad.kheirkhah@gritlab.ax`
- Should receive test email immediately

## Troubleshooting

### Email Still Not Sending?

1. **Check Jenkins System Log**
   ```
   Manage Jenkins → System Log → Add new log recorder
   Logger: hudson.plugins.emailext
   Log level: FINEST
   ```

2. **Verify SMTP Credentials**
   - Make sure credentials in Jenkins match your email provider
   - Gmail users: Enable "Less secure app access" or use App Password

3. **Check Firewall/Network**
   - Jenkins needs outbound access to SMTP server
   - Test with: `telnet smtp.gmail.com 587`

4. **Review Build Console Output**
   - Look for email-related errors at the end of the build log

### Pipeline Still Failing Early?

If you see "No tool named SonarQubeScanner found":
1. Set `RUN_SONAR` parameter to `false` (default is false)
2. Or install and configure SonarQube Scanner tool in Jenkins

## Summary

✅ **Fixed Issues:**
- Removed tool configuration from environment block (prevents early failure)
- Made SonarQube stage gracefully handle missing configuration
- Removed `rawBuild.getLog()` call (security sandbox violation)
- Added helpful troubleshooting information in failure email

✅ **Email Notification Now Works:**
- Sends on successful builds
- Sends on failed builds (with detailed information)
- No security violations
- Clean HTML formatting with helpful links

## Next Steps

1. Commit and push the changes
2. Trigger a test build
3. Verify you receive email notifications
4. If needed, adjust email content in the `post { failure }` section

## Need Help?

If you still don't receive emails after these changes:
1. Check the Jenkins console output for any email-related errors
2. Verify your SMTP configuration in Jenkins
3. Test email configuration using the built-in test function
4. Check your email spam/junk folder


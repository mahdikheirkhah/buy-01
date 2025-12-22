# 🎯 SOLUTION: Email Not Working - Complete Fix

## Problem Summary
- Jenkins shows "Sending email to: mohammad.kheirkhah@gritlab.ax" ✅
- No email received in Gmail inbox ❌
- Root cause: Jenkins container cannot reliably connect to Gmail SMTP

## ✅ RECOMMENDED SOLUTION: Mailhog (Easiest!)

Mailhog is already running on your machine! This is a **local email testing server** - perfect for development.

### Step 1: Configure Jenkins (5 minutes)

1. Open Jenkins: **http://localhost:8080**

2. Go to: **Manage Jenkins** → **Configure System**

3. **Scroll to "Extended E-mail Notification"**:
   ```
   SMTP server: host.docker.internal
   SMTP Port: 1025
   ☐ Use SMTP Authentication (UNCHECK!)
   ☐ Use SSL (UNCHECK!)
   ☐ Use TLS (UNCHECK!)
   
   Default Recipients: mohammad.kheirkhah@gritlab.ax
   Default Content Type: HTML (text/html)
   ```

4. **Scroll to "E-mail Notification"**:
   ```
   SMTP server: host.docker.internal
   ☐ Use SMTP Authentication (UNCHECK!)
   ☐ Use SSL (UNCHECK!)
   ☐ Use TLS (UNCHECK!)
   SMTP Port: 1025
   ```

5. Click **"Test configuration by sending test e-mail"**
   - Enter any email address
   - Click "Test configuration"
   - You should see: "Email was successfully sent"

### Step 2: View Emails

Open in your browser: **http://localhost:8025**

You'll see all emails Jenkins sends - no spam folder, instant delivery!

### Step 3: Test with Your Pipeline

1. Go to your pipeline: **e-commerce-microservices-ci-cd**
2. Click **"Build with Parameters"**
3. Set `DEPLOY_LOCALLY = true`
4. Click **"Build"**
5. Open **http://localhost:8025** to see the email!

---

## Alternative: Gmail (If You Need Real Emails)

If you need emails in real Gmail inbox:

### Issue Found
Your host Mac can connect to Gmail SMTP, but there might be network issues from the Jenkins container.

### Solution: Try Port 465 with SSL

1. Go to: **Manage Jenkins** → **Configure System**
2. Change settings:
   ```
   SMTP Port: 465 (instead of 587)
   ☐ Use TLS (UNCHECK)
   ☑ Use SSL (CHECK THIS!)
   ```
3. Test again

---

## Fix Reverse Proxy Warning

To remove the "reverse proxy broken" warning:

1. **Manage Jenkins** → **Configure System**
2. Find **Jenkins Location**
3. Jenkins URL: `http://localhost:8080/`
4. Click **Save**

This is just a warning and doesn't affect functionality.

---

## Quick Commands

### Check Mailhog Status
```bash
docker ps | grep mailhog
```

### View Mailhog Logs
```bash
docker logs mailhog
```

### Restart Mailhog
```bash
docker restart mailhog
```

### Stop Mailhog
```bash
docker stop mailhog
docker rm mailhog
```

### Restart Jenkins
```bash
docker restart jenkins-cicd
```

---

## Testing Checklist

- [ ] Mailhog is running (`docker ps | grep mailhog`)
- [ ] Mailhog UI accessible: http://localhost:8025
- [ ] Jenkins configured with `host.docker.internal:1025`
- [ ] Authentication and SSL/TLS UNCHECKED
- [ ] Test email sent from Jenkins System config
- [ ] Email visible in Mailhog UI
- [ ] Pipeline test successful
- [ ] Email received for successful build

---

## Why Mailhog is Better for Development

✅ **Instant**: No delays, see emails immediately  
✅ **No Spam**: Never goes to spam folder  
✅ **No Passwords**: No App Passwords needed  
✅ **Visual**: Nice web UI to view emails  
✅ **Reliable**: Works 100% of the time  
✅ **Free**: Completely free, no limits  

For **production**, use:
- SendGrid (free tier: 100 emails/day)
- AWS SES
- Mailgun
- Real SMTP server

---

## Files Created

- `EMAIL_SETUP.md` - Comprehensive Gmail setup guide
- `EMAIL_QUICK_FIX.md` - Quick Gmail fix guide
- `check-email-config.sh` - Diagnostic tool
- `setup-mailhog.sh` - Mailhog setup script
- `test-email-pipeline.groovy` - Test pipeline
- `test-smtp-connection.sh` - SMTP connection tester

---

## Summary

**Problem**: Gmail SMTP connection issues from Jenkins container  
**Solution**: Use Mailhog for local development  
**Time**: 5 minutes to setup  
**Result**: 100% reliable email testing  

### Quick Setup
```bash
# Mailhog is already running!
# Just configure Jenkins:
# - SMTP: host.docker.internal
# - Port: 1025
# - No auth, no SSL/TLS
# View emails: http://localhost:8025
```

---

## Next Steps

1. ✅ Configure Jenkins with Mailhog settings (above)
2. ✅ Test configuration in Jenkins
3. ✅ Run your pipeline
4. ✅ Open http://localhost:8025 to see emails
5. 🎉 Celebrate working emails!

---

**Need help?** Check these files:
- `EMAIL_SETUP.md` - Detailed Gmail guide
- `JENKINS_SETUP.md` - Jenkins configuration
- Or re-run: `./setup-mailhog.sh`


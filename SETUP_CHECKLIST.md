# ✅ Quick Setup Checklist

Copy and paste these exact settings into Jenkins.

## 🔧 Step 1: Fix Reverse Proxy Warning (30 seconds)

1. Open: http://localhost:8080
2. Click: **Manage Jenkins** → **Configure System**
3. Find: **Jenkins Location**
4. Set **Jenkins URL** to exactly: `http://localhost:8080/`
5. Click: **Save**

✅ Done! Warning will disappear.

---

## 📧 Step 2: Configure Email (5 minutes)

### Part A: Extended E-mail Notification

1. Open: http://localhost:8080
2. Click: **Manage Jenkins** → **Configure System**
3. Scroll to: **Extended E-mail Notification**
4. Copy these settings EXACTLY:

```
┌─────────────────────────────────────────────────────────┐
│ SMTP server:              host.docker.internal          │
│ SMTP Port:                1025                          │
│ Credentials:              - none -                      │
│ Default user E-mail:      jenkins@localhost             │
│ ☐ Use SMTP Authentication          (UNCHECK THIS!)     │
│ ☐ Use SSL                           (UNCHECK THIS!)     │
│ ☐ Use TLS                           (UNCHECK THIS!)     │
│ Charset:                  UTF-8                         │
│ Default Recipients:       mohammad.kheirkhah@gritlab.ax │
│ Default Content Type:     HTML (text/html)              │
└─────────────────────────────────────────────────────────┘
```

### Part B: E-mail Notification

5. Scroll down to: **E-mail Notification**
6. Copy these settings EXACTLY:

```
┌─────────────────────────────────────────────────────────┐
│ SMTP server:              host.docker.internal          │
│ Default user e-mail:      jenkins@localhost             │
│ ☐ Use SMTP Authentication          (UNCHECK THIS!)     │
│ ☐ Use SSL                           (UNCHECK THIS!)     │
│ ☐ Use TLS                           (UNCHECK THIS!)     │
│ SMTP Port:                1025                          │
│ Reply-To Address:         jenkins@localhost             │
│ Charset:                  UTF-8                         │
└─────────────────────────────────────────────────────────┘
```

7. Enter test email: `mohammad.kheirkhah@gritlab.ax`
8. Click: **"Test configuration by sending test e-mail"**
9. Should show: "Email was successfully sent" ✅

---

## 📬 Step 3: Verify Email Works

1. Open in browser: **http://localhost:8025**
2. You should see the test email!
3. If you see it: **✅ Success!**

---

## 🚀 Step 4: Test with Pipeline

1. Go to your pipeline: **e-commerce-microservices-ci-cd**
2. Click: **"Build with Parameters"**
3. Set:
   - `BRANCH: main`
   - `DEPLOY_LOCALLY: ✓` (checked)
   - `SKIP_DEPLOY: ✓` (checked)
4. Click: **"Build"**
5. Wait for build to complete
6. Open: **http://localhost:8025**
7. You should see the email! 🎉

---

## ⚠️ Important Notes

### DO NOT:
- ❌ Use `localhost` (it won't work!)
- ❌ Check "Use SMTP Authentication"
- ❌ Check "Use SSL"
- ❌ Check "Use TLS"
- ❌ Add any credentials

### DO:
- ✅ Use exactly: `host.docker.internal`
- ✅ Use port: `1025`
- ✅ Leave authentication UNCHECKED
- ✅ Leave SSL/TLS UNCHECKED

---

## 🆘 If Something Goes Wrong

### Email test fails?

```bash
# Check Mailhog is running
docker ps --filter "name=mailhog"

# If not running
./setup-mailhog.sh
```

### Mailhog not accessible?

```bash
# Restart Mailhog
docker restart mailhog

# Or reinstall
docker stop mailhog && docker rm mailhog
./setup-mailhog.sh
```

### Still not working?

```bash
# Run diagnostic
./check-email-config.sh

# Check Jenkins logs
docker logs jenkins-cicd | tail -50
```

---

## ✅ Success Checklist

Once setup, verify:

- [ ] No reverse proxy warning in Jenkins
- [ ] Test email shows "Email was successfully sent"
- [ ] Test email visible at http://localhost:8025
- [ ] Pipeline build completes successfully
- [ ] Pipeline email visible at http://localhost:8025

---

## 📚 Documentation

- **COMPLETE_SOLUTION.md** - Full explanation
- **EMAIL_SOLUTION.md** - Detailed email guide
- **EMAIL_QUICK_FIX.md** - Gmail alternative
- **EMAIL_SETUP.md** - Comprehensive troubleshooting

---

## 🎯 Quick Reference

| What | Value |
|------|-------|
| Jenkins URL | http://localhost:8080 |
| Mailhog Web UI | http://localhost:8025 |
| SMTP Server | host.docker.internal |
| SMTP Port | 1025 |
| Authentication | None |
| SSL/TLS | None |

---

**That's it!** If all checkboxes are ✅, you're done! 🎉


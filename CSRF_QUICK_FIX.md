# Jenkins CSRF Quick Reference

## 🚨 Problem: Getting 403 "No valid crumb was included in the request"

This error occurs when GitHub webhooks try to trigger Jenkins builds but CSRF protection blocks them.

## ✅ Quick Solutions

### Solution 1: Use Polling (EASIEST - Already Configured!)

**No CSRF configuration needed!**

1. The Jenkinsfile is already set to poll GitHub every 5 minutes
2. Go to Jenkins: http://localhost:8080
3. Click your job: `e-commerce-microservices-ci-cd`
4. Click "Build Now" once (activates the trigger)
5. Done! Builds will trigger automatically within 5 minutes of any push

```bash
# Test it:
git commit -m "test auto build" --allow-empty
git push origin main
# Wait up to 5 minutes, then check Jenkins
```

**Pros:** No setup, no CSRF issues, works behind firewalls
**Cons:** 5-minute delay (usually acceptable)

---

### Solution 2: Enable Proxy Compatibility (For Webhooks)

**For immediate build triggers on push**

#### Quick Steps:

1. **Enable Proxy Compatibility in Jenkins:**
   ```
   http://localhost:8080/configureSecurity/
   → Find "CSRF Protection"
   → ✓ Check "Enable proxy compatibility"
   → Click "Save"
   ```

2. **Setup ngrok (for local development):**
   ```bash
   # Install
   brew install ngrok
   
   # Run
   ngrok http 8080
   
   # Get your URL (e.g., https://xxxx.ngrok-free.app)
   ```

3. **Update Jenkins URL:**
   ```
   http://localhost:8080/configure
   → Find "Jenkins Location"
   → Set "Jenkins URL" to: https://xxxx.ngrok-free.app/
   → Click "Save"
   ```

4. **Configure GitHub Webhook:**
   ```
   https://github.com/mahdikheirkhah/buy-01/settings/hooks
   → Click "Add webhook"
   → Payload URL: https://xxxx.ngrok-free.app/github-webhook/
   → Content type: application/json
   → Just the push event
   → Click "Add webhook"
   ```

5. **Update Jenkinsfile:**
   ```groovy
   triggers {
       githubPush()  // Uncomment this
       // pollSCM('H/5 * * * *')  // Comment out this
   }
   ```

**Pros:** Immediate triggers
**Cons:** Requires public URL, more setup

---

### Solution 3: Disable CSRF (⚠️ NOT RECOMMENDED)

**Only for isolated development environments**

```
http://localhost:8080/configureSecurity/
→ Find "CSRF Protection"
→ ☐ Uncheck "Prevent Cross Site Request Forgery exploits"
→ Click "Save"
```

**⚠️ Security Warning:** This exposes Jenkins to CSRF attacks. Never use in production!

---

## 🔧 Automated Configuration

Use our helper script:

```bash
cd /Users/mohammad.kheirkhah/Desktop/buy-01
./configure-jenkins-csrf.sh
```

Choose:
- Option 1: Configure polling (easiest)
- Option 2: Setup webhooks with ngrok
- Option 3: View current config
- Option 4: Disable CSRF (not recommended)
- Option 5: Reset to defaults

---

## 🔍 Checking Current Configuration

```bash
# Check if CSRF is enabled
docker exec jenkins-cicd cat /var/jenkins_home/config.xml | grep -A 5 "crumbIssuer"

# Check Jenkins URL
docker exec jenkins-cicd cat /var/jenkins_home/jenkins.model.JenkinsLocationConfiguration.xml

# View Jenkins logs
docker logs jenkins-cicd --tail 100

# Check ngrok status (if using webhooks)
curl http://localhost:4040/api/tunnels
```

---

## 🧪 Testing Your Setup

### Test Polling:
```bash
git commit -m "test polling" --allow-empty
git push origin main
# Wait up to 5 minutes, check Jenkins
```

### Test Webhook:
```bash
git commit -m "test webhook" --allow-empty
git push origin main
# Should trigger immediately
```

### Test webhook manually:
```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"ref":"refs/heads/main"}' \
  https://your-jenkins-url/github-webhook/
```

---

## 📊 Which Approach to Use?

| Scenario | Recommendation |
|----------|----------------|
| Local development | Polling (Option 1) |
| Need immediate builds | Webhooks with ngrok (Option 2) |
| Production deployment | Webhooks on public server |
| Behind corporate firewall | Polling (Option 1) |
| Quick testing | Polling (Option 1) |

---

## 🆘 Troubleshooting

### Webhook shows "Connection refused"
- **Cause:** No public URL
- **Fix:** Use ngrok OR switch to polling

### ngrok URL keeps changing
- **Cause:** Free ngrok generates new URLs
- **Fix:** Get ngrok Pro OR use polling

### Still getting 403 errors
1. Verify proxy compatibility is enabled
2. Check Jenkins URL is set correctly
3. Verify webhook URL ends with `/github-webhook/`
4. Check GitHub webhook delivery logs
5. Try polling instead (no CSRF issues)

### Builds not triggering with polling
1. Did you click "Build Now" once? (activates trigger)
2. Check Jenkins system log for errors
3. Verify pollSCM is in triggers section
4. Wait full 5 minutes

---

## 📚 Documentation

- Full guide: `JENKINS_CSRF_SETUP.md`
- Configuration script: `./configure-jenkins-csrf.sh`
- Jenkins docs: https://www.jenkins.io/doc/book/security/csrf-protection/

---

## 🎯 Recommended Setup (TL;DR)

**For 99% of cases, use polling:**

1. Jenkinsfile already configured ✅
2. Go to Jenkins, click your job
3. Click "Build Now" once
4. Done! Builds trigger within 5 minutes

**Only use webhooks if:**
- You need immediate triggers (< 5 min)
- You have a public Jenkins URL
- You've configured CSRF proxy compatibility

---

## 💡 Pro Tips

1. **Polling is perfectly fine** for most CI/CD workflows
2. **5-minute delay** is usually acceptable
3. **Webhooks add complexity** - only use if you really need instant triggers
4. **Never disable CSRF** in production
5. **Use https** for Jenkins in production
6. **Monitor webhook deliveries** in GitHub settings

---

## Quick Commands

```bash
# Start configuration wizard
./configure-jenkins-csrf.sh

# View full CSRF guide
cat JENKINS_CSRF_SETUP.md

# Check Jenkins
open http://localhost:8080

# Check Jenkins logs
docker logs jenkins-cicd -f

# Restart Jenkins
docker restart jenkins-cicd
```

---

**Need Help?** See `JENKINS_CSRF_SETUP.md` for detailed explanations.


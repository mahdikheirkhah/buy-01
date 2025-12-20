# 🚀 GitHub Webhook - Quick Start Card

## 🎯 3 Steps to Automatic Builds

### 1️⃣ Start ngrok
```bash
./setup-webhook.sh
```
**Result:** You'll get a URL like `https://abc123.ngrok.io`

### 2️⃣ Add to GitHub
```
🌐 Go to: github.com/mahdikheirkhah/buy-01/settings/hooks
➕ Click: "Add webhook"
📋 Paste: https://abc123.ngrok.io/github-webhook/
📦 Select: application/json
✅ Click: "Add webhook"
```

### 3️⃣ Test It
```bash
./test-webhook.sh
```
**Result:** Jenkins builds automatically! 🎉

---

## 📖 Documentation

| File | Purpose |
|------|---------|
| `WEBHOOK_SETUP.md` | 📖 Complete step-by-step guide |
| `QUICK_REFERENCE.md` | 📋 Quick reference for all features |
| `WEBHOOK_SETUP_COMPLETE.md` | 🎊 What was done & achieved |
| `setup-webhook.sh` | 🚀 Automated ngrok setup |
| `test-webhook.sh` | 🧪 Test if webhook works |

---

## 🔍 Quick Checks

### ✅ Is ngrok running?
```bash
curl http://localhost:4040
# Should show ngrok web interface
```

### ✅ Is Jenkins running?
```bash
docker ps | grep jenkins-cicd
# Should show jenkins container
```

### ✅ Is webhook working?
```bash
./test-webhook.sh
# Automatically tests everything
```

---

## 🌐 Access URLs

| Service | URL |
|---------|-----|
| Jenkins | http://localhost:8080 |
| ngrok Dashboard | http://localhost:4040 |
| Frontend | http://localhost:4200 |
| API Gateway | https://localhost:8443 |
| Eureka | http://localhost:8761 |

---

## 🎯 Current Setup

```
✅ Automatic Builds:    ON
✅ GitHub Webhooks:     CONFIGURED
✅ Local Deploy:        ON
✅ Docker Publish:      ON
✅ SSH Required:        NO
```

---

## 🔧 Troubleshooting

| Problem | Solution |
|---------|----------|
| Webhook not triggering | Check ngrok is running |
| 502 error | Start Jenkins container |
| 404 error | Check webhook URL format |
| Build doesn't start | Enable "GitHub hook trigger" in Jenkins |

**Full Guide:** See `WEBHOOK_SETUP.md`

---

## 💡 Pro Tips

1. **Keep ngrok running** in a separate terminal
2. **Check ngrok dashboard** at http://localhost:4040 to debug
3. **Use test script** (`./test-webhook.sh`) to verify setup
4. **Free ngrok URLs change** when you restart - update GitHub webhook

---

## 🎉 What You Get

### Before:
```
Push → Open Jenkins → Click Build → Wait
```

### After:
```
Push → Automatic Build! 🚀
```

---

## 📞 Need Help?

1. Run: `./setup-webhook.sh` - Follow the instructions
2. Read: `WEBHOOK_SETUP.md` - Complete guide
3. Test: `./test-webhook.sh` - Verify it works

---

**🎊 Setup complete! Enjoy automatic builds!** 🚀


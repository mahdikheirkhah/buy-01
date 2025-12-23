# Jenkins Gmail Configuration - Visual Guide

## ⚡ QUICK FIX FOR YOUR CURRENT SETUP

### What You Need to Change RIGHT NOW:

**In Extended E-mail Notification:**
```
✅ SMTP server: smtp.gmail.com (CORRECT)
✅ SMTP Port: 465 (CORRECT)
✅ Credentials: Added (CORRECT)
✅ Use SSL: ☑ Checked (CORRECT)
❌ Use TLS: ☐ UNCHECK THIS! (Currently wrong if checked)
✅ Default Recipients: mohammad.kheirkhah@gritlab.ax (CORRECT)
```

**In E-mail Notification (scroll down to this section):**
```
✅ SMTP server: smtp.gmail.com (CORRECT)

Then click "Advanced" button and configure:
☑ Use SMTP Authentication ← MUST BE CHECKED!
   User Name: mahdikheirkhah060@gmail.com
   Password: [Your 16-character Gmail App Password]
   
☑ Use SSL ← MUST BE CHECKED!
☐ Use TLS ← MUST BE UNCHECKED!
   SMTP Port: 465
   Reply-To Address: mohammad.kheirkhah@gritlab.ax
   Charset: UTF-8
```

**Then TEST IT:**
```
In E-mail Notification section, scroll down:
☑ Test configuration by sending test e-mail
   Test e-mail recipient: mohammad.kheirkhah@gritlab.ax
   [Test configuration] ← CLICK THIS!
```

**Expected Result:**
```
✅ "Email was successfully sent"
```

---

## 🎯 Quick Visual Reference

### Where to Configure:

```
Jenkins Dashboard
    ↓
Manage Jenkins
    ↓
System (or Configure System)
    ↓
Scroll down to find these TWO sections:
    ↓
┌─────────────────────────────────────────────┐
│  Extended E-mail Notification               │  ← Configure THIS FIRST
│  ─────────────────────────────────────────  │
│  SMTP server: smtp.gmail.com                │
│                                             │
│  [Advanced...] ← Click this                 │
│      ↓                                      │
│      ☑ Use SMTP Authentication              │
│      User Name: your-email@gmail.com        │
│      Password: [16-char app password]       │
│      ☑ Use SSL                              │
│      SMTP Port: 465                         │
│      Default Recipients: your-email         │
└─────────────────────────────────────────────┘
    ↓
Continue scrolling...
    ↓
┌─────────────────────────────────────────────┐
│  E-mail Notification                        │  ← Configure THIS SECOND
│  ─────────────────────────────────────────  │
│  SMTP server: smtp.gmail.com                │
│                                             │
│  [Advanced...] ← Click this                 │
│      ↓                                      │
│      ☑ Use SMTP Authentication              │
│      User Name: your-email@gmail.com        │
│      Password: [same 16-char app password]  │
│      ☑ Use SSL                              │
│      SMTP Port: 465                         │
│      Reply-To Address: your-email           │
│                                             │
│  ☑ Test configuration by sending test      │
│    e-mail ← Check this!                     │
│    Test e-mail recipient: your-email        │
│    [Test configuration] ← Click this!       │
└─────────────────────────────────────────────┘
```

---

## 📋 Configuration Checklist:

### Before You Start:
- [ ] Gmail 2FA is enabled
- [ ] Gmail App Password created (16 characters)
- [ ] Email address ready: mohammad.kheirkhah@gritlab.ax

### Extended E-mail Notification:
- [ ] SMTP server: `smtp.gmail.com`
- [ ] Clicked "Advanced..."
- [ ] ☑ Use SMTP Authentication checked
- [ ] User Name: `mohammad.kheirkhah@gritlab.ax`
- [ ] Password: `[your-16-char-app-password]`
- [ ] ☑ Use SSL checked
- [ ] SMTP Port: `465`
- [ ] Default Recipients: `mohammad.kheirkhah@gritlab.ax`

### E-mail Notification:
- [ ] SMTP server: `smtp.gmail.com`
- [ ] Clicked "Advanced..."
- [ ] ☑ Use SMTP Authentication checked
- [ ] User Name: `mohammad.kheirkhah@gritlab.ax`
- [ ] Password: `[same-16-char-app-password]`
- [ ] ☑ Use SSL checked
- [ ] SMTP Port: `465`
- [ ] Reply-To: `mohammad.kheirkhah@gritlab.ax`
- [ ] Tested configuration ✅
- [ ] Received test email ✅

### Final Steps:
- [ ] Clicked "Apply"
- [ ] Clicked "Save"
- [ ] Checked email inbox (and spam!)
- [ ] Marked test email as "Not Spam"
- [ ] Pushed code to GitHub to test webhook
- [ ] Received build notification email ✅

---

## 🔧 Exact Values to Use:

```yaml
SMTP Server:        smtp.gmail.com
SMTP Port:          465
Use SSL:            ☑ YES (checked)
Use TLS:            ☐ NO (unchecked)
Authentication:     ☑ YES (checked)
Username:           mohammad.kheirkhah@gritlab.ax
Password:           [16-character Gmail App Password]
Default Recipients: mohammad.kheirkhah@gritlab.ax
Reply-To:           mohammad.kheirkhah@gritlab.ax
Charset:            UTF-8
```

---

## 🎬 Step-by-Step Screenshots Guide:

### Step 1: Access Jenkins Configuration
```
URL: http://localhost:8080/manage
Click: "System" or "Configure System"
```

### Step 2: Find Extended E-mail Notification
```
Action: Scroll down until you see section titled:
        "Extended E-mail Notification"
```

### Step 3: Configure Basic Settings
```
Field: SMTP server
Value: smtp.gmail.com

Then: Click "Advanced..." button (to the right)
```

### Step 4: Configure Authentication
```
Checkbox: ☑ Use SMTP Authentication (CHECK IT!)

Field: User Name
Value: mohammad.kheirkhah@gritlab.ax

Field: Password
Value: [Paste your 16-character Gmail App Password here]
      Example: xxxx xxxx xxxx xxxx (no spaces!)

Checkbox: ☑ Use SSL (CHECK IT!)

Field: SMTP port
Value: 465

Field: Default Recipients
Value: mohammad.kheirkhah@gritlab.ax
```

### Step 5: Configure Standard E-mail Notification
```
Action: Continue scrolling down
Find: Section titled "E-mail Notification"

Repeat: Same configuration as Step 4
```

### Step 6: Test Configuration
```
In "E-mail Notification" section:

Checkbox: ☑ Test configuration by sending test e-mail

Field: Test e-mail recipient
Value: mohammad.kheirkhah@gritlab.ax

Button: Click "Test configuration"

Expected: "Email was successfully sent"
```

### Step 7: Save Configuration
```
Button: Click "Apply" (bottom of page)
Button: Click "Save" (bottom of page)
```

### Step 8: Verify Email Received
```
1. Open Gmail inbox
2. Check spam/junk folder if not in inbox
3. Look for email with subject: "Test email #1"
4. If found: Mark as "Not Spam"
```

---

## 🚨 Common Mistakes to Avoid:

### ❌ DON'T:
- Use your Gmail password (use App Password!)
- Leave spaces in the App Password
- Forget to click "Advanced..."
- Skip the test configuration
- Forget to check spam folder
- Use port 25 or other ports

### ✅ DO:
- Use 16-character Gmail App Password
- Remove all spaces from password
- Check both email sections
- Test before saving
- Check spam folder first
- Use port 465 with SSL

---

## 🔍 Verification Steps:

After configuration, verify each step:

1. **Check Jenkins Logs:**
   ```bash
   docker logs jenkins-cicd | grep -i mail | tail -20
   ```

2. **Test SMTP Connection:**
   ```bash
   ./test-gmail-smtp.sh
   ```

3. **Trigger a Build:**
   ```bash
   git add . && git commit -m "test email" && git push
   ```

4. **Check Email:**
   - Inbox
   - Spam folder
   - All Mail folder

---

## 📱 Quick Reference Card:

```
╔════════════════════════════════════════╗
║  GMAIL SMTP QUICK REFERENCE            ║
╠════════════════════════════════════════╣
║  Server:  smtp.gmail.com               ║
║  Port:    465                          ║
║  SSL:     ✓ Enabled                    ║
║  TLS:     ✗ Disabled                   ║
║  Auth:    ✓ Required                   ║
║  User:    full email address           ║
║  Pass:    16-char App Password         ║
╚════════════════════════════════════════╝
```

---

## 📞 Getting Help:

If stuck:
1. Read: `GMAIL_SETUP.md` (detailed guide)
2. Run: `./test-gmail-smtp.sh` (connectivity test)
3. Check: Jenkins logs for errors
4. Verify: App Password is correct

---

**Total Time:** 5-10 minutes
**Difficulty:** Easy
**Result:** Automatic email notifications on every build! 🎉

---

*Pro Tip: Take a screenshot of your working configuration for future reference!*


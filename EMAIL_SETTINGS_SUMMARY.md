# 📧 Jenkins Email Settings - Complete Configuration

## 🔧 Section 1: Extended E-mail Notification

```
Location: Manage Jenkins → System → Extended E-mail Notification

┌─────────────────────────────────────────────────────┐
│ SMTP server                                         │
│ ┌─────────────────────────────────────────────┐    │
│ │ smtp.gmail.com                              │    │
│ └─────────────────────────────────────────────┘    │
│                                                     │
│ SMTP Port                                          │
│ ┌──────┐                                           │
│ │ 465  │                                           │
│ └──────┘                                           │
│                                                     │
│ [Advanced] ← Click this                            │
│                                                     │
│ Credentials                                        │
│ ┌─────────────────────────────────────────────┐    │
│ │ mahdikheirkhah060@gmail.com/****** (Gmail   │    │
│ │ SMTP Credentials)                           │    │
│ └─────────────────────────────────────────────┘    │
│ [Add]                                              │
│                                                     │
│ ☑ Use SSL                                          │
│ ☐ Use TLS        ← MUST BE UNCHECKED!              │
│ ☐ Use OAuth 2.0                                    │
│                                                     │
│ Charset                                            │
│ ┌──────────┐                                       │
│ │ UTF-8    │                                       │
│ └──────────┘                                       │
│                                                     │
│ Default Recipients                                 │
│ ┌─────────────────────────────────────────────┐    │
│ │ mohammad.kheirkhah@gritlab.ax               │    │
│ └─────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────┘
```

---

## 🔧 Section 2: E-mail Notification

```
Location: Manage Jenkins → System → E-mail Notification
(Scroll down below Extended E-mail Notification)

┌─────────────────────────────────────────────────────┐
│ SMTP server                                         │
│ ┌─────────────────────────────────────────────┐    │
│ │ smtp.gmail.com                              │    │
│ └─────────────────────────────────────────────┘    │
│                                                     │
│ [Advanced] ← Click this button!                    │
│                                                     │
│ ┌─────────────────────────────────────────────┐    │
│ │ Advanced Settings (expanded):               │    │
│ │                                             │    │
│ │ ☑ Use SMTP Authentication                   │    │
│ │   ⚠️ MUST BE CHECKED!                        │    │
│ │                                             │    │
│ │ User Name                                   │    │
│ │ ┌─────────────────────────────────────┐     │    │
│ │ │ mahdikheirkhah060@gmail.com         │     │    │
│ │ └─────────────────────────────────────┘     │    │
│ │                                             │    │
│ │ Password                                    │    │
│ │ ┌─────────────────────────────────────┐     │    │
│ │ │ [Your 16-char Gmail App Password]   │     │    │
│ │ └─────────────────────────────────────┘     │    │
│ │                                             │    │
│ │ ☑ Use SSL    ⚠️ MUST BE CHECKED!            │    │
│ │ ☐ Use TLS    ⚠️ MUST BE UNCHECKED!          │    │
│ │                                             │    │
│ │ SMTP Port                                   │    │
│ │ ┌──────┐                                    │    │
│ │ │ 465  │                                    │    │
│ │ └──────┘                                    │    │
│ │                                             │    │
│ │ Reply-To Address                            │    │
│ │ ┌─────────────────────────────────────┐     │    │
│ │ │ mohammad.kheirkhah@gritlab.ax       │     │    │
│ │ └─────────────────────────────────────┘     │    │
│ │                                             │    │
│ │ Charset                                     │    │
│ │ ┌──────────┐                                │    │
│ │ │ UTF-8    │                                │    │
│ │ └──────────┘                                │    │
│ └─────────────────────────────────────────────┘    │
│                                                     │
│ ☑ Test configuration by sending test e-mail       │
│                                                     │
│ Test e-mail recipient                              │
│ ┌─────────────────────────────────────────────┐    │
│ │ mohammad.kheirkhah@gritlab.ax               │    │
│ └─────────────────────────────────────────────┘    │
│                                                     │
│ [Test configuration] ← CLICK THIS!                 │
└─────────────────────────────────────────────────────┘
```

---

## ✅ Configuration Checklist

### Before Starting:
- [ ] Gmail 2FA enabled
- [ ] Gmail App Password created (16 characters, no spaces)
- [ ] App Password saved securely

### Extended E-mail Notification:
- [ ] SMTP server: `smtp.gmail.com`
- [ ] SMTP Port: `465`
- [ ] Credentials added and selected
- [ ] ☑ Use SSL (checked)
- [ ] ☐ Use TLS (unchecked)
- [ ] Charset: `UTF-8`
- [ ] Default Recipients: `mohammad.kheirkhah@gritlab.ax`

### E-mail Notification:
- [ ] SMTP server: `smtp.gmail.com`
- [ ] Clicked "Advanced" button
- [ ] ☑ Use SMTP Authentication (checked)
- [ ] User Name: `mahdikheirkhah060@gmail.com`
- [ ] Password: [16-char app password entered]
- [ ] ☑ Use SSL (checked)
- [ ] ☐ Use TLS (unchecked)
- [ ] SMTP Port: `465`
- [ ] Reply-To: `mohammad.kheirkhah@gritlab.ax`
- [ ] Charset: `UTF-8`

### Testing:
- [ ] ☑ Test configuration by sending test e-mail (checked)
- [ ] Test recipient: `mohammad.kheirkhah@gritlab.ax`
- [ ] Clicked "Test configuration" button
- [ ] Received success message: "Email was successfully sent"
- [ ] Checked email inbox
- [ ] Received test email
- [ ] Marked as "Not Spam" if needed

### Saving:
- [ ] Clicked "Apply" button
- [ ] Clicked "Save" button
- [ ] No errors displayed

---

## 🚨 Common Issues & Solutions

### Issue 1: "Authentication failed"
**Solution:**
- You're using your regular Gmail password instead of App Password
- Generate a new App Password: https://myaccount.google.com/apppasswords
- Use the 16-character password (remove spaces)

### Issue 2: "Connection timeout"
**Solution:**
- Check that both sections use port 465
- Verify SSL is checked, TLS is unchecked
- Ensure Jenkins can reach smtp.gmail.com (firewall/network)

### Issue 3: "Test email not received"
**Solution:**
- Check spam/junk folder
- Check "All Mail" folder
- Wait 1-2 minutes (may be delayed)
- Mark Jenkins emails as "Not Spam"

### Issue 4: "Use SMTP Authentication" checkbox missing
**Solution:**
- Make sure you clicked "Advanced" button in E-mail Notification section
- Scroll down after clicking Advanced
- The checkbox appears in the expanded section

### Issue 5: Build succeeds but no email sent
**Solution:**
- Verify both sections are configured (not just one)
- Check Jenkins console output for email errors
- Verify recipient email in Jenkinsfile matches: `mohammad.kheirkhah@gritlab.ax`
- Check if "Email was successfully sent" appears in console

---

## 📋 Critical Settings Comparison

| Setting | Extended E-mail | E-mail Notification |
|---------|----------------|---------------------|
| SMTP Server | smtp.gmail.com | smtp.gmail.com |
| SMTP Port | 465 | 465 |
| Authentication | Via Credentials | Via Username/Password |
| Use SSL | ☑ YES | ☑ YES |
| Use TLS | ☐ NO | ☐ NO |
| Username | (in credentials) | mahdikheirkhah060@gmail.com |
| Password | (in credentials) | [16-char app password] |

---

## 🎯 Step-by-Step Actions

1. **Go to:** `http://localhost:8080/manage/configure`

2. **Find Extended E-mail Notification** (scroll down)
   - Set SMTP server: `smtp.gmail.com`
   - Set SMTP Port: `465`
   - Click "Advanced"
   - Select your credentials
   - ☑ Check "Use SSL"
   - ☐ Uncheck "Use TLS"
   - Set Default Recipients: `mohammad.kheirkhah@gritlab.ax`

3. **Continue scrolling to E-mail Notification**
   - Set SMTP server: `smtp.gmail.com`
   - Click "Advanced" button
   - ☑ Check "Use SMTP Authentication"
   - Enter User Name: `mahdikheirkhah060@gmail.com`
   - Enter Password: [your 16-char app password]
   - ☑ Check "Use SSL"
   - ☐ Uncheck "Use TLS"
   - Set SMTP Port: `465`
   - Set Reply-To: `mohammad.kheirkhah@gritlab.ax`

4. **Test Configuration**
   - ☑ Check "Test configuration by sending test e-mail"
   - Enter: `mohammad.kheirkhah@gritlab.ax`
   - Click "Test configuration" button
   - Wait for success message

5. **Save Everything**
   - Click "Apply"
   - Click "Save"

6. **Verify**
   - Check email inbox
   - If in spam, mark as "Not Spam"
   - Push code to GitHub to trigger build
   - Check for build notification email

---

## 📞 Quick Help

**Jenkins System Config:** http://localhost:8080/manage/configure

**Gmail App Password:** https://myaccount.google.com/apppasswords

**Test SMTP:** `./test-gmail-smtp.sh` (if available)

**Jenkins Logs:** `docker logs jenkins-cicd -f | grep -i mail`

**Detailed Guide:** See `GMAIL_SETUP.md`

---

**Last Updated:** December 22, 2025
**Email:** mohammad.kheirkhah@gritlab.ax
**Gmail:** mahdikheirkhah060@gmail.com


# 🎯 Step-by-Step: How to Enable SSH on Your Remote Server

## ❌ What Went Wrong

You tried running Linux commands (`apt`) on your **Mac** - that won't work!
- **Your Mac:** Uses macOS → uses `brew` package manager
- **Remote Server (213.204.48.87):** Uses Linux → uses `apt` package manager

---

## ✅ Correct Process

### Step 1: Find Out Your Hosting Provider

**Which service is hosting your server at 213.204.48.87?**

Common providers:
- DigitalOcean
- AWS (Amazon Web Services)
- Linode
- Vultr
- Hetzner
- Azure
- Google Cloud

**Don't know?** Check:
1. Your email for server setup notifications
2. Your billing/invoices
3. Run this on your Mac:
   ```bash
   whois 213.204.48.87
   ```

---

### Step 2: Access Your Server's Web Console

#### For DigitalOcean:
1. Go to https://cloud.digitalocean.com/
2. Click **Droplets** in left menu
3. Find your droplet (213.204.48.87)
4. Click **Access** → **Launch Droplet Console**

#### For AWS EC2:
1. Go to https://console.aws.amazon.com/ec2/
2. Click **Instances** in left menu
3. Select your instance (213.204.48.87)
4. Click **Connect** → **Session Manager** or **EC2 Instance Connect**

#### For Linode:
1. Go to https://cloud.linode.com/
2. Click your Linode (213.204.48.87)
3. Click **Launch LISH Console** (top right)

#### For Vultr:
1. Go to https://my.vultr.com/
2. Click your instance
3. Click **View Console** button

---

### Step 3: Run Commands IN THE WEB CONSOLE

**⚠️ IMPORTANT:** Type these commands in the **browser console window**, NOT on your Mac terminal!

```bash
# 1. Update package list
sudo apt update

# 2. Install SSH server
sudo apt install openssh-server -y

# 3. Start SSH service
sudo systemctl start ssh
sudo systemctl enable ssh

# 4. Check if SSH is running
sudo systemctl status ssh
# You should see "active (running)" in green

# 5. Allow SSH through firewall
sudo ufw allow 22/tcp
sudo ufw reload

# 6. Verify port is listening
sudo ss -tlnp | grep :22
# Should show something like: LISTEN 0 128 0.0.0.0:22
```

---

### Step 4: Test SSH Connection From Your Mac

**NOW you can run this on your Mac:**

```bash
# Test SSH connection
ssh admin@213.204.48.87

# If it asks "Are you sure you want to continue connecting?", type: yes
```

**Success looks like:**
```
Welcome to Ubuntu 22.04 LTS (GNU/Linux 5.15.0-58-generic x86_64)
admin@server:~$
```

---

### Step 5: Set Up SSH Keys (After SSH Works)

**On your Mac terminal:**

```bash
# Copy your SSH key to server
ssh-copy-id admin@213.204.48.87

# Test passwordless login
ssh admin@213.204.48.87
# Should log in without asking for password
```

---

### Step 6: Add Jenkins SSH Key

**Get Jenkins public key:**
```bash
docker exec jenkins-cicd cat /root/.ssh/id_rsa.pub
```

**Add to server (run on server via SSH):**
```bash
ssh admin@213.204.48.87
mkdir -p ~/.ssh
echo "PASTE_JENKINS_PUBLIC_KEY_HERE" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
chmod 700 ~/.ssh
exit
```

**Test Jenkins can connect:**
```bash
docker exec jenkins-cicd ssh -o StrictHostKeyChecking=no admin@213.204.48.87 "echo 'Jenkins SSH working!'"
```

---

## 🆘 Still Can't Access Web Console?

### Option A: Contact Support

Email/chat your hosting provider's support:

> Subject: Need SSH access to server 213.204.48.87
> 
> Hello,
> 
> I cannot SSH to my server at 213.204.48.87 (port 22 connection refused).
> 
> Could you please:
> 1. Enable SSH service on this server
> 2. Ensure port 22 is open in the firewall
> 3. Provide web console access if possible
> 
> Thank you!

### Option B: Check Cloud Provider Firewall

#### DigitalOcean Firewall:
1. Go to **Networking** → **Firewalls**
2. Find firewall applied to your droplet
3. Add inbound rule: **SSH (TCP 22)** from **All IPv4**

#### AWS Security Groups:
1. Go to **EC2** → **Security Groups**
2. Select security group for your instance
3. Click **Edit inbound rules**
4. Add rule: **Type: SSH**, **Port: 22**, **Source: 0.0.0.0/0**

#### Azure Network Security Group:
1. Go to **Virtual Machines** → Select VM
2. Click **Networking** → **Add inbound port rule**
3. **Destination port ranges:** 22
4. **Protocol:** TCP
5. **Name:** Allow-SSH

---

## 📋 Summary

### What to do on YOUR MAC (macOS):
✅ `ssh admin@213.204.48.87` - Test connection
✅ `ssh-copy-id admin@213.204.48.87` - Copy your SSH key
✅ `docker exec jenkins-cicd ...` - Jenkins commands
❌ `apt` commands - DON'T RUN THESE!

### What to do on REMOTE SERVER (Linux):
✅ `sudo apt install openssh-server` - Install SSH
✅ `sudo systemctl start ssh` - Start SSH
✅ `sudo ufw allow 22/tcp` - Open firewall
❌ Run these on your Mac - WON'T WORK!

---

## 🎯 Your Current Step

**You are HERE:** ⬇️

```
┌─────────────────────────────────────────────┐
│ Step 1: Find your hosting provider          │ ← START HERE
├─────────────────────────────────────────────┤
│ Step 2: Access web console                  │
├─────────────────────────────────────────────┤
│ Step 3: Run SSH setup commands IN CONSOLE   │
├─────────────────────────────────────────────┤
│ Step 4: Test from Mac: ssh admin@...        │
├─────────────────────────────────────────────┤
│ Step 5: Copy SSH keys                       │
├─────────────────────────────────────────────┤
│ Step 6: Add Jenkins key                     │
├─────────────────────────────────────────────┤
│ Step 7: Run Jenkins pipeline! 🚀            │
└─────────────────────────────────────────────┘
```

---

**Next Action:** Find out which hosting provider you use, then access the web console to run the SSH setup commands.

Need help identifying your provider? Run on your Mac:
```bash
whois 213.204.48.87 | grep -i "org\|name"
```


# 🚨 URGENT: SSH Access Issue

## Current Problem
❌ **Cannot connect to deployment server:** `ssh admin@213.204.48.87`  
❌ **Error:** `Connection refused on port 22`  
✅ **Server is reachable:** Ping works (5-11ms response time)

---

## ⚠️ IMPORTANT: These Are Remote Server Commands

**DO NOT run these commands on your Mac!** 

The commands below must be executed on your **deployment server (213.204.48.87)** via your hosting provider's web console/terminal.

Your Mac uses **macOS** (uses `brew`, not `apt`). Your deployment server uses **Linux** (uses `apt`).

---

## What This Means
SSH service is **not running** or **port 22 is blocked** on your deployment server (213.204.48.87).

---

## 🔧 Quick Fix Options

### Option 1: Enable SSH via Web Console (Fastest) ⭐ RECOMMENDED

**🖥️ WHERE TO RUN THESE:** On your **remote Linux server** via web console

1. **Log into your hosting provider's dashboard:**
   - DigitalOcean: https://cloud.digitalocean.com/
   - AWS: https://console.aws.amazon.com/
   - Azure: https://portal.azure.com/
   - Google Cloud: https://console.cloud.google.com/

2. **Find your server** (213.204.48.87) and click **"Access Console"** or **"Connect via Browser"**

3. **In the web console terminal, run these commands:**
   ```bash
   # ⚠️ RUN THESE ON THE REMOTE SERVER, NOT YOUR MAC!
   
   # Install SSH
   sudo apt update && sudo apt install openssh-server -y
   
   # Start SSH
   sudo systemctl start ssh
   sudo systemctl enable ssh
   
   # Open firewall
   sudo ufw allow 22/tcp
   sudo ufw status
   ```

4. **Test from your Mac:**
   ```bash
   ssh admin@213.204.48.87
   ```

### Option 2: Check Cloud Provider Security Rules

- **AWS:** EC2 → Security Groups → Add inbound rule for SSH (port 22)
- **DigitalOcean:** Networking → Firewalls → Allow SSH
- **Azure:** NSG → Add inbound SSH rule
- **Google Cloud:** Firewall Rules → Allow TCP port 22

---

## 📋 Once SSH Works, Do This:

### 1. Copy your SSH key
```bash
ssh-copy-id admin@213.204.48.87
```

### 2. Copy Jenkins SSH key
```bash
# Get Jenkins public key
docker exec jenkins-cicd cat /root/.ssh/id_rsa.pub

# Add to server
ssh admin@213.204.48.87
echo "PASTE_JENKINS_PUBLIC_KEY_HERE" >> ~/.ssh/authorized_keys
exit
```

### 3. Prepare deployment directory
```bash
ssh admin@213.204.48.87
sudo mkdir -p /opt/ecommerce
sudo chown admin:admin /opt/ecommerce
exit
```

### 4. Copy deployment files
```bash
scp docker-compose.yml admin@213.204.48.87:/opt/ecommerce/
scp deployment.env admin@213.204.48.87:/opt/ecommerce/.env
```

### 5. Install Docker on server
```bash
ssh admin@213.204.48.87
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
exit
```

### 6. Configure Jenkins
- Add SSH credential with ID: `deployment-ssh-key`
- Add Docker Hub credential with ID: `dockerhub-creds`
- Create pipeline job pointing to your GitHub repo

### 7. Run your first deployment!
Click "Build Now" in Jenkins 🚀

---

## 📚 Full Documentation

- **SSH_TROUBLESHOOTING.md** - Complete troubleshooting guide
- **JENKINS_CONTAINER_ACCESS.md** - How to work with Jenkins container
- **JENKINS_SETUP.md** - Complete Jenkins setup instructions

---

## 🆘 Need Help?

Contact your hosting provider support and ask them to:
1. ✅ Enable SSH on server 213.204.48.87
2. ✅ Ensure port 22 is open in firewall
3. ✅ Provide web console access if SSH can't be enabled immediately

---

**Status:** Waiting for SSH to be enabled on deployment server before Jenkins pipeline can deploy.


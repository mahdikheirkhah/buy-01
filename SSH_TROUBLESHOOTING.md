# SSH Connection Troubleshooting Guide

## Current Issue: Connection Refused to 213.204.48.87:22

### Problem Diagnosed
✅ Server is **reachable** (ping works)  
❌ SSH port 22 is **closed/blocked**  
❌ Common alternative SSH ports (2222, 22222, 8022, 10022) are also **closed**

---

## Solutions

### Option 1: Enable SSH on the Remote Server (Recommended)

You need to access the server through its web console (VPS provider dashboard, AWS Console, etc.) and:

#### For Ubuntu/Debian:
```bash
# 1. Install OpenSSH Server
sudo apt update
sudo apt install openssh-server -y

# 2. Enable and start SSH service
sudo systemctl enable ssh
sudo systemctl start ssh

# 3. Check status
sudo systemctl status ssh

# 4. Allow SSH through firewall
sudo ufw allow 22/tcp
sudo ufw reload

# 5. Verify SSH is listening
sudo netstat -tlnp | grep :22
```

#### For RHEL/CentOS/Rocky:
```bash
# 1. Install OpenSSH Server
sudo dnf install openssh-server -y

# 2. Enable and start SSH service
sudo systemctl enable sshd
sudo systemctl start sshd

# 3. Check status
sudo systemctl status sshd

# 4. Allow SSH through firewall
sudo firewall-cmd --permanent --add-service=ssh
sudo firewall-cmd --reload

# 5. Verify SSH is listening
sudo ss -tlnp | grep :22
```

---

### Option 2: Configure Custom SSH Port

If port 22 is intentionally blocked and SSH runs on a different port:

1. **Find the actual SSH port** (check with your hosting provider)
2. **Update your SSH command:**
   ```bash
   ssh -p CUSTOM_PORT admin@213.204.48.87
   ```
3. **Copy SSH key with custom port:**
   ```bash
   ssh-copy-id -p CUSTOM_PORT admin@213.204.48.87
   ```

---

### Option 3: Check Cloud Provider Firewall/Security Groups

#### AWS EC2:
1. Go to EC2 Dashboard → Security Groups
2. Find the security group attached to your instance
3. Add inbound rule:
   - Type: SSH
   - Protocol: TCP
   - Port: 22
   - Source: Your IP or 0.0.0.0/0 (for testing only!)

#### Digital Ocean:
1. Go to Networking → Firewalls
2. Add inbound rule for SSH (port 22)

#### Azure:
1. Go to Network Security Groups
2. Add inbound security rule for port 22

#### Google Cloud:
1. Go to VPC Network → Firewall Rules
2. Create firewall rule allowing TCP port 22

---

### Option 4: Use Alternative Access Methods

If you can't enable SSH immediately:

#### A. Use your VPS provider's web console
Most providers offer a browser-based terminal (VNC/Serial Console)

#### B. Use a bastion host/jump server
```bash
ssh -J bastion-user@bastion-host admin@213.204.48.87
```

---

## Once SSH is Working

### 1. Copy your SSH key from your Mac:
```bash
ssh-copy-id admin@213.204.48.87
```

### 2. Copy Jenkins container's SSH key:
```bash
# Get Jenkins public key
docker exec jenkins-cicd cat /root/.ssh/id_rsa.pub

# Manually add it to the server
ssh admin@213.204.48.87
echo "paste-jenkins-public-key-here" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
exit
```

### 3. Test Jenkins SSH connection:
```bash
# From Jenkins container
docker exec -it jenkins-cicd bash
ssh -o StrictHostKeyChecking=no admin@213.204.48.87 "echo 'SSH working!'"
exit
```

---

## Update Jenkinsfile Configuration

Once SSH is working, update these values in `Jenkinsfile`:

```groovy
REMOTE_USER = 'admin'              // ✅ Correct username
REMOTE_HOST = '213.204.48.87'     // ✅ Correct IP
SSH_CREDENTIAL_ID = 'deployment-ssh-key'  // Jenkins credential ID
DEPLOYMENT_DIR = '/opt/ecommerce'  // Where to deploy on remote server
```

---

## Verify Everything Works

### Test 1: Manual SSH from your Mac
```bash
ssh admin@213.204.48.87
```

### Test 2: SSH from Jenkins container
```bash
docker exec jenkins-cicd ssh admin@213.204.48.87 "whoami"
```

### Test 3: Full deployment test
```bash
docker exec jenkins-cicd ssh admin@213.204.48.87 "
  cd /opt/ecommerce && \
  docker compose ps
"
```

---

## Next Steps for Jenkins Deployment

After SSH is working:

1. ✅ **Prepare remote server:**
   ```bash
   ssh admin@213.204.48.87
   sudo mkdir -p /opt/ecommerce
   sudo chown admin:admin /opt/ecommerce
   cd /opt/ecommerce
   ```

2. ✅ **Copy deployment files to server:**
   ```bash
   scp docker-compose.yml admin@213.204.48.87:/opt/ecommerce/
   scp .env admin@213.204.48.87:/opt/ecommerce/
   ```

3. ✅ **Create `.env` file on remote server:**
   ```bash
   ssh admin@213.204.48.87
   cd /opt/ecommerce
   cat > .env << 'EOF'
   IMAGE_TAG=latest
   DOCKER_REPO=mahdikheirkhah
   EOF
   ```

4. ✅ **Install Docker on remote server** (if not installed):
   ```bash
   ssh admin@213.204.48.87
   curl -fsSL https://get.docker.com | sh
   sudo usermod -aG docker $USER
   ```

5. ✅ **Test deployment manually first:**
   ```bash
   ssh admin@213.204.48.87
   cd /opt/ecommerce
   docker compose pull
   docker compose up -d
   ```

6. ✅ **Configure Jenkins credentials:**
   - Add SSH private key as credential ID: `deployment-ssh-key`
   - Add Docker Hub credentials as: `dockerhub-creds`

7. ✅ **Run Jenkins pipeline!**

---

## Common Errors & Solutions

### "Permission denied (publickey)"
```bash
# Your public key isn't on the server
ssh-copy-id admin@213.204.48.87
```

### "Host key verification failed"
```bash
# Remove old host key
ssh-keygen -R 213.204.48.87
# Then reconnect
ssh admin@213.204.48.87
```

### "Connection timeout"
```bash
# Check if server firewall allows your IP
# Or if SSH is running on different port
```

### Docker commands fail on remote server
```bash
# Add user to docker group
ssh admin@213.204.48.87 "sudo usermod -aG docker $USER"
# Log out and back in for group changes to take effect
```

---

## Security Best Practices

1. ✅ **Use SSH keys** (not passwords)
2. ✅ **Disable root SSH login:** `PermitRootLogin no` in `/etc/ssh/sshd_config`
3. ✅ **Use strong passwords/keys**
4. ✅ **Enable fail2ban** to prevent brute force attacks
5. ✅ **Use a non-standard SSH port** (security through obscurity)
6. ✅ **Keep SSH updated:** `sudo apt update && sudo apt upgrade openssh-server`
7. ✅ **Use firewall rules** to limit SSH access to specific IPs

---

## Contact Your Hosting Provider

If none of the above works, contact your VPS/hosting provider support:
- Ask them to **enable SSH access** on your server
- Ask for the **correct SSH port** if not 22
- Ask them to **check firewall rules**
- Request access to the **web console/terminal**

---

**Current Status:**
- ❌ SSH not accessible on 213.204.48.87:22
- ⏳ Waiting for SSH to be enabled on remote server
- 📋 Jenkins pipeline ready once SSH is configured


# Jenkins Container Access Guide

## ✅ Correct Way to Access Jenkins Container

You're trying to use `su - jenkins-cicd`, but that's incorrect because `jenkins-cicd` is the **container name**, not a system user.

### Access the Container Shell

```bash
# Enter the Jenkins container
docker exec -it jenkins-cicd bash

# You'll see a prompt like:
# root@e0a6addb5d24:/#
```

### Common Commands Inside Jenkins Container

```bash
# 1. View Jenkins home directory
ls -la /var/jenkins_home

# 2. Get initial admin password
cat /var/jenkins_home/secrets/initialAdminPassword

# 3. Check Jenkins logs
tail -f /var/jenkins_home/jenkins.log

# 4. Install Docker CLI (if needed)
apt-get update && apt-get install -y docker.io

# 5. Test Docker access
docker ps

# 6. Exit the container
exit
```

## SSH Key Management (For Deployment)

### Generate SSH Key (Already Done!)

You already generated an SSH key. To view it:

```bash
# Inside the container
docker exec -it jenkins-cicd bash

# View public key
cat /root/.ssh/id_rsa.pub

# Copy this key to your deployment server
```

### Add Public Key to Deployment Server

```bash
# On your deployment server (not in container)
# Add the public key from Jenkins container to authorized_keys
echo "ssh-rsa AAAA..." >> ~/.ssh/authorized_keys
```

### Configure Jenkins SSH Credentials

1. Go to Jenkins UI: http://localhost:8080
2. Navigate to: **Manage Jenkins → Credentials → System → Global credentials**
3. Click **Add Credentials**
4. Select **SSH Username with private key**
5. Fill in:
   - **ID**: `deployment-ssh-key` (must match Jenkinsfile)
   - **Username**: `your-deploy-server-username`
   - **Private Key**: Select "Enter directly"
   - Copy from container:
     ```bash
     docker exec jenkins-cicd cat /root/.ssh/id_rsa
     ```
   - Paste the entire private key (including BEGIN and END lines)
6. Click **OK**

## Quick Reference Commands

### View Container Status
```bash
docker ps -a --filter name=jenkins-cicd
```

### View Container Logs
```bash
docker logs jenkins-cicd
docker logs -f jenkins-cicd  # Follow logs
```

### Stop Jenkins Container
```bash
docker stop jenkins-cicd
```

### Start Jenkins Container
```bash
docker start jenkins-cicd
```

### Restart Jenkins Container
```bash
docker restart jenkins-cicd
```

### Remove Jenkins Container (keeps data in volume)
```bash
docker stop jenkins-cicd
docker rm jenkins-cicd
```

### Remove Everything Including Data
```bash
docker compose -f jenkins-docker-compose.yml down -v
```

## Docker Access From Jenkins

The Jenkins container is configured to access your host's Docker daemon via the socket mount:

```yaml
volumes:
  - /var/run/docker.sock:/var/run/docker.sock
```

This means Jenkins can build and push Docker images using your host's Docker.

### Test Docker Access

```bash
# Inside Jenkins container
docker exec -it jenkins-cicd bash
docker ps
docker images
```

## File Structure Inside Container

```
/var/jenkins_home/
├── config.xml                    # Jenkins configuration
├── credentials.xml               # Stored credentials
├── jobs/                         # Pipeline jobs
│   └── ecommerce-microservices/  # Your pipeline job
├── secrets/                      
│   └── initialAdminPassword      # Initial admin password
├── workspace/                    # Build workspaces
│   └── ecommerce-microservices/  # Your project workspace
└── plugins/                      # Installed plugins
```

## Troubleshooting

### Can't Access Jenkins UI

```bash
# Check if container is running
docker ps -a --filter name=jenkins-cicd

# Check logs
docker logs jenkins-cicd

# Restart container
docker restart jenkins-cicd
```

### Docker Commands Fail Inside Container

```bash
# Install Docker CLI
docker exec -it jenkins-cicd bash
apt-get update && apt-get install -y docker.io

# Verify access
docker ps
```

### Permission Denied on Docker Socket

```bash
# On host machine, give permissions
sudo chmod 666 /var/run/docker.sock

# Or better: add Jenkins user to docker group (inside container)
docker exec -it jenkins-cicd bash
groupadd -g $(stat -c '%g' /var/run/docker.sock) docker
usermod -aG docker jenkins
```

### Reset Jenkins (Start Fresh)

```bash
# Stop and remove container + data
docker compose -f jenkins-docker-compose.yml down -v

# Start fresh
docker compose -f jenkins-docker-compose.yml up -d

# Get new password
docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword
```

## Next Steps

1. ✅ You're already in the Jenkins container
2. ✅ SSH key is generated
3. 🔄 Copy public key to deployment server
4. 🔄 Add SSH credentials in Jenkins UI
5. 🔄 Add Docker Hub credentials in Jenkins UI
6. 🔄 Create pipeline job
7. 🔄 Run first build

---

**Remember**: Never use `su - jenkins-cicd`. Always use `docker exec -it jenkins-cicd bash` to access the container!


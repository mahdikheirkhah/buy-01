# Jenkins Setup Guide for Local Development

## Quick Start with Docker (Recommended)

### 1. Start Jenkins Container

```bash
cd /Users/mohammad.kheirkhah/Desktop/buy-01
docker compose -f jenkins-docker-compose.yml up -d
```

### 2. Get Initial Admin Password

```bash
docker exec jenkins-cicd cat /var/jenkins_home/secrets/initialAdminPassword
```

Copy this password - you'll need it for the web setup.

### 3. Access Jenkins

Open your browser and go to: http://localhost:8080

- Paste the initial admin password
- Install suggested plugins
- Create your first admin user
- Start using Jenkins!

### 4. Configure Jenkins for Your Project

#### A. Install Required Plugins

Go to **Manage Jenkins → Plugins → Available Plugins** and install:
- Docker Pipeline
- Git plugin (usually pre-installed)
- SSH Agent Plugin (for deployment)

#### B. Add Docker Hub Credentials

1. Go to **Manage Jenkins → Credentials → System → Global credentials**
2. Click **Add Credentials**
3. Choose **Username with password**
4. Set:
   - **ID**: `dockerhub-creds` (must match Jenkinsfile)
   - **Username**: `mahdikheirkhah`
   - **Password**: Your Docker Hub Personal Access Token
   - **Description**: Docker Hub Credentials

#### C. Create a New Pipeline Job

1. Click **New Item**
2. Enter name: `ecommerce-microservices`
3. Choose **Pipeline**
4. Click **OK**
5. In the **Pipeline** section:
   - **Definition**: Pipeline script from SCM
   - **SCM**: Git
   - **Repository URL**: `https://github.com/mahdikheirkhah/buy-01.git`
   - **Branch**: `*/main`
   - **Script Path**: `Jenkinsfile`
6. Click **Save**

### 5. Run Your First Build

Click **Build Now** and watch the pipeline execute!

---

## Alternative: Install Jenkins Directly on macOS

If you prefer to install Jenkins as a native service:

```bash
# Install via Homebrew
brew install jenkins-lts

# Start Jenkins
brew services start jenkins-lts

# Access at http://localhost:8080
# Get initial password:
cat /usr/local/var/jenkins_home/secrets/initialAdminPassword
```

---

## Troubleshooting

### Jenkins can't access Docker

If you're using Docker-in-Docker, ensure the Jenkins container has access to the Docker socket:

```bash
# Already configured in jenkins-docker-compose.yml
# The volume mount: /var/run/docker.sock:/var/run/docker.sock
```

### Permission Issues

If Jenkins can't execute Docker commands:

```bash
# Enter Jenkins container
docker exec -it jenkins-cicd bash

# Install Docker CLI inside Jenkins container
apt-get update && apt-get install -y docker.io

# Or use the jenkins/jenkins:lts-jdk11 image which includes Docker
```

### Build Fails on Maven

The Jenkinsfile expects `./mvnw` in the project root. Make sure your Maven wrapper exists:

```bash
cd /Users/mohammad.kheirkhah/Desktop/buy-01
ls -la mvnw  # Should exist
```

If not, you need to add Maven wrapper to your project.

---

## Without Jenkins (Alternative: Use deploy.sh directly)

If you don't want to set up Jenkins right now, you can use the `deploy.sh` script directly:

```bash
cd /Users/mohammad.kheirkhah/Desktop/buy-01

# Set environment variables
export DOCKER_USERNAME=mahdikheirkhah
export DOCKER_PASSWORD=your_docker_token_here
export IMAGE_TAG=v1.0.0

# Run deployment
bash deploy.sh
```

This achieves the same result as the Jenkins pipeline but manually.

---

## Next Steps

1. ✅ Choose Jenkins setup method (Docker or native)
2. ✅ Configure credentials
3. ✅ Create pipeline job
4. ✅ Run first build
5. 🔄 Set up remote deployment server (optional)
6. 🔄 Configure SSH credentials for deployment stage

---

**Note**: The Jenkinsfile deployment stage requires a remote server. If you're just testing locally, you can skip that stage or modify it to deploy locally.


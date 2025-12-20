# Jenkins Pipeline - Deployment Options

## What Changed?

### Before:
- Only remote deployment (SSH required)
- Pipeline failed because SSH wasn't configured
- No easy way to deploy locally

### After:
- ✅ **Local deployment** (default, no SSH needed)
- ✅ Remote deployment (optional, requires SSH)
- Clear separation between the two approaches

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      JENKINS MACHINE                         │
│                                                              │
│  ┌─────────────┐        ┌──────────────┐                   │
│  │   Jenkins   │───────▶│    Docker    │                   │
│  │   Pipeline  │        │    Engine    │                   │
│  └─────────────┘        └──────────────┘                   │
│         │                       │                           │
│         │                       │ LOCAL DEPLOYMENT          │
│         │                       ▼                           │
│         │              ┌─────────────────┐                 │
│         │              │  Your Services  │                 │
│         │              │  (Containers)   │                 │
│         │              └─────────────────┘                 │
│         │                                                   │
│         │ REMOTE DEPLOYMENT (SSH)                          │
│         │                                                   │
└─────────┼───────────────────────────────────────────────────┘
          │
          │ SSH Connection
          │ (Optional)
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│                     REMOTE SERVER                            │
│                   (192.168.1.100)                           │
│                                                              │
│              ┌─────────────────┐                            │
│              │  Your Services  │                            │
│              │  (Containers)   │                            │
│              └─────────────────┘                            │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## Deployment Flow Comparison

### Local Deployment (DEPLOY_LOCALLY=true)

```
1. Jenkins builds code
2. Jenkins creates Docker images
3. Jenkins pushes to DockerHub
4. Jenkins runs: docker compose up -d
   ↓
5. Containers start on SAME machine
6. Access: http://localhost:4200
```

**No SSH needed** ✅

---

### Remote Deployment (SKIP_DEPLOY=false)

```
1. Jenkins builds code
2. Jenkins creates Docker images
3. Jenkins pushes to DockerHub
4. Jenkins connects via SSH
   ↓
5. Copies docker-compose.yml to remote
6. Runs: docker compose up -d on remote
   ↓
7. Containers start on REMOTE machine
8. Access: http://192.168.1.100:4200
```

**Requires SSH setup** ⚠️

---

## Pipeline Parameters Explained

| Parameter | Default | Description |
|-----------|---------|-------------|
| `DEPLOY_LOCALLY` | `true` | Deploy to Jenkins machine (no SSH) |
| `SKIP_DEPLOY` | `true` | Skip remote deployment |
| `RUN_TESTS` | `false` | Run tests (currently disabled) |
| `RUN_SONAR` | `false` | Run SonarQube analysis |
| `BRANCH` | `main` | Git branch to build |

---

## Common Scenarios

### Scenario 1: Development (Current Setup)
**Goal**: Build and run locally on Jenkins machine

**Parameters**:
```
DEPLOY_LOCALLY = true
SKIP_DEPLOY = true
```

**Result**:
- ✅ Builds and deploys locally
- ✅ No SSH needed
- ✅ Fast and simple

---

### Scenario 2: Production Deployment
**Goal**: Build on Jenkins, deploy to production server

**Parameters**:
```
DEPLOY_LOCALLY = false
SKIP_DEPLOY = false
```

**Requirements**:
- ⚠️ SSH configured
- ⚠️ Credentials added to Jenkins
- ⚠️ Remote server accessible

---

### Scenario 3: Build Only (No Deployment)
**Goal**: Just build and publish images

**Parameters**:
```
DEPLOY_LOCALLY = false
SKIP_DEPLOY = true
```

**Result**:
- ✅ Builds code
- ✅ Publishes to DockerHub
- ❌ No deployment

---

## Why Two Deployment Methods?

### Local Deployment (Simple)
**Pros**:
- No SSH configuration
- Faster deployment
- Easier troubleshooting
- Perfect for development

**Cons**:
- Only works on Jenkins machine
- Not suitable for production

---

### Remote Deployment (Production-Ready)
**Pros**:
- Separates build and runtime
- Can deploy to multiple servers
- Production-ready
- Better security

**Cons**:
- Requires SSH setup
- More complex
- Harder to troubleshoot

---

## Next Steps

### For Development (Now):
1. ✅ Keep current settings (DEPLOY_LOCALLY=true)
2. ✅ Run pipeline
3. ✅ Access app at localhost:4200

### For Production (Later):
1. Set up SSH keys
2. Add Jenkins credentials
3. Change parameters
4. Deploy to remote server

---

## Questions?

**Q: Do I need SSH for local deployment?**
A: **No!** Local deployment works without SSH.

**Q: Can I use both local and remote deployment?**
A: No, enable only one at a time.

**Q: Why did previous builds fail?**
A: Because SKIP_DEPLOY was false and SSH wasn't configured.

**Q: Is local deployment secure?**
A: For development, yes. For production, use remote deployment with proper security.

**Q: Can I deploy to multiple servers?**
A: Yes, but you'll need to modify the pipeline to loop through multiple servers.


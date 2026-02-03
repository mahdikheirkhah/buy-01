# SonarQube Project Key Fix Guide

## Problem

SonarQube is showing projects with incorrect keys:

- `buy01-backend` instead of `buy-01-backend`
- `buy01-frontend` instead of `buy-01-frontend`

## Root Causefkfkfk

The projects were likely created without explicit project keys, and SonarQube auto-generated keys based on directory names or Maven artifactId (which is "backend" in pom.xml).

## Solution

### Option 1: Automated Script (Recommended)

```bash
# Get your SonarQube token first
# Go to: http://localhost:9000 → My Account → Security → Generate Token

# Run the fix script
./fix-sonarqube-projects.sh YOUR_SONARQUBE_TOKEN
```

### Option 2: Manual Fix via SonarQube UI

#### Step 1: Delete Old Projects

1. Open SonarQube: http://localhost:9000
2. Login (default: admin/admin)
3. Go to **Administration** → **Projects** → **Management**
4. Find and delete:
   - `buy01-backend`
   - `buy01-frontend`

#### Step 2: Verify Configuration Files

The following files have been created/updated with correct project keys:

✅ `/backend/pom.xml` - Added:

```xml
<sonar.projectKey>buy-01-backend</sonar.projectKey>
<sonar.projectName>Buy-01 Backend</sonar.projectName>
```

✅ `/backend/sonar-project.properties` - Created with:

```properties
sonar.projectKey=buy-01-backend
sonar.projectName=Buy-01 Backend
```

✅ `/frontend/sonar-project.properties` - Already correct:

```properties
sonar.projectKey=buy-01-frontend
sonar.projectName=Frontend
```

#### Step 3: Trigger New Analysis

Option A - From Jenkins:

1. Go to Jenkins: http://localhost:8080
2. Select your pipeline job
3. Click "Build Now"
4. Wait for SonarQube Analysis stage to complete

Option B - Manually from command line:

```bash
# Backend
cd backend
mvn clean install sonar:sonar \
  -Dsonar.projectKey=buy-01-backend \
  -Dsonar.projectName="Buy-01 Backend" \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=YOUR_TOKEN

# Frontend
cd frontend
npm run sonar -- -Dsonar.login=YOUR_TOKEN
```

#### Step 4: Verify

1. Go to SonarQube: http://localhost:9000/projects
2. You should now see:
   - ✅ `buy-01-backend` (Buy-01 Backend)
   - ✅ `buy-01-frontend` (Frontend)

## Changes Made

### Files Created:

- `backend/sonar-project.properties` - Backend SonarQube configuration
- `fix-sonarqube-projects.sh` - Automated fix script

### Files Modified:

- `backend/pom.xml` - Added SonarQube properties in `<properties>` section

### Configuration Updated:

- Backend project key: `buy-01-backend`
- Frontend project key: `buy-01-frontend`
- Jenkins pipeline already uses correct keys in Jenkinsfile

## Verification Commands

Check if SonarQube container is running:

```bash
docker ps | grep sonarqube
```

List current projects via API:

```bash
curl -s -u admin:admin http://localhost:9000/api/projects/search | jq '.components[] | {key: .key, name: .name}'
```

Delete project via API (if needed):

```bash
curl -u admin:admin -X POST "http://localhost:9000/api/projects/delete?project=buy01-backend"
curl -u admin:admin -X POST "http://localhost:9000/api/projects/delete?project=buy01-frontend"
```

## Next Steps

After fixing the project keys:

1. ✅ Problem 1 (Project keys) - SOLVED
2. 🔧 Problem 2 (GitHub-Jenkins-SonarQube connection) - Next to address

The connection issue will be addressed separately after confirming project keys are correct.

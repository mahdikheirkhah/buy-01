# Quick Start Audit Testing Guide

## Phase 1: Quick Verification (15 minutes) ✅

### 1. Check Infrastructure Status

```bash
# All these should be running:
docker ps | grep -E "jenkins|sonarqube|frontend|user-service|product-service|media-service"

# Expected:
# ✅ jenkins-cicd:latest (port 8080)
# ✅ sonarqube:latest (port 9000)
# ✅ frontend:latest (port 4200)
# ✅ user-service (port 8001)
# ✅ product-service (port 8002)
# ✅ media-service (port 8003)
```

### 2. Test Jenkins Pipeline

```bash
# Open browser: http://localhost:8080
# Login with Jenkins credentials
# Check: Buy-01-Pipeline job
# - Last build status: ✅ SUCCESS (blue ball)
# - Build duration: Should be under 10 minutes
# - Console Output: Shows all stages completed
```

**Expected Stages to Pass:**

- ✅ Checkout (from Gitea)
- ✅ Build Backend (Maven)
- ✅ Build Frontend (npm)
- ✅ Test Backend (unit tests)
- ✅ Test Frontend (karma tests)
- ✅ SonarQube Analysis
- ✅ Dockerize & Push (images pushed)
- ✅ Deploy Locally (containers started)

### 3. Test SonarQube

```bash
# Open browser: http://localhost:9000
# Login with credentials (admin/admin)
# Check Projects:
# - buy-01-backend (Java/Spring code)
# - buy-01-frontend (Angular/TypeScript code)
```

**Expected to See:**

- Projects with recent analysis
- Quality gate status
- Issues/Code smells/Security hotspots
- Code coverage percentage

---

## Phase 2: Application Testing (30 minutes)

### 1. Test Frontend Access

```bash
# Open browser: http://localhost:4200
# You should see:
✅ Buy-01 logo/branding
✅ Sign In / Sign Up buttons
✅ Navigation menu
✅ Product listing page
✅ Responsive design (try mobile view)
```

### 2. Test User Registration (Signup)

```bash
# Go to Sign Up page
# Register as CLIENT:
- Email: client@test.com
- Password: Client@123 (strong password)
- Name: Test Client
- Role: Client

# Expected:
✅ Form validation works
✅ Password strength indicator
✅ Success message
✅ Redirected to dashboard or login
```

### 3. Test User Login (Auth)

```bash
# Go to Sign In page
# Login with client@test.com / Client@123

# Expected:
✅ Can login successfully
✅ Redirected to client dashboard
✅ Can see "My Profile" / "My Orders"
✅ Logout works
```

### 4. Test Seller Registration & Product Creation

```bash
# Register as SELLER:
- Email: seller@test.com
- Password: Seller@123
- Name: Test Seller
- Role: Seller

# Expected: ✅ Seller account created

# After login:
# Go to Product Management
# Create Product:
- Name: Test Product
- Price: 99.99
- Category: Electronics
- Description: Test product description

# Expected:
✅ Product created successfully
✅ Product appears in dashboard
✅ Can edit product
✅ Can delete product
```

### 5. Test Media Upload

```bash
# In product management:
# Click "Upload Media"
# Upload a test image (JPG/PNG under 5MB)

# Expected:
✅ File upload succeeds
✅ Image preview shown
✅ File associated with product
✅ Can view image in product listing

# Test constraints:
# Try uploading:
- Invalid file type (.exe, .txt) → Should be REJECTED
- Large file (>10MB) → Should be REJECTED or timeout
- Correct type (.jpg, .png) → Should be ACCEPTED
```

### 6. Test Product Listing & Search

```bash
# Go to Products page
# Expected to see:
✅ All products listed with images
✅ Product cards show: Name, Price, Rating, Image
✅ Can search by product name
✅ Can filter by category
✅ Can sort by price (low-high, high-low)
✅ Pagination works (if many products)
```

### 7. Test Error Handling

```bash
# Test DUPLICATE EMAIL:
- Try signing up with existing email (client@test.com)
# Expected: ❌ 409 Conflict or "Email already registered"

# Test INVALID INPUT:
- Try signup with weak password
# Expected: ❌ "Password must contain..."

- Try signup with invalid email
# Expected: ❌ "Invalid email format"

# Test MISSING FIELDS:
- Try login without email/password
# Expected: ❌ "Field required"
```

---

## Phase 3: Jenkins Build Verification (10 minutes)

### 1. Verify Automatic Build Triggers

```bash
# Option A - If using GitHub webhook:
# Make a change: echo "# test" >> README.md
# Commit: git commit -am "test: trigger build"
# Push: git push origin main
# Within 30 seconds, Jenkins should start building

# Option B - If using Gitea polling (H/5 * * * *):
# Make a change: echo "# test" >> README.md
# Commit: git commit -am "test: trigger build"
# Push: git push gitea main
# Within 5 minutes, Jenkins should detect and build

# To test immediately:
# Jenkins UI → Buy-01-Pipeline → "Build Now"
```

**Verify Build Started:**

```bash
# Jenkins UI should show:
✅ New build queued
✅ Build running (blue ball spinning)
✅ All stages executing
✅ Build completes in ~5 minutes
```

### 2. Check Build Parameters

```bash
# Jenkins UI → Buy-01-Pipeline → Build with Parameters
# You should see:
✅ BRANCH (default: main)
✅ RUN_TESTS (default: true)
✅ RUN_INTEGRATION_TESTS (default: false)
✅ RUN_SONAR (default: true)
✅ SKIP_DEPLOY (default: true)
✅ DEPLOY_LOCALLY (default: true)
✅ SKIP_FRONTEND_BUILD (default: false)

# Try different combinations:
- Build with RUN_TESTS=false → Should skip test stage
- Build with RUN_SONAR=false → Should skip SonarQube
- Build with DEPLOY_LOCALLY=true SKIP_DEPLOY=false → Should deploy
```

### 3. Verify Test Results

```bash
# After build completes:
# Jenkins UI → Last Build → Test Result

# Expected:
✅ Passed: X (> 0 tests passed)
✅ Failed: 0
✅ Skipped: 0 (or some if skipped)

# Can also check:
# Jenkins UI → Last Build → Code Coverage (if configured)
```

### 4. Verify Docker Images Pushed

```bash
# Check Docker Hub:
# https://hub.docker.com/r/mahdikheirkhah

# Expected to see:
✅ user-service:X (where X = build number)
✅ product-service:X
✅ media-service:X
✅ discovery-service:X
✅ api-gateway:X
✅ frontend:X
✅ user-service:stable
✅ frontend:stable (etc.)

# Or check locally:
docker images | grep mahdikheirkhah
# Should show images with BUILD_NUMBER and 'stable' tags
```

---

## Phase 4: SonarQube Verification (10 minutes)

### 1. Access SonarQube Dashboard

```bash
# Open: http://localhost:9000
# Login: admin / admin
```

### 2. Check Project Analysis

```bash
# Projects → buy-01-backend
# Expected to see:
✅ Code analyzed (Java/Spring)
✅ Lines of code: X,XXX
✅ Issues found: N/A-level issues
✅ Code smells identified
✅ Security hotspots
✅ Code coverage: X%
✅ Quality gate: ✅ PASSED or ⚠️ FAILED

# Projects → buy-01-frontend
# Expected to see:
✅ Code analyzed (TypeScript/Angular)
✅ Lines of code: X,XXX
✅ Issues found
✅ Code coverage: X%
✅ Quality gate: ✅ PASSED or ⚠️ FAILED
```

### 3. Review Issues

```bash
# For each project:
# Issues tab:
✅ See list of issues by type:
  - Bugs (code errors)
  - Code Smells (bad practices)
  - Security Hotspots (potential vulnerabilities)

# Expected count: Should show issues that need fixing

# Click on issues to see:
✅ Line number in code
✅ Severity level (Critical, Major, Minor, Info)
✅ Suggested fix
```

### 4. Track Quality Gate

```bash
# Project page → Quality Gate
# Status:
✅ PASSED - Code meets quality standards
⚠️ FAILED - Code has issues exceeding threshold

# Threshold typically:
- Max X% code duplication
- Min X% code coverage
- Max N critical issues
- Max N bugs
```

---

## Quick Checklist Summary

### ✅ Infrastructure Ready

- [x] Jenkins running (http://localhost:8080)
- [x] SonarQube running (http://localhost:9000)
- [x] Frontend running (http://localhost:4200)
- [x] Backend services running
- [x] Docker images pushed successfully

### ✅ CI/CD Pipeline Working

- [x] Build completes successfully
- [x] Tests run automatically
- [x] SonarQube analysis runs
- [x] Docker images built and pushed
- [x] Parameterized builds available

### 📋 Testing Needed

- [ ] Test frontend signup/login
- [ ] Test seller product creation
- [ ] Test media upload with constraints
- [ ] Test error handling
- [ ] Verify automatic build triggers
- [ ] Review SonarQube issues
- [ ] Check code quality improvements

---

## Common Issues & Fixes

### Frontend Not Loading (http://localhost:4200)

```bash
# Check if container is running:
docker ps | grep frontend

# If not running:
docker compose up -d frontend

# Check logs:
docker logs frontend | tail -20
```

### Jenkins Build Failing

```bash
# Check console output:
# Jenkins UI → Build → Console Output

# Common issues:
- Docker socket not mounted: Check docker-compose.jenkins.yml
- Workspace path issue: Already fixed (using ${WORKSPACE})
- Gitea not synced: Push to Gitea after changes
- Credentials missing: Check Jenkins Credentials Store
```

### SonarQube Analysis Not Running

```bash
# Verify SonarQube is running:
docker ps | grep sonarqube

# Check token:
# Jenkins → Manage Jenkins → Manage Credentials
# Should have: sonarqube-token

# Verify analysis in Jenkinsfile:
# Should have mvn sonar:sonar for backend
# Should have npx sonar-scanner for frontend
```

### Tests Failing

```bash
# Check test reports:
# Jenkins → Build → Test Result

# Run tests locally:
cd backend/user-service
mvn test -B

# Frontend:
cd frontend
npm run test -- --watch=false
```

---

## Success Criteria

### ✅ Phase 1: Infrastructure (DONE)

- All containers running and healthy
- Jenkins, SonarQube, Frontend accessible
- No Docker errors

### ✅ Phase 2: Application (In Progress)

- [ ] Can signup as client
- [ ] Can signup as seller
- [ ] Can create products
- [ ] Can upload media
- [ ] Can search/filter products
- [ ] Error handling works

### ✅ Phase 3: Jenkins (In Progress)

- [ ] Build succeeds
- [ ] Tests run and pass
- [ ] Images pushed to registry
- [ ] Automatic triggers work
- [ ] Parameters available

### ✅ Phase 4: SonarQube (In Progress)

- [ ] Projects analyzed
- [ ] Issues identified
- [ ] Quality gate configured
- [ ] Code coverage tracked
- [ ] IDE integration working

---

## Next Actions

1. **Right Now:** Open http://localhost:4200 in browser and test signup
2. **Next 30 minutes:** Test all CRUD operations and error cases
3. **Next 1 hour:** Verify Jenkins build parameters and test results
4. **Before Submission:** Review and fix SonarQube issues, document audit findings

---

**Time to Complete Full Audit:** 2-3 hours
**Priority:** All three categories equally important for evaluation

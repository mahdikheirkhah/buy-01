# Buy-01 Audit - Test Execution Checklist

**Date:** ******\_******  
**Tester:** ******\_******  
**Build Number:** ******\_******

---

## PHASE 1: Infrastructure Verification (15 min)

### Container Status

- [ ] Docker ps shows 8+ containers running
- [ ] Jenkins container healthy
- [ ] SonarQube container healthy
- [ ] Frontend container healthy
- [ ] MongoDB container running
- [ ] All microservices running (user, product, media, discovery)

### Service Accessibility

- [ ] Jenkins: http://localhost:8080 - Accessible
- [ ] SonarQube: http://localhost:9000 - Accessible & UP status
- [ ] Frontend: http://localhost:4200 - Loading
- [ ] API Gateway: http://localhost:8080/api/health - 200 OK

### Test Commands

```bash
docker ps | wc -l     # Should show 8+
curl http://localhost:9000/api/system/status   # {"status":"UP"}
curl http://localhost:8080/api/health          # Health OK
```

**Phase 1 Result:** ☐ PASS ☐ FAIL

---

## PHASE 2: Jenkins Build Verification (10 min)

### Latest Build Status

- [ ] Job name: "Buy-01-Pipeline"
- [ ] Last build number: ****\_\_****
- [ ] Last build status: ☐ SUCCESS (blue) ☐ FAILURE (red)
- [ ] Build duration: ******\_****** minutes (should be < 10)

### Build Stages Completed

- [ ] ⏳ Initialization - PASSED
- [ ] 📥 Checkout - PASSED (from Gitea)
- [ ] 🏗️ Build Backend - PASSED
- [ ] 🏗️ Build Frontend - PASSED
- [ ] 🧪 Test Backend (Unit) - PASSED
- [ ] 🧪 Test Frontend - PASSED
- [ ] 📊 SonarQube Analysis - PASSED
- [ ] 🐳 Dockerize & Push - PASSED
- [ ] 🚀 Deploy Locally - PASSED

### Build Artifacts

- [ ] Backend JAR files created
- [ ] Frontend bundle created (dist folder)
- [ ] Docker images built
- [ ] Docker images pushed to Docker Hub
- [ ] Docker images tagged with build number and 'stable'

**Test:**

```bash
# Check Jenkins job exists
curl http://localhost:8080/api/json | grep Buy-01

# Check last build
curl http://localhost:8080/job/Buy-01-Pipeline/lastBuild/api/json | grep result

# Check Docker Hub images
docker images | grep mahdikheirkhah
```

**Phase 2 Result:** ☐ PASS ☐ FAIL

---

## PHASE 3: Application Testing (45 min)

### 3.1 Frontend Access

- [ ] Can load http://localhost:4200 without errors
- [ ] Page title shows "Buy-01" or similar
- [ ] Navigation menu visible
- [ ] Sign In / Sign Up buttons visible
- [ ] Product listing visible
- [ ] Responsive design works (test mobile view)

**Evidence:** Screenshot ****\_\_\_\_****

### 3.2 User Registration - Client

- [ ] Click "Sign Up"
- [ ] Select Role: "CLIENT"
- [ ] Enter Email: `client@test.com`
- [ ] Enter Password: `Client@123`
- [ ] Enter Name: `Test Client`
- [ ] Form validation works (try empty fields)
- [ ] Password strength indicator visible
- [ ] Submit button works
- [ ] Success message shown
- [ ] Redirected to login or dashboard

**Expected:** ✅ Account created  
**Actual:** ☐ PASS ☐ FAIL

### 3.3 User Login - Client

- [ ] Click "Sign In"
- [ ] Enter Email: `client@test.com`
- [ ] Enter Password: `Client@123`
- [ ] Submit button works
- [ ] No errors returned
- [ ] Redirected to dashboard
- [ ] Dashboard shows "Welcome" or user name
- [ ] "Logout" or "Profile" link visible

**Expected:** ✅ Login successful  
**Actual:** ☐ PASS ☐ FAIL

### 3.4 User Registration - Seller

- [ ] Logout from client account
- [ ] Click "Sign Up"
- [ ] Select Role: "SELLER"
- [ ] Enter Email: `seller@test.com`
- [ ] Enter Password: `Seller@123`
- [ ] Enter Name: `Test Seller`
- [ ] Enter Shop Name: `Test Shop` (if applicable)
- [ ] Submit and verify success

**Expected:** ✅ Seller account created  
**Actual:** ☐ PASS ☐ FAIL

### 3.5 Seller Product Creation

- [ ] Login as seller
- [ ] Navigate to "Product Management" or "My Products"
- [ ] Click "Create Product" or "Add Product"
- [ ] Enter Product Name: `Test Product`
- [ ] Enter Price: `99.99`
- [ ] Select Category: `Electronics`
- [ ] Enter Description: `Test product description`
- [ ] Set Stock: `50`
- [ ] Submit form
- [ ] Success message shown
- [ ] Product appears in product list

**Expected:** ✅ Product created  
**Actual:** ☐ PASS ☐ FAIL

### 3.6 Media Upload

- [ ] Click on created product
- [ ] Click "Upload Media" or "Add Image"
- [ ] Select test image file (JPG/PNG)
- [ ] Preview shown before upload
- [ ] Click upload
- [ ] Progress indicator shown
- [ ] Success message displayed
- [ ] Image visible in product details

**Expected:** ✅ Media uploaded successfully  
**Actual:** ☐ PASS ☐ FAIL

**Test Constraints:**

- [ ] Try uploading `.txt` file → Should be REJECTED
- [ ] Try uploading `.exe` file → Should be REJECTED
- [ ] Try uploading very large file → Should handle gracefully

**Expected:** ✅ Constraints enforced  
**Actual:** ☐ PASS ☐ FAIL

### 3.7 Product Listing & Search

- [ ] Logout and return to product listing
- [ ] See all products displayed with:
  - [ ] Product image
  - [ ] Product name
  - [ ] Price
  - [ ] Rating (if applicable)
- [ ] Search functionality works
  - [ ] Search for product name
  - [ ] See filtered results
- [ ] Filter by category works (if available)
- [ ] Sort by price works (if available)
- [ ] Pagination works (if many products)

**Expected:** ✅ All listing features working  
**Actual:** ☐ PASS ☐ FAIL

### 3.8 Error Handling - Duplicate Email

- [ ] Go to Sign Up
- [ ] Try registering with `client@test.com` (already exists)
- [ ] Enter different password
- [ ] Submit form
- [ ] Error message shown: "Email already registered" or similar
- [ ] Account not created (verify by trying to login with old password)

**Expected:** ✅ 409 Conflict error, duplicate prevented  
**Actual:** ☐ PASS ☐ FAIL

### 3.9 Error Handling - Invalid Input

- [ ] Try signup with weak password (e.g., "123")
  - [ ] Error: Password too weak
- [ ] Try signup with invalid email (e.g., "notanemail")
  - [ ] Error: Invalid email format
- [ ] Try login without email
  - [ ] Error: Email required
- [ ] Try login without password
  - [ ] Error: Password required

**Expected:** ✅ All validations work  
**Actual:** ☐ PASS ☐ FAIL

### 3.10 Role-Based Access Control

- [ ] Login as client
- [ ] Verify client CANNOT:
  - [ ] Create products
  - [ ] Access "Product Management"
  - [ ] Upload media as seller
- [ ] Logout and login as seller
- [ ] Verify seller CAN:
  - [ ] Create products
  - [ ] Upload media
  - [ ] Edit own products

**Expected:** ✅ RBAC enforced  
**Actual:** ☐ PASS ☐ FAIL

**Phase 3 Result:** ☐ PASS ☐ FAIL

**Issues Found:**

---

---

---

## PHASE 4: Jenkins Advanced Testing (15 min)

### 4.1 Parameterized Builds

- [ ] Jenkins → Buy-01-Pipeline → "Build with Parameters"
- [ ] See BRANCH parameter
- [ ] See RUN_TESTS parameter
- [ ] See DEPLOY_LOCALLY parameter
- [ ] See SKIP_DEPLOY parameter
- [ ] Select: RUN_TESTS = false
- [ ] Click "Build"
- [ ] Build should skip test stage
- [ ] Verify in console output

**Expected:** ✅ Parameters working  
**Actual:** ☐ PASS ☐ FAIL

### 4.2 Automatic Trigger (Optional)

If using Gitea polling:

- [ ] Make code change: `echo "# test" >> README.md`
- [ ] Commit: `git commit -am "test commit"`
- [ ] Push: `git push gitea main`
- [ ] Wait up to 5 minutes
- [ ] Jenkins detects change and triggers build
- [ ] Build completes successfully

**Expected:** ✅ Auto-trigger works within 5 minutes  
**Actual:** ☐ PASS ☐ FAIL ☐ SKIPPED

### 4.3 Build Notifications

- [ ] Check email inbox for build notification
- [ ] Email should contain:
  - [ ] Build status (SUCCESS/FAILURE)
  - [ ] Build number
  - [ ] Link to Jenkins job
  - [ ] Recipient: mohammad.kheirkhah@gritlab.ax

**Expected:** ✅ Email received and informative  
**Actual:** ☐ PASS ☐ FAIL

**Phase 4 Result:** ☐ PASS ☐ FAIL

---

## PHASE 5: SonarQube Analysis (10 min)

### 5.1 Projects Analyzed

- [ ] SonarQube shows project: "buy-01-backend"

  - [ ] Code analyzed (Java)
  - [ ] Issues shown: ****\_\_**** (count)
  - [ ] Code coverage: ****\_\_\_****%
  - [ ] Quality gate: ☐ PASSED ☐ FAILED

- [ ] SonarQube shows project: "buy-01-frontend"
  - [ ] Code analyzed (TypeScript)
  - [ ] Issues shown: ****\_\_**** (count)
  - [ ] Code coverage: ****\_\_\_****%
  - [ ] Quality gate: ☐ PASSED ☐ FAILED

**Test:**

```
SonarQube → Projects → buy-01-backend → Issues
SonarQube → Projects → buy-01-frontend → Issues
```

### 5.2 Issue Categories

**Backend (buy-01-backend):**

- [ ] Bugs: ****\_\_****
- [ ] Code Smells: ****\_\_****
- [ ] Security Hotspots: ****\_\_****
- [ ] Duplications: ****\_\_****

**Frontend (buy-01-frontend):**

- [ ] Bugs: ****\_\_****
- [ ] Code Smells: ****\_\_****
- [ ] Security Hotspots: ****\_\_****
- [ ] Duplications: ****\_\_****

### 5.3 Code Quality

- [ ] Can view issue details
- [ ] Line numbers shown
- [ ] Severity levels shown (Critical, Major, Minor)
- [ ] Issue descriptions clear
- [ ] Suggested fixes provided

**Expected:** ✅ Issues identified and categorized  
**Actual:** ☐ PASS ☐ FAIL

**Phase 5 Result:** ☐ PASS ☐ FAIL

---

## OVERALL RESULTS

### Score Summary

| Category         | Result        | Notes |
| ---------------- | ------------- | ----- |
| Infrastructure   | ☐ PASS ☐ FAIL |       |
| Jenkins CI/CD    | ☐ PASS ☐ FAIL |       |
| Application      | ☐ PASS ☐ FAIL |       |
| Jenkins Advanced | ☐ PASS ☐ FAIL |       |
| SonarQube        | ☐ PASS ☐ FAIL |       |
| **OVERALL**      | ☐ PASS ☐ FAIL |       |

### Total Issues Found: ****\_\_****

### Critical Issues (Must Fix):

---

---

---

### Major Issues (Should Fix):

---

---

---

### Minor Issues (Nice to Fix):

---

---

---

---

## Sign-Off

**Tested By:** ************\_************  
**Date:** ************\_************  
**Time:** ******\_****** to ******\_******  
**Overall Assessment:** ☐ READY ☐ NEEDS FIXES

**Approved By:** ************\_************  
**Date:** ************\_************

---

**Notes & Comments:**

---

---

---

---

**Next Steps:**

- ☐ Fix critical issues
- ☐ Address major issues
- ☐ Re-test affected areas
- ☐ Submit audit findings
- ☐ Archive this checklist

---

**Generated:** 2026-01-06  
**Valid Until:** 2026-01-20 (or project submission date)

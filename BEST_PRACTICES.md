# Best Practices Ecosystem Setup

This document outlines the collaborative development process, CI/CD integration, and code review workflow for the Buy-01 E-Commerce Platform.

---

## 1. Pull Request & Code Review Process

### Opening a PR

1. **Create Feature Branch**

   ```bash
   git checkout main
   git pull origin main
   git checkout -b feature/your-feature-name
   ```

2. **Make Changes** (follow commit message standards)

   ```bash
   git add .
   git commit -m "feat: add shopping cart functionality"
   git push origin feature/your-feature-name
   ```

3. **Open PR on GitHub/Gitea**
   - Go to repository and click "New Pull Request"
   - Select your feature branch → main
   - Use the PR template (auto-populated)
   - Request at least 2 reviewers
   - Link related issues

### Code Review Requirements

**Minimum Requirements to Merge:**

- ✅ At least 2 approvals
- ✅ All CI/CD checks passing (green)
- ✅ SonarQube quality gate passed
- ✅ No merge conflicts
- ✅ Code coverage ≥80% for new code

**Reviewer Responsibilities:**

- [ ] Code quality and readability
- [ ] Security and best practices
- [ ] Testing coverage
- [ ] Documentation updates
- [ ] No breaking changes

**Author Responsibilities:**

- [ ] Address feedback promptly
- [ ] Keep PR focused (not >400 lines)
- [ ] Run tests locally before pushing
- [ ] Provide clear PR description

---

## 2. CI/CD Pipeline Integration

### Automated Checks (Jenkins)

The pipeline automatically runs on every PR push:

```
1. Build          → Compile Maven + Frontend
2. Unit Tests     → Run test suite with coverage
3. Code Quality   → SonarQube analysis
4. Security Scan  → Vulnerability detection
5. Docker Build   → Build container images
6. Deploy (main)  → Deploy to staging/production
```

### PR Pipeline Status

Your PR shows:

```
✅ Checks passed       - Ready to merge
❌ Checks failed       - Review logs and fix issues
⏳ Checks in progress  - Wait for completion
```

Click each check for detailed logs and error messages.

### Common Pipeline Issues

| Issue              | Solution                                                |
| ------------------ | ------------------------------------------------------- |
| Build fails        | Run `mvn clean install` locally to find compiler errors |
| Tests fail         | Run `mvn test` locally and debug failing tests          |
| Coverage low       | Add unit tests for new code (target ≥80%)               |
| SonarQube fails    | Fix code smells in SonarQube dashboard                  |
| Docker build fails | Check Dockerfile syntax and verify all dependencies     |

---

## 3. Branch Management

### Branch Naming Conventions

```
feature/description           - New features
bugfix/description            - Bug fixes
hotfix/critical-issue         - Critical production fixes
refactor/component-name       - Code refactoring
test/feature-name             - Test additions
docs/documentation-topic      - Documentation updates
chore/task-description        - Maintenance tasks
```

### Branch Lifecycle

1. **Create** from `main` branch
2. **Push** to remote and open PR
3. **Review** code (≥2 approvals required)
4. **Merge** to main via GitHub/Gitea
5. **Delete** feature branch after merge

---

## 4. Commit Message Standards

### Format

```
<type>: <subject>

<body>

<footer>
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `refactor`: Code refactoring
- `test`: Adding/updating tests
- `docs`: Documentation changes
- `chore`: Maintenance tasks
- `perf`: Performance improvements
- `ci`: CI/CD configuration

### Examples

```
feat: implement shopping cart functionality

Add cart entity, service, and API endpoints.
Enable users to add/remove products and manage quantities.

Closes #42

---

fix: resolve order status update race condition

Use pessimistic locking to prevent concurrent updates.

Closes #123

---

refactor: simplify product filtering logic

Extract into separate utility class for reusability.
```

---

## 5. Testing Requirements

### Before Submitting PR

```bash
# Backend - Run all tests
cd backend
mvn clean test          # Unit tests
mvn clean verify        # Including integration tests

# Frontend - Run tests
cd frontend
npm test                # Unit tests
npm run build          # Production build
```

### Coverage Requirements

- **Target**: ≥80% for new code
- **Framework**: JUnit 5 + Mockito (Backend), Jasmine (Frontend)
- **Must-Have**: All new features require unit tests

### Test Naming Convention

```java
@Test
void testFeatureWhenConditionThenResult() {
    // Arrange

    // Act

    // Assert
}

// Examples
void testAddToCartWhenProductValidThenCartUpdated() {}
void testGetOrdersWhenUserAuthenticatedThenReturnOrders() {}
void testOrderCancelWhenStatusPendingThenCancelled() {}
```

---

## 6. Code Quality Standards

### SonarQube Quality Gate

**PR Must Pass:**

- ✅ Quality Gate: PASSED
- ✅ Rating: Target Grade A
- ✅ Coverage: >80% for new code
- ✅ No critical vulnerabilities
- ✅ Code smells addressed

**Dashboard:** http://localhost:9000

### Java Style Guide

- Max line length: 120 characters
- Indentation: 4 spaces
- Follow Google Java Style
- Use meaningful names
- Comments explain "why"

### TypeScript/Angular Style Guide

- Max line length: 100 characters
- Indentation: 2 spaces
- Follow Angular style guide
- Use strict mode
- Add type annotations

### Tools

```bash
# Java linting
mvn checkstyle:check

# TypeScript linting
npm run lint

# Format code
mvn formatter:format
npm run format
```

---

## 7. Security & Validation

### Before Merge, Ensure

- ✅ No hardcoded secrets/passwords
- ✅ Input validation implemented
- ✅ SQL injection protection verified
- ✅ Authentication/authorization enforced
- ✅ Sensitive data not logged

### SonarQube Security

- Check for Security Hotspots (review carefully)
- Resolve critical vulnerabilities
- Address taint analysis warnings

---

## 8. Documentation

### Required Documentation

- [ ] **API Docs**: Swagger/OpenAPI for new endpoints
- [ ] **Code Comments**: JavaDoc, TypeScript JSDoc
- [ ] **Commit Messages**: Follow conventions
- [ ] **README**: Update if adding features
- [ ] **Database**: Schema changes documented

### Examples

```java
/**
 * Adds a product to the user's shopping cart.
 *
 * @param cartId the cart ID
 * @param productId the product ID
 * @param quantity the quantity to add
 * @return the updated cart
 * @throws NotFoundException if cart or product not found
 * @throws InvalidQuantityException if quantity is invalid
 */
public Cart addToCart(Long cartId, Long productId, Integer quantity) {
    // implementation
}
```

---

## 9. Merge Process

### Prerequisites

- ✅ 2+ approvals obtained
- ✅ All CI/CD checks passing
- ✅ No merge conflicts
- ✅ SonarQube quality gate passed
- ✅ Code coverage threshold met

### Merge Steps

1. **Resolve Conflicts** (if any)

   ```bash
   git fetch origin
   git merge origin/main
   # Fix conflicts in editor
   git add .
   git commit -m "resolve merge conflicts"
   git push origin feature/your-branch
   ```

2. **Squash & Merge** (via GitHub/Gitea UI)

   - Combines all commits into one
   - Keeps history clean
   - Recommended for feature branches

3. **Verify Merge**

   - Check main branch built successfully
   - Verify deployment (if auto-deploy enabled)
   - Monitor logs for errors

4. **Clean Up**
   - Feature branch auto-deleted (GitHub)
   - Manual delete if needed: `git branch -d feature/your-branch`

---

## 10. Collaboration Best Practices

### For Authors

✅ **Do**

- Keep PRs focused and small (<400 lines)
- Run tests locally before pushing
- Respond to feedback promptly
- Provide clear descriptions
- Ask for clarification if needed

❌ **Don't**

- Mix unrelated changes
- Ignore test failures
- Merge without approvals
- Commit sensitive data
- Skip documentation

### For Reviewers

✅ **Do**

- Review within 24 hours
- Be constructive and kind
- Suggest improvements
- Praise good code
- Check security and performance

❌ **Don't**

- Approve without reading
- Be dismissive
- Demand changes without explanation
- Approve code you don't understand
- Request unnecessary changes

---

## 11. Workflow Summary

```
┌─────────────────────────────────────────────────────────┐
│           E-Commerce Platform Development Workflow       │
└─────────────────────────────────────────────────────────┘

1. Create Branch
   git checkout -b feature/your-feature

2. Make Changes
   └─ Follow commit conventions
   └─ Add unit tests
   └─ Update documentation

3. Push & Open PR
   git push origin feature/your-feature
   └─ Use PR template
   └─ Request 2+ reviewers
   └─ Link related issues

4. CI/CD Pipeline Runs (Automatic)
   ├─ Build (Maven + Frontend)
   ├─ Unit Tests (JUnit + Jasmine)
   ├─ SonarQube Analysis
   ├─ Security Scan
   └─ Docker Build

5. Code Review
   ├─ Reviewer 1 reviews
   ├─ Reviewer 2 reviews
   └─ Author addresses feedback

6. Quality Gate
   ├─ SonarQube: PASSED ✅
   ├─ Tests: ALL PASSING ✅
   ├─ Coverage: ≥80% ✅
   └─ Security: NO CRITICAL ISSUES ✅

7. Approval & Merge
   ├─ 2+ Approvals obtained ✅
   ├─ All checks passing ✅
   ├─ Squash & Merge to main
   └─ Delete feature branch

8. Post-Merge
   └─ Monitor main build
   └─ Verify deployment
   └─ Close related issues

```

---

## 12. Troubleshooting & Support

### PR Blocked Issues

**Merge Conflicts**

```bash
git fetch origin
git rebase origin/main
# Resolve conflicts
git add .
git rebase --continue
git push -f origin feature/your-branch
```

**SonarQube Failure**

- Click SonarQube check link
- Review code smells and security hotspots
- Fix issues locally
- Push corrections to PR branch

**Test Failures**

```bash
# Run locally to debug
mvn test -Dtest=TestClassName#testMethodName
npm test -- --include='**/component.spec.ts'
```

**Build Timeout**

- Check Jenkins logs for bottleneck
- Consider disabling slow tests for PR builds
- Contact DevOps team if recurring

---

## 13. Key Contacts & Resources

- **DevOps Lead**: For Jenkins and Docker issues
- **Security Lead**: For security questions
- **Code Reviewer**: For review feedback
- **Project Manager**: For timeline questions
- **Documentation**: This guide + CONTRIBUTING.md

---

## 14. Summary

By following this ecosystem, we ensure:

✅ **Code Quality**: All code reviewed and tested  
✅ **Security**: Vulnerabilities caught early  
✅ **Maintainability**: Clear history and documentation  
✅ **Team Learning**: Knowledge sharing through reviews  
✅ **Reliability**: Automated checks prevent regressions  
✅ **Velocity**: Smooth collaboration and fewer conflicts

**Remember**: This process exists to support the team and improve code quality, not to create bureaucracy!

🚀 **Happy Contributing!**

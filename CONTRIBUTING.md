# Contributing to Buy-01 E-Commerce Platform

Thank you for contributing to the Buy-01 E-Commerce Platform project! This document outlines the collaborative development process, code review standards, and CI/CD integration guidelines.

---

## Table of Contents

1. [Development Process](#development-process)
2. [Branch Naming Conventions](#branch-naming-conventions)
3. [Pull Request Workflow](#pull-request-workflow)
4. [Code Review Guidelines](#code-review-guidelines)
5. [CI/CD Pipeline](#cicd-pipeline)
6. [Commit Message Standards](#commit-message-standards)
7. [Testing Requirements](#testing-requirements)
8. [Code Quality Standards](#code-quality-standards)
9. [Merging & Integration](#merging--integration)

---

## Development Process

### Collaborative Workflow

We follow a **Gitflow-based** development process with peer review and automated testing.

```
main (stable) ← PR with 2+ approvals ← develop (integration) ← feature branches
```

### Key Principles

- **Feature Isolation**: Each feature/fix is developed on a separate branch
- **Code Review**: All changes require at least 2 peer reviews before merge
- **Automated Testing**: CI/CD pipeline runs automatic tests on every PR
- **Quality Gates**: Code quality, security, and test coverage must meet standards
- **Documentation**: Changes must include updated documentation

---

## Branch Naming Conventions

Follow these naming patterns for clarity and automation:

```
feature/description          - New features
bugfix/description           - Bug fixes
hotfix/description           - Critical production fixes
refactor/description         - Code refactoring
test/description             - Test additions
docs/description             - Documentation updates
chore/description            - Maintenance tasks
```

### Examples

```
feature/shopping-cart-implementation
bugfix/order-status-update-issue
hotfix/critical-security-patch
refactor/api-gateway-restructure
test/add-integration-tests
docs/api-documentation-update
chore/update-dependencies
```

---

## Pull Request Workflow

### Step 1: Create Feature Branch

```bash
# Update main branch
git checkout main
git pull origin main

# Create feature branch from main
git checkout -b feature/your-feature-name
```

### Step 2: Make Changes

- Make atomic, focused commits
- Write clear commit messages
- Run tests locally before pushing
- Ensure code follows style guidelines

### Step 3: Push to Remote

```bash
# Push your branch to the remote repository
git push origin feature/your-feature-name
```

### Step 4: Open Pull Request

- Go to GitHub/Gitea and create a PR from your branch to `main`
- Use the PR template (see below)
- Add detailed description of changes
- Link related issues (if applicable)
- Request reviewers (minimum 2)

### Step 5: Code Review

- Address review comments promptly
- Push updates to the same branch
- Respond to suggestions with explanations
- Request re-review after changes

### Step 6: CI/CD Validation

- Wait for automated pipeline to complete
- Ensure all checks pass:
  - ✅ Build successful
  - ✅ Unit tests passing
  - ✅ Integration tests passing (if applicable)
  - ✅ SonarQube quality gate passed
  - ✅ Code coverage threshold met

### Step 7: Approval & Merge

- Obtain approvals from at least 2 reviewers
- Ensure all CI/CD checks pass
- Squash and merge to main
- Delete feature branch after merge

---

## Code Review Guidelines

### For Reviewers

#### Review Checklist

- [ ] **Code Quality**

  - [ ] Code is readable and well-structured
  - [ ] No unnecessary complexity (KISS principle)
  - [ ] No code duplication
  - [ ] Proper naming conventions followed
  - [ ] Comments explain "why", not "what"

- [ ] **Functionality**

  - [ ] Logic is correct and handles edge cases
  - [ ] Changes don't break existing features
  - [ ] Error handling is appropriate
  - [ ] Performance impact considered

- [ ] **Security**

  - [ ] No hardcoded secrets/passwords
  - [ ] Input validation and sanitization present
  - [ ] No SQL injection vulnerabilities
  - [ ] Authentication/authorization checked
  - [ ] Sensitive data not logged

- [ ] **Testing**

  - [ ] New code has corresponding unit tests
  - [ ] Test coverage is adequate (>80%)
  - [ ] Edge cases are tested
  - [ ] Existing tests still pass
  - [ ] Integration tests added if needed

- [ ] **Documentation**

  - [ ] Code changes documented
  - [ ] API endpoints documented (if applicable)
  - [ ] README updated (if needed)
  - [ ] Comments added for complex logic
  - [ ] CHANGELOG updated

- [ ] **Best Practices**
  - [ ] Follows project style guide
  - [ ] No hardcoded values (use config)
  - [ ] Proper error messages
  - [ ] Logging is appropriate
  - [ ] No deprecated methods used

#### Review Comments

- **Be Constructive**: Provide suggestions, not demands
- **Be Specific**: Point to exact lines and explain issues
- **Be Kind**: Remember this is a learning experience
- **Praise Good Code**: Acknowledge well-written sections

### For Authors (Submitting Code)

- **Be Responsive**: Address feedback promptly
- **Ask for Clarification**: If feedback is unclear
- **Explain Decisions**: Justify architectural choices
- **Be Open**: Accept constructive criticism
- **Keep PRs Small**: Aim for <400 lines of changes

---

## CI/CD Pipeline

### Automated Checks

The Jenkins CI/CD pipeline automatically:

1. **Build**: Compiles all services (Maven + Frontend)
2. **Unit Tests**: Runs test suite with coverage reporting
3. **Integration Tests**: Tests service interactions (if enabled)
4. **Code Quality**: Analyzes code with SonarQube
5. **Security Scan**: Checks for vulnerabilities
6. **Docker Build**: Builds container images
7. **Deployment** (main branch only): Deploys to staging/production

### PR Pipeline Status

Your PR will show the status of all checks:

```
✅ All checks passed
❌ Some checks failed
⏳ Checks in progress
```

Click on each check to see details and logs.

### Common Pipeline Failures

| Issue              | Solution                                 |
| ------------------ | ---------------------------------------- |
| Build fails        | Check compiler errors in logs            |
| Tests fail         | Run `mvn test` locally to debug          |
| Coverage low       | Add unit tests for new code              |
| SonarQube issues   | Fix code smells in SonarQube dashboard   |
| Docker build fails | Check Dockerfile syntax and dependencies |

---

## Commit Message Standards

### Format

```
<type>: <subject>

<body>

<footer>
```

### Type

Use one of:

- `feat`: New feature
- `fix`: Bug fix
- `refactor`: Code refactoring
- `test`: Adding/updating tests
- `docs`: Documentation changes
- `chore`: Maintenance tasks
- `perf`: Performance improvements
- `ci`: CI/CD configuration changes

### Subject (50 characters max)

- Use imperative mood ("add", not "added")
- Don't capitalize first letter
- No period at the end

### Body (72 characters per line)

- Explain what and why, not how
- Separate from subject with blank line
- Wrap at 72 characters

### Footer

```
Closes #123
Relates to #456
BREAKING CHANGE: describe the change
```

### Examples

```
feat: implement shopping cart functionality

Add shopping cart entity, service, and API endpoints.
Enable users to add/remove products and manage quantities.

Closes #42

---

fix: resolve order status update race condition

Use pessimistic locking to prevent concurrent updates.
Ensures order status remains consistent across transactions.

Closes #123

---

refactor: simplify product filtering logic

Extract filter logic into separate utility class.
Improves code readability and testability.

---

test: add integration tests for checkout process

Test complete flow from cart to order confirmation.
Mock external payment service for testing.

---

docs: update API documentation for order endpoints

Add request/response examples and error scenarios.
Include authentication requirements.
```

---

## Testing Requirements

### Unit Tests

- **Coverage Target**: ≥80% for new code
- **Framework**: JUnit 5 + Mockito (Backend), Jasmine (Frontend)
- **Run Locally**: `mvn test` (Backend), `npm test` (Frontend)
- **Required**: All new features must have tests

### Integration Tests

- **Coverage Target**: Key workflows
- **Scope**: Service interactions, database operations
- **Run Locally**: `mvn verify -Pintegration`
- **CI Pipeline**: Optional (can be toggled via Jenkins parameters)

### Test Naming Convention

```java
// Unit Tests
@Test
void testFeatureWhenConditionThenExpectedResult() {
    // Arrange

    // Act

    // Assert
}

// Examples
void testAddToCartWhenProductValidThenCartUpdated() {}
void testGetOrdersWhenUserAuthenticatedThenReturnUserOrders() {}
void testOrderCancelWhenStatusPendingThenStatusCancelled() {}
```

### Before Submitting PR

```bash
# Backend
cd backend
mvn clean test
mvn clean verify -Pintegration  # if adding integration tests

# Frontend
cd frontend
npm test
npm run build
```

---

## Code Quality Standards

### SonarQube Requirements

- **Quality Gate**: Must pass (no "FAILED" status)
- **Rating**: Target Grade A
- **Coverage**: >80% for new code
- **Security**: No critical vulnerabilities
- **Code Smells**: Address all blocking issues

### Code Style

#### Java

- Use Spring conventions
- Follow Google Java Style Guide
- Max line length: 120 characters
- Use meaningful variable names
- 4-space indentation

#### TypeScript/Angular

- Follow Angular style guide
- Use meaningful component names
- 2-space indentation
- Use TypeScript strict mode
- Add type annotations

### Tools

```bash
# Java Linting
mvn checkstyle:check

# TypeScript Linting
npm run lint

# Format Code
mvn formatter:format
npm run format
```

---

## Merging & Integration

### Merge Requirements

Before merging to main, ensure:

- ✅ PR has at least 2 approvals
- ✅ All CI/CD checks pass (green)
- ✅ No merge conflicts
- ✅ Branch is up-to-date with main
- ✅ SonarQube quality gate passed
- ✅ Code coverage meets threshold
- ✅ All conversations resolved

### Merge Strategy

- **Squash & Merge**: For feature branches (keeps history clean)
- **Create Merge Commit**: For release/hotfix branches (preserves history)

### After Merge

1. Delete feature branch (automatic on GitHub/Gitea)
2. Monitor main branch build
3. Verify deployment (if auto-deploy enabled)
4. Address any post-merge issues immediately

### Issue Resolution

After merging, PR author should:

- [ ] Verify changes in main branch
- [ ] Check deployment status
- [ ] Monitor logs for errors
- [ ] Close related issues
- [ ] Update project tracking board

---

## PR Template

When creating a PR, use this template:

```markdown
## Description

Brief description of changes

## Type of Change

- [ ] Feature
- [ ] Bug fix
- [ ] Refactoring
- [ ] Documentation update

## Related Issues

Closes #123

## Changes Made

- Change 1
- Change 2
- Change 3

## Testing

Describe testing performed:

- [ ] Unit tests added/updated
- [ ] Manual testing completed
- [ ] Edge cases considered

## Checklist

- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex logic
- [ ] Documentation updated
- [ ] Tests passing locally
- [ ] No new warnings introduced
```

---

## Troubleshooting

### PR Blocked by Merge Conflicts

```bash
git fetch origin
git merge origin/main
# Resolve conflicts in editor
git add .
git commit -m "resolve merge conflicts"
git push origin feature/your-feature
```

### CI/CD Pipeline Failure

1. Click on the failed check to see logs
2. Fix the issue locally
3. Run same check locally to verify fix
4. Push corrected code to PR branch

### Need Help?

- Review this guide again
- Check similar PRs for examples
- Ask in PR comments or team chat
- Create discussion issue

---

## Summary

By following this process, we ensure:

✅ **Code Quality**: All code reviewed and tested  
✅ **Security**: Vulnerabilities caught before production  
✅ **Maintainability**: Clear commit history and documentation  
✅ **Team Learning**: Knowledge sharing through reviews  
✅ **Reliability**: Automated checks prevent regressions

Thank you for contributing! 🚀

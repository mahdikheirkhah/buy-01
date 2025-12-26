# SonarQube Rules & Quality Gates Configuration
# This file documents all SonarQube rules and quality gate settings

## Quality Gate Configuration

### Overview
A Quality Gate is a set of measures and thresholds that your project must pass to be considered ready for production.

### Default Quality Gate: "Sonar Way"

#### Conditions Applied:

```
Rule 1: Coverage
  - Metric: Coverage
  - Operator: >=
  - Error Threshold: 80%
  - Error Message: Coverage is less than 80%

Rule 2: Duplicated Lines Density
  - Metric: Duplicated Lines (%)
  - Operator: <=
  - Error Threshold: 3%
  - Error Message: Duplicated Lines > 3%

Rule 3: Maintainability Rating
  - Metric: Maintainability Rating
  - Operator: <=
  - Error Threshold: A (Excellent)
  - Error Message: Code has too many code smells

Rule 4: Reliability Rating
  - Metric: Reliability Rating
  - Operator: <=
  - Error Threshold: A (No bugs expected)
  - Error Message: Code has bugs

Rule 5: Security Rating
  - Metric: Security Rating
  - Operator: <=
  - Error Threshold: A (No vulnerabilities)
  - Error Message: Code has vulnerabilities

Rule 6: Security Hotspots Reviewed
  - Metric: Security Hotspots Reviewed (%)
  - Operator: >=
  - Error Threshold: 100%
  - Error Message: Security hotspots not reviewed
```

## Enabled Rules by Language

### Java Backend Services

#### Security Issues (CWE Coverage)
```
Rule Categories Enabled:

✓ Weak Cryptography
  - Inadequate hash function usage
  - Use of insecure random number generation
  - Detection: S2076, S2083

✓ Injection Vulnerabilities
  - SQL Injection
  - LDAP Injection
  - XPath Injection
  - Detection: S2078, S2083

✓ Broken Authentication & Session Management
  - Hardcoded passwords
  - Session fixation
  - Credentials in logs
  - Detection: S2115, S2092

✓ Cross-Site Scripting (XSS)
  - Unsafe HTML rendering
  - DOM-based XSS
  - Detection: S2631

✓ CSRF Protection
  - Missing CSRF tokens
  - Unsafe state-changing operations
  - Detection: S3330

✓ XXE (XML External Entity)
  - Unsafe XML parsing
  - DTD processing
  - Detection: S2755

✓ Sensitive Data Exposure
  - Logging sensitive data
  - Exposing secrets
  - Detection: S2068, S2384

✓ Insecure Deserialization
  - Unsafe object deserialization
  - Remote code execution risks
  - Detection: S2384

✓ Missing Access Controls
  - Privilege escalation risks
  - Missing authorization checks
  - Detection: S3305
```

#### Reliability Issues
```
Rule Categories Enabled:

✓ Null Pointer Dereferences
  - Unchecked null values
  - Detection: S2259

✓ Resource Leaks
  - Unclosed streams/connections
  - File handle leaks
  - Database connection leaks
  - Detection: S2070, S1944

✓ Exception Handling
  - Caught but ignored exceptions
  - Overly broad exception catching
  - Detection: S1163, S1166

✓ Thread Safety
  - Data races
  - Race conditions
  - Detection: S2445

✓ Logic Errors
  - Infinite loops
  - Unreachable code
  - Dead variables
  - Detection: S1147, S1144
```

#### Maintainability Issues
```
Rule Categories Enabled:

✓ Complexity Limits
  - Cyclomatic Complexity (max: 15)
  - Cognitive Complexity (max: 25)
  - NPath Complexity limits
  - Detection: S1541, S3776

✓ Code Duplication
  - Duplicated code blocks
  - Similar code detection
  - Threshold: 5%
  - Detection: S1192

✓ Comment Coverage
  - Insufficient documentation
  - Missing JavaDoc
  - Minimum: 30% code coverage
  - Detection: S1141

✓ Code Smell Prevention
  - Large classes (>200 lines)
  - Long methods (>20 lines)
  - Parameter count (>7 parameters)
  - Detection: S1311, S1312, S2370

✓ Naming Conventions
  - Variable naming (camelCase)
  - Class naming (PascalCase)
  - Method naming standards
  - Detection: S1313-S1320
```

#### Code Coverage Requirements
```
Minimum Coverage: 80%
  - Line Coverage: Tests must cover 80% of executable lines
  - Branch Coverage: Tests must cover decision branches
  - Uncovered lines: Highlighted in red on dashboard

Exception Sources:
  - Test files (src/test/**)
  - Generated code
  - Configuration classes
```

### TypeScript/Angular Frontend

#### Security Issues
```
Rule Categories Enabled:

✓ DOM-based XSS Prevention
  - innerHTML usage
  - Unsafe property binding
  - Detection: S5631, S5632

✓ eval() and Function() Detection
  - Dynamic code execution
  - Remote code injection risks
  - Detection: S1523

✓ Insecure Cryptography
  - Weak encryption algorithms
  - Hardcoded secrets
  - Detection: S3330

✓ Session Security
  - Secure cookie flags
  - HttpOnly attribute
  - SameSite policy
  - Detection: S2092

✓ CORS Misconfiguration
  - Overly permissive origins
  - Wildcard usage
  - Detection: S5122

✓ Unsafe Dependencies
  - Known CVE libraries
  - Outdated packages
  - Detection: npm audit integration
```

#### Reliability Issues
```
Rule Categories Enabled:

✓ Unused Variables
  - Unused imports
  - Dead code elimination
  - Detection: S1128, S1144

✓ Error Handling
  - Unhandled promise rejections
  - Missing error handlers
  - Detection: S1192

✓ Type Safety
  - Any type usage
  - Implicit any types
  - Type checking
  - Detection: S4325

✓ Loop Safety
  - Infinite loop detection
  - Unreachable code
  - Detection: S1147
```

#### Maintainability Issues
```
Rule Categories Enabled:

✓ Complexity Limits
  - Cyclomatic Complexity (max: 15)
  - Cognitive Complexity (max: 25)
  - Detection: S3776

✓ Code Duplication
  - Duplicated logic blocks
  - Copy-paste detection
  - Threshold: 5%
  - Detection: S1192

✓ Naming Conventions
  - Variable naming (camelCase)
  - Class naming (PascalCase)
  - Constant naming (UPPER_CASE)
  - Detection: S1313-S1320

✓ Comment Coverage
  - Component documentation
  - Public API comments
  - Minimum: 30%
  - Detection: S1141

✓ File Size Limits
  - Max lines per file: 400
  - Max lines per function: 100
  - Detection: S104, S105
```

## Rules Configuration in SonarQube

### How to View/Modify Rules

#### In SonarQube Web Interface:

1. **Quality Profiles:**
   - Administration → Quality Profiles
   - Select Language (Java, TypeScript)
   - View enabled/disabled rules

2. **Rule Details:**
   - Rules → Select rule
   - View description, severity, tags
   - See examples and remediation

3. **Create Custom Profile:**
   - Administration → Quality Profiles
   - Create new profile
   - Enable/disable rules as needed
   - Set as default

### Rule Severity Levels

```
BLOCKER
  - Build must fail
  - Examples: SQL Injection, XXE, Critical security issue
  - Quality Gate: Always fails on BLOCKER

CRITICAL
  - Build should fail
  - Examples: Memory leak, thread safety issue
  - Quality Gate: Prevents merge

MAJOR
  - Build may fail (depends on threshold)
  - Examples: Code duplication, complexity
  - Quality Gate: Count-based rules

MINOR
  - For code improvement
  - Examples: Missing comments, naming
  - Quality Gate: Usually ignored

INFO
  - Informational only
  - Examples: Code style preferences
  - Quality Gate: Not enforced
```

## Project-Specific Rule Configuration

### Backend (buy-01-backend)

#### Analysis Profile: "Sonar Way Java"
- Language: Java
- Number of Rules: 400+
- Rules Active: 350
- Rules Disabled: 50 (too strict for project)

#### Custom Rules (Project-Specific):
```yaml
1. Maximum Method Complexity: 15 (Cyclomatic)
2. Maximum Method Cognitive Complexity: 25
3. Minimum Test Coverage: 80%
4. Maximum Duplicated Code: 3%
5. Security Review Required: 100% hotspots
6. No CRITICAL security issues allowed
7. No BLOCKER issues allowed
```

#### Current Status:
```
Active Rules: 350
- Security: 60 active
- Reliability: 80 active
- Maintainability: 210 active

Enforcement:
✓ Quality Gate: Sonar Way (Strict)
✓ Build Breaker: Enabled
✓ Analysis Failure: On Quality Gate Fail
```

### Frontend (buy-01-frontend)

#### Analysis Profile: "Sonar Way TypeScript"
- Language: TypeScript
- Number of Rules: 250+
- Rules Active: 200
- Rules Disabled: 50

#### Custom Rules (Project-Specific):
```yaml
1. Maximum Method Complexity: 15
2. Maximum Method Cognitive Complexity: 25
3. Minimum Test Coverage: 80%
4. Maximum Duplicated Code: 3%
5. XSS Prevention: 100% review
6. No CRITICAL issues: Blocking merge
7. eval() usage: Forbidden
```

#### Current Status:
```
Active Rules: 200
- Security: 40 active
- Reliability: 60 active
- Maintainability: 100 active

Enforcement:
✓ Quality Gate: Sonar Way (Strict)
✓ Build Breaker: Enabled
✓ Analysis Failure: On Quality Gate Fail
```

## Issue Remediation Examples

### Example 1: SQL Injection (CRITICAL)

**Issue Found:**
```java
String query = "SELECT * FROM users WHERE id = " + userId;
```

**Fix:**
```java
String query = "SELECT * FROM users WHERE id = ?";
preparedStatement = connection.prepareStatement(query);
preparedStatement.setInt(1, userId);
```

### Example 2: Code Duplication (MAJOR)

**Issue:** Same code in 3 places (15 lines)

**Fix:**
```java
// Create utility method
private String processUserData(User user) {
    // Shared logic
}

// Refactor all 3 occurrences to use method
```

### Example 3: High Complexity (MAJOR)

**Issue:** Method has Cyclomatic Complexity of 22 (max: 15)

**Fix:**
```java
// Split large method into smaller methods
private void handleUserRegistration(User user) {
    validateUser(user);           // Extracted method
    createUserAccount(user);      // Extracted method
    sendWelcomeEmail(user);       // Extracted method
}
```

### Example 4: Missing Test Coverage (MAJOR)

**Issue:** 65% coverage (min: 80%)

**Fix:**
- Add unit tests for uncovered branches
- Increase line coverage to 80%+
- Use SonarQube coverage report to identify gaps

### Example 5: Security Hotspot - Hardcoded Password (BLOCKER)

**Issue:**
```java
String password = "admin123";
```

**Fix:**
```java
String password = System.getenv("DB_PASSWORD");
```

## Monitoring & Continuous Improvement

### Dashboard Metrics to Track

```
Daily Monitoring:
✓ Quality Gate Status (PASS/FAIL)
✓ Code Coverage Trend
✓ Bug Count (decreasing)
✓ Vulnerability Count (zero)
✓ Code Smell Count (decreasing)
✓ Duplication % (< 3%)
✓ Security Hotspot Count

Weekly Review:
✓ New issues introduced
✓ Closed issues count
✓ Coverage trend
✓ Complexity distribution
✓ Rule violations by category

Monthly Goals:
✓ Quality rating improvement
✓ Coverage increase
✓ Issue resolution rate > 90%
✓ Zero critical security issues
```

### Recommended Rule Changes Over Time

```
Phase 1 (Current):
- Baseline established
- Quality Gate: Pass threshold
- Coverage: >= 80%

Phase 2 (Next):
- Coverage: >= 85%
- Duplication: < 2%
- Zero Security CRITICAL issues

Phase 3 (Long-term):
- Coverage: >= 90%
- Duplication: < 1%
- Maintainability A rating
```

---

**Last Updated:** December 26, 2025
**SonarQube Version:** LTS Community Edition
**Project:** buy-01 E-Commerce Platform

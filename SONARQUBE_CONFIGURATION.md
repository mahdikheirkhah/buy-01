# SonarQube Configuration Files

## sonarqube-project.properties

Create `sonarqube-project.properties` in your project root:

```properties
# Project identification
sonar.projectKey=buy-01
sonar.projectName=buy-01 E-Commerce
sonar.projectVersion=1.0.0

# Source code location
sonar.sources=backend,frontend/src
sonar.exclusions=**/target/**,**/node_modules/**,**/dist/**,**/coverage/**

# Language-specific settings
sonar.java.binaries=backend/*/target/classes
sonar.java.libraries=backend/*/target/lib
sonar.java.source=21
sonar.java.target=21

# Test coverage
sonar.coverage.jacoco.xmlReportPaths=backend/*/target/site/jacoco/jacoco.xml
sonar.javascript.lcov.reportPaths=frontend/coverage/lcov.info

# Code coverage exclusions
sonar.coverage.exclusions=**/dto/**,**/config/**,**/entity/**,**/model/**

# Duplicated lines threshold
sonar.duplications.skip=false

# Test files
sonar.test.inclusions=**/test/**,**/*Test.java,**/*.spec.ts

# Issue tracking
sonar.externalIssuesReportPaths=sonar-issues.json

# Quality gate name
sonar.qualitygate=buy-01-quality-gate
```

## Backend Maven Configuration

In `backend/pom.xml`, add SonarQube plugin:

```xml
<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>3.9.1.2184</version>
    <configuration>
        <sonarSources>src/main</sonarSources>
        <sonarTests>src/test</sonarTests>
        <sonarJavaSource>21</sonarJavaSource>
        <sonarJavaTarget>21</sonarJavaTarget>
    </configuration>
</plugin>

<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## Frontend Configuration

Create `frontend/sonarqube.properties`:

```properties
sonar.projectKey=buy-01-frontend
sonar.projectName=buy-01 Frontend
sonar.sources=src
sonar.exclusions=node_modules/**,dist/**,*.spec.ts,**/*.d.ts
sonar.tests=src
sonar.test.inclusions=**/*.spec.ts
sonar.coverage.exclusions=src/main.ts,src/polyfills.ts
sonar.javascript.lcov.reportPaths=coverage/lcov.info
sonar.typescript.tslint.configPath=tslint.json
sonar.typescript.eslint.configPath=.eslintrc.json
```

## Quality Gate Rules

Create these rules in SonarQube Admin panel:

### Critical Issues
- Blocker Issues > 0 → FAIL
- Critical Issues > 0 → FAIL
- Code Smells > 20 → FAIL

### Coverage
- Overall Code Coverage < 30% → FAIL
- New Code Coverage < 50% → FAIL

### Duplications
- Duplicated Lines (%) > 3% → FAIL

### Security
- Security Hotspots Reviewed (%) < 50% → FAIL
- Vulnerabilities > 0 → FAIL

### Maintainability
- Major Issues > 5 → FAIL
- Cyclomatic Complexity > 15 → INFO
- Lines of Code Density > 15 → INFO

## IDE Integration

### VS Code - SonarLint

Create `.vscode/settings.json`:

```json
{
  "sonarlint.ls.javaHome": "/usr/lib/jvm/java-21-openjdk",
  "sonarlint.connectedMode.project": "buy-01",
  "sonarlint.connectedMode.servers": [
    {
      "connectionId": "localhost",
      "serverUrl": "http://localhost:9000",
      "token": "your-sonarqube-token"
    }
  ],
  "sonarlint.showDescriptionInHovers": true,
  "sonarlint.showAnalysisProgress": true
}
```

### IntelliJ IDEA - SonarLint

1. Install SonarLint plugin
2. Go to Settings → Tools → SonarLint → Server Connections
3. Add connection:
   - Server URL: `http://localhost:9000`
   - Token: `your-sonarqube-token`
4. Enable Project Binding:
   - Project Key: `buy-01`
   - Server: `localhost`

## Jenkins Configuration

### Add SonarQube Credential

1. Go to Credentials → System → Global Credentials
2. Add credential:
   - Kind: Secret text
   - Secret: (paste SonarQube token)
   - ID: `sonarqube-token`

### Configure SonarQube Server

1. Manage Jenkins → System Configuration
2. Find SonarQube Servers
3. Add SonarQube Server:
   - Name: `SonarQube`
   - Server URL: `http://localhost:9000`
   - Disable Server Authentication (using credentials instead)

## GitHub Actions Secrets

Add to GitHub repository settings:

1. SONARQUBE_TOKEN: (your SonarQube token)
2. SONARQUBE_HOST: `http://localhost:9000` (or your server URL)
3. SLACK_WEBHOOK: (optional, for notifications)

## Webhook Configuration

### SonarQube to GitHub

In SonarQube Admin → Webhooks:

```
Name: GitHub
URL: https://github.com/mahdikheirkhah/buy-01/settings/hooks
Secret: (generate in GitHub)
Events: 
  - Quality Gate Status
  - Issue Status
```

### GitHub to SonarQube

Handled by GitHub Actions workflow

## Manual Analysis Commands

```bash
# Backend
cd backend
mvn sonar:sonar \
  -Dsonar.projectKey=buy-01-backend \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=your-token

# Frontend
cd frontend
sonar-scanner \
  -Dsonar.projectKey=buy-01-frontend \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=your-token
```

## Dashboard URLs

- Dashboard: http://localhost:9000/dashboard?id=buy-01-backend
- Issues: http://localhost:9000/project/issues?id=buy-01-backend
- Code: http://localhost:9000/project/code?id=buy-01-backend
- Measures: http://localhost:9000/project/measurements?id=buy-01-backend
- Branches: http://localhost:9000/project/branches?id=buy-01-backend

## Troubleshooting

### Token Issues
```bash
# Generate new token
curl -u admin:admin -X POST http://localhost:9000/api/user_tokens/generate \
  -d "name=jenkins-token"
```

### Quality Gate Not Working
```bash
# Check if default QG is set
curl -u admin:admin http://localhost:9000/api/qualitygates/list
curl -u admin:admin http://localhost:9000/api/qualitygates/default
```

### Analysis Not Starting
```bash
# Check SonarQube logs
docker compose logs sonarqube | tail -50

# Verify connectivity
curl -u admin:admin http://localhost:9000/api/system/status
```



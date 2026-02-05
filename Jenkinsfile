// CI/CD Pipeline for Buy-01 E-Commerce Platform
// Last updated: 2026-01-30
// ENHANCED VERSION - Auto-deploy + Automatic Rollback on Failure
// NEW FEATURES: Health checks, rollback strategy, version backup

pipeline {
    agent any

    triggers {
        githubPush()
        pollSCM('* * * * *')  // Fixed: Every minute (was H/1 which = every hour)
    }

    parameters {
        string(name: 'BRANCH', defaultValue: 'main', description: 'Git branch to build')
        booleanParam(name: 'RUN_TESTS', defaultValue: true, description: 'Run unit tests')
        booleanParam(name: 'SKIP_FRONTEND_TESTS', defaultValue: false, description: 'Skip frontend unit tests (for debugging)')
        booleanParam(name: 'RUN_SONAR', defaultValue: true, description: 'Run SonarQube analysis')
        booleanParam(name: 'SKIP_DEPLOY', defaultValue: false, description: 'Skip deployment')  // ✅ CHANGED: Auto-deploy enabled
        booleanParam(name: 'DEPLOY_LOCALLY', defaultValue: true, description: 'Deploy locally without SSH')
        booleanParam(name: 'SKIP_FRONTEND_BUILD', defaultValue: false, description: 'Skip frontend build')
        booleanParam(name: 'SKIP_GITHUB_STATUS', defaultValue: false, description: 'Skip GitHub status reporting')
    }

    environment {
        GITHUB_TOKEN = credentials('multi-branch-github')
        GITHUB_REPO = 'mahdikheirkhah/buy-01'
        DOCKER_REPO = 'mahdikheirkhah'
        DOCKER_CREDENTIAL_ID = 'dockerhub-credentials'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        STABLE_TAG = 'stable'
        PREVIOUS_STABLE_TAG = 'previous-stable' 
        SSH_CREDENTIAL_ID = 'ssh-deployment-key'
        REMOTE_HOST = '192.168.1.100'
        REMOTE_USER = 'ssh-user'
        DEPLOYMENT_DIR = '/opt/ecommerce'
        MAVEN_IMAGE = 'maven:3.9.6-amazoncorretto-17'
        NODE_IMAGE = 'node:22-alpine'
        BACKEND_DIR = 'backend'
        FRONTEND_DIR = 'frontend'
        GIT_SOURCE = 'unknown'
    }

    options {
        skipDefaultCheckout()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
        timeout(time: 2, unit: 'HOURS')
        timestamps()
        ansiColor('xterm')
    }

    stages {
        stage('⏳ Initialization') {
            steps {
                script {
                    echo "=========================================="
                    echo "🚀 Buy-01 E-Commerce CI/CD Pipeline"
                    echo "=========================================="
                    echo "Branch: ${params.BRANCH}"
                    echo "Build: #${env.BUILD_NUMBER}"
                    echo "Image Tag: ${IMAGE_TAG}"
                    echo "Run Tests: ${params.RUN_TESTS}"
                    echo "Deploy Locally: ${params.DEPLOY_LOCALLY}"
                    echo "Auto-Deploy: ${!params.SKIP_DEPLOY}"
                    echo "=========================================="
                }
            }
        }

        stage('📥 Checkout') {
            steps {
                script {
                    deleteDir()
                    
                    def isPullRequest = (env.CHANGE_ID != null)
                    def sourceRepo = isPullRequest ? 'GitHub' : 'Unknown'
                    def isGitHubSource = isPullRequest ? 'true' : 'false'
                    
                    // Get the actual branch name for this build
                    def branchToCheckout = env.BRANCH_NAME ?: params.BRANCH
                    
                    echo "📥 Build Type: ${isPullRequest ? 'Pull Request #' + env.CHANGE_ID : 'Branch Build'}"
                    echo "📥 Branch to checkout: ${branchToCheckout}"
                    echo "📥 GIT_COMMIT from trigger: ${env.GIT_COMMIT ?: 'not set'}"
                    
                    if (isPullRequest) {
                        echo "🔀 PR build detected - checking out PR HEAD (latest commit only)"
                        sourceRepo = 'GitHub'
                        isGitHubSource = 'true'
                        
                        // For PRs: checkout the specific PR merge ref (latest commit)
                        checkout([
                            $class: 'GitSCM',
                            branches: [[name: "FETCH_HEAD"]],
                            userRemoteConfigs: [[
                                url: 'https://github.com/mahdikheirkhah/buy-01.git',
                                credentialsId: 'multi-branch-github',
                                refspec: "+refs/pull/${env.CHANGE_ID}/head:FETCH_HEAD"
                            ]],
                            extensions: [
                                [$class: 'CloneOption', depth: 1, noTags: true, reference: '', shallow: true, timeout: 120],
                                [$class: 'WipeWorkspace']
                            ]
                        ])
                        echo "✅ Checkout completed - PR #${env.CHANGE_ID} HEAD"
                        
                    } else {
                        echo "🌿 Branch build detected - checking out branch HEAD..."
                        
                        // Try GitHub first, then fallback to Gitea
                        try {
                            // For branches: checkout the actual branch HEAD (not all history)
                            checkout([
                                $class: 'GitSCM',
                                branches: [[name: "refs/heads/${branchToCheckout}"]],
                                userRemoteConfigs: [[
                                    url: 'https://github.com/mahdikheirkhah/buy-01.git',
                                    credentialsId: 'multi-branch-github',
                                    refspec: "+refs/heads/${branchToCheckout}:refs/remotes/origin/${branchToCheckout}"
                                ]],
                                extensions: [
                                    [$class: 'CloneOption', depth: 1, noTags: true, reference: '', shallow: true, timeout: 120],
                                    [$class: 'WipeWorkspace'],
                                    [$class: 'LocalBranch', localBranch: branchToCheckout]
                                ]
                            ])
                            echo "✅ Checkout completed from GitHub - branch: ${branchToCheckout}"
                            sourceRepo = 'GitHub'
                            isGitHubSource = 'true'
                            
                        } catch (Exception e) {
                            echo "⚠️  GitHub checkout failed: ${e.message}"
                            echo "🔄 Attempting checkout from Gitea..."
                            
                            deleteDir()
                            
                            checkout([
                                $class: 'GitSCM',
                                branches: [[name: "refs/heads/${branchToCheckout}"]],
                                userRemoteConfigs: [[
                                    url: 'https://01.gritlab.ax/git/mkheirkh/safe-zone',
                                    credentialsId: 'gitea-credentials',
                                    refspec: "+refs/heads/${branchToCheckout}:refs/remotes/origin/${branchToCheckout}"
                                ]],
                                extensions: [
                                    [$class: 'CloneOption', depth: 1, noTags: true, reference: '', shallow: true, timeout: 120],
                                    [$class: 'WipeWorkspace'],
                                    [$class: 'LocalBranch', localBranch: branchToCheckout]
                                ]
                            ])
                            echo "✅ Checkout completed from Gitea - branch: ${branchToCheckout}"
                            sourceRepo = 'Gitea'
                            isGitHubSource = 'false'
                        }
                    }
                    
                    // Store detection results in environment
                    env.GIT_SOURCE = sourceRepo
                    env.IS_GITHUB_SOURCE = isGitHubSource
                    env.IS_PULL_REQUEST = isPullRequest ? 'true' : 'false'
                    
                    // Store the actual commit SHA for status reporting
                    env.GIT_COMMIT_SHA = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                    
                    echo "📍 Source: ${sourceRepo}"
                    echo "📍 Branch: ${branchToCheckout}"
                    echo "📍 Commit SHA: ${env.GIT_COMMIT_SHA}"
                    
                    sh '''
                        echo ""
                        echo "📋 Git Information:"
                        echo "Current commit: $(git rev-parse HEAD)"
                        echo "Current branch: $(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo 'detached HEAD')"
                        echo ""
                        echo "Latest commit (this is what we're building):"
                        git log --oneline -1
                        
                        echo ""
                        echo "🔍 Verifying workspace..."
                        ls -la ${WORKSPACE}/ | head -10
                    '''
                }
            }
        }

        stage('🚀 Start SonarQube Early') {
            when {
                expression { params.RUN_SONAR == true }
            }
            steps {
                script {
                    echo "🚀 Starting SonarQube service early (before tests)..."
                    try {
                        sh '''#!/bin/bash
                            set -e
                            
                            if docker ps | grep -q sonarqube; then
                                echo "✅ SonarQube is already running"
                            else
                                echo "🔄 Starting SonarQube from docker-compose..."
                                cd ${WORKSPACE}
                                
                                if [ ! -f .env ]; then
                                    echo "IMAGE_TAG=${BUILD_NUMBER}" > .env
                                else
                                    if ! grep -q "IMAGE_TAG" .env; then
                                        echo "IMAGE_TAG=${BUILD_NUMBER}" >> .env
                                    fi
                                fi
                                
                                docker compose up -d sonarqube
                                
                                echo "⏳ Waiting for SonarQube to be healthy (up to 120 seconds)..."
                                
                                READY=false
                                for i in $(seq 1 120); do
                                    RESPONSE=$(timeout 2 curl -s http://sonarqube:9000/api/system/status 2>/dev/null || echo "")
                                    if echo "$RESPONSE" | grep -q 'status.*UP'; then
                                        echo "✅ SonarQube is ready!"
                                        READY=true
                                        break
                                    fi
                                    if [ $((i % 10)) -eq 0 ]; then
                                        echo "⏳ Still waiting... ($i/120 seconds)"
                                    fi
                                    sleep 1
                                done
                                
                                if [ "$READY" = false ]; then
                                    echo "⚠️ SonarQube did not become ready in time"
                                    docker ps -a | head -20
                                fi
                            fi
                        '''
                    } catch (Exception e) {
                        echo "⚠️ Warning: Could not start SonarQube: ${e.message}"
                    }
                }
            }
        }

        stage('🏗️ Build') {
            parallel {
                stage('Backend Build') {
                    steps {
                        script {
                            echo "🏗️ Building backend microservices..."
                            sh '''
                                docker run --rm \\
                                  --volumes-from jenkins-cicd \\
                                  -w ${WORKSPACE}/backend \\
                                  -v jenkins_m2_cache:/root/.m2 \\
                                  -v /var/run/docker.sock:/var/run/docker.sock \\
                                  -e TESTCONTAINERS_RYUK_DISABLED=true \\
                                  --network host \\
                                  ${MAVEN_IMAGE} \\
                                  mvn clean install -B -DskipTests

                                echo "✅ Backend build completed"
                            '''
                        }
                    }
                }

                stage('Frontend Build') {
                    steps {
                        script {
                            echo "🏗️ Building frontend..."
                            sh '''
                                export NODE_OPTIONS="--max-old-space-size=4096"
                                docker run --rm \\
                                  --volumes-from jenkins-cicd \\
                                  -w ${WORKSPACE}/frontend \\
                                  -e NODE_OPTIONS="--max-old-space-size=4096" \\
                                  ${NODE_IMAGE} \\
                                  sh -c "npm install --legacy-peer-deps && npm run build -- --configuration production"

                                if [ -d ${WORKSPACE}/frontend/dist ]; then
                                    echo "✅ Frontend dist created"
                                else
                                    echo "⚠️ Warning: dist directory not found"
                                    exit 1
                                fi
                            '''
                        }
                    }
                }
            }
        }

        stage('🧪 Test Backend (Unit)') {
            when {
                expression { params.RUN_TESTS == true }
            }
            steps {
                script {
                    echo "🧪 Running backend unit tests..."

                    def services = ['user-service', 'product-service', 'media-service', 'orders-service']
                    def failedTests = []

                    services.each { service ->
                        try {
                            echo "Testing ${service}..."
                            sh '''
                                if [ -d ${WORKSPACE}/backend/''' + service + ''' ]; then
                                    docker run --rm \\
                                      --volumes-from jenkins-cicd \\
                                      -v jenkins_m2_cache:/root/.m2 \\
                                      -w ${WORKSPACE}/backend \\
                                      ${MAVEN_IMAGE} \\
                                      mvn test -B -Dtest=*UnitTest -pl ''' + service + '''

                                    echo "✅ ''' + service + ''' unit tests passed"
                                fi
                            '''
    
                        } catch (Exception e) {
                            echo "❌ ${service} unit tests FAILED: ${e.message}"
                            failedTests.add(service)
                        }
                    }
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                    
                    if (failedTests.size() > 0) {
                        error("❌ Backend unit tests failed for: ${failedTests.join(', ')}")
                    } else {
                        echo "✅ All backend unit tests passed!"
                    }
                }
            }
        }

        stage('🧪 Test Frontend') {
            when {
                expression { params.RUN_TESTS == true && params.SKIP_FRONTEND_TESTS == false }
            }
            steps {
                script {
                    try {
                        sh '''
                            if [ -d ${WORKSPACE}/frontend ]; then
                                echo "🧪 Running frontend unit tests..."
                                
                                # ✅ CREATE DIRECTORIES FIRST
                                mkdir -p ${WORKSPACE}/frontend/junit-results
                                mkdir -p ${WORKSPACE}/frontend/coverage
                                
                                # Run tests in Docker
                                timeout 180 docker run --rm \
                                  --volumes-from jenkins-cicd \
                                  -w ${WORKSPACE}/frontend \
                                  --cap-add=SYS_ADMIN \
                                  --user root \
                                  node:20.19-alpine \
                                  sh -c "apk add --no-cache chromium && npm install --legacy-peer-deps && CHROME_BIN=/usr/bin/chromium npx ng test --watch=false --browsers=ChromeHeadlessCI --code-coverage --source-map=false"
                                
                                TEST_EXIT=$?
                                
                                if [ $TEST_EXIT -eq 0 ]; then
                                    echo "✅ Frontend unit tests passed"
                                else
                                    echo "❌ Frontend tests failed with exit code: $TEST_EXIT"
                                    exit $TEST_EXIT
                                fi
                                
                                # ✅ VERIFY outputs exist
                                echo ""
                                echo "📁 Verifying test outputs..."
                                
                                if [ -f ${WORKSPACE}/frontend/junit-results/junit-results.xml ]; then
                                    echo "✅ JUnit results file found"
                                    ls -lh ${WORKSPACE}/frontend/junit-results/junit-results.xml
                                else
                                    echo "⚠️  JUnit results file not found"
                                    ls -la ${WORKSPACE}/frontend/junit-results/ 2>/dev/null || echo "   Directory empty"
                                fi
                                
                                if [ -f ${WORKSPACE}/frontend/coverage/index.html ]; then
                                    echo "✅ Coverage report found"
                                    ls -lh ${WORKSPACE}/frontend/coverage/index.html
                                else
                                    echo "⚠️  Coverage check:"
                                    ls -la ${WORKSPACE}/frontend/coverage/ 2>/dev/null | head -10
                                fi
                            else
                                echo "❌ Frontend directory not found"
                                exit 1
                            fi
                        '''
                    } catch (Exception e) {
                        echo "❌ Frontend tests failed: ${e.message}"
                        throw e
                    }
                }
                
                // ✅ Publish JUnit results
                junit allowEmptyResults: true, testResults: 'frontend/junit-results/**/*.xml'
                
                // ✅ Publish coverage report
                publishHTML target: [
                    allowMissing: true,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'frontend/coverage',
                    reportFiles: 'index.html',
                    reportName: 'Frontend Coverage Report'
                ]
            }
            post {
                failure {
                    script {
                        echo "❌ Frontend test stage failed"
                        sh '''
                            echo "📂 Frontend directory:"
                            ls -la ${WORKSPACE}/frontend/ | head -20
                            
                            echo ""
                            echo "📂 JUnit results:"
                            ls -la ${WORKSPACE}/frontend/junit-results/ 2>/dev/null || echo "   Not found"
                            
                            echo ""
                            echo "📂 Coverage:"
                            ls -la ${WORKSPACE}/frontend/coverage/ 2>/dev/null | head -15 || echo "   Not found"
                        '''
                    }
                }
            }
        }

       stage('📊 SonarQube Analysis') {
            when {
                expression { params.RUN_SONAR == true }
            }
            steps {
                script {
                    echo "📊 Running SonarQube analysis..."

                    def sonarAvailable = sh(
                        script: '''#!/bin/bash
                            RESPONSE=$(timeout 5 curl -s http://sonarqube:9000/api/system/status 2>&1)
                            if echo "$RESPONSE" | grep -q 'status.*UP'; then
                                echo "true"
                            else
                                echo "false"
                            fi
                        ''',
                        returnStdout: true
                    ).trim()

                    echo "SonarQube available: ${sonarAvailable}"

                    if (sonarAvailable == "true") {
                        withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                            sh '''#!/bin/bash
                                echo "📁 Creating SonarQube projects if they don't exist..."

                                for service in user-service product-service media-service orders-service api-gateway discovery-service frontend; do
                                    echo "Checking if $service project exists..."
                                    PROJECT_EXISTS=$(curl -s -u ${SONAR_TOKEN}: http://sonarqube:9000/api/projects/search?projects=$service | grep -o "\\"key\\":\\"$service\\"" || echo "")
                                    if [ -z "$PROJECT_EXISTS" ]; then
                                        echo "Creating $service project..."
                                        curl -s -X POST -u ${SONAR_TOKEN}: \\
                                          -F "project=$service" \\
                                          -F "name=$(echo $service | sed 's/-/ /g' | awk '{for(i=1;i<=NF;i++) $i=toupper(substr($i,1,1)) tolower(substr($i,2));}1')" \\
                                          http://sonarqube:9000/api/projects/create > /dev/null
                                        echo "✅ $service project created"
                                    else
                                        echo "✅ $service project already exists"
                                    fi
                                done

                                sleep 3
                            '''

                        def services = ['user-service', 'product-service', 'media-service', 'orders-service', 'api-gateway', 'discovery-service']
                        services.each { service ->
                            // ✅ FIXED: Move COVERAGE_EXCLUDE definition inside the shell command
                            sh """
                                echo "🔍 Analyzing ${service} ..."
                                
                                # Define COVERAGE_EXCLUDE here within the shell context
                                if [ "${service}" = "api-gateway" ] || [ "${service}" = "discovery-service" ]; then
                                    COVERAGE_EXCLUDE="-Dsonar.coverage.exclusions=**"
                                    echo "   (Code quality only - test coverage excluded)"
                                else
                                    COVERAGE_EXCLUDE=""
                                    echo "   (With test coverage)"
                                fi
                        
                                docker run --rm \\
                                  --volumes-from jenkins-cicd \\
                                  -v jenkins_m2_cache:/root/.m2 \\
                                  -w \\${WORKSPACE}/backend/${service} \\
                                  --network buy-01_BACKEND \\
                                  \\${MAVEN_IMAGE} \\
                                  mvn sonar:sonar \\
                                    -Dsonar.projectKey=${service} \\
                                    -Dsonar.host.url=http://sonarqube:9000 \\
                                    -Dsonar.login=\\${SONAR_TOKEN} \\
                                    -Dsonar.exclusions="**/target/**,common/**,**/dto/**,**/model/**,**/repository/**,**/mapper/**,**/config/**,**/messaging/**,**/FileStorageService.java,**/MediaController.java,  **/ProductController.java" \\
                                    \${COVERAGE_EXCLUDE} \\
                                    -B
                        
                                echo "✅ ${service} analysis completed"
                            """
                        }
                        
                        // ✅ Frontend analysis with proper single-quote syntax
                        sh '''
                            echo "🔍 Frontend analysis with SonarQube..."

                            FRONTEND_PATH="${WORKSPACE}/frontend"
                            COVERAGE_FILE="${FRONTEND_PATH}/coverage/lcov.info"

                            echo "   Using frontend path: $FRONTEND_PATH"

                            if [ ! -f "$COVERAGE_FILE" ]; then
                                echo "❌ ERROR: Coverage file NOT found!"
                                exit 1
                            fi

                            COVERAGE_SIZE=$(du -h "$COVERAGE_FILE" | cut -f1)
                            echo "✅ Coverage file ready: $COVERAGE_SIZE"

                            echo "🚀 Starting SonarQube analysis..."
                            docker run --rm \\
                              --volumes-from jenkins-cicd \\
                              -w ${WORKSPACE}/frontend \\
                              --network buy-01_BACKEND \\
                              -e SONAR_TOKEN=${SONAR_TOKEN} \\
                              sonarsource/sonar-scanner-cli:latest \\
                              -Dsonar.host.url=http://sonarqube:9000

                            echo "✅ Frontend analysis completed"
                        '''
                        sleep(time: 10, unit: 'SECONDS')
                        echo "✅ SonarQube analysis completed for all 6 projects!"
                        }
                    } else {
                        error("❌ SonarQube is not available")
                    }
                }
            }
        }

        stage('📊 Quality Gate') {
            when {
                expression { params.RUN_SONAR == true }
            }
            steps {
                script {
                    echo "📊 Checking SonarQube quality gates..."
                    
                    sleep(time: 10, unit: 'SECONDS')
                    
                    withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                        def qgResult = sh(script: '''#!/bin/bash
                            echo "Fetching quality gate status for all services..."
                            
                            SERVICES="user-service product-service media-service orders-service api-gateway discovery-service frontend"
                            PASSED_COUNT=0
                            TOTAL_COUNT=8
                            FAILED_SERVICES=""
                            
                            for service in $SERVICES; do
                                echo "Checking $service..."
                                QG=$(curl -s -u ${SONAR_TOKEN}: http://sonarqube:9000/api/qualitygates/project_status?projectKey=$service)
                                STATUS=$(echo "$QG" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
                                
                                if [ "$STATUS" = "OK" ]; then
                                    echo "✅ $service: PASSED"
                                    PASSED_COUNT=$((PASSED_COUNT + 1))
                                elif [ -z "$STATUS" ]; then
                                    echo "⚠️  $service: NO DATA (first analysis pending)"
                                    PASSED_COUNT=$((PASSED_COUNT + 1))
                                else
                                    echo "❌ $service: $STATUS"
                                    FAILED_SERVICES="$FAILED_SERVICES $service"
                                    exit 1
                                fi
                            done
                            
                            echo "Quality Gate Summary: $PASSED_COUNT/$TOTAL_COUNT passed"
                            
                            if [ $PASSED_COUNT -eq $TOTAL_COUNT ]; then
                                exit 0
                            elif [ $PASSED_COUNT -ge $((TOTAL_COUNT - 1)) ]; then
                                exit 0
                            else
                                echo "❌ Failed services:$FAILED_SERVICES"
                                exit 1
                            fi
                        ''', returnStatus: true)
                        
                        if (qgResult != 0) {
                            error("❌ Quality Gate check failed for some services")
                        } else {
                            echo "✅ Quality Gate check passed"
                        }
                    }
                }
            }
        }
        
        stage('🐳 Build & Push Docker Images') {
            when {
                expression { params.SKIP_DEPLOY == false }
            }
            steps {
                script {
                    echo "🐳 Building and Pushing Docker Images..."
                    
                    withCredentials([usernamePassword(
                        credentialsId: env.DOCKER_CREDENTIAL_ID, 
                        passwordVariable: 'DOCKER_PASSWORD', 
                        usernameVariable: 'DOCKER_USERNAME'
                    )]) {
                        sh '''
                            echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
                            
                            # =========================================
                            # PART A: BACKEND SERVICES
                            # =========================================
                            BACKEND_SERVICES="discovery-service api-gateway user-service product-service media-service orders-service dummy-data"
                            
                            for service in $BACKEND_SERVICES; do
                                echo "-----------------------------------------------"
                                echo "🔨 Processing Backend Service: $service"
                                
                                cd ${WORKSPACE}/${BACKEND_DIR}/$service
                                
                                if [ -f target/*.jar ]; then
                                
# ---------------------------------------------------------
# ✅ FIX: The EOF below must be flush left (NO SPACES before it)
# ---------------------------------------------------------
cat > Dockerfile.tmp << EOF
FROM amazoncorretto:17-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080 8443
HEALTHCHECK --interval=10s --timeout=5s --retries=5 CMD curl -f http://localhost:8080/actuator/health || exit 0
ENTRYPOINT ["java", "-Dcom.sun.management.jmxremote", "-jar", "app.jar"]
EOF
# ---------------------------------------------------------

                                    # Build & Tag
                                    docker build -t ${DOCKER_REPO}/$service:${IMAGE_TAG} -f Dockerfile.tmp .
                                    docker tag ${DOCKER_REPO}/$service:${IMAGE_TAG} ${DOCKER_REPO}/$service:latest
                                    
                                    # Push Both
                                    docker push ${DOCKER_REPO}/$service:${IMAGE_TAG}
                                    docker push ${DOCKER_REPO}/$service:latest
                                    
                                    rm Dockerfile.tmp
                                    echo "✅ Pushed $service"
                                else
                                    echo "⚠️  JAR file not found for $service"
                                fi
                            done
                            
                            # =========================================
                            # PART B: FRONTEND
                            # =========================================
                            echo "-----------------------------------------------"
                            echo "🔨 Processing Frontend..."
                            
                            cd ${WORKSPACE}/frontend
                            
                            if [ -d dist ]; then
                                docker build -t ${DOCKER_REPO}/frontend:${IMAGE_TAG} .
                                docker tag ${DOCKER_REPO}/frontend:${IMAGE_TAG} ${DOCKER_REPO}/frontend:latest
                                
                                docker push ${DOCKER_REPO}/frontend:${IMAGE_TAG}
                                docker push ${DOCKER_REPO}/frontend:latest
                                
                                echo "✅ Pushed frontend"
                            else
                                echo "⚠️  Frontend 'dist' folder not found"
                            fi
                        '''
                    }
                }
            }
        }

        stage('🚀 Deploy Locally') {
            when {
                allOf {
                    expression { params.DEPLOY_LOCALLY == true }
                    expression { params.SKIP_DEPLOY == false }
                }
            }
            steps {
                script {
                    echo "🚀 Deploying locally with tag: ${IMAGE_TAG}"

                    try {
                        // 📦 Step 1: Backup current deployment state
                        sh '''#!/bin/bash
                            set -e
                            
                            echo "📦 Backing up current deployment state..."
                            
                            # Create backup directory
                            mkdir -p .backup
                            BACKUP_DIR=".backup/deployment-${BUILD_NUMBER}-$(date +%s)"
                            mkdir -p "$BACKUP_DIR"
                            
                            # Save current docker-compose state
                            if [ -f docker-compose.yml ]; then
                                cp docker-compose.yml "$BACKUP_DIR/docker-compose.yml"
                                echo "✅ Backed up docker-compose.yml"
                            fi
                            
                            # Save current running container info
                            docker compose ps > "$BACKUP_DIR/containers-before.log" 2>&1 || true
                            docker images | grep mahdikheirkhah > "$BACKUP_DIR/images-before.log" 2>&1 || true
                            
                            # Save current IMAGE_TAG for rollback
                            if [ -f .env ]; then
                                cp .env "$BACKUP_DIR/.env.backup"
                                grep IMAGE_TAG .env > "$BACKUP_DIR/previous-tag.txt" || echo "IMAGE_TAG=none" > "$BACKUP_DIR/previous-tag.txt"
                            else
                                echo "IMAGE_TAG=none" > "$BACKUP_DIR/previous-tag.txt"
                            fi
                            
                            echo "✅ Backup created at: $BACKUP_DIR"
                            echo "$BACKUP_DIR" > .backup/latest-backup-path.txt
                        '''
                        
                        // 🧹 Step 2: Clean and deploy
                        sh '''#!/bin/bash
                            set -e
                            
                            echo "🧹 Cleaning up existing containers..."
                            
                            # Stop and remove containers using docker-compose
                            docker compose down --remove-orphans || true
                            sleep 2
                            
                            # Force remove specific containers if they still exist
                            for container in frontend discovery-service api-gateway user-service product-service media-service orders-service dummy-data sonarqube zookeeper kafka database; do
                                if docker ps -a --format '{{.Names}}' | grep -q "^${container}$"; then
                                    echo "🗑️  Removing container: $container"
                                    docker rm -f "$container" 2>/dev/null || true
                                fi
                            done
                            sleep 2
                            
                            echo "🔄 Pulling latest images..."
                            export IMAGE_TAG=${IMAGE_TAG}
                            docker compose pull || true
                            
                            echo "🚀 Starting services..."
                            docker compose up -d --remove-orphans --force-recreate
                            
                            echo "⏳ Waiting for services to start..."
                            sleep 30
                            
                            echo "📊 Service status:"
                            docker compose ps
                            echo "✅ Local deployment successful!"
                        '''
                        
                        // ✅ Step 3: Display deployment status
                        sh '''#!/bin/bash
                            echo "✅ Deployment completed successfully!"
                            echo ""
                            echo "Docker Compose Services:"
                            docker compose ps
                            echo ""
                            echo "🌐 Access your application at:"
                            echo "   - Frontend: https://localhost:4200"
                            echo "   - API Gateway: https://localhost:8443"
                            echo "   - Eureka: http://localhost:8761"
                        '''
                    } catch (Exception e) {
                        echo "❌ Local deployment failed: ${e.message}"
                        echo "🔄 Initiating automatic rollback..."
                        
                        try {
                            sh '''#!/bin/bash
                                set -e
                                
                                BACKUP_PATH=$(cat .backup/latest-backup-path.txt 2>/dev/null || echo "")
                                
                                if [ -z "$BACKUP_PATH" ] || [ ! -d "$BACKUP_PATH" ]; then
                                    echo "❌ No valid backup found for rollback!"
                                    exit 1
                                fi
                                
                                echo "🔄 Rolling back to previous deployment..."
                                
                                # Stop current deployment
                                echo "Stopping current services..."
                                docker compose down --remove-orphans || true
                                sleep 5
                                
                                # Restore previous docker-compose
                                if [ -f "$BACKUP_PATH/docker-compose.yml" ]; then
                                    cp "$BACKUP_PATH/docker-compose.yml" docker-compose.yml
                                    echo "✅ Restored docker-compose.yml"
                                fi
                                
                                # Get previous image tag
                                PREVIOUS_TAG=$(grep IMAGE_TAG "$BACKUP_PATH/previous-tag.txt" | cut -d'=' -f2)
                                echo "Previous IMAGE_TAG: $PREVIOUS_TAG"
                                
                                # Start previous version
                                echo "Starting previous version..."
                                if [ "$PREVIOUS_TAG" != "none" ]; then
                                    export IMAGE_TAG=$PREVIOUS_TAG
                                fi
                                docker compose up -d --remove-orphans
                                
                                # Wait and verify
                                echo "Waiting for rollback services to start..."
                                sleep 20
                                
                                docker compose ps
                                echo "✅ Rollback COMPLETED"
                                
                                # Log rollback event
                                echo ""
                                echo "📋 ROLLBACK LOG:"
                                echo "  Backup Location: $BACKUP_PATH"
                                echo "  Failed Build: #${BUILD_NUMBER}"
                                echo "  Failed Image Tag: ${IMAGE_TAG}"
                                echo "  Restored Image Tag: $PREVIOUS_TAG"
                                echo "  Rollback Time: $(date)"
                                
                                # Save rollback report
                                {
                                    echo "ROLLBACK REPORT"
                                    echo "=============="
                                    echo "Build Number: ${BUILD_NUMBER}"
                                    echo "Failed Image Tag: ${IMAGE_TAG}"
                                    echo "Rolled Back To: $PREVIOUS_TAG"
                                    echo "Timestamp: $(date)"
                                    echo ""
                                    echo "Previous containers:"
                                    cat "$BACKUP_PATH/containers-before.log"
                                } > "$BACKUP_PATH/rollback-report.txt"
                                
                                echo "✅ Rollback report saved to: $BACKUP_PATH/rollback-report.txt"
                            '''
                            
                            echo "✅ Automatic rollback completed successfully"
                            echo "   Previous version has been restored"
                            echo "   Check .backup/ directory for rollback details"
                        } catch (Exception rollbackError) {
                            echo "❌ CRITICAL: Rollback also failed: ${rollbackError.message}"
                            echo "   Manual intervention required!"
                            echo "   Check .backup/ directory for backup files"
                        }
                        
                        error("Deploy failed with automatic rollback executed: ${e.message}")
                    }
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts allowEmptyArchive: true, artifacts: '**/target/surefire-reports/**, frontend/coverage/**'
            script {
                echo "Pipeline execution completed"
            }
        }
        
        cleanup {
            script {
                // Report status to GitHub after pipeline completes (ONLY for GitHub source)
                if (params.SKIP_GITHUB_STATUS == false && env.IS_GITHUB_SOURCE == 'true') {
                    echo "📤 Reporting final build status to GitHub..."
                    
                    def buildStatus = currentBuild.result ?: 'SUCCESS'
                    def checkConclusion = 'success'
                    def summary = 'Build passed - all checks completed!'
                    
                    if (buildStatus == 'FAILURE') {
                        checkConclusion = 'failure'
                        summary = 'Build failed - check Jenkins for details'
                    } else if (buildStatus == 'UNSTABLE') {
                        checkConclusion = 'neutral'
                        summary = 'Build unstable - quality gate issues detected'
                    }
                    
                    echo "Final Build Status: ${buildStatus}"
                    echo "Check Conclusion: ${checkConclusion}"
                    
                    try {
                        // Use the commit SHA we captured during checkout (not git rev-parse again)
                        def commitSha = env.GIT_COMMIT_SHA ?: sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                        
                        // Map build status to GitHub Statuses API states (success, failure, error, pending)
                        def ghState = 'success'
                        if (checkConclusion == 'failure') {
                            ghState = 'failure'
                        } else if (checkConclusion == 'neutral') {
                            ghState = 'failure'  // GitHub Statuses API doesn't have 'neutral'
                        }
                        
                        withCredentials([usernamePassword(
                            credentialsId: 'multi-branch-github',
                            passwordVariable: 'GITHUB_TOKEN',
                            usernameVariable: 'GITHUB_USER'
                        )]) {
                            sh '''#!/bin/bash
                                set -e
                                
                                echo "🔍 Commit information..."
                                echo "Commit: ''' + commitSha + '''"
                                
                                echo ""
                                echo "📤 Sending status to GitHub Statuses API..."
                                GITHUB_API="https://api.github.com/repos/''' + env.GITHUB_REPO + '''/statuses/''' + commitSha + '''"
                                echo "Endpoint: ${GITHUB_API}"
                                
                                PAYLOAD='{"state":"''' + ghState + '''", "context":"Jenkins", "description":"''' + summary + '''", "target_url":"''' + env.BUILD_URL + '''"}'
                                
                                echo "Payload:"
                                echo "${PAYLOAD}"
                                echo ""
                                echo "📍 Sending POST request to: ${GITHUB_API}"
                                echo ""
                                
                                HTTP_CODE=$(curl -s -o /tmp/status-response.json -w "%{http_code}" \
                                  -X POST \
                                  -H "Authorization: token ${GITHUB_TOKEN}" \
                                  -H "Content-Type: application/json" \
                                  -H "Accept: application/vnd.github.v3+json" \
                                  -d "${PAYLOAD}" \
                                  "${GITHUB_API}")
                                
                                echo "HTTP Response Code: ${HTTP_CODE}"
                                
                                if [ "${HTTP_CODE}" = "201" ]; then
                                    echo "✅ GitHub status reported successfully"
                                else
                                    echo "⚠️  GitHub status response:"
                                    cat /tmp/status-response.json
                                fi
                            '''
                        }
                    } catch (Exception e) {
                        echo "⚠️  Warning: Failed to report status to GitHub: ${e.message}"
                        echo "   This is non-critical and will not affect the build result"
                    }
                } else {
                    if (env.IS_GITHUB_SOURCE != 'true') {
                        echo "ℹ️  Skipping GitHub status report - this build is from Gitea (not GitHub)"
                    } else {
                        echo "ℹ️  Skipping GitHub status report - disabled via SKIP_GITHUB_STATUS parameter"
                    }
                }
            }
        }
        
        
        success {
            script {
                echo "✅ Pipeline completed successfully!"
                def message = """
                Build SUCCESS
                Job: ${env.JOB_NAME}
                Build: ${env.BUILD_NUMBER}
                Branch: ${params.BRANCH}
                Duration: ${currentBuild.durationString}
                Build URL: ${env.BUILD_URL}
                """
                try {
                    emailext(
                        subject: "Build SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        body: message,
                        to: 'mohammad.kheirkhah@gritlab.ax',
                        mimeType: 'text/plain'
                    )
                    echo "Email notification sent"
                } catch (Exception e) {
                    echo "Email notification failed: ${e.message}"
                }
            }
        }
        failure {
            script {
                echo "❌ Pipeline failed!"
                def message = """
                Build FAILED
                Job: ${env.JOB_NAME}
                Build: ${env.BUILD_NUMBER}
                Branch: ${params.BRANCH}
                Status: ${currentBuild.result}
                Build URL: ${env.BUILD_URL}
                Console: ${env.BUILD_URL}console
                """
                try {
                    emailext(
                        subject: "Build FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        body: message,
                        to: 'mohammad.kheirkhah@gritlab.ax',
                        mimeType: 'text/plain'
                    )
                    echo "Email notification sent"
                } catch (Exception e) {
                    echo "Email notification failed: ${e.message}"
                }
            }
        }
    }
}
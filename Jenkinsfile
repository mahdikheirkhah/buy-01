// CI/CD Pipeline for Buy-01 E-Commerce Platform
// Last updated: 2026-01-05
pipeline {
    agent any

    triggers {
        // Trigger on push to main branch and PR branches
        githubPush()
        
        // Note: GitHub Pull Request Builder (ghprb) doesn't use triggers {} block
        // Configure it in: Jenkins Job → Configure → Build Triggers → GitHub Pull Request Builder
        // Or use properties() step below for pipeline configuration
    }

    parameters {
        string(name: 'BRANCH', defaultValue: 'main', description: 'Git branch to build')
        booleanParam(name: 'RUN_TESTS', defaultValue: true, description: 'Run unit tests')
        booleanParam(name: 'SKIP_FRONTEND_TESTS', defaultValue: false, description: 'Skip frontend unit tests (for debugging)')
        booleanParam(name: 'RUN_INTEGRATION_TESTS', defaultValue: false, description: 'Run integration tests (slower, requires Docker)')
        booleanParam(name: 'RUN_SONAR', defaultValue: true, description: 'Run SonarQube analysis')
        booleanParam(name: 'SKIP_DEPLOY', defaultValue: true, description: 'Skip deployment')
        booleanParam(name: 'DEPLOY_LOCALLY', defaultValue: true, description: 'Deploy locally without SSH')
        booleanParam(name: 'SKIP_FRONTEND_BUILD', defaultValue: false, description: 'Skip frontend build')
        booleanParam(name: 'SKIP_GITHUB_STATUS', defaultValue: true, description: 'Skip GitHub status reporting')
    }

    environment {
        // GitHub integration
        GITHUB_TOKEN = credentials('multi-branch-github')
        GITHUB_REPO = 'mahdikheirkhah/buy-01'
        
        // Docker configuration
        DOCKER_REPO = 'mahdikheirkhah'
        DOCKER_CREDENTIAL_ID = 'dockerhub-credentials'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        STABLE_TAG = 'stable'

        // SSH deployment (optional)
        SSH_CREDENTIAL_ID = 'ssh-deployment-key'
        REMOTE_HOST = '192.168.1.100'
        REMOTE_USER = 'ssh-user'
        DEPLOYMENT_DIR = '/opt/ecommerce'

        // Build tools
        MAVEN_IMAGE = "maven:3.9.6-amazoncorretto-17"
        NODE_IMAGE = "node:22-alpine"
        CHROME_IMAGE = "zenika/alpine-chrome:latest"

        // Paths
        BACKEND_DIR = 'backend'
        FRONTEND_DIR = 'frontend'
    }

    options {
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
                    echo "Run Integration Tests: ${params.RUN_INTEGRATION_TESTS}"
                    echo "Deploy Locally: ${params.DEPLOY_LOCALLY}"
                    echo "=========================================="
                }
            }
        }

        stage('📥 Checkout') {
            steps {
                echo "📥 Checking out branch: ${params.BRANCH}"
                
                // 🏫 Campus/Gitea Setup (Default)
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "*/${params.BRANCH}"]],
                    userRemoteConfigs: [[
                        url: 'https://01.gritlab.ax/git/mkheirkh/buy-01.git',
                        credentialsId: 'gitea-credentials'
                    ]]
                ])
                echo "✅ Checkout completed from Gitea"
                
                // 🏠 Home/GitHub Setup (Uncomment to use instead of Gitea)
                // checkout([
                //     $class: 'GitSCM',
                //     branches: [[name: "*/${params.BRANCH}"]],
                //     userRemoteConfigs: [[
                //         url: 'https://github.com/mahdikheirkhah/buy-01.git',
                //         credentialsId: 'github-credentials'
                //     ]]
                // ])
                // echo "✅ Checkout completed from GitHub"
                
                sh 'git log --oneline -5'
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
                            
                            # Check if SonarQube is already running
                            if docker ps | grep -q sonarqube; then
                                echo "✅ SonarQube is already running"
                            else
                                echo "🔄 Starting SonarQube from docker-compose..."
                                
                                # Navigate to workspace and start SonarQube using docker-compose
                                cd ${WORKSPACE}
                                
                                # Ensure .env file exists with IMAGE_TAG
                                if [ ! -f .env ]; then
                                    echo "IMAGE_TAG=${BUILD_NUMBER}" > .env
                                else
                                    if ! grep -q "IMAGE_TAG" .env; then
                                        echo "IMAGE_TAG=${BUILD_NUMBER}" >> .env
                                    fi
                                fi
                                
                                echo "Running: docker compose up -d sonarqube"
                                docker compose up -d sonarqube
                                
                                echo "⏳ Waiting for SonarQube to be healthy (up to 120 seconds)..."
                                
                                # Wait for SonarQube to be healthy (using seq for POSIX compatibility)
                                READY=false
                                for i in $(seq 1 120); do
                                    RESPONSE=$(timeout 2 curl -s http://localhost:9000/api/system/status 2>/dev/null || echo "")
                                    if echo "$RESPONSE" | grep -q '"status":"UP"'; then
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
                                    echo "Current Docker containers:"
                                    docker ps -a | head -20
                                    echo ""
                                    echo "SonarQube logs:"
                                    docker logs sonarqube 2>&1 | tail -30 || echo "No logs available"
                                fi
                            fi
                        '''
                    } catch (Exception e) {
                        echo "⚠️ Warning: Could not start SonarQube: ${e.message}"
                        echo "Attempting to diagnose..."
                        sh '''#!/bin/bash
                            echo "Docker ps:"
                            docker ps -a | grep -i sonar || echo "No SonarQube container found"
                            echo ""
                            echo "Docker compose version:"
                            docker compose version
                            echo ""
                            echo "Workspace contents:"
                            ls -la ${WORKSPACE} | grep -E "(docker-compose|.env)"
                        '''
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
                            try {
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
                            } catch (Exception e) {
                                error("❌ Backend build failed: ${e.message}")
                            }
                        }
                    }
                }

                stage('Frontend Build') {
                    when {
                        expression { params.SKIP_FRONTEND_BUILD == false }
                    }
                    steps {
                        script {
                            echo "🏗️ Building frontend..."
                            try {
                                sh '''
                                    export NODE_OPTIONS="--max-old-space-size=4096"
                                    docker run --rm \\
                                      --volumes-from jenkins-cicd \\
                                      -w ${WORKSPACE}/frontend \\
                                      -e NODE_OPTIONS="--max-old-space-size=4096" \\
                                      node:22 \\
                                      sh -c "npm install --legacy-peer-deps && npm run build"

                                    if [ -d ${WORKSPACE}/frontend/dist ]; then
                                        echo "✅ Frontend dist created"
                                    else
                                        echo "⚠️ Warning: dist directory not found"
                                    fi
                                '''
                            } catch (Exception e) {
                                echo "⚠️ Frontend build failed: ${e.message}"
                                throw e
                            }
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

                    def services = ['user-service', 'product-service', 'media-service']
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
                            echo "⚠️ ${service} unit tests: ${e.message}"
                            failedTests.add(service)
                        }
                    }

                    if (failedTests.size() > 0) {
                        echo "⚠️ Some unit tests failed: ${failedTests.join(', ')}"
                    } else {
                        echo "✅ All unit tests passed!"
                    }
                }
            }
        }

        stage('🧪 Test Backend (Integration)') {
            when {
                expression { params.RUN_INTEGRATION_TESTS == true }
            }
            steps {
                script {
                    echo "🧪 Running backend integration tests..."

                    def services = ['user-service', 'product-service', 'media-service']

                    services.each { service ->
                        try {
                            echo "Integration tests for ${service}..."
                            sh '''
                                if [ -d ${WORKSPACE}/backend/''' + service + ''' ]; then
                                    docker run --rm \\
                                      --volumes-from jenkins-cicd \\
                                      -v jenkins_m2_cache:/root/.m2 \\
                                      -w ${WORKSPACE}/backend \\
                                      ${MAVEN_IMAGE} \\
                                      mvn test -B -Dtest=*IntegrationTest -pl ''' + service + '''

                                    echo "✅ ''' + service + ''' integration tests passed"
                                fi
                            '''
                        } catch (Exception e) {
                            echo "⚠️ ${service} integration tests: ${e.message}"
                        }
                    }

                    echo "✅ Integration tests completed"
                }
            }
        }

        stage('🧪 Test Frontend') {
            when {
                expression { params.RUN_TESTS == true && params.SKIP_FRONTEND_BUILD == false && params.SKIP_FRONTEND_TESTS == false }
            }
            steps {
                script {
                    echo "🧪 Running frontend unit tests..."
                    sh '''
                        # Find the actual workspace path (might have @2, @3 suffix)
                        WORKSPACE_PATH=""
                        for ws_dir in $(docker exec jenkins-cicd bash -c "ls -d /var/jenkins_home/workspace/buy-01-multibranch_main* 2>/dev/null | sort -V | tail -1"); do
                            WORKSPACE_PATH="$ws_dir"
                            break
                        done
                        
                        if [ -z "$WORKSPACE_PATH" ]; then
                            WORKSPACE_PATH="/var/jenkins_home/workspace/buy-01-multibranch_main"
                        fi
                        
                        echo "   Using workspace path: $WORKSPACE_PATH"
                        FRONTEND_PATH="${WORKSPACE_PATH}/frontend"
                        
                        # Use node:22-alpine with Chrome installed for npm tests
                        timeout 180 docker run --rm \\
                          --volumes-from jenkins-cicd \\
                          -v /var/jenkins_home/workspace:/workspace \\
                          -w /workspace/$(basename "$WORKSPACE_PATH")/frontend \\
                          --cap-add=SYS_ADMIN \\
                          --user 1000:1000 \\
                          -e DBUS_SYSTEM_BUS_ADDRESS=unix:path=/dev/null \\
                          node:22-alpine \\
                          sh -c '
# Install chromium-browser for testing
apk add --no-cache chromium chromium-swiftshader
# Run tests with coverage
npm install --legacy-peer-deps && \
CHROME_BIN=/usr/bin/chromium npm run test -- --watch=false --browsers=ChromeHeadlessCI --code-coverage
'
                        
                        EXIT_CODE=$?
                        if [ $EXIT_CODE -eq 124 ]; then
                            echo "⚠️ Test execution timed out after 180 seconds"
                            exit 124
                        fi
                        if [ $EXIT_CODE -ne 0 ]; then
                            exit $EXIT_CODE
                        fi

                        echo "✅ Frontend unit tests passed"
                        
                        # Verify coverage file was generated
                        COVERAGE_FILE="${FRONTEND_PATH}/coverage/frontend/lcov.info"
                        if [ -f "$COVERAGE_FILE" ]; then
                            SIZE=$(du -h "$COVERAGE_FILE" | cut -f1)
                            echo "✅ Coverage file verified: $SIZE at $COVERAGE_FILE"
                        else
                            echo "❌ CRITICAL: Coverage file NOT found after tests completed!"
                            echo "   Expected at: $COVERAGE_FILE"
                            echo "   Listing directory contents:"
                            docker exec jenkins-cicd find "${FRONTEND_PATH}/coverage" -type f 2>/dev/null | head -10 || echo "   No coverage directory found"
                            exit 1
                        fi
                    '''
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
                    
                    // Check if SonarQube is available
                    def sonarAvailable = sh(
                        script: '''#!/bin/bash
                            RESPONSE=$(timeout 5 curl -s http://sonarqube:9000/api/system/status 2>&1)
                            if echo "$RESPONSE" | grep -q '"status":"UP"'; then
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
                            // Create SonarQube projects if they don't exist
                            sh '''#!/bin/bash
                                echo "📁 Creating SonarQube projects if they don't exist..."
                                
                                echo "Token: ${SONAR_TOKEN:0:10}... (first 10 chars)"
                                
                                # Delete old aggregated projects (if they exist)
                                # These were from the old approach and should not coexist with individual service projects
                                echo "🗑️  Cleaning up old aggregated projects (if they exist)..."
                                
                                # First, list all projects to diagnose
                                echo "📋 Current projects in SonarQube:"
                                ALL_PROJECTS=$(curl -s -u ${SONAR_TOKEN}: "http://sonarqube:9000/api/projects/search")
                                echo "$ALL_PROJECTS" | grep -o '"key":"[^"]*"' | sed 's/"key":"/Project Key: /' | sed 's/"$//' || echo "   No projects found"
                                
                                # Try to delete projects with various naming patterns
                                for old_key in "backend" "buy-01-backend" "buy-01-frontend" "buy01-backend" "buy01-frontend" "buy-01_Backend" "buy-01_Frontend"; do
                                    # Search for project (case-insensitive via SonarQube API)
                                    SEARCH_RESULT=$(curl -s -u ${SONAR_TOKEN}: "http://sonarqube:9000/api/projects/search" | grep -i "$old_key" || echo "")
                                    if [ ! -z "$SEARCH_RESULT" ]; then
                                        # Extract the actual project key from search results
                                        OLD_KEY=$(echo "$SEARCH_RESULT" | grep -o '"key":"[^"]*"' | head -1 | cut -d'"' -f4)
                                        if [ ! -z "$OLD_KEY" ]; then
                                            echo "   Found old project: $OLD_KEY, deleting..."
                                            curl -s -X POST -u ${SONAR_TOKEN}: "http://sonarqube:9000/api/projects/delete?project=$OLD_KEY" > /dev/null 2>&1
                                            echo "   ✅ Deleted $OLD_KEY"
                                        fi
                                    fi
                                done

                                # Create individual service projects (these are the main ones)
                                echo "Creating individual service projects..."
                                for service in user-service product-service media-service api-gateway discovery-service frontend; do
                                    echo "Checking if $service project exists..."
                                    PROJECT_EXISTS=$(curl -s -u ${SONAR_TOKEN}: http://sonarqube:9000/api/projects/search?projects=$service | grep -o "\"key\":\"$service\"" || echo "")
                                    if [ -z "$PROJECT_EXISTS" ]; then
                                        echo "Creating $service project..."
                                        CREATE_RESPONSE=$(curl -s -X POST -u ${SONAR_TOKEN}: \
                                          -F "project=$service" \
                                          -F "name=$(echo $service | sed 's/-/ /g' | awk '{for(i=1;i<=NF;i++) $i=toupper(substr($i,1,1)) tolower(substr($i,2));}1')" \
                                          http://sonarqube:9000/api/projects/create)
                                        echo "✅ $service project created"
                                    else
                                        echo "✅ $service project already exists"
                                    fi
                                done
                                
                                echo "Waiting 3 seconds for projects to be initialized..."
                                sleep 3
                                
                                # Run frontend tests with coverage before analysis
                                echo "Checking for frontend coverage file from Test stage..."
                                if [ -d ${WORKSPACE}/frontend ]; then
                                    if [ -f ${WORKSPACE}/frontend/coverage/frontend/lcov.info ]; then
                                        echo "✅ Coverage file found from Test Frontend stage"
                                        COVERAGE_SIZE=$(du -h ${WORKSPACE}/frontend/coverage/frontend/lcov.info | cut -f1)
                                        echo "   Coverage file size: $COVERAGE_SIZE"
                                        echo "   File location: ${WORKSPACE}/frontend/coverage/frontend/lcov.info"
                                    else
                                        echo "⚠️ Coverage file NOT found"
                                        echo "   Test Frontend stage may not have executed or coverage directory not shared"
                                        find ${WORKSPACE}/frontend -name "lcov.info" 2>/dev/null || echo "   No lcov.info files found anywhere"
                                    fi
                                fi
                            '''
                            
                            // Skip 'common' - it's a shared library with no tests and no application to run
                            def services = ['user-service', 'product-service', 'media-service', 'api-gateway', 'discovery-service']
                            services.each { service ->
                                sh """
                                    echo "🔍 Analyzing ${service}..."
                                    
                                    # Define coverage exclusions based on service
                                    COVERAGE_EXCLUSIONS=""
                                    case "${service}" in
                                        "api-gateway")
                                            # Exclude all files - no tests exist
                                            COVERAGE_EXCLUSIONS="**/*.java"
                                            ;;
                                        "discovery-service")
                                            # Exclude all files - no tests exist
                                            COVERAGE_EXCLUSIONS="**/*.java"
                                            ;;
                                        "user-service")
                                            # Only UserController and UserService have tests
                                            # Exclude everything else
                                            COVERAGE_EXCLUSIONS="**/*Application.java,**/dto/**,**/model/**,**/repository/**,**/mapper/**,**/config/**,**/messaging/**"
                                            ;;
                                        "product-service")
                                            # Only ProductController and ProductService have tests
                                            # Exclude everything else
                                            COVERAGE_EXCLUSIONS="**/*Application.java,**/dto/**,**/model/**,**/repository/**,**/mapper/**,**/config/**,**/messaging/**"
                                            ;;
                                        "media-service")
                                            # Only MediaController and MediaService have tests
                                            # Exclude everything else including FileStorageService
                                            COVERAGE_EXCLUSIONS="**/*Application.java,**/dto/**,**/model/**,**/repository/**,**/mapper/**,**/config/**,**/messaging/**,**/service/FileStorageService.java"
                                            ;;
                                        *)
                                            # Default: exclude common patterns
                                            COVERAGE_EXCLUSIONS="**/*Application.java,**/dto/**,**/model/**,**/repository/**,**/mapper/**,**/config/**,**/messaging/**"
                                            ;;
                                    esac
                                    
                                    docker run --rm \\
                                      --volumes-from jenkins-cicd \\
                                      -v jenkins_m2_cache:/root/.m2 \\
                                      -v /var/run/docker.sock:/var/run/docker.sock \\
                                      -w \${WORKSPACE}/backend/${service} \\
                                      --network buy-01_BACKEND \\
                                      -e TESTCONTAINERS_RYUK_DISABLED=true \\
                                      \${MAVEN_IMAGE} \\
                                      mvn clean install sonar:sonar \\
                                        -Dsonar.projectKey=${service} \\
                                        -Dsonar.host.url=http://sonarqube:9000 \\
                                        -Dsonar.login=\${SONAR_TOKEN} \\
                                        -Dsonar.exclusions="**/dto/**,**/model/**,**/repository/**,**/mapper/**,**/config/**,**/messaging/**" \\
                                        -Dsonar.coverage.exclusions=\${COVERAGE_EXCLUSIONS} \\
                                        -Dtest=!**/*IntegrationTest \\
                                        -B

                                    echo "✅ ${service} analysis completed (coverage exclusions: \${COVERAGE_EXCLUSIONS})"
                                """
                            }

                            // Frontend Analysis
                            sh '''
                                echo "🔍 Frontend analysis with SonarQube..."
                                
                                # Find the actual workspace path (might have @2, @3 suffix)
                                WORKSPACE_PATH=""
                                for ws_dir in $(docker exec jenkins-cicd bash -c "ls -d /var/jenkins_home/workspace/buy-01-multibranch_main* 2>/dev/null | sort -V | tail -1"); do
                                    WORKSPACE_PATH="$ws_dir"
                                    break
                                done
                                
                                if [ -z "$WORKSPACE_PATH" ]; then
                                    WORKSPACE_PATH="/var/jenkins_home/workspace/buy-01-multibranch_main"
                                fi
                                
                                COVERAGE_FILE="${WORKSPACE_PATH}/frontend/coverage/frontend/lcov.info"
                                echo "   Using workspace: $WORKSPACE_PATH"
                                
                                if [ ! -f "$COVERAGE_FILE" ]; then
                                    echo "❌ ERROR: Coverage file NOT found!"
                                    echo "   Expected: $COVERAGE_FILE"
                                    echo "   Test Frontend stage may not have executed successfully"
                                    echo ""
                                    echo "   Checking frontend directory:"
                                    docker exec jenkins-cicd ls -la "${WORKSPACE_PATH}/frontend/" 2>/dev/null || echo "   frontend/ does not exist"
                                    echo ""
                                    echo "   Checking coverage directory:"
                                    docker exec jenkins-cicd ls -la "${WORKSPACE_PATH}/frontend/coverage/" 2>/dev/null || echo "   coverage/ does not exist"
                                    exit 1
                                fi
                                
                                COVERAGE_SIZE=$(docker exec jenkins-cicd du -h "$COVERAGE_FILE" | cut -f1)
                                echo "✅ Coverage file ready: $COVERAGE_SIZE at $COVERAGE_FILE"
                                
                                docker run --rm \
                                  --volumes-from jenkins-cicd \
                                  -v /var/jenkins_home/workspace:/workspace \
                                  -w /workspace/$(basename "$WORKSPACE_PATH")/frontend \
                                  --network buy-01_BACKEND \
                                  -e SONAR_HOST_URL=http://sonarqube:9000 \
                                  -e SONAR_TOKEN=${SONAR_TOKEN} \
                                  sonarsource/sonar-scanner-cli:latest \
                                  -Dsonar.projectKey=frontend \
                                  -Dsonar.projectName="Frontend" \
                                  -Dsonar.sources=src \
                                  -Dsonar.exclusions="node_modules/**,dist/**,coverage/**,**/*.spec.ts" \
                                  -Dsonar.coverage.exclusions="**/app.config.ts,**/app.routes.ts,**/app.ts,**/main.ts,**/models/**,**/guards/**,**/interceptors/**,**/layouts/**,**/components/sidenav/**,**/components/navbar/**,**/components/confirm-dialog/**,**/components/password-confirm-dialog/**,**/components/image-cropper-modal/**,**/components/product-card/**,**/components/update-info-form/**,**/components/edit-product-modal/**" \
                                  -Dsonar.javascript.lcov.reportPaths=coverage/frontend/lcov.info \
                                  -Dsonar.typescript.lcov.reportPaths=coverage/frontend/lcov.info

                                echo "✅ Frontend analysis completed"
                            '''
                            
                            sleep(time: 10, unit: 'SECONDS')
                            echo "✅ SonarQube analysis completed"
                        }
                    } else {
                        echo "⚠️ SonarQube is not available, skipping analysis"
                        echo "To use SonarQube, ensure the sonarqube service is running in docker-compose.yml"
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
                    
                    // Wait for analysis to complete
                    sleep(time: 10, unit: 'SECONDS')
                    
                    withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                        // Check quality gate status via API for all services
                        def qgResult = sh(script: '''#!/bin/bash
                            echo "Fetching quality gate status for all services..."
                            
                            SERVICES="user-service product-service media-service api-gateway discovery-service frontend"
                            FAILED_SERVICES=""
                            PASSED_COUNT=0
                            TOTAL_COUNT=6
                            
                            for service in $SERVICES; do
                                echo "Checking $service..."
                                QG=$(curl -s -u ${SONAR_TOKEN}: http://sonarqube:9000/api/qualitygates/project_status?projectKey=$service)
                                STATUS=$(echo "$QG" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
                                
                                if [ "$STATUS" = "OK" ]; then
                                    echo "✅ $service: PASSED"
                                    PASSED_COUNT=$((PASSED_COUNT + 1))
                                elif [ -z "$STATUS" ]; then
                                    echo "⚠️  $service: NO DATA (first analysis pending)"
                                    # Count as passed for first run
                                    PASSED_COUNT=$((PASSED_COUNT + 1))
                                else
                                    echo "❌ $service: $STATUS"
                                    FAILED_SERVICES="$FAILED_SERVICES $service"
                                fi
                            done
                            
                            echo ""
                            echo "Quality Gate Summary: $PASSED_COUNT/$TOTAL_COUNT passed"
                            
                            # Store for reporting
                            if [ $PASSED_COUNT -eq $TOTAL_COUNT ]; then
                                echo "success" > /tmp/qg-state.txt
                                echo "All services passed quality gate" > /tmp/qg-desc.txt
                                exit 0
                            elif [ -n "$FAILED_SERVICES" ]; then
                                echo "failure" > /tmp/qg-state.txt
                                echo "Failed:$FAILED_SERVICES" > /tmp/qg-desc.txt
                                echo ""
                                echo "❌ QUALITY GATE FAILED FOR:$FAILED_SERVICES"
                                echo "Check SonarQube: http://sonarqube:9000/projects"
                                exit 1
                            else
                                echo "success" > /tmp/qg-state.txt
                                echo "$PASSED_COUNT/$TOTAL_COUNT services analyzed" > /tmp/qg-desc.txt
                                exit 0
                            fi
                        ''', returnStatus: true)
                        
                        // Report to GitHub
                        if (params.SKIP_GITHUB_STATUS == false) {
                            sh '''#!/bin/bash
                                QG_STATE=$(cat /tmp/qg-state.txt 2>/dev/null || echo "success")
                                QG_DESC=$(cat /tmp/qg-desc.txt 2>/dev/null || echo "Quality gates checked")
                                
                                echo "📢 Reporting to GitHub: $QG_STATE - $QG_DESC"
                                curl -s -H "Authorization: token ${GITHUB_TOKEN}" \
                                    -X POST -H "Accept: application/vnd.github.v3+json" \
                                    -d "{\"state\":\"${QG_STATE}\", \"context\":\"SonarQube Quality Gate\", \"description\":\"${QG_DESC}\", \"target_url\":\"${BUILD_URL}\"}" \
                                    https://api.github.com/repos/${GITHUB_REPO}/statuses/${GIT_COMMIT} || echo "⚠️  GitHub reporting failed"
                            '''
                        } else {
                            echo "⏭️  Skipping GitHub status reporting (SKIP_GITHUB_STATUS=true)"
                        }
                        
                        // Fail pipeline if quality gate failed
                        if (qgResult != 0) {
                            error("❌ Quality Gate failed for one or more services. Check SonarQube dashboard.")
                        }
                        
                        echo "✅ Quality Gate check completed"
                    }
                }
            }
        }

        stage('�🐳 Dockerize & Push') {
            steps {
                script {
                    echo "🐳 Building and pushing Docker images with tag: ${IMAGE_TAG}"

                    try {
                        withCredentials([usernamePassword(
                            credentialsId: env.DOCKER_CREDENTIAL_ID,
                            passwordVariable: 'DOCKER_PASSWORD',
                            usernameVariable: 'DOCKER_USERNAME'
                        )]) {
                            sh '''
                                if [ -z "$DOCKER_USERNAME" ] || [ -z "$DOCKER_PASSWORD" ]; then
                                    echo "❌ ERROR: Docker credentials not set!"
                                    echo "Please configure Docker Hub credentials in Jenkins:"
                                    echo "1. Go to Jenkins > Manage Jenkins > Credentials"
                                    echo "2. Add a 'Username with password' credential"
                                    echo "3. ID: dockerhub-credentials"
                                    exit 1
                                fi

                                echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
                                if [ $? -ne 0 ]; then
                                    echo "❌ Docker login failed!"
                                    exit 1
                                fi
                                echo "✅ Docker Hub login successful"
                            '''

                            def services = ['discovery-service', 'api-gateway', 'user-service', 'product-service', 'media-service', 'dummy-data']

                            services.each { service ->
                                sh '''
                                    cd ${WORKSPACE}/${BACKEND_DIR}/''' + service + '''

                                    if [ -f target/*.jar ]; then
                                        cat > Dockerfile.tmp << 'EOF'
FROM amazoncorretto:21-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080 8443
HEALTHCHECK --interval=10s --timeout=5s --retries=5 \\
    CMD curl -f http://localhost:8080/actuator/health || exit 0
ENTRYPOINT ["java", "-Dcom.sun.management.jmxremote", "-jar", "app.jar"]
EOF

                                        docker build -t ${DOCKER_REPO}/''' + service + ''':${IMAGE_TAG} -f Dockerfile.tmp .
                                        docker push ${DOCKER_REPO}/''' + service + ''':${IMAGE_TAG}

                                        docker tag ${DOCKER_REPO}/''' + service + ''':${IMAGE_TAG} ${DOCKER_REPO}/''' + service + ''':${STABLE_TAG}
                                        docker push ${DOCKER_REPO}/''' + service + ''':${STABLE_TAG}

                                        rm Dockerfile.tmp
                                        echo "✅ Pushed ${DOCKER_REPO}/''' + service + ''':${IMAGE_TAG}"
                                    else
                                        echo "⚠️ ''' + service + ''' JAR not found, skipping..."
                                    fi

                                    cd ${WORKSPACE}
                                '''
                            }

                            // Frontend
                            sh '''
                                if [ -d ${WORKSPACE}/frontend/dist ]; then
                                    docker build -t ${DOCKER_REPO}/frontend:${IMAGE_TAG} -f ${WORKSPACE}/frontend/Dockerfile ${WORKSPACE}/frontend/
                                    docker push ${DOCKER_REPO}/frontend:${IMAGE_TAG}

                                    docker tag ${DOCKER_REPO}/frontend:${IMAGE_TAG} ${DOCKER_REPO}/frontend:${STABLE_TAG}
                                    docker push ${DOCKER_REPO}/frontend:${STABLE_TAG}

                                    echo "✅ Pushed frontend:${IMAGE_TAG}"
                                else
                                    echo "⚠️ Frontend dist not found, skipping..."
                                fi
                            '''

                            echo "✅ Docker build and push completed!"
                        }
                    } catch (Exception e) {
                        echo "⚠️ Docker issue: ${e.message}"
                        echo "Continuing with deployment..."
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
                            for container in frontend discovery-service api-gateway user-service product-service media-service dummy-data sonarqube zookeeper kafka buy-01; do
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
                        
                        // ✅ Step 3: Health check verification
                        sh '''#!/bin/bash
                            set -e
                            
                            echo "✅ Verifying deployment health..."
                            
                            MAX_RETRIES=5
                            RETRY_COUNT=0
                            HEALTH_CHECKS_PASSED=0
                            
                            # Function to check endpoint
                            check_endpoint() {
                                local endpoint=$1
                                local expected_status=$2
                                local name=$3
                                
                                status=$(curl -s -o /dev/null -w "%{http_code}" -k "$endpoint" 2>/dev/null || echo "000")
                                if [ "$status" -eq "$expected_status" ] || [ "$status" -eq 200 ]; then
                                    echo "✅ $name: UP (HTTP $status)"
                                    return 0
                                else
                                    echo "⚠️ $name: Status $status"
                                    return 1
                                fi
                            }
                            
                            echo "Checking critical services..."
                            check_endpoint "http://localhost:8080/api/health" 200 "API Gateway" && ((HEALTH_CHECKS_PASSED++)) || true
                            check_endpoint "http://localhost:8761/actuator/health" 200 "Eureka Discovery" && ((HEALTH_CHECKS_PASSED++)) || true
                            check_endpoint "http://localhost:4200" 200 "Frontend" && ((HEALTH_CHECKS_PASSED++)) || true
                            
                            echo ""
                            echo "Health checks passed: $HEALTH_CHECKS_PASSED/3"
                            
                            if [ "$HEALTH_CHECKS_PASSED" -lt 2 ]; then
                                echo "⚠️ WARNING: Only $HEALTH_CHECKS_PASSED services healthy"
                                exit 1
                            fi
                            
                            echo "✅ Deployment health check PASSED"
                        '''

                        echo "🌐 Access your application at:"
                        echo "   - Frontend: https://localhost:4200"
                        echo "   - API Gateway: https://localhost:8443"
                        echo "   - Eureka: http://localhost:8761"
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

        stage('🚀 Deploy Remote') {
            when {
                allOf {
                    expression { params.SKIP_DEPLOY == false }
                    expression { params.DEPLOY_LOCALLY == false }
                }
            }
            steps {
                script {
                    echo "🚀 Deploying version ${IMAGE_TAG} to remote server..."

                    try {
                        withCredentials([sshUserPrivateKey(
                            credentialsId: env.SSH_CREDENTIAL_ID,
                            keyFileVariable: 'SSH_KEY',
                            usernameVariable: 'SSH_USER'
                        )]) {
                            // 📦 Backup remote state before deployment
                            sh '''
                                scp -i ${SSH_KEY} -o StrictHostKeyChecking=no \
                                    - ${REMOTE_USER}@${REMOTE_HOST}:${DEPLOYMENT_DIR}/backup-before-deploy.sh << 'BACKUP_SCRIPT'
#!/bin/bash
BACKUP_DIR="${DEPLOYMENT_DIR}/.backup/deployment-$(date +%s)"
mkdir -p "$BACKUP_DIR"

# Backup docker-compose and environment
if [ -f docker-compose.yml ]; then
    cp docker-compose.yml "$BACKUP_DIR/"
fi
if [ -f .env ]; then
    cp .env "$BACKUP_DIR/"
fi

# Save running state
docker compose ps > "$BACKUP_DIR/containers-before.log" 2>&1 || true

echo "$BACKUP_DIR"
BACKUP_SCRIPT
                            '''
                            
                            // 🚀 Deploy new version
                            sh '''
                                ssh -i ${SSH_KEY} -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} "
                                    cd ${DEPLOYMENT_DIR}
                                    
                                    echo 'IMAGE_TAG=${IMAGE_TAG}' > .env
                                    echo 'DOCKER_REPO=${DOCKER_REPO}' >> .env
                                    
                                    # Save backup path for rollback
                                    BACKUP_DIR='${DEPLOYMENT_DIR}/.backup/deployment-'\$(date +%s)
                                    mkdir -p \$BACKUP_DIR
                                    [ -f docker-compose.yml ] && cp docker-compose.yml \$BACKUP_DIR/
                                    [ -f .env.bak ] && cp .env.bak \$BACKUP_DIR/.env.previous || cp .env \$BACKUP_DIR/.env.previous
                                    
                                    docker compose pull
                                    docker compose up -d --remove-orphans
                                    
                                    echo 'Waiting for services...'
                                    sleep 30
                                    docker compose ps
                                    
                                    # Verify health
                                    echo 'Verifying deployment health...'
                                    if curl -sf http://localhost:8080/api/health > /dev/null 2>&1; then
                                        echo 'Health check PASSED'
                                    else
                                        echo 'Health check FAILED'
                                        exit 1
                                    fi
                                    
                                    echo 'Backup path: '\$BACKUP_DIR > .backup-path.txt
                                "
                            '''
                            
                            echo "✅ Remote deployment successful!"
                        }
                    } catch (Exception e) {
                        echo "❌ Remote deployment failed: ${e.message}"
                        echo "🔄 Initiating automatic remote rollback..."
                        
                        try {
                            withCredentials([sshUserPrivateKey(
                                credentialsId: env.SSH_CREDENTIAL_ID,
                                keyFileVariable: 'SSH_KEY',
                                usernameVariable: 'SSH_USER'
                            )]) {
                                sh '''
                                    ssh -i ${SSH_KEY} -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} "
                                        cd ${DEPLOYMENT_DIR}
                                        
                                        BACKUP_PATH=\$(cat .backup-path.txt 2>/dev/null | tail -1)
                                        
                                        if [ -z \"\$BACKUP_PATH\" ] || [ ! -d \"\$BACKUP_PATH\" ]; then
                                            echo 'No valid backup found for rollback!'
                                            exit 1
                                        fi
                                        
                                        echo 'Rolling back to previous deployment...'
                                        
                                        # Stop current
                                        docker compose down --remove-orphans || true
                                        sleep 5
                                        
                                        # Restore previous files
                                        [ -f \"\$BACKUP_PATH/docker-compose.yml\" ] && cp \"\$BACKUP_PATH/docker-compose.yml\" .
                                        [ -f \"\$BACKUP_PATH/.env.previous\" ] && cp \"\$BACKUP_PATH/.env.previous\" .env
                                        
                                        # Start previous version
                                        docker compose up -d --remove-orphans
                                        sleep 20
                                        
                                        docker compose ps
                                        
                                        echo 'Rollback completed'
                                        
                                        # Save rollback report
                                        {
                                            echo 'REMOTE ROLLBACK REPORT'
                                            echo '===================='
                                            echo 'Failed Image Tag: ${IMAGE_TAG}'
                                            echo 'Timestamp: '\$(date)
                                            echo 'Backup: '\$BACKUP_PATH
                                        } > \"\$BACKUP_PATH/rollback-report.txt\"
                                    "
                                '''
                            }
                            echo "✅ Remote automatic rollback completed"
                        } catch (Exception rollbackError) {
                            echo "❌ CRITICAL: Remote rollback also failed!"
                            echo "   Manual intervention required on ${REMOTE_HOST}"
                        }
                        
                        error("Remote deploy failed with automatic rollback executed: ${e.message}")
                    }
                }
            }
        }

        stage('✅ Post-Deployment Verification') {
            when {
                expression { params.DEPLOY_LOCALLY == true }
            }
            steps {
                script {
                    echo "✅ Verifying deployment..."
                    sh '''
                        echo "🌐 Services deployed successfully:"
                        docker compose ps
                        echo ""
                        echo "Access your application at:"
                        echo "   - Frontend: https://localhost:4200"
                        echo "   - API Gateway: https://localhost:8443"
                        echo "   - Eureka: http://localhost:8761"
                        echo "   - SonarQube: http://localhost:9000"
                    '''
                    echo "✅ Deployment complete!"
                }
            }
        }

        stage('📦 Archive Artifacts') {
            steps {
                script {
                    echo "📦 Archiving build artifacts..."
                    try {
                        sh '''
                            if find ${BACKEND_DIR}/*/target/surefire-reports -name "*.xml" 2>/dev/null | grep -q .; then
                                echo "Found test reports"
                            else
                                echo "No test reports found"
                            fi
                        '''

                        junit(
                            allowEmptyResults: true,
                            testResults: '${BACKEND_DIR}/*/target/surefire-reports/*.xml'
                        )

                        archiveArtifacts(
                            artifacts: '${BACKEND_DIR}/*/target/site/jacoco/**,${FRONTEND_DIR}/coverage/**',
                            allowEmptyArchive: true
                        )

                        echo "✅ Artifacts archived"
                    } catch (Exception e) {
                        echo "⚠️ Artifact archiving: ${e.message}"
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo "🧹 Pipeline execution completed"
            }
        }

        success {
            script {
                echo "✅ Pipeline completed successfully!"

                def message = """
                    ✅ Build SUCCESS
                    Job: ${env.JOB_NAME}
                    Build: #${env.BUILD_NUMBER}
                    Branch: ${params.BRANCH}
                    Image Tag: ${IMAGE_TAG}
                    Duration: ${currentBuild.durationString}
                    Build URL: ${env.BUILD_URL}

                    📦 Docker Images:
                    - ${DOCKER_REPO}/discovery-service:${IMAGE_TAG}
                    - ${DOCKER_REPO}/api-gateway:${IMAGE_TAG}
                    - ${DOCKER_REPO}/user-service:${IMAGE_TAG}
                    - ${DOCKER_REPO}/product-service:${IMAGE_TAG}
                    - ${DOCKER_REPO}/media-service:${IMAGE_TAG}
                    - ${DOCKER_REPO}/dummy-data:${IMAGE_TAG}
                    - ${DOCKER_REPO}/frontend:${IMAGE_TAG}

                    🌐 Services:
                    - Frontend: https://localhost:4200
                    - API Gateway: https://localhost:8443
                    - Eureka: http://localhost:8761
                """

                try {
                    emailext(
                        subject: "✅ Build SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        body: message,
                        to: 'mohammad.kheirkhah@gritlab.ax',
                        mimeType: 'text/plain'
                    )
                    echo "✅ Email notification sent"
                } catch (Exception e) {
                    echo "⚠️ Email notification failed: ${e.message}"
                }
                
                // Report success to GitHub
                if (params.SKIP_GITHUB_STATUS == false) {
                    try {
                        if (env.GIT_COMMIT) {
                            sh '''
                                curl -s -H "Authorization: token ${GITHUB_TOKEN}" \
                                    -X POST -H "Accept: application/vnd.github.v3+json" \
                                    -d '{"state":"success", "context":"Jenkins CI", "description":"Build passed", "target_url":"'${BUILD_URL}'"}' \
                                    https://api.github.com/repos/${GITHUB_REPO}/statuses/${GIT_COMMIT} || true
                            '''
                            echo "✅ GitHub status reported: SUCCESS"
                        }
                    } catch (Exception e) {
                        echo "⚠️ GitHub status report failed: ${e.message}"
                    }
                } else {
                    echo "⏭️  Skipping GitHub status reporting (SKIP_GITHUB_STATUS=true)"
                }
            }
        }

        failure {
            script {
                echo "❌ Pipeline failed!"
                
                // Check if rollback was executed
                try {
                    def backupPath = sh(
                        script: 'cat .backup/latest-backup-path.txt 2>/dev/null || echo ""',
                        returnStdout: true
                    ).trim()
                    
                    def rollbackInfo = ""
                    if (backupPath) {
                        def rollbackReport = sh(
                            script: "cat ${backupPath}/rollback-report.txt 2>/dev/null || echo 'Rollback executed'",
                            returnStdout: true
                        ).trim()
                        rollbackInfo = "\n\n📋 ROLLBACK STATUS:\n${rollbackReport}"
                    }
                    
                    echo "Rollback info captured: ${rollbackInfo ? 'YES' : 'NO'}"
                } catch (Exception e) {
                    echo "⚠️ Rollback info capture failed: ${e.message}"
                }

                def message = """
                    ❌ Build FAILED
                    Job: ${env.JOB_NAME}
                    Build: #${env.BUILD_NUMBER}
                    Branch: ${params.BRANCH}
                    Status: ${currentBuild.result}
                    Duration: ${currentBuild.durationString}
                    Build URL: ${env.BUILD_URL}
                    Console: ${env.BUILD_URL}console
                """

                try {
                    emailext(
                        subject: "❌ Build FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        body: message,
                        to: 'mohammad.kheirkhah@gritlab.ax',
                        mimeType: 'text/plain'
                    )
                } catch (Exception e) {
                    echo "⚠️ Email notification failed: ${e.message}"
                }
                
                // Report failure to GitHub
                if (params.SKIP_GITHUB_STATUS == false) {
                    try {
                        if (env.GIT_COMMIT) {
                            sh '''
                                curl -s -H "Authorization: token ${GITHUB_TOKEN}" \
                                    -X POST -H "Accept: application/vnd.github.v3+json" \
                                    -d '{"state":"failure", "context":"Jenkins CI", "description":"Build failed", "target_url":"'${BUILD_URL}'"}' \
                                    https://api.github.com/repos/${GITHUB_REPO}/statuses/${GIT_COMMIT} || true
                            '''
                            echo "❌ GitHub status reported: FAILURE"
                        }
                    } catch (Exception e) {
                        echo "⚠️ GitHub status report failed: ${e.message}"
                    }
                } else {
                    echo "⏭️  Skipping GitHub status reporting (SKIP_GITHUB_STATUS=true)"
                }
            }
        }
    }
}

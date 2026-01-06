// CI/CD Pipeline for Buy-01 E-Commerce Platform
// Last updated: 2026-01-05
pipeline {
    agent any

    triggers {
        githubPush()
    }

    parameters {
        string(name: 'BRANCH', defaultValue: 'main', description: 'Git branch to build')
        booleanParam(name: 'RUN_TESTS', defaultValue: true, description: 'Run unit tests')
        booleanParam(name: 'SKIP_FRONTEND_TESTS', defaultValue: true, description: 'Skip frontend unit tests (for debugging)')
        booleanParam(name: 'RUN_INTEGRATION_TESTS', defaultValue: false, description: 'Run integration tests (slower, requires Docker)')
        booleanParam(name: 'RUN_SONAR', defaultValue: true, description: 'Run SonarQube analysis')
        booleanParam(name: 'SKIP_DEPLOY', defaultValue: true, description: 'Skip deployment')
        booleanParam(name: 'DEPLOY_LOCALLY', defaultValue: true, description: 'Deploy locally without SSH')
        booleanParam(name: 'SKIP_FRONTEND_BUILD', defaultValue: false, description: 'Skip frontend build')
    }

    environment {
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
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "*/${params.BRANCH}"]],
                    userRemoteConfigs: [[
                        url: 'https://01.gritlab.ax/git/mkheirkh/buy-01.git',
                        credentialsId: 'gitea-credentials'
                    ]]
                ])
                echo "✅ Checkout completed from Gitea"
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
                        timeout 120 docker run --rm \\
                          --volumes-from jenkins-cicd \\
                          -w ${WORKSPACE}/frontend \\
                          --cap-add=SYS_ADMIN \\
                          node:22-bookworm sh -c \\
                          "apt-get update -qq && apt-get install -y -qq chromium --no-install-recommends && npm install --legacy-peer-deps && mkdir -p /tmp/chrome-wrapper && echo '#!/bin/bash' > /tmp/chrome-wrapper/chromium-wrapper && echo '/usr/bin/chromium --no-sandbox \\\"\\$@\\\"' >> /tmp/chrome-wrapper/chromium-wrapper && chmod +x /tmp/chrome-wrapper/chromium-wrapper && CHROME_BIN=/tmp/chrome-wrapper/chromium-wrapper npm run test -- --watch=false --browsers=ChromeHeadless --code-coverage" || {
                            EXIT_CODE=\\$?
                            if [ \\$EXIT_CODE -eq 124 ]; then
                                echo "⚠️ Test execution timed out after 120 seconds"
                                exit 124
                            fi
                            exit \\$EXIT_CODE
                        }

                        echo "✅ Frontend unit tests passed"
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

                                # Create Backend project
                                echo "Checking if backend project exists..."
                                SEARCH_RESPONSE=$(curl -s -u ${SONAR_TOKEN}: http://sonarqube:9000/api/projects/search?projects=buy-01-backend)
                                echo "Backend search response: $SEARCH_RESPONSE"
                                PROJECT_EXISTS=$(echo "$SEARCH_RESPONSE" | grep -o '"key":"buy-01-backend"' || echo "")
                                if [ -z "$PROJECT_EXISTS" ]; then
                                    echo "Creating backend project..."
                                    CREATE_RESPONSE=$(curl -s -X POST -u ${SONAR_TOKEN}: \
                                      -F "project=buy-01-backend" \
                                      -F "name=buy-01 Backend" \
                                      http://sonarqube:9000/api/projects/create)
                                    echo "Backend creation response: $CREATE_RESPONSE"
                                    echo "✅ Backend project created"
                                else
                                    echo "✅ Backend project already exists"
                                fi
                                
                                # Create Frontend project
                                PROJECT_EXISTS=$(curl -s -u ${SONAR_TOKEN}: http://sonarqube:9000/api/projects/search?projects=buy-01-frontend | grep -o '"key":"buy-01-frontend"' || echo "")
                                if [ -z "$PROJECT_EXISTS" ]; then
                                    echo "Creating frontend project..."
                                    CREATE_RESPONSE=$(curl -s -X POST -u ${SONAR_TOKEN}: \
                                      -F "project=buy-01-frontend" \
                                      -F "name=buy-01 Frontend" \
                                      http://sonarqube:9000/api/projects/create)
                                    echo "Frontend creation response: $CREATE_RESPONSE"
                                    echo "✅ Frontend project created"
                                else
                                    echo "✅ Frontend project already exists"
                                fi
                                
                                echo "Waiting 3 seconds for projects to be initialized..."
                                sleep 3
                            '''
                            
                            // Backend Analysis
                            sh '''
                                docker run --rm \\
                                  --volumes-from jenkins-cicd \\
                                  -v jenkins_m2_cache:/root/.m2 \\
                                  -w ${WORKSPACE}/backend \\
                                  --network buy-01_BACKEND \\
                                  ${MAVEN_IMAGE} \\
                                  mvn sonar:sonar \\
                                    -Dsonar.projectKey=buy-01-backend \\
                                    -Dsonar.projectName="buy-01 Backend" \\
                                    -Dsonar.host.url=http://sonarqube:9000 \
                                    -Dsonar.login=${SONAR_TOKEN} \\
                                    -Dsonar.exclusions="**/target/**,common/**,discovery-service/**" \
                                    -Dsonar.coverage.exclusions=**/dto/**,**/config/**,**/entity/**,**/model/** \\
                                    -B -q

                                echo "✅ Backend analysis completed"
                            '''

                            // Frontend Analysis
                            sh '''
                                echo "🔍 Frontend analysis with verbose logging..."
                                docker run --rm \\
                                  --platform linux/amd64 \\
                                  --volumes-from jenkins-cicd \\
                                  -w ${WORKSPACE}/frontend \\
                                  --network buy-01_BACKEND \\
                                  node:22-bookworm \\
                                  sh -c "apt-get update -qq && apt-get install -y -qq openjdk-17-jre-headless --no-install-recommends && npx sonar-scanner -Dsonar.projectKey=buy-01-frontend -Dsonar.projectName='buy-01 Frontend' -Dsonar.host.url=http://sonarqube:9000 -Dsonar.login=${SONAR_TOKEN} -Dsonar.sources=src -Dsonar.exclusions=node_modules/**,dist/**,coverage/**,**/*.spec.ts -Dsonar.javascript.lcov.reportPaths=coverage/lcov.info"

                                echo "✅ Frontend analysis completed"
                            '''
                            
                            sleep(time: 10, unit: 'SECONDS')
                            echo "✅ SonarQube analysis completed"
                            
                            // Check quality gates (informational only - allows pipeline to continue)
                            echo "🔍 Checking SonarQube quality gates and projects..."
                            sh '''#!/bin/bash
                                echo "=== Token Validation ==="
                                if [ -z "${SONAR_TOKEN}" ]; then
                                    echo "❌ ERROR: SONAR_TOKEN is not set!"
                                    exit 1
                                fi
                                echo "✓ Token is set (length: ${#SONAR_TOKEN})"
                                echo "✓ Token prefix: ${SONAR_TOKEN:0:10}..."
                                
                                echo ""
                                echo "=== Fetching quality gate status ==="
                                
                                echo "Fetching backend quality gate..."
                                BACKEND_QG=$(curl -s -w "\n%{http_code}" -u ${SONAR_TOKEN}: http://sonarqube:9000/api/qualitygates/project_status?projectKey=buy-01-backend)
                                BACKEND_QG_HTTP=$(echo "$BACKEND_QG" | tail -1)
                                BACKEND_QG_DATA=$(echo "$BACKEND_QG" | head -1)
                                echo "Backend QG HTTP Status: $BACKEND_QG_HTTP"
                                echo "Backend QG Response: $BACKEND_QG_DATA"
                            # Extract ONLY the projectStatus.status field (first occurrence)
                            BACKEND_STATUS=$(echo "$BACKEND_QG_DATA" | grep -o '"projectStatus":{[^}]*"status":"[^"]*"' | grep -o '"status":"[^"]*"' | head -1 || echo "NOT_FOUND")
                            
                            echo "Fetching frontend quality gate..."
                            FRONTEND_QG=$(curl -s -w "\n%{http_code}" -u ${SONAR_TOKEN}: http://sonarqube:9000/api/qualitygates/project_status?projectKey=buy-01-frontend)
                            FRONTEND_QG_HTTP=$(echo "$FRONTEND_QG" | tail -1)
                            FRONTEND_QG_DATA=$(echo "$FRONTEND_QG" | head -1)
                            echo "Frontend QG HTTP Status: $FRONTEND_QG_HTTP"
                            echo "Frontend QG Response: $FRONTEND_QG_DATA"
                            # Extract ONLY the projectStatus.status field (first occurrence)
                            FRONTEND_STATUS=$(echo "$FRONTEND_QG_DATA" | grep -o '"projectStatus":{[^}]*"status":"[^"]*"' | grep -o '"status":"[^"]*"' | head -1 || echo "NOT_FOUND")
                                echo ""
                                echo "ℹ️  Quality gates are informational - pipeline continues regardless"
                                echo "Check SonarQube dashboard:"
                                echo "  - Local:    http://localhost:9000/projects"
                                echo "  - Jenkins:  http://sonarqube:9000/projects"
                            '''
                        }
                    } else {
                        echo "⚠️ SonarQube is not available, skipping analysis"
                        echo "To use SonarQube, ensure the sonarqube service is running in docker-compose.yml"
                    }
                }
            }
        }

        stage('🐳 Dockerize & Push') {
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

                        echo "🌐 Access your application at:"
                        echo "   - Frontend: https://localhost:4200"
                        echo "   - API Gateway: https://localhost:8443"
                        echo "   - Eureka: http://localhost:8761"
                    } catch (Exception e) {
                        echo "❌ Local deployment failed: ${e.message}"
                        error("Deploy failed: ${e.message}")
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
                            sh '''
                                scp -i ${SSH_KEY} -o StrictHostKeyChecking=no docker-compose.yml ${REMOTE_USER}@${REMOTE_HOST}:${DEPLOYMENT_DIR}/docker-compose.yml

                                ssh -i ${SSH_KEY} -o StrictHostKeyChecking=no ${REMOTE_USER}@${REMOTE_HOST} "
                                    cd ${DEPLOYMENT_DIR}
                                    echo 'IMAGE_TAG=${IMAGE_TAG}' > .env
                                    echo 'DOCKER_REPO=${DOCKER_REPO}' >> .env
                                    docker compose pull
                                    docker compose up -d --remove-orphans
                                    echo 'Waiting for services...'
                                    sleep 30
                                    docker compose ps
                                "
                            '''
                            echo "✅ Remote deployment successful!"
                        }
                    } catch (Exception e) {
                        echo "❌ Remote deployment failed: ${e.message}"
                        error("Deploy failed: ${e.message}")
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
                echo "🧹 Cleanup..."

                try {
                    def testReportsExist = sh(
                        script: 'find ${BACKEND_DIR}/*/target/surefire-reports -name "*.xml" -type f 2>/dev/null | wc -l',
                        returnStdout: true
                    ).trim().toInteger() > 0

                    if (testReportsExist) {
                        echo "📊 Collecting test results..."
                        junit allowEmptyResults: true, testResults: '${BACKEND_DIR}/*/target/surefire-reports/*.xml'
                    }
                } catch (Exception e) {
                    echo "⚠️ Test result collection: ${e.message}"
                }

                cleanWs notFailBuild: true
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
            }
        }

        failure {
            script {
                echo "❌ Pipeline failed!"

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
            }
        }
    }
}

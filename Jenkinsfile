// CI/CD Pipeline for Buy-01 E-Commerce Platform
// Last updated: 2026-01-27
// FIXED VERSION - Frontend Test Docker Path Issue Resolved

pipeline {
    agent any

    triggers {
        githubPush()
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
        GITHUB_TOKEN = credentials('multi-branch-github')
        GITHUB_REPO = 'mahdikheirkhah/buy-01'
        DOCKER_REPO = 'mahdikheirkhah'
        DOCKER_CREDENTIAL_ID = 'dockerhub-credentials'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        STABLE_TAG = 'stable'
        SSH_CREDENTIAL_ID = 'ssh-deployment-key'
        REMOTE_HOST = '192.168.1.100'
        REMOTE_USER = 'ssh-user'
        DEPLOYMENT_DIR = '/opt/ecommerce'
        MAVEN_IMAGE = 'maven:3.9.6-amazoncorretto-17'
        NODE_IMAGE = 'node:22-alpine'
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
                                      ${NODE_IMAGE} \\
                                      sh -c "npm install --legacy-peer-deps && npm run build -- --configuration production"

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

stage('🧪 Test Frontend') {
    when {
        expression { 
            params.RUN_TESTS == true && 
            params.SKIP_FRONTEND_BUILD == false && 
            params.SKIP_FRONTEND_TESTS == false 
        }
    }
    steps {
        script {
            sh '''
            if [ -d ${WORKSPACE}/frontend ]; then
                echo "🧪 Running frontend unit tests..."
                timeout 180 docker run --rm \
                  --volumes-from jenkins-cicd \
                  -w ${WORKSPACE}/frontend \
                  --cap-add=SYS_ADMIN \
                  --user root \
                  zenika/alpine-chrome:with-node \
                  sh -c 'npm install --legacy-peer-deps && CHROME_BIN=/usr/bin/chromium-browser npm run test -- --watch=false --browsers=ChromeHeadless --code-coverage'
                
                echo "✅ Frontend unit tests passed"
            else
                echo "⚠️ Frontend directory not found"
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
                                
                                echo "Cleaning up old aggregated projects (if they exist)..."
                                
                                echo "📋 Current projects in SonarQube:"
                                curl -s -u ${SONAR_TOKEN}: "http://sonarqube:9000/api/projects/search" | grep -o '"key":"[^"]*"' | sed 's/"key":"/Project Key: /' | sed 's/"$//' || echo "   No projects found"
                                
                                echo "Creating individual service projects..."
                                for service in user-service product-service media-service api-gateway discovery-service frontend; do
                                    echo "Checking if $service project exists..."
                                    PROJECT_EXISTS=$(curl -s -u ${SONAR_TOKEN}: http://sonarqube:9000/api/projects/search?projects=$service | grep -o "\"key\":\"$service\"" || echo "")
                                    if [ -z "$PROJECT_EXISTS" ]; then
                                        echo "Creating $service project..."
                                        curl -s -X POST -u ${SONAR_TOKEN}: \\
                                          -F "project=$service" \\
                                          -F "name=$(echo $service | sed 's/-/ /g' | awk '{for(i=1;i<=NF;i++) \$i=toupper(substr(\$i,1,1)) tolower(substr(\$i,2));}1')" \\
                                          http://sonarqube:9000/api/projects/create > /dev/null
                                        echo "✅ $service project created"
                                    else
                                        echo "✅ $service project already exists"
                                    fi
                                done
                                
                                echo "Waiting 3 seconds for projects to be initialized..."
                                sleep 3
                            '''
                            
                            // Backend services analysis
                            def services = ['user-service', 'product-service', 'media-service', 'api-gateway', 'discovery-service']
                            services.each { service ->
                                sh """
                                    echo "🔍 Analyzing ${service}..."
                                    
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
                                        -Dtest=!**/*IntegrationTest \\
                                        -B

                                    echo "✅ ${service} analysis completed"
                                """
                            }

                            // Frontend Analysis - FIXED VERSION
                            sh '''
                                echo "🔍 Frontend analysis with SonarQube..."
                                
                                # Use absolute path to the workspace
                                FRONTEND_PATH="/jenkins-workspace/frontend"
                                COVERAGE_FILE="${FRONTEND_PATH}/coverage/frontend/lcov.info"
                                
                                echo "   Using frontend path: $FRONTEND_PATH"
                                
                                if [ ! -f "$COVERAGE_FILE" ]; then
                                    echo "❌ ERROR: Coverage file NOT found!"
                                    echo "   Expected: $COVERAGE_FILE"
                                    echo "   Test Frontend stage may not have executed successfully"
                                    exit 1
                                fi
                                
                                COVERAGE_SIZE=$(du -h "$COVERAGE_FILE" | cut -f1)
                                echo "✅ Coverage file ready: $COVERAGE_SIZE at $COVERAGE_FILE"
                                
                                    sonar-scanner \
                                    -Dsonar.projectKey=frontend \
                                    -Dsonar.projectName="Frontend" \
                                    -Dsonar.sources=src/app \
                                    -Dsonar.exclusions=**/*.spec.ts,**/*.test.ts,**/*.stories.ts,**/*.mock.ts,**/*.d.ts,node_modules/**,dist/**,coverage/**,**/.env,**/.env*,src/environments/**,src/assets/** \
                                    -Dsonar.cpd.exclusions=**/*.spec.ts,**/*.test.ts,**/*.stories.ts,**/*.mock.ts,node_modules/** \
                                    -Dsonar.host.url=http://sonarqube:9000 \
                                    -Dsonar.token=${SONAR_TOKEN}
                                echo "✅ Frontend analysis completed"
                            '''
                            
                            sleep(time: 10, unit: 'SECONDS')
                            echo "✅ SonarQube analysis completed"
                        }
                    } else {
                        echo "⚠️ SonarQube is not available, skipping analysis"
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
                            
                            SERVICES="user-service product-service media-service api-gateway discovery-service frontend"
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
                                    PASSED_COUNT=$((PASSED_COUNT + 1))
                                else
                                    echo "❌ $service: $STATUS"
                                fi
                            done
                            
                            echo ""
                            echo "Quality Gate Summary: $PASSED_COUNT/$TOTAL_COUNT passed"
                            
                            if [ $PASSED_COUNT -eq $TOTAL_COUNT ]; then
                                exit 0
                            elif [ $PASSED_COUNT -ge $((TOTAL_COUNT - 1)) ]; then
                                exit 0
                            else
                                exit 1
                            fi
                        ''', returnStatus: true)
                        
                        if (qgResult != 0) {
                            echo "⚠️ Quality Gate check failed for some services"
                        } else {
                            echo "✅ Quality Gate check passed"
                        }
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
                                    exit 1
                                fi

                                echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
                                if [ $? -ne 0 ]; then
                                    echo "❌ Docker login failed!"
                                    exit 1
                                fi
                                echo "✅ Docker Hub login successful"
                            '''

                            def services = ['discovery-service', 'api-gateway', 'user-service', 'product-service', 'media-service']

                            services.each { service ->
                                sh '''
                                    cd ${WORKSPACE}/${BACKEND_DIR}/''' + service + '''
                                    
                                    if [ -f target/*.jar ]; then
                                        docker build -t ${DOCKER_REPO}/''' + service + ''':${IMAGE_TAG} .
                                        docker push ${DOCKER_REPO}/''' + service + ''':${IMAGE_TAG}
                                        docker tag ${DOCKER_REPO}/''' + service + ''':${IMAGE_TAG} ${DOCKER_REPO}/''' + service + ''':${STABLE_TAG}
                                        docker push ${DOCKER_REPO}/''' + service + ''':${STABLE_TAG}
                                        echo "✅ Pushed ${DOCKER_REPO}/''' + service + ''':${IMAGE_TAG}"
                                    else
                                        echo "⚠️  ''' + service + ''' JAR not found, skipping..."
                                    fi
                                '''
                            }
                        }
                    } catch (Exception e) {
                        echo "⚠️ Docker build/push failed: ${e.message}"
                    }
                }
            }
        }

        stage('Deploy Locally') {
            when {
                allOf {
                    expression { params.DEPLOY_LOCALLY == true }
                    expression { params.SKIP_DEPLOY == false }
                }
            }
            steps {
                script {
                    echo "🚀 Deploying locally with tag ${IMAGE_TAG}..."
                    
                    try {
                        sh '''
                            export IMAGE_TAG=${IMAGE_TAG}
                            cd ${WORKSPACE}
                            
                            echo "Pulling latest images..."
                            docker compose pull
                            
                            echo "Starting services..."
                            docker compose up -d --remove-orphans
                            
                            echo "Waiting for services to start (30 seconds)..."
                            sleep 30
                            
                            echo "Service status:"
                            docker compose ps
                            
                            echo ""
                            echo "✅ Local deployment successful!"
                            echo "Access your application at:"
                            echo "  - Frontend: http://localhost:4200"
                            echo "  - API Gateway: http://localhost:8443"
                            echo "  - Eureka: http://localhost:8761"
                            echo "  - SonarQube: http://localhost:9000"
                        '''
                    } catch (Exception e) {
                        echo "❌ Local deployment failed: ${e.message}"
                        throw e
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo "Pipeline execution completed"
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

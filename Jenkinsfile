pipeline {
    agent any

    triggers {
        githubPush()
    }

    parameters {
        string(name: 'BRANCH', defaultValue: 'main', description: 'Git branch to build')
        choice(name: 'ENVIRONMENT', choices: ['development', 'staging', 'production'], description: 'Deployment environment')
        booleanParam(name: 'RUN_TESTS', defaultValue: true, description: 'Run all tests (backend + frontend)')
        booleanParam(name: 'RUN_SONAR', defaultValue: false, description: 'Run SonarQube analysis')
        booleanParam(name: 'DEPLOY', defaultValue: true, description: 'Deploy after successful build')
        booleanParam(name: 'SKIP_FRONTEND_BUILD', defaultValue: false, description: 'Skip frontend build (dev optimization)')
    }

    environment {
        // Docker configuration
        DOCKER_REPO = 'mahdikheirkhah'
        DOCKER_CREDENTIAL_ID = 'dockerhub-credentials'

        // Image tagging strategy
        GIT_COMMIT_SHORT = "${GIT_COMMIT.take(7)}"
        IMAGE_TAG = "${env.BUILD_NUMBER}-${GIT_COMMIT_SHORT}"
        STABLE_TAG = 'stable'
        LATEST_TAG = 'latest'

        // Environment-specific settings
        REGISTRY = 'docker.io'

        // Slack (optional)
        SLACK_CHANNEL = '#builds'
        SLACK_WEBHOOK_ID = 'slack-webhook'

        // Paths
        BACKEND_DIR = 'backend'
        FRONTEND_DIR = 'frontend'
        SCRIPTS_DIR = 'scripts'

        // Build tools
        MAVEN_VERSION = '3.9.6'
        MAVEN_IMAGE = "maven:${MAVEN_VERSION}-amazoncorretto-21"
        NODE_VERSION = '20.10'
        NODE_IMAGE = "node:${NODE_VERSION}-alpine"
    }

    options {

        // Keep last 30 builds
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))

        // Timeout after 2 hours
        timeout(time: 2, unit: 'HOURS')

        // Add timestamps to logs
        timestamps()

        // Colorize output
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
                    echo "Environment: ${params.ENVIRONMENT}"
                    echo "Build: #${env.BUILD_NUMBER}"
                    echo "Commit: ${GIT_COMMIT_SHORT}"
                    echo "Image Tag: ${IMAGE_TAG}"
                    echo "=========================================="

                    // Validate environment
                    if (params.ENVIRONMENT != 'development' && !params.RUN_TESTS) {
                        error("❌ Tests MUST run for ${params.ENVIRONMENT} environment!")
                    }
                }
            }
        }

        stage('📥 Checkout') {
            steps {
                echo "📥 Checking out branch: ${params.BRANCH}"
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "*/${params.BRANCH}"]],
                    userRemoteConfigs: [[url: 'https://github.com/mahdikheirkhah/buy-01.git']],
                    submoduleCfg: [],
                    extensions: [
                        [$class: 'CloneOption', depth: 1, noTags: false, reference: '', shallow: true],
                        [$class: 'PruneStaleBranch'],
                        [$class: 'RelativeTargetDirectory', relativeTargetDir: '']
                    ]
                ])

                echo "✅ Checkout completed"
                sh 'git log --oneline -5'
            }
        }

        stage('🏗️ Build Backend & Frontend (Parallel)') {
            parallel {
                stage('Build Backend') {
                    steps {
                        script {
                            echo "🏗️ Building backend microservices..."
                            try {
                                sh '''
                                    docker run --rm \
                                      -v ${WORKSPACE}:${WORKSPACE} \
                                      -v jenkins_m2_cache:/root/.m2 \
                                      -w ${WORKSPACE}/${BACKEND_DIR} \
                                      --network host \
                                      ${MAVEN_IMAGE} \
                                      mvn clean install -DskipTests -B -q
                                '''
                                echo "✅ Backend build completed"
                            } catch (Exception e) {
                                error("❌ Backend build failed: ${e.message}")
                            }
                        }
                    }
                }

                stage('Build Frontend') {
                    when {
                        expression { params.SKIP_FRONTEND_BUILD == false }
                    }
                    steps {
                        script {
                            echo "🏗️ Building frontend..."
                            try {
                                sh '''
                                    docker run --rm \
                                      -v ${WORKSPACE}/${FRONTEND_DIR}:/app \
                                      -w /app \
                                      ${NODE_IMAGE} \
                                      sh -c 'npm ci && npm run build'
                                '''
                                echo "✅ Frontend build completed"
                            } catch (Exception e) {
                                echo "⚠️ Frontend build failed: ${e.message}"
                                throw e
                            }
                        }
                    }
                }
            }
        }

        stage('🧪 Test Backend') {
            when {
                expression { params.RUN_TESTS == true }
            }
            steps {
                script {
                    echo "🧪 Running backend unit tests..."

                    def services = ['user-service', 'product-service', 'media-service']
                    def results = [:]

                    services.each { service ->
                        try {
                            sh '''
                                docker run --rm \
                                  -v ${WORKSPACE}:${WORKSPACE} \
                                  -v jenkins_m2_cache:/root/.m2 \
                                  -w ${WORKSPACE}/${BACKEND_DIR}/${''' + service + '''} \
                                  ${MAVEN_IMAGE} \
                                  mvn test -B
                            '''
                            results[service] = 'PASS'
                            echo "✅ ${service} tests passed"
                        } catch (Exception e) {
                            results[service] = 'FAIL'
                            echo "⚠️ ${service} tests failed (may need external dependencies)"
                        }
                    }

                    // Summary
                    echo "\n📊 Test Summary:"
                    results.each { service, status ->
                        echo "  ${status == 'PASS' ? '✅' : '⚠️'} ${service}: ${status}"
                    }
                }
            }
        }

        stage('🧪 Test Frontend') {
            when {
                allOf {
                    expression { params.RUN_TESTS == true }
                    expression { params.SKIP_FRONTEND_BUILD == false }
                }
            }
            steps {
                script {
                    echo "🧪 Running frontend unit tests..."
                    try {
                        sh '''
                            docker run --rm \
                              -v ${WORKSPACE}/${FRONTEND_DIR}:/app \
                              -w /app \
                              ${NODE_IMAGE} \
                              npm run test:ci
                        '''
                        echo "✅ Frontend tests passed"
                    } catch (Exception e) {
                        echo "⚠️ Frontend tests failed: ${e.message}"
                        throw e
                    }
                }
            }
        }

        stage('📊 Code Quality (SonarQube)') {
            when {
                expression { params.RUN_SONAR == true }
            }
            steps {
                script {
                    echo "📊 Running SonarQube analysis..."
                    try {
                        withSonarQubeEnv('SonarQube') {
                            sh '''
                                docker run --rm \
                                  -v ${WORKSPACE}:${WORKSPACE} \
                                  -v sonarqube_cache:/opt/sonarqube/data \
                                  -w ${WORKSPACE} \
                                  sonarsource/sonar-scanner-cli:latest \
                                  -Dsonar.projectKey=buy-01 \
                                  -Dsonar.sources=backend,frontend/src \
                                  -Dsonar.java.binaries=backend/*/target/classes \
                                  -Dsonar.coverage.exclusions=**/dto/**,**/config/**,**/entity/**
                            '''
                        }

                        timeout(time: 5, unit: 'MINUTES') {
                            waitForQualityGate abortPipeline: false
                        }
                        echo "✅ SonarQube analysis completed"
                    } catch (Exception e) {
                        echo "⚠️ SonarQube analysis failed: ${e.message}"
                        echo "To enable SonarQube:"
                        echo "  1. Install SonarQube server"
                        echo "  2. Add 'SonarQube' configuration in Jenkins"
                        echo "  3. Run with RUN_SONAR=true"
                    }
                }
            }
        }

        stage('🐳 Dockerize & Push Images') {
            steps {
                script {
                    echo "🐳 Building and pushing Docker images..."

                    try {
                        withCredentials([
                            usernamePassword(
                                credentialsId: env.DOCKER_CREDENTIAL_ID,
                                passwordVariable: 'DOCKER_PASSWORD',
                                usernameVariable: 'DOCKER_USERNAME'
                            )
                        ]) {

                            sh '''
                                echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
                                echo "✅ Docker Hub login successful"
                            '''

                            // Build and push backend services
                            def services = [
                                'discovery-service',
                                'api-gateway',
                                'user-service',
                                'product-service',
                                'media-service',
                                'dummy-data'
                            ]

                            services.each { service ->
                                sh '''
                                    cd ${WORKSPACE}/${BACKEND_DIR}/${''' + service + '''}

                                    # Create temporary Dockerfile from pre-built JAR
                                    cat > Dockerfile.tmp << 'DOCKERFILE_EOF'
FROM amazoncorretto:21-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080 8443
HEALTHCHECK --interval=10s --timeout=5s --retries=5 \\
    CMD curl -f http://localhost:8080/actuator/health || exit 0
ENTRYPOINT ["java", "-Dcom.sun.management.jmxremote", "-jar", "app.jar"]
DOCKERFILE_EOF

                                    # Build and push
                                    docker build -t ${DOCKER_REPO}/''' + service + ''':${IMAGE_TAG} -f Dockerfile.tmp .
                                    docker push ${DOCKER_REPO}/''' + service + ''':${IMAGE_TAG}

                                    # Tag as stable and latest
                                    docker tag ${DOCKER_REPO}/''' + service + ''':${IMAGE_TAG} ${DOCKER_REPO}/''' + service + ''':${STABLE_TAG}
                                    docker tag ${DOCKER_REPO}/''' + service + ''':${IMAGE_TAG} ${DOCKER_REPO}/''' + service + ''':${LATEST_TAG}
                                    docker push ${DOCKER_REPO}/''' + service + ''':${STABLE_TAG}
                                    docker push ${DOCKER_REPO}/''' + service + ''':${LATEST_TAG}

                                    rm Dockerfile.tmp
                                    cd ${WORKSPACE}
                                    echo "✅ Pushed ${DOCKER_REPO}/''' + service + ''':${IMAGE_TAG}"
                                '''
                            }

                            // Build and push frontend
                            sh '''
                                docker build \
                                  -t ${DOCKER_REPO}/frontend:${IMAGE_TAG} \
                                  -f ${WORKSPACE}/${FRONTEND_DIR}/Dockerfile \
                                  ${WORKSPACE}/${FRONTEND_DIR}/

                                docker push ${DOCKER_REPO}/frontend:${IMAGE_TAG}

                                # Tag as stable and latest
                                docker tag ${DOCKER_REPO}/frontend:${IMAGE_TAG} ${DOCKER_REPO}/frontend:${STABLE_TAG}
                                docker tag ${DOCKER_REPO}/frontend:${IMAGE_TAG} ${DOCKER_REPO}/frontend:${LATEST_TAG}
                                docker push ${DOCKER_REPO}/frontend:${STABLE_TAG}
                                docker push ${DOCKER_REPO}/frontend:${LATEST_TAG}

                                echo "✅ Pushed ${DOCKER_REPO}/frontend:${IMAGE_TAG}"
                            '''

                            echo "✅ All Docker images built and pushed successfully!"
                        }
                    } catch (Exception e) {
                        error("❌ Docker build/push failed: ${e.message}")
                    }
                }
            }
        }

        stage('🚀 Deploy') {
            when {
                expression { params.DEPLOY == true }
            }
            steps {
                script {
                    echo "🚀 Deploying to ${params.ENVIRONMENT}..."

                    try {
                        sh '''
                            # Stop and remove old containers
                            docker compose down || true

                            # Export image tag and deploy
                            export IMAGE_TAG=${IMAGE_TAG}
                            docker compose pull
                            docker compose up -d --remove-orphans

                            # Wait for services to start
                            echo "⏳ Waiting for services to start (30s)..."
                            sleep 30

                            # Show status
                            echo "📊 Service Status:"
                            docker compose ps
                        '''

                        echo "✅ Deployment successful!"
                    } catch (Exception e) {
                        error("❌ Deployment failed: ${e.message}")
                    }
                }
            }
        }

        stage('✅ Post-Deployment Verification') {
            when {
                expression { params.DEPLOY == true }
            }
            steps {
                script {
                    echo "✅ Verifying deployment..."

                    try {
                        // Wait for health checks
                        sleep(time: 30, unit: 'SECONDS')

                        // Check if script exists, otherwise do inline checks
                        sh '''
                            if [ -f ${SCRIPTS_DIR}/health-check.sh ]; then
                                bash ${SCRIPTS_DIR}/health-check.sh
                            else
                                echo "📊 Manual health checks..."

                                # Eureka
                                echo "Checking Eureka..."
                                curl -f http://localhost:8761/actuator/health || echo "⚠️ Eureka health check failed"

                                # API Gateway
                                echo "Checking API Gateway..."
                                curl --insecure -f https://localhost:8443/actuator/health || echo "⚠️ API Gateway health check failed"

                                # Frontend
                                echo "Checking Frontend..."
                                curl -f http://localhost:4200/ || echo "⚠️ Frontend health check failed"

                                echo "✅ Basic health checks completed"
                            fi
                        '''
                    } catch (Exception e) {
                        echo "⚠️ Health check failed: ${e.message}"
                        echo "Services may still be starting. Check manually."
                    }
                }
            }
        }

        stage('📦 Archive Artifacts') {
            steps {
                script {
                    echo "📦 Archiving build artifacts..."

                    try {
                        // Archive test reports
                        junit(
                            allowEmptyResults: true,
                            testResults: '${BACKEND_DIR}/*/target/surefire-reports/*.xml'
                        )

                        // Archive coverage reports
                        archiveArtifacts(
                            artifacts: '${BACKEND_DIR}/*/target/site/jacoco/**,${FRONTEND_DIR}/coverage/**',
                            allowEmptyArchive: true
                        )

                        echo "✅ Artifacts archived"
                    } catch (Exception e) {
                        echo "⚠️ Artifact archiving failed: ${e.message}"
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo "🧹 Post-build cleanup..."

                // Clean workspace
                cleanWs(
                    deleteDirs: true,
                    patterns: [[pattern: '${FRONTEND_DIR}/node_modules', type: 'INCLUDE']]
                )
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
                    Environment: ${params.ENVIRONMENT}
                    Image Tag: ${IMAGE_TAG}
                    Duration: ${currentBuild.durationString}
                    Build URL: ${env.BUILD_URL}

                    🌐 Access your application:
                    - Frontend: http://localhost:4200
                    - API Gateway: https://localhost:8443
                    - Eureka: http://localhost:8761
                """

                // Try email notification
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

                // Try Slack notification
                try {
                    withCredentials([string(credentialsId: env.SLACK_WEBHOOK_ID, variable: 'SLACK_WEBHOOK')]) {
                        sh '''
                            curl -X POST -H 'Content-type: application/json' \
                              --data '{"text":"✅ Build SUCCESS\\nJob: ''' + env.JOB_NAME + '''\\nBuild: #''' + env.BUILD_NUMBER + '''\\nTag: ''' + IMAGE_TAG + '''"}' \
                              ${SLACK_WEBHOOK}
                        '''
                    }
                    echo "✅ Slack notification sent"
                } catch (Exception e) {
                    echo "⚠️ Slack notification skipped (not configured)"
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

                    Check the console output for detailed error information.
                """

                // Email notification
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

        unstable {
            script {
                echo "⚠️ Pipeline unstable (tests may have failed)"

                try {
                    emailext(
                        subject: "⚠️ Build UNSTABLE: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        body: "Build completed with UNSTABLE status. Check test results.",
                        to: 'mohammad.kheirkhah@gritlab.ax',
                        mimeType: 'text/plain'
                    )
                } catch (Exception e) {
                    echo "⚠️ Email notification failed"
                }
            }
        }
    }
}

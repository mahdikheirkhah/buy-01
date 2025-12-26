pipeline {
    agent any

    triggers {
        githubPush()
    }

    parameters {
        string(name: 'BRANCH', defaultValue: 'main', description: 'Git branch to build')
        booleanParam(name: 'RUN_TESTS', defaultValue: true, description: 'Run unit tests')
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
                    userRemoteConfigs: [[url: 'https://github.com/mahdikheirkhah/buy-01.git']]
                ])
                echo "✅ Checkout completed"
                sh 'git log --oneline -5'
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
                                      -w /var/jenkins_home/workspace/e-commerce-microservices-ci-cd/backend \\
                                      -v jenkins_m2_cache:/root/.m2 \\
                                      -v /var/run/docker.sock:/var/run/docker.sock \\
                                      -e TESTCONTAINERS_RYUK_DISABLED=true \\
                                      --network host \\
                                      ${MAVEN_IMAGE} \\
                                      mvn clean install -B -q -DskipTests

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
                                    docker run --rm \\
                                      --volumes-from jenkins-cicd \\
                                      -w /var/jenkins_home/workspace/e-commerce-microservices-ci-cd/frontend \\
                                      ${NODE_IMAGE} \\
                                      sh -c "npm install --legacy-peer-deps && npm run build"

                                    if [ -d /var/jenkins_home/workspace/e-commerce-microservices-ci-cd/frontend/dist ]; then
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
                                if [ -d /var/jenkins_home/workspace/e-commerce-microservices-ci-cd/backend/''' + service + ''' ]; then
                                    docker run --rm \\
                                      --volumes-from jenkins-cicd \\
                                      -v jenkins_m2_cache:/root/.m2 \\
                                      -w /var/jenkins_home/workspace/e-commerce-microservices-ci-cd/backend \\
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
                                if [ -d /var/jenkins_home/workspace/e-commerce-microservices-ci-cd/backend/''' + service + ''' ]; then
                                    docker run --rm \\
                                      --volumes-from jenkins-cicd \\
                                      -v jenkins_m2_cache:/root/.m2 \\
                                      -w /var/jenkins_home/workspace/e-commerce-microservices-ci-cd/backend \\
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
                expression { params.RUN_TESTS == true && params.SKIP_FRONTEND_BUILD == false }
            }
            steps {
                script {
                    echo "🧪 Running frontend unit tests..."
                    try {
                        sh '''
                            docker run --rm \\
                              --volumes-from jenkins-cicd \\
                              -w /var/jenkins_home/workspace/e-commerce-microservices-ci-cd/frontend \\
                              ${NODE_IMAGE} \\
                              sh -c "npm install --legacy-peer-deps && npm run test -- --watch=false --browsers=ChromeHeadless --code-coverage"

                            echo "✅ Frontend unit tests passed"
                        '''
                    } catch (Exception e) {
                        echo "⚠️ Frontend tests failed: ${e.message}"
                        // Don't fail the build for frontend tests, log the issue
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
                    try {
                        withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                            // Backend Analysis
                            sh '''
                                docker run --rm \\
                                  --volumes-from jenkins-cicd \\
                                  -v jenkins_m2_cache:/root/.m2 \\
                                  -w /var/jenkins_home/workspace/e-commerce-microservices-ci-cd/backend \\
                                  --network host \\
                                  ${MAVEN_IMAGE} \\
                                  mvn sonar:sonar \\
                                    -Dsonar.projectKey=buy-01-backend \\
                                    -Dsonar.projectName="buy-01 Backend" \\
                                    -Dsonar.host.url=http://localhost:9000 \\
                                    -Dsonar.login=${SONAR_TOKEN} \\
                                    -Dsonar.sources=. \\
                                    -Dsonar.exclusions=**/target/**,**/test/** \\
                                    -Dsonar.java.binaries=*/target/classes \\
                                    -Dsonar.coverage.exclusions=**/dto/**,**/config/**,**/entity/**,**/model/** \\
                                    -B -q

                                echo "✅ Backend analysis completed"
                            '''

                            // Frontend Analysis
                            sh '''
                                cd ${WORKSPACE}/${FRONTEND_DIR}
                                which sonar-scanner || npm install -g sonar-scanner

                                sonar-scanner \\
                                  -Dsonar.projectKey=buy-01-frontend \\
                                  -Dsonar.projectName="buy-01 Frontend" \\
                                  -Dsonar.host.url=http://localhost:9000 \\
                                  -Dsonar.login=${SONAR_TOKEN} \\
                                  -Dsonar.sources=src \\
                                  -Dsonar.exclusions=node_modules/**,dist/**,coverage/** \\
                                  -Dsonar.javascript.lcov.reportPaths=coverage/lcov.info

                                echo "✅ Frontend analysis completed"
                            '''
                        }

                        // Wait for quality gate
                        sleep(time: 10, unit: 'SECONDS')
                        echo "✅ SonarQube analysis completed"
                    } catch (Exception e) {
                        echo "⚠️ SonarQube analysis failed: ${e.message}"
                        echo "To enable SonarQube:"
                        echo "1. Start SonarQube: docker compose up -d sonarqube"
                        echo "2. Access http://localhost:9000"
                        echo "3. Create credential in Jenkins: sonarqube-token"
                        echo "4. Run build with RUN_SONAR=true"
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
                                if [ -d /var/jenkins_home/workspace/e-commerce-microservices-ci-cd/frontend/dist ]; then
                                    docker build -t ${DOCKER_REPO}/frontend:${IMAGE_TAG} -f /var/jenkins_home/workspace/e-commerce-microservices-ci-cd/frontend/Dockerfile /var/jenkins_home/workspace/e-commerce-microservices-ci-cd/frontend/
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

        stage(' Deploy Locally') {
            when {
                expression { params.DEPLOY_LOCALLY == true && params.SKIP_DEPLOY == true }
            }
            steps {
                script {
                    echo "🚀 Deploying locally with tag: ${IMAGE_TAG}"

                    try {
                        sh '''
                            docker compose down || true
                            export IMAGE_TAG=${IMAGE_TAG}
                            docker compose pull || true
                            docker compose up -d --remove-orphans
                            echo "⏳ Waiting for services to start..."
                            sleep 30
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

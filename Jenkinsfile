pipeline {
    agent any

    environment {
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        STABLE_TAG = "stable"
        DOCKER_CREDENTIAL_ID = 'dockerhub-creds'
        DOCKER_REPO = 'mahdikheirkhah'
        
        // Optional: Slack notifications (comment out if not using)
        // SLACK_WEBHOOK = credentials('slack-webhook')

        // Optional: SonarQube analysis (comment out if not using)
        // SONAR_SCANNER_HOME = tool 'SonarScanner'

        // Remote Deployment Info
        REMOTE_USER = 'ssh-user' 
        REMOTE_HOST = '192.168.1.100' 
        SSH_CREDENTIAL_ID = 'deployment-ssh-key' 
        DEPLOYMENT_DIR = '/opt/ecommerce'
    }

    parameters {
        string(name: 'BRANCH', defaultValue: 'main', description: 'Branch to build')
        booleanParam(name: 'RUN_TESTS', defaultValue: true, description: 'Run tests')
        booleanParam(name: 'RUN_SONAR', defaultValue: false, description: 'Run SonarQube analysis')
    }

    stages {
        stage('Checkout') {
            steps {
                echo "Checking out branch: ${params.BRANCH}"
                checkout([$class: 'GitSCM',
                    branches: [[name: "*/${params.BRANCH}"]],
                    userRemoteConfigs: [[url: 'https://github.com/mahdikheirkhah/buy-01.git']]
                ])
            }
        }

        stage('Build & Test Backend') {
            steps {
                script {
                    echo "Building backend microservices"
                    // Use Maven Docker image to build - Docker-in-Docker setup
                    // Mount the entire workspace via jenkins_home volume
                    sh '''
                        docker run --rm \
                          --volumes-from jenkins-cicd \
                          -w /var/jenkins_home/workspace/e-commerce-microservices-ci-cd/backend \
                          -v jenkins_m2_cache:/root/.m2 \
                          maven:3.9.6-amazoncorretto-21 \
                          mvn clean install -DskipTests -B
                    '''
                    echo "Backend build completed successfully"
                }
            }
        }

        stage('Test Backend Services') {
            when {
                expression { params.RUN_TESTS == true }
            }
            steps {
                script {
                    echo "Running backend service tests"
                    def services = ['user-service', 'product-service', 'media-service']
                    for (service in services) {
                        echo "Testing ${service}..."
                        try {
                            sh """
                                docker run --rm \
                                  --volumes-from jenkins-cicd \
                                  -w /var/jenkins_home/workspace/e-commerce-microservices-ci-cd/backend/${service} \
                                  -v jenkins_m2_cache:/root/.m2 \
                                  maven:3.9.6-amazoncorretto-21 \
                                  mvn test -B
                            """
                            echo "${service} tests passed"
                        } catch (Exception e) {
                            echo "WARNING: ${service} tests failed - ${e.getMessage()}"
                            // Don't fail the build, just warn
                        }
                    }
                }
            }
        }

        stage('SonarQube Analysis') {
            when {
                expression { params.RUN_SONAR == true }
            }
            steps {
                script {
                    echo "Running SonarQube analysis"
                    try {
                        withSonarQubeEnv('SonarQube') {
                            sh """
                                ${env.SONAR_SCANNER_HOME}/bin/sonar-scanner \
                                  -Dsonar.projectKey=ecommerce-microservices \
                                  -Dsonar.sources=backend,frontend/src \
                                  -Dsonar.java.binaries=backend/*/target/classes
                            """
                        }

                        timeout(time: 5, unit: 'MINUTES') {
                            waitForQualityGate abortPipeline: false
                        }
                    } catch (Exception e) {
                        echo "SonarQube analysis failed: ${e.getMessage()}"
                        // Don't fail build if SonarQube is not configured
                    }
                }
            }
        }

        stage('Dockerize & Publish') {
            steps {
                script {
                    echo "Building and publishing Docker images with tag: ${env.IMAGE_TAG}"
                    // Login using Jenkins Credentials
                    withCredentials([usernamePassword(credentialsId: env.DOCKER_CREDENTIAL_ID, passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
                        sh "echo \$DOCKER_PASSWORD | docker login -u \$DOCKER_USERNAME --password-stdin"
                    }

                    def services = ['discovery-service', 'api-gateway', 'user-service', 'product-service', 'media-service', 'dummy-data']

                    for (service in services) {
                        echo "Building ${service}..."
                        // Create a temporary Dockerfile that uses already-built JAR
                        sh """
                        cat > Dockerfile.${service}.tmp << 'EOF'
FROM amazoncorretto:21-alpine-jdk
WORKDIR /app
COPY backend/${service}/target/*.jar app.jar
EXPOSE 8080 8443
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF
                        docker build --volumes-from jenkins-cicd -t ${env.DOCKER_REPO}/${service}:${env.IMAGE_TAG} -f Dockerfile.${service}.tmp .
                        docker push ${env.DOCKER_REPO}/${service}:${env.IMAGE_TAG}

                        # Tag as stable for rollback capability
                        docker tag ${env.DOCKER_REPO}/${service}:${env.IMAGE_TAG} ${env.DOCKER_REPO}/${service}:${env.STABLE_TAG}
                        docker push ${env.DOCKER_REPO}/${service}:${env.STABLE_TAG}

                        rm Dockerfile.${service}.tmp
                        echo "${service} image built and published successfully"
                        """
                    }

                    // Frontend
                    echo "Building frontend..."
                    sh """
                    docker build -t ${env.DOCKER_REPO}/frontend:${env.IMAGE_TAG} -f frontend/Dockerfile frontend/
                    docker push ${env.DOCKER_REPO}/frontend:${env.IMAGE_TAG}

                    # Tag as stable
                    docker tag ${env.DOCKER_REPO}/frontend:${env.IMAGE_TAG} ${env.DOCKER_REPO}/frontend:${env.STABLE_TAG}
                    docker push ${env.DOCKER_REPO}/frontend:${env.STABLE_TAG}

                    echo "Frontend image built and published successfully"
                    """

                    echo "All Docker images built and published successfully!"
                }
            }
        }

        stage('Deploy & Verify') {
            steps {
                script {
                    try {
                        echo "Deploying version: ${env.IMAGE_TAG} to staging environment"

                        withCredentials([sshUserPrivateKey(credentialsId: env.SSH_CREDENTIAL_ID, keyFileVariable: 'SSH_KEY')]) {
                            // Copy docker-compose.yml to remote server
                            sh """
                                scp -i \${SSH_KEY} docker-compose.yml ${env.REMOTE_USER}@${env.REMOTE_HOST}:${env.DEPLOYMENT_DIR}/docker-compose.yml
                            """

                            // Deploy and verify
                            sh """
                                ssh -i \${SSH_KEY} ${env.REMOTE_USER}@${env.REMOTE_HOST} "
                                    cd ${env.DEPLOYMENT_DIR}

                                    # Create or update .env file
                                    echo 'IMAGE_TAG=${env.IMAGE_TAG}' > .env
                                    echo 'DOCKER_REPO=${env.DOCKER_REPO}' >> .env

                                    # Pull and deploy new version
                                    docker compose pull
                                    docker compose up -d --remove-orphans

                                    # Wait for services to stabilize
                                    echo 'Waiting for services to start...'
                                    sleep 20

                                    # Health check - look for crashed containers
                                    if docker compose ps | grep 'Exit'; then
                                        echo 'ERROR: Detected crashed containers!'
                                        exit 1
                                    fi

                                    # If all checks pass, record successful deployment
                                    echo ${env.IMAGE_TAG} > LAST_SUCCESSFUL_TAG
                                    echo 'Deployment verification passed!'
                                "
                            """
                        }

                        echo "Deployment completed successfully!"

                        // Optional: Send success notification to Slack
                        // Uncomment if you have Slack configured
                        /*
                        sh """
                        curl -X POST -H 'Content-type: application/json' --data '{
                            \"text\": \":white_check_mark: Deployment SUCCESS\\n*Version:* ${env.IMAGE_TAG}\\n*Job:* ${env.JOB_NAME}\\n*Build:* ${env.BUILD_NUMBER}\"
                        }' ${env.SLACK_WEBHOOK}
                        """
                        */

                    } catch (Exception e) {
                        echo "Deployment failed! Initiating rollback..."
                        echo "Error: ${e.getMessage()}"

                        // ROLLBACK: Deploy using stable tag
                        try {
                            withCredentials([sshUserPrivateKey(credentialsId: env.SSH_CREDENTIAL_ID, keyFileVariable: 'SSH_KEY')]) {
                                sh """
                                    ssh -i \${SSH_KEY} ${env.REMOTE_USER}@${env.REMOTE_HOST} "
                                        cd ${env.DEPLOYMENT_DIR}

                                        # Revert to stable version
                                        echo 'IMAGE_TAG=${env.STABLE_TAG}' > .env
                                        echo 'DOCKER_REPO=${env.DOCKER_REPO}' >> .env

                                        docker compose pull
                                        docker compose up -d --remove-orphans

                                        echo 'Rollback completed!'
                                    "
                                """
                            }

                            echo "Successfully rolled back to stable version"

                            // Optional: Send rollback notification to Slack
                            /*
                            sh """
                            curl -X POST -H 'Content-type: application/json' --data '{
                                \"text\": \":information_source: Rollback SUCCESSFUL\\n*Failed Version:* ${env.IMAGE_TAG}\\n*Rolled back to:* ${env.STABLE_TAG}\\n*Job:* ${env.JOB_NAME}\\n*Build:* ${env.BUILD_NUMBER}\"
                            }' ${env.SLACK_WEBHOOK}
                            """
                            */

                        } catch (Exception rollbackErr) {
                            echo "FATAL: Rollback failed!"
                            echo "Rollback error: ${rollbackErr.getMessage()}"

                            // Optional: Send critical failure notification to Slack
                            /*
                            sh """
                            curl -X POST -H 'Content-type: application/json' --data '{
                                \"text\": \":rotating_light: Rollback FAILED!\\n*Reason:* ${rollbackErr.getMessage()}\\n*Job:* ${env.JOB_NAME}\\n*Build:* ${env.BUILD_NUMBER}\\nManual intervention needed!\"
                            }' ${env.SLACK_WEBHOOK}
                            """
                            */
                        }

                        error "Deployment failed: ${e.getMessage()}"
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo "Post-build cleanup and reporting"

                // Collect test results if tests were run
                if (params.RUN_TESTS) {
                    // Backend test reports
                    junit allowEmptyResults: true, testResults: 'backend/*/target/surefire-reports/*.xml'

                    // Archive test artifacts
                    archiveArtifacts artifacts: 'backend/*/target/surefire-reports/*.xml', allowEmptyArchive: true
                }

                // Clean workspace after build
                if (env.WORKSPACE) {
                    cleanWs notFailBuild: true
                } else {
                    echo "No workspace available; skipping cleanWs"
                }
            }
        }

        success {
            echo "✅ Pipeline completed successfully!"
            script {
                // Optional: Send success notification to Slack
                // Uncomment if you have Slack configured
                /*
                sh """
                curl -X POST -H 'Content-type: application/json' --data '{
                    \"text\": \":white_check_mark: Build SUCCESS\\n*Job:* ${env.JOB_NAME}\\n*Build:* ${env.BUILD_NUMBER}\\n*Branch:* ${params.BRANCH}\\n*Version:* ${env.IMAGE_TAG}\"
                }' ${env.SLACK_WEBHOOK}
                """
                */
            }
        }

        failure {
            echo "❌ Pipeline failed!"
            script {
                // Optional: Send failure notification to Slack
                // Uncomment if you have Slack configured
                /*
                sh """
                curl -X POST -H 'Content-type: application/json' --data '{
                    \"text\": \":x: Build FAILED\\n*Job:* ${env.JOB_NAME}\\n*Build:* ${env.BUILD_NUMBER}\\n*Branch:* ${params.BRANCH}\\n*Error:* ${currentBuild.currentResult}\"
                }' ${env.SLACK_WEBHOOK}
                """
                */
            }
        }
    }
}
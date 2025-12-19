/**
 * Jenkinsfile: CI/CD Pipeline for E-commerce Microservices
 * This declarative pipeline automates the build, test, and containerization
 * of the multi-service platform, pushing images to Docker Hub.
 */
pipeline {
    agent any

    // Environment variables used throughout the pipeline
    environment {
        // Tag used for Docker images (e.g., '1.0.0' or 'latest')
        IMAGE_TAG = "${env.BUILD_NUMBER}"

        // Using Docker Hub credentials ID stored in Jenkins
        // *** IMPORTANT: You need to create a Jenkins credential with this ID. ***
        DOCKER_CREDENTIAL_ID = 'dockerhub-creds'

        // Docker Hub Organization or Username where the packages will be pushed.
        DOCKER_REPO = 'mahdikheirkhah'

        // Deployment configuration
        REMOTE_USER = 'ssh-user' // User for SSH connection to deploy server
        REMOTE_HOST = '192.168.1.100' // IP/Hostname of the deployment server
        SSH_CREDENTIAL_ID = 'deployment-ssh-key' // Jenkins ID for SSH key credentials
        DEPLOYMENT_DIR = '/opt/ecommerce' // Directory on the remote host
    }

    stages {
        // -----------------------------------------------------------------
        // 1. Checkout & Setup
        // -----------------------------------------------------------------
        stage('Checkout Source Code') {
            steps {
                // Ensure the Git plugin is installed in Jenkins
                checkout scm
            }
        }

        // -----------------------------------------------------------------
        // 2. Build Backend Services (Java/Maven)
        // -----------------------------------------------------------------
        stage('Build & Test Backend') {
            steps {
                script {
                    echo 'Building all Java microservices...'
                    // Use the Maven wrapper to ensure environment consistency
                    // We skip tests here to speed up the build stage; they run in the next stage
                    sh './mvnw clean install -DskipTests'
                }
            }
        }

        // -----------------------------------------------------------------
        // 3. Run Automated Tests
        // -----------------------------------------------------------------
        stage('Automated Testing') {
            parallel {
                // Run JUnit tests for all Java services
                stage('Backend Unit Tests') {
                    steps {
                        script {
                            echo 'Running JUnit tests...'
                            // Use Maven to run tests and fail the build if any test fails
                            sh './mvnw test'
                        }
                    }
                }

                // Run Angular tests (Placeholder: requires Node.js/Karma/Jasmine setup)
                stage('Frontend Unit Tests') {
                    // This requires an agent with Node.js setup
                    agent { label 'node-agent' } // IMPORTANT: Replace 'node-agent' with your actual agent label or set up Node on the main Jenkins server.
                    steps {
                        script {
                            echo 'Installing Node dependencies and running Angular tests...'
                            // Assuming 'frontend' directory contains the Angular project
                            sh 'cd frontend && npm install && npm test -- --no-watch --browsers=ChromeHeadless'
                        }
                    }
                }
            }
            // Post-action to publish JUnit reports after tests run
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        // -----------------------------------------------------------------
        // 4. Build and Push Docker Images
        // -----------------------------------------------------------------
        stage('Dockerize & Publish') {
            steps {
                script {
                    // Authenticate with Docker Hub (defaults to docker.io)
                    withCredentials([usernamePassword(credentialsId: env.DOCKER_CREDENTIAL_ID, passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
                        // Logging into Docker Hub (docker.io)
                        sh "echo \$DOCKER_PASSWORD | docker login -u \$DOCKER_USERNAME --password-stdin"
                    }

                    // Define services to build
                    def services = ['discovery-service', 'api-gateway', 'user-service', 'product-service', 'media-service', 'dummy-data']

                    // Build and tag each service
                    for (int i = 0; i < services.size(); i++) {
                        def service = services[i]
                        // Tagging structure for Docker Hub: USERNAME/SERVICE:TAG
                        sh """
                        echo "Building Docker image for ${service}"
                        docker build -t ${env.DOCKER_REPO}/${service}:${env.IMAGE_TAG} \
                            -f Dockerfile.java \
                            --build-arg SERVICE_NAME=${service} .

                        echo "Pushing Docker image for ${service}"
                        docker push ${env.DOCKER_REPO}/${service}:${env.IMAGE_TAG}
                        """
                    }

                    // Frontend build (using its own Dockerfile)
                    sh """
                    echo "Building Frontend Docker image"
                    docker build -t ${env.DOCKER_REPO}/frontend:${env.IMAGE_TAG} \
                        -f frontend/Dockerfile frontend/
                    docker push ${env.DOCKER_REPO}/frontend:${env.IMAGE_TAG}
                    """
                }
            }
        }

        // -----------------------------------------------------------------
        // 5. Deployment & Rollback Strategy
        // -----------------------------------------------------------------
        stage('Deploy to Staging') {
            steps {
                // Ensure deployment script on the remote host is available and uses the correct .yml
                withCredentials([sshUserPrivateKey(credentialsId: env.SSH_CREDENTIAL_ID, keyFileVariable: 'SSH_KEY')]) {
                    sh """
                        echo "Deploying version ${env.IMAGE_TAG} to ${env.REMOTE_HOST}..."

                        # 1. SSH into the remote host and execute the deployment script
                        ssh -i \${SSH_KEY} ${env.REMOTE_USER}@${env.REMOTE_HOST} " \
                            cd ${env.DEPLOYMENT_DIR} && \
                            # Update the tag in the remote .env file \
                            sed -i 's/^IMAGE_TAG=.*/IMAGE_TAG=${env.IMAGE_TAG}/g' .env && \
                            # Pull the new images and recreate containers (using the existing docker-compose.yml) \
                            docker compose pull && \
                            docker compose up -d --remove-orphans \
                        "
                        echo "Deployment command sent successfully."
                    """
                }
            }
            post {
                failure {
                    script {
                        echo "Deployment FAILED. Initiating rollback..."
                        // NOTE: For this to work, the remote host needs to save the previous successful tag.
                        // We assume a file named 'LAST_SUCCESSFUL_TAG' exists on the remote host.
                        withCredentials([sshUserPrivateKey(credentialsId: env.SSH_CREDENTIAL_ID, keyFileVariable: 'SSH_KEY')]) {
                            sh """
                                ssh -i \${SSH_KEY} ${env.REMOTE_USER}@${env.REMOTE_HOST} " \
                                    cd ${env.DEPLOYMENT_DIR} && \
                                    LAST_TAG=\$(cat LAST_SUCCESSFUL_TAG) && \
                                    echo "Rolling back to version \${LAST_TAG}..." && \
                                    sed -i 's/^IMAGE_TAG=.*/IMAGE_TAG=\${LAST_TAG}/g' .env && \
                                    docker compose pull && \
                                    docker compose up -d --remove-orphans \
                                "
                                echo "Rollback completed to last successful tag."
                            """
                        }
                    }
                }
                success {
                    // Save the current successful tag for future rollbacks
                    withCredentials([sshUserPrivateKey(credentialsId: env.SSH_CREDENTIAL_ID, keyFileVariable: 'SSH_KEY')]) {
                        sh """
                            ssh -i \${SSH_KEY} ${env.REMOTE_USER}@${env.REMOTE_HOST} " \
                                cd ${env.DEPLOYMENT_DIR} && \
                                echo ${env.IMAGE_TAG} > LAST_SUCCESSFUL_TAG \
                            "
                        """
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------
    // 6. Notifications (Post-Build Actions)
    // -----------------------------------------------------------------
    post {
        always {
            echo "Pipeline finished. Status: ${currentBuild.result}"
        }
        success {
            // Requires the Email Extension Plugin or Slack Plugin
            echo 'Sending SUCCESS notification.'
        }
        failure {
            echo 'Sending FAILURE notification.'
        }
    }
}
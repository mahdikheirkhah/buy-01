pipeline {
    agent any

    environment {
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        DOCKER_CREDENTIAL_ID = 'dockerhub-creds'
        DOCKER_REPO = 'mahdikheirkhah'
        
        // Remote Deployment Info
        REMOTE_USER = 'ssh-user' 
        REMOTE_HOST = '192.168.1.100' 
        SSH_CREDENTIAL_ID = 'deployment-ssh-key' 
        DEPLOYMENT_DIR = '/opt/ecommerce'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test Backend') {
            steps {
                script {
                    // Use Maven Docker image to build - no need to install Maven in Jenkins
                    sh '''
                        docker run --rm \
                          -v "$PWD/backend":/app \
                          -v "$HOME/.m2":/root/.m2 \
                          -w /app \
                          maven:3.9.6-amazoncorretto-21 \
                          mvn clean install -DskipTests -B -f pom.xml
                    '''
                }
            }
        }

        stage('Dockerize & Publish') {
            steps {
                script {
                    // Login using Jenkins Credentials
                    withCredentials([usernamePassword(credentialsId: env.DOCKER_CREDENTIAL_ID, passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
                        sh "echo \$DOCKER_PASSWORD | docker login -u \$DOCKER_USERNAME --password-stdin"
                    }

                    def services = ['discovery-service', 'api-gateway', 'user-service', 'product-service', 'media-service', 'dummy-data']

                    for (service in services) {
                        sh """
                        docker build -t ${env.DOCKER_REPO}/${service}:${env.IMAGE_TAG} \
                            -f Dockerfile.java \
                            --build-arg SERVICE_NAME=${service} .
                        docker push ${env.DOCKER_REPO}/${service}:${env.IMAGE_TAG}
                        """
                    }

                    // Frontend
                    sh """
                    docker build -t ${env.DOCKER_REPO}/frontend:${env.IMAGE_TAG} -f frontend/Dockerfile frontend/
                    docker push ${env.DOCKER_REPO}/frontend:${env.IMAGE_TAG}
                    """
                }
            }
        }

        stage('Deploy to Staging') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: env.SSH_CREDENTIAL_ID, keyFileVariable: 'SSH_KEY')]) {
                    sh """
                        echo "Deploying version ${env.IMAGE_TAG}..."

                        # 1. Copy the docker-compose.yml to the remote server to ensure it's up to date
                        scp -i \${SSH_KEY} docker-compose.yml ${env.REMOTE_USER}@${env.REMOTE_HOST}:${env.DEPLOYMENT_DIR}/docker-compose.yml

                        # 2. Update the .env file on the remote server and restart
                        ssh -i \${SSH_KEY} ${env.REMOTE_USER}@${env.REMOTE_HOST} "
                            cd ${env.DEPLOYMENT_DIR}
                            
                            # Create or update .env file
                            echo 'IMAGE_TAG=${env.IMAGE_TAG}' > .env
                            echo 'DOCKER_REPO=${env.DOCKER_REPO}' >> .env
                            
                            # Pull and Update
                            docker compose pull
                            docker compose up -d --remove-orphans
                            
                            # Record success for rollback logic
                            echo ${env.IMAGE_TAG} > LAST_SUCCESSFUL_TAG
                        "
                    """
                }
            }
        }
    }

    post {
        failure {
            echo "Deployment Failed. Manual intervention required or check Rollback logic."
        }
    }
}
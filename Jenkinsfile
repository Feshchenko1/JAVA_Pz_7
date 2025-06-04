pipeline {
    agent any
    environment {
        IMAGE_NAME = "pz41-app"
        IMAGE_TAG = "latest"
        K8S_DEPLOYMENT_NAME = "pz41-app-deployment"
        K8S_SERVICE_NAME = "pz41-app-service"
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Fetching code from repository...'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'Compiling the project...'
                sh 'chmod +x ./mvnw'
                sh './mvnw clean compile'
            }
        }

        stage('Test') {
            steps {
                echo 'Running tests...'
                sh './mvnw test'
            }
            post {
                always {
                    echo 'Archiving JUnit test results...'
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                echo 'Packaging the application...'
                sh './mvnw package -DskipTests'
            }
        }

        stage('Archive Artifacts') {
            steps {
                echo 'Archiving the JAR file...'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    echo "Building Docker image ${IMAGE_NAME}:${IMAGE_TAG}..."

                    try {

                        sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ."
                        echo "Docker image ${IMAGE_NAME}:${IMAGE_TAG} built successfully."
                    } catch (e) {
                        echo "Failed to build Docker image: ${e.getMessage()}"
                        error "Docker image build failed"
                    }
                }
            }
        }

        stage('Deploy to Minikube') {
            steps {
                script {
                    echo "Deploying ${IMAGE_NAME}:${IMAGE_TAG} to Minikube..."

                    try {

                        if (IMAGE_TAG != "latest") {
                           sh "kubectl set image deployment/${K8S_DEPLOYMENT_NAME} ${K8S_DEPLOYMENT_NAME}=${IMAGE_NAME}:${IMAGE_TAG} --record"
                        } else {

                           sh "kubectl rollout restart deployment/${K8S_DEPLOYMENT_NAME}"
                        }

                        sh 'kubectl apply -f k8s/deployment.yaml'
                        sh 'kubectl apply -f k8s/service.yaml'

                        echo "Waiting for deployment ${K8S_DEPLOYMENT_NAME} to complete..."
                        timeout(time: 5, unit: 'MINUTES') {
                            sh "kubectl rollout status deployment/${K8S_DEPLOYMENT_NAME} --namespace=default --watch=true"
                        }
                        echo "Application deployed successfully to Minikube."
                        sh "minikube service ${K8S_SERVICE_NAME} --url --namespace=default"

                    } catch (e) {
                        echo "Failed to deploy to Minikube: ${e.getMessage()}"
                        error "Minikube deployment failed"
                    }
                }
            }
        }
    }

    post {
        success {
            echo 'CI/CD Pipeline successfully completed! Application should be deployed.'
        }
        failure {
            echo 'CI/CD Pipeline failed. Check console output for details.'
        }
    }
}
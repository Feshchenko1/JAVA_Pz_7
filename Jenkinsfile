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
        echo 'Building the project (compile + package)...'
        sh 'chmod +x ./mvnw'
        echo 'Packaging the application...'
        sh './mvnw clean package -DskipTests'
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

                        docker.build("${IMAGE_NAME}:${IMAGE_TAG}", ".")
                        echo "Docker image ${IMAGE_NAME}:${IMAGE_TAG} built successfully."
                    } catch (e) {
                        echo "Failed to build Docker image: ${e.getMessage()}"
                        error "Docker image build failed"
                    }
                }
            }
        }
        stage('Push Docker Image') {
            when { expression { env.IMAGE_TAG != 'latest' || someOtherCondition } }
            steps {
                script {
                    docker.withRegistry('https://index.docker.io/v1/', DOCKERHUB_CREDENTIALS_ID) {
                        docker.image("${IMAGE_NAME}:${IMAGE_TAG}").push()

                    }
                    echo "Docker image ${IMAGE_NAME}:${IMAGE_TAG} pushed."
                }
            }
        }
        stage('Deploy to Minikube') {
            steps {
                script {
                    echo "Deploying to Minikube..."

                    try {

                        if (env.IMAGE_TAG == "latest") {
                            sh "kubectl rollout restart deployment/${K8S_DEPLOYMENT_NAME} --namespace=default"
                            sleep 10
                        } else {

                           sh "kubectl set image deployment/${K8S_DEPLOYMENT_NAME} ${K8S_DEPLOYMENT_NAME}=${IMAGE_NAME}:${IMAGE_TAG} --namespace=default --record"
                        }

                        sh 'kubectl apply -f k8s/deployment.yaml'
                        sh 'kubectl apply -f k8s/service.yaml'

                        echo "Waiting for deployment ${K8S_DEPLOYMENT_NAME} to complete..."
                        timeout(time: 5, unit: 'MINUTES') {
                            sh "kubectl rollout status deployment/${K8S_DEPLOYMENT_NAME} --namespace=default --watch=true"
                        }
                        echo "Application deployed successfully to Minikube."
                        // sh "minikube service ${K8S_SERVICE_NAME} --url --namespace=default"

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
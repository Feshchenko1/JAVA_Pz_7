pipeline {
    agent any

    environment {
        IMAGE_NAME = "pz41-app"
        IMAGE_TAG = "latest"
        K8S_DEPLOYMENT_NAME = "pz41-app-deployment"
        K8S_SERVICE_NAME = "pz41-app-service"
        // Видаляємо зайві змінні, щоб уникнути плутанини
    }

    stages {
        stage('Checkout') {
            steps {
                echo '🔄 Fetching code from repository...'
                checkout scm
            }
        }

        stage('Build & Package') {
            steps {
                echo '🔧 Compiling and packaging the project...'
                sh 'chmod +x ./mvnw'
                // Пропускаємо тести тут, бо для них є окремий етап
                sh './mvnw clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                echo '🧪 Running tests...'
                sh './mvnw test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Archive Artifacts') {
            steps {
                echo '🗂 Archiving the JAR file...'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        // --- ВИПРАВЛЕНІ ЕТАПИ CD ---

        stage('Build Docker Image into Minikube') {
            steps {
                script {
                    echo "🎯 Pointing Docker CLI to Minikube's Docker daemon..."
                    // Ця команда виконує 'docker build' УСЕРЕДИНІ контексту Minikube.
                    // Це найнадійніший спосіб зробити образ видимим для Kubernetes.
                    sh '''
                        eval $(minikube -p minikube docker-env) && \
                        echo "⚙️ Building Docker image ${IMAGE_NAME}:${IMAGE_TAG}..." && \
                        docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .
                    '''
                    echo "✅ Docker image is now available inside Minikube."
                }
            }
        }

        stage('Deploy to Minikube') {
            steps {
                script {
                    echo "🚀 Deploying to Minikube..."
                    // Завдяки змонтованому /home/jenkins/.kube, kubectl автоматично
                    // знайде правильну конфігурацію. Жодних складних налаштувань не потрібно.

                    echo "📝 Applying Kubernetes manifests..."
                    sh 'kubectl apply -f k8s/deployment.yaml'
                    sh 'kubectl apply -f k8s/service.yaml'

                    echo "♻️ Triggering a rollout restart to ensure the latest image is used..."
                    sh "kubectl rollout restart deployment/${K8S_DEPLOYMENT_NAME}"

                    echo "⏳ Waiting for deployment to complete..."
                    timeout(time: 5, unit: 'MINUTES') {
                        sh "kubectl rollout status deployment/${K8S_DEPLOYMENT_NAME}"
                    }

                    echo "✅ Application deployed successfully to Minikube."
                    echo "🔗 Getting service URL..."
                    sh "minikube service ${K8S_SERVICE_NAME} --url"
                }
            }
        }
    }

    post {
        success {
            echo '🎉 CI/CD Pipeline successfully completed! Application is deployed.'
        }
        failure {
            echo '🚨 CI/CD Pipeline failed! Check above logs for details.'
        }
    }
}
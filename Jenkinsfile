pipeline {
    agent any

    environment {
        IMAGE_NAME = "pz41-app"
        IMAGE_TAG = "latest"
        K8S_DEPLOYMENT_NAME = "pz41-app-deployment"
        K8S_SERVICE_NAME = "pz41-app-service"
        MINIKUBE_HOME = "/home/jenkins"
        DOCKER_HOST = "tcp://host.docker.internal:2375"

                HTTP_PROXY = "http://http.docker.internal:3128"
                HTTPS_PROXY = "http://http.docker.internal:3128"
                NO_PROXY = "hubproxy.docker.internal,127.0.0.1,localhost"
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

stages {
    stage('Build Docker Image') {
        steps {
            script {
                echo "⚙️ Configuring Docker and Minikube environment..."

                // Проксі (якщо потрібно)
                sh 'export HTTP_PROXY="${HTTP_PROXY}"'
                sh 'export HTTPS_PROXY="${HTTPS_PROXY}"'
                sh 'export NO_PROXY="${NO_PROXY}"'

                // Отримуємо змінні середовища від Minikube
                def minikubeEnv = sh(script: 'minikube -p minikube docker-env', returnStdout: true).trim()

                // Парсимо змінні середовища та додаємо їх у середовище Jenkins, крім DOCKER_HOST і проксі
                minikubeEnv.split('\n').each { line ->
                    if (line.startsWith('export ')) {
                        def parts = line.substring('export '.length()).split('=', 2)
                        if (parts.length == 2) {
                            def key = parts[0].trim()
                            def value = parts[1].trim().replaceAll('"', '')
                            if (!(key in ['DOCKER_HOST', 'HTTP_PROXY', 'HTTPS_PROXY', 'NO_PROXY'])) {
                                env."${key}" = value
                            }
                        }
                    }
                }

                echo "✅ Docker and Minikube environment configured."

                // Збірка Docker-образу
                echo "🐳 Building Docker image ${IMAGE_NAME}:${IMAGE_TAG}..."
                sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ."
                echo "✅ Docker image ${IMAGE_NAME}:${IMAGE_TAG} built successfully."
            }
        }
    }

        stage('Deploy to Minikube') {
            steps {
                script {
                    echo "🚀 Deploying to Minikube..."
                    try {
                        echo "📝 Applying Kubernetes manifests..."
                        sh 'kubectl apply -f k8s/deployment.yaml'
                        sh 'kubectl apply -f k8s/service.yaml'

                        echo "♻️ Triggering a rollout restart to apply the new image..."
                        sh "kubectl rollout restart deployment/${K8S_DEPLOYMENT_NAME} --namespace=default"

                        echo "⏳ Waiting for deployment rollout to complete..."
                        timeout(time: 5, unit: 'MINUTES') {
                            sh "kubectl rollout status deployment/${K8S_DEPLOYMENT_NAME} --namespace=default --watch=true"
                        }

                        echo "✅ Application deployed successfully to Minikube."

                        echo "🔗 Service URL:"
                        sh "minikube service ${K8S_SERVICE_NAME} --url"

                    } catch (e) {
                        echo "❌ Failed to deploy to Minikube: ${e.getMessage()}"
                        error "Minikube deployment failed"
                    }
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
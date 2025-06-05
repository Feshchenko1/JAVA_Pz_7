pipeline {
    agent any
    environment {
        IMAGE_NAME = "pz41-app"
        IMAGE_TAG = "latest"
        K8S_DEPLOYMENT_NAME = "pz41-app-deployment"
        K8S_SERVICE_NAME = "pz41-app-service"
        DOCKERHUB_CREDENTIALS_ID = 'dockerhub-credentials' // Задай свій ID креденшалів для DockerHub тут
    }

    stages {
        stage('Checkout') {
            steps {
                echo '🔄 Fetching code from repository...'
                checkout scm
                echo '✅ Code checkout completed.'
            }
            post {
                failure {
                    echo '❌ Checkout failed! Перевірте налаштування доступу до Git.'
                }
            }
        }

        stage('Build') {
            steps {
                echo '🔧 Building the project (compile + package)...'
                sh 'chmod +x ./mvnw'
                echo '📦 Packaging the application...'
                sh './mvnw clean package -DskipTests'
            }
            post {
                success {
                    echo '✅ Build and packaging successful.'
                }
                failure {
                    echo '❌ Build failed! Перевірте лог збірки Maven.'
                }
                unstable {
                    echo '⚠️ Build completed з попередженнями. Перевірте залежності Maven.'
                }
            }
        }

        stage('Test') {
            steps {
                echo '🧪 Running tests...'
                sh './mvnw test'
            }
            post {
                always {
                    echo '📁 Archiving JUnit test results...'
                    junit 'target/surefire-reports/*.xml'
                }
                success {
                    echo '✅ Tests passed successfully.'
                }
                failure {
                    echo '❌ Тести не пройшли! Перевірте результати тестування.'
                }
            }
        }

        stage('Archive Artifacts') {
            steps {
                echo '🗂 Archiving the JAR file...'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                            if (!fileExists('Dockerfile')) {
                                error "❌ Dockerfile не знайдено в робочій директорії!"
                            }
                            echo '📋 Dockerfile знайдено. Продовжуємо...'

                    echo '📋 Перевіряємо вміст робочої директорії:'
                    sh 'ls -la $WORKSPACE'
                    echo '📋 Вивід Dockerfile:'
                    sh 'cat $WORKSPACE/Dockerfile'
                    echo '📋 Вміст папки target:'
                    sh 'ls -la $WORKSPACE/target'

                    echo "🐳 Building Docker image ${IMAGE_NAME}:${IMAGE_TAG}..."
                                try {
                                    sh 'docker info'
                                } catch (e) {
                                    error "❌ Docker daemon недоступний! Перевірте налаштування Jenkins агента."
                                }
                    try {
                        docker.build("${IMAGE_NAME}:${IMAGE_TAG}", ".")
                        echo "✅ Docker image ${IMAGE_NAME}:${IMAGE_TAG} built successfully."

                    } catch (e) {
                        echo "❌ Failed to build Docker image: ${e.getMessage()}"
                        error "Docker image build failed"
                    }

                }
            }
        }

        stage('Push Docker Image') {
            when {
                expression { return env.IMAGE_TAG != 'latest' }
            }
            steps {
                script {
                    echo "🚀 Pushing Docker image ${IMAGE_NAME}:${IMAGE_TAG} to DockerHub..."
                    try {
                        docker.withRegistry('https://index.docker.io/v1/', DOCKERHUB_CREDENTIALS_ID) {
                            docker.image("${IMAGE_NAME}:${IMAGE_TAG}").push()
                        }
                        echo "✅ Docker image pushed successfully."
                    } catch (e) {
                        echo "❌ Failed to push Docker image: ${e.getMessage()}"
                        error "Docker push failed"
                    }
                }
            }
        }

        stage('Deploy to Minikube') {
            steps {
                script {
                 def kubectlVersion = sh(script: "kubectl version --client --short", returnStdout: true).trim()
                            echo "ℹ️ Kubectl version: ${kubectlVersion}"

                            def nodes = sh(script: "kubectl get nodes --no-headers | wc -l", returnStdout: true).trim()
                            if (nodes == '0') {
                                error "❌ Немає доступних нод у кластері Kubernetes!"
                            }
                    echo "📦 Deploying to Minikube..."
                    try {
                        if (env.IMAGE_TAG == "latest") {
                            echo "♻️ Rolling restart of deployment ${K8S_DEPLOYMENT_NAME}..."
                            sh "kubectl rollout restart deployment/${K8S_DEPLOYMENT_NAME} --namespace=default"
                            sleep 10
                        } else {
                            echo "🔄 Updating deployment image to ${IMAGE_NAME}:${IMAGE_TAG}..."
                            sh "kubectl set image deployment/${K8S_DEPLOYMENT_NAME} ${K8S_DEPLOYMENT_NAME}=${IMAGE_NAME}:${IMAGE_TAG} --namespace=default --record"
                        }

                        echo "📝 Applying Kubernetes manifests..."
                        sh 'kubectl apply -f k8s/deployment.yaml'
                        sh 'kubectl apply -f k8s/service.yaml'

                        echo "⏳ Waiting for deployment rollout to complete..."
                        timeout(time: 5, unit: 'MINUTES') {
                            sh "kubectl rollout status deployment/${K8S_DEPLOYMENT_NAME} --namespace=default --watch=true"
                        }

                        echo "✅ Application deployed successfully to Minikube."
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
        always {
            echo '🔚 Pipeline finished.'
        }
    }
}

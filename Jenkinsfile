pipeline {
    agent any
    environment {
        IMAGE_NAME = "pz41-app"
        IMAGE_TAG = "latest" // Для простоти використовуємо latest, оскільки в Minikube буде локальний образ
        K8S_DEPLOYMENT_NAME = "pz41-app-deployment"
        K8S_SERVICE_NAME = "pz41-app-service"
        // DOCKERHUB_CREDENTIALS_ID = 'dockerhub-credentials' // Не потрібно, якщо не пушимо в Docker Hub
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
                        error "❌ Dockerfile не знайдено в робочій диреторії!"
                    }
                    echo '📋 Dockerfile знайдено. Продовжуємо...'

                    echo '📋 Перевіряємо вміст робочої директорії:'
                    sh 'ls -la $WORKSPACE'
                    echo '📋 Вивід Dockerfile:'
                    sh 'cat $WORKSPACE/Dockerfile'
                    echo '📋 Вміст папки target:'
                    sh 'ls -la $WORKSPACE/target'

                    // **ВАЖЛИВО:** Налаштовуємо Docker CLI на Minikube Daemon
                    echo "⚙️ Налаштовуємо Docker на Minikube демон..."
                    // Об'єднуємо налаштування середовища та подальші Docker команди в один sh блок
                    sh """
                        eval $(minikube -p minikube docker-env)
                        echo "DOCKER_HOST: \${DOCKER_HOST}"
                        echo "DOCKER_CERT_PATH: \${DOCKER_CERT_PATH}"
                        echo "DOCKER_TLS_VERIFY: \${DOCKER_TLS_VERIFY}"

                        echo "🐳 Building Docker image ${IMAGE_NAME}:${IMAGE_TAG}..."
                        docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .
                        echo "✅ Docker image ${IMAGE_NAME}:${IMAGE_TAG} built successfully."
                    """
                }
            }
        }
        // Stage 'Push Docker Image' може бути видалений або зроблений умовним,
        // якщо ви НЕ плануєте пушити образ в зовнішній реєстр для Minikube.
        // Для локального розгортання через `minikube docker-env` це не потрібно.
        /*
        stage('Push Docker Image') {
            when {
                expression { return env.IMAGE_TAG != 'latest' } // Push only if not 'latest' or for specific tags
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
        */

        stage('Deploy to Minikube') {
            steps {
                script {
                    echo "⚙️ Налаштовуємо Kubectl на контекст Minikube..."
                    sh 'kubectl config use minikube' // Переконайтесь, що kubectl використовує правильний контекст
                    sh 'kubectl config current-context' // Перевіряємо поточний контекст

                    def kubectlVersion = sh(script: "kubectl version --client --short", returnStdout: true).trim()
                    echo "ℹ️ Kubectl version: ${kubectlVersion}"

                    def nodes = sh(script: "kubectl get nodes --no-headers | wc -l", returnStdout: true).trim()
                    if (nodes == '0') {
                        error "❌ Немає доступних нод у кластері Kubernetes! Переконайтесь, що Minikube запущено."
                    }
                    echo "📦 Deploying to Minikube..."
                    try {
                        // Оскільки ми використовуємо :latest і очікуємо локальний образ,
                        // найкращий спосіб змусити Minikube перевантажити Pods - це rollout restart.
                        // Якщо ви використовували IMAGE_TAG = "${env.BUILD_ID}" і оновлювали deployment.yaml,
                        // то тоді `kubectl set image` був би більш доречним.
                        echo "📝 Applying Kubernetes manifests..."
                        sh 'kubectl apply -f k8s/deployment.yaml'
                        sh 'kubectl apply -f k8s/service.yaml'

                        echo "♻️ Rolling restart of deployment ${K8S_DEPLOYMENT_NAME}..."
                        sh "kubectl rollout restart deployment/${K8S_DEPLOYMENT_NAME} --namespace=default"


                        echo "⏳ Waiting for deployment rollout to complete..."
                        timeout(time: 5, unit: 'MINUTES') {
                            sh "kubectl rollout status deployment/${K8S_DEPLOYMENT_NAME} --namespace=default --watch=true"
                        }

                        echo "✅ Application deployed successfully to Minikube."

                        // Отримання URL сервісу
                        echo "🔗 Сервіс доступний за URL:"
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
        always {
            echo '🔚 Pipeline finished.'
        }
    }
}
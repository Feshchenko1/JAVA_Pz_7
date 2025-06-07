pipeline {
    agent any

    environment {
        IMAGE_NAME = "pz41-app"
        IMAGE_TAG = "latest"
        K8S_DEPLOYMENT_NAME = "pz41-app-deployment"
        K8S_SERVICE_NAME = "pz41-app-service"
        MINIKUBE_HOME = "/home/jenkins"

        // Proxy variables remain
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

        stage('Build Docker Image') {
                    steps {
                        script {
                            echo "⚙️ Configuring Docker and Minikube environment..."

                            // Proxy variables need to be exported for docker build
                            // We will explicitly set them for the docker build command
                            def http_proxy_env = "HTTP_PROXY=\"${HTTP_PROXY}\""
                            def https_proxy_env = "HTTPS_PROXY=\"${HTTPS_PROXY}\""
                            def no_proxy_env = "NO_PROXY=\"${NO_PROXY}\""

                            // Build Docker image using the host's Docker daemon (via the mounted socket)
                            // Ensure DOCKER_HOST is explicitly unset for this specific command.
                            echo "🐳 Building Docker image ${IMAGE_NAME}:${IMAGE_TAG} on host Docker..."
                            sh """
                                unset DOCKER_HOST # Ensure we use the local /var/run/docker.sock
                                ${http_proxy_env} ${https_proxy_env} ${no_proxy_env} docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .
                            """
                            echo "✅ Docker image ${IMAGE_NAME}:${IMAGE_TAG} built successfully."

                            // Now, configure the environment to interact with Minikube's Docker daemon
                            // This is needed for subsequent stages if they directly use Docker within Minikube,
                            // e.g., if you were pushing images directly into Minikube's daemon.
                            // For now, we'll keep the Minikube env setup, but the build is separate.
                            def minikubeEnv = sh(script: 'minikube -p minikube docker-env', returnStdout: true).trim()
                            minikubeEnv.split('\n').each { line ->
                                if (line.startsWith('export ')) {
                                    def parts = line.substring('export '.length()).split('=', 2)
                                    if (parts.length == 2) {
                                        def key = parts[0].trim()
                                        def value = parts[1].trim().replaceAll('"', '')
                                        // We specifically want DOCKER_HOST from minikube here if needed for later docker commands
                                        // However, for kubectl and minikube service, it's not typically required as they use kubeconfig/internal mechanisms.
                                        // For simplicity and to avoid interference, we'll only set environment variables that are NOT DOCKER_HOST or proxy.
                                        if (!(key in ['DOCKER_HOST', 'HTTP_PROXY', 'HTTPS_PROXY', 'NO_PROXY'])) {
                                            env."${key}" = value
                                        }
                                    }
                                }
                            }
                            echo "✅ Minikube environment variables sourced for kubectl/minikube."
                        }
                    }
                }

        stage('Deploy to Minikube') {
            steps {
                script {
                    echo "🚀 Deploying to Minikube..."
                    try {
                        // Ensure DOCKER_HOST is correctly set for kubectl if it relies on it
                        // For kubectl, minikube docker-env sets up necessary things, but kubectl doesn't typically need DOCKER_HOST
                        // minikube commands directly use the minikube context or internal mechanisms.

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
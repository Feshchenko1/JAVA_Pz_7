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

                    // Step 1: Build Docker image on the host's Docker daemon.
                    // This requires explicitly unsetting DOCKER_HOST and providing proxy settings.
                    echo "🐳 Building Docker image ${IMAGE_NAME}:${IMAGE_TAG} on host Docker..."
                    sh """
                        unset DOCKER_HOST # Ensure we use the local /var/run/docker.sock for this build
                        HTTP_PROXY="${HTTP_PROXY}" HTTPS_PROXY="${HTTPS_PROXY}" NO_PROXY="${NO_PROXY}" docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .
                    """
                    echo "✅ Docker image ${IMAGE_NAME}:${IMAGE_TAG} built successfully."

                    // Step 2: Now, source Minikube's environment to set up kubectl to talk to the Minikube cluster.
                    // This will include KUBECONFIG and any other necessary Minikube-specific variables.
                    echo "🔄 Sourcing Minikube environment for kubectl..."
                    def minikubeEnvCommand = "minikube -p minikube docker-env --shell bash" // Use bash shell to get export commands
                    def minikubeEnvOutput = sh(script: minikubeEnvCommand, returnStdout: true).trim()

                    // Parse the output and set environment variables in Jenkins
                    minikubeEnvOutput.split('\n').each { line ->
                        if (line.startsWith('export ')) {
                            def parts = line.substring('export '.length()).split('=', 2)
                            if (parts.length == 2) {
                                def key = parts[0].trim()
                                def value = parts[1].trim().replaceAll('"', '')
                                // Set ALL environment variables from minikube docker-env
                                // except the proxy ones which we manage globally in Jenkins environment block.
                                // We also explicitly want to NOT set DOCKER_HOST here if we are ALWAYS using the host's Docker daemon
                                // for general docker commands. If you *do* want to use minikube's docker daemon later,
                                // then remove 'DOCKER_HOST' from this exclusion list.
                                if (!(key in ['HTTP_PROXY', 'HTTPS_PROXY', 'NO_PROXY'])) {
                                    env."${key}" = value
                                    echo "   - Setting env: ${key}=${value}" // For debugging
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
                        def minikubeInternalIp = sh(script: 'minikube -p minikube ip', returnStdout: true).trim()
                        echo "   - Minikube Internal IP: ${minikubeInternalIp}"

                        env.KUBECONFIG = "/home/jenkins/.kube/config"
                        echo "   - Setting KUBECONFIG=${env.KUBECONFIG}"

                        // Use --insecure-skip-tls-verify here as well for kubectl config view
                        def minikubeApiServerUrl = sh(script: "KUBECONFIG=${env.KUBECONFIG} kubectl config view --minify --output jsonpath='{.clusters[?(@.name==\"minikube\")].cluster.server}' --insecure-skip-tls-verify", returnStdout: true).trim()
                        echo "   - Minikube API Server URL (from host's kubeconfig): ${minikubeApiServerUrl}"

                        def minikubeApiServerPort = (minikubeApiServerUrl =~ /:(\d+)$/)[0][1]
                        echo "   - Minikube API Server Port: ${minikubeApiServerPort}"

                        // --- IMPORTANT: Dynamically fix paths and server in kubeconfig for Jenkins container ---
                        sh "kubectl config set-cluster minikube --server=https://host.docker.internal:${minikubeApiServerPort} --kubeconfig=${env.KUBECONFIG}"
                        sh "kubectl config set-credentials minikube --client-certificate=/home/jenkins/.minikube/profiles/minikube/client.crt --client-key=/home/jenkins/.minikube/profiles/minikube/client.key --embed-certs=true --kubeconfig=${env.KUBECONFIG}"
                        sh "kubectl config set-cluster minikube --certificate-authority=/home/jenkins/.minikube/ca.crt --embed-certs=true --kubeconfig=${env.KUBECONFIG}"
                        // -------------------------------------------------------------------------------------

                        echo "   - Verifying kubeconfig setup (with --insecure-skip-tls-verify)..."
                        sh "kubectl config current-context --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify"
                        sh "kubectl config get-contexts --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify"

                        echo "📝 Applying Kubernetes manifests..."
                        // ADDED --insecure-skip-tls-verify here
                        sh "kubectl apply -f k8s/deployment.yaml --kubeconfig=${env.KUBECONFIG} --validate=false --insecure-skip-tls-verify"
                        sh "kubectl apply -f k8s/service.yaml --kubeconfig=${env.KUBECONFIG} --validate=false --insecure-skip-tls-verify"

                        echo "♻️ Triggering a rollout restart to apply the new image..."
                        // ADDED --insecure-skip-tls-verify here
                        sh "kubectl rollout restart deployment/${K8S_DEPLOYMENT_NAME} --namespace=default --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify"

                        echo "⏳ Waiting for deployment rollout to complete..."
                        timeout(time: 5, unit: 'MINUTES') {
                            // ADDED --insecure-skip-tls-verify here
                            sh "kubectl rollout status deployment/${K8S_DEPLOYMENT_NAME} --namespace=default --watch=true --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify"
                        }

                        echo "✅ Application deployed successfully to Minikube."

                        echo "🔗 Service URL:"
                        // minikube service does not interact with the API server in the same way, so it doesn't need --insecure-skip-tls-verify
                        sh "minikube service ${K8S_SERVICE_NAME} --url"

                    } catch (e) {
                        echo "❌ Failed to deploy to Minikube: ${e.getMessage()}"
                        error "Minikube deployment failed"
                    }
                }
            }
        }}

    post {
        success {
            echo '🎉 CI/CD Pipeline successfully completed! Application is deployed.'
        }
        failure {
            echo '🚨 CI/CD Pipeline failed! Check above logs for details.'
        }
    }
}
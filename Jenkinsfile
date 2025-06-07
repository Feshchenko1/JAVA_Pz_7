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
        NO_PROXY = "hubproxy.docker.internal,127.0.0.1,localhost,192.168.49.2"
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
            echo "⚙️ Ensuring Docker environment points to host's Docker daemon for Minikube image build..."


            def minikubeIp = sh(script: 'minikube -p minikube ip', returnStdout: true).trim()
            echo "💡 Minikube IP detected: ${minikubeIp}"

            // Get the environment variables from minikube docker-env on the host (Minikube's context)
            def minikubeDockerEnvOutput = sh(script: 'minikube -p minikube docker-env --shell bash', returnStdout: true).trim()
            def dockerEnvVarsForAgent = [:]
            minikubeDockerEnvOutput.split('\n').each { line ->
                if (line.startsWith('export ')) {
                    def parts = line.substring('export '.length()).split('=', 2)
                    if (parts.length == 2) {
                        def key = parts[0].trim()
                        def value = parts[1].trim().replaceAll('"', '')
                        // We explicitly *don't* override DOCKER_HOST here to point to minikubeIp directly,

                        if (key in ['DOCKER_TLS_VERIFY', 'DOCKER_CERT_PATH']) {
                            dockerEnvVarsForAgent."${key}" = value
                            echo "    - Staging env for agent: ${key}=${value}" // For debugging
                        }
                    }
                }
            }



            echo "Assuming host's Docker context is set to Minikube. Jenkins agent will use host's Docker daemon."


            dockerEnvVarsForAgent.DOCKER_TLS_VERIFY = "1"
            dockerEnvVarsForAgent.DOCKER_CERT_PATH = "/home/jenkins/.minikube/certs"


            dockerEnvVarsForAgent.DOCKER_HOST = "unix:///var/run/docker.sock"
            echo "    - Explicitly setting DOCKER_HOST for agent to: ${dockerEnvVarsForAgent.DOCKER_HOST}"


            withEnv(dockerEnvVarsForAgent.collect { k, v -> "${k}=${v}" }) {
                echo "Attempting docker info with correctly configured DOCKER_HOST and certificates..."
                sh "docker info"

                echo "🐳 Building Docker image ${IMAGE_NAME}:${IMAGE_TAG} directly into Minikube's Docker daemon..."
                sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ."
                echo "✅ Docker image ${IMAGE_NAME}:${IMAGE_TAG} built successfully in Minikube."
            }
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

                        def minikubeApiServerUrl = sh(script: "KUBECONFIG=${env.KUBECONFIG} kubectl config view --minify --output jsonpath='{.clusters[?(@.name==\"minikube\")].cluster.server}' --insecure-skip-tls-verify", returnStdout: true).trim()
                        echo "   - Minikube API Server URL (from host's kubeconfig): ${minikubeApiServerUrl}"

                        def minikubeApiServerPort = (minikubeApiServerUrl =~ /:(\d+)$/)[0][1]
                        echo "   - Minikube API Server Port: ${minikubeApiServerPort}"

                        sh "kubectl config set-cluster minikube --server=https://host.docker.internal:${minikubeApiServerPort} --kubeconfig=${env.KUBECONFIG}"
                        sh "kubectl config set-credentials minikube --client-certificate=/home/jenkins/.minikube/profiles/minikube/client.crt --client-key=/home/jenkins/.minikube/profiles/minikube/client.key --embed-certs=true --kubeconfig=${env.KUBECONFIG}"
                        sh "kubectl config set-cluster minikube --certificate-authority=/home/jenkins/.minikube/ca.crt --embed-certs=true --kubeconfig=${env.KUBECONFIG}"

                        echo "   - Verifying kubeconfig setup (with --insecure-skip-tls-verify)..."
                        sh "kubectl config current-context --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify"
                        sh "kubectl config get-contexts --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify"

                        echo "📝 Applying Kubernetes manifests..."
                        sh "kubectl apply -f k8s/deployment.yaml --kubeconfig=${env.KUBECONFIG} --validate=false --insecure-skip-tls-verify"
                        sh "kubectl apply -f k8s/service.yaml --kubeconfig=${env.KUBECONFIG} --validate=false --insecure-skip-tls-verify"

                        echo "♻️ Triggering a rollout restart to apply the new image..."
                        sh "kubectl rollout restart deployment/${K8S_DEPLOYMENT_NAME} --namespace=default --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify"

                        echo "⏳ Waiting for deployment rollout to complete..."
                        timeout(time: 5, unit: 'MINUTES') {
                            sh "kubectl rollout status deployment/${K8S_DEPLOYMENT_NAME} --namespace=default --watch=true --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify"
                        }

                        echo "✅ Application deployed successfully to Minikube."
                        echo "🔗 Service URL:"
                        sh "minikube service ${K8S_SERVICE_NAME} --url"

                    } catch (e) {
                        echo "❌ Failed to deploy to Minikube: ${e.getMessage()}"

                        // --- DIAGNOSTIC STEPS ADDED HERE ---
                        echo "--- DIAGNOSTIC INFORMATION ---"
                        echo "Retrieving deployment status:"
                        sh "kubectl describe deployment ${K8S_DEPLOYMENT_NAME} --namespace=default --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify"

                        echo "Retrieving pod statuses:"
                        sh "kubectl get pods -l app=${IMAGE_NAME} --namespace=default -o wide --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify"

                        echo "Retrieving logs from potentially problematic pods (adjust selector if needed):"
                        sh "kubectl get pods -l app=${IMAGE_NAME} --namespace=default -o jsonpath='{.items[*].metadata.name}' --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify"
                        def podNames = sh(script: "kubectl get pods -l app=${IMAGE_NAME} --namespace=default -o jsonpath='{.items[*].metadata.name}' --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify", returnStdout: true).trim()
                        podNames.split(' ').each { podName ->
                            echo "--- Logs for pod: ${podName} ---"
                            sh "kubectl logs ${podName} --namespace=default --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify || true" // || true to prevent build failure if logs can't be fetched (e.g., pod not running)
                            sh "kubectl describe pod ${podName} --namespace=default --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify || true"
                        }
                        echo "--- END DIAGNOSTIC INFORMATION ---"
                        // --- END DIAGNOSTIC STEPS ---

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
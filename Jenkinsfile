pipeline {
    agent any

    environment {
        IMAGE_NAME = "pz41-app"
        IMAGE_TAG = "latest"
        K8S_DEPLOYMENT_NAME = "pz41-app-deployment"
        K8S_SERVICE_NAME = "pz41-app-service"
        MINIKUBE_HOME = '/home/jenkins' // Ensure this path is correct for your Jenkins agent
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

        stage('Build Docker Image into Minikube') {
            steps {
                script {
                    echo "🎯 Getting Minikube's Docker environment..."
                    def dockerEnv = sh(script: "minikube -p minikube docker-env", returnStdout: true).trim()

                    withEnv(["${dockerEnv}"]) {
                        echo "⚙️ Building Docker image ${IMAGE_NAME}:${IMAGE_TAG}..."
                        sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ."

                        echo "✅ Docker image is now available inside Minikube. Verifying..."
                        sh "docker images | grep ${IMAGE_NAME}"
                    }
                }
            }
        }

        stage('Deploy to Minikube') {
            steps {
                script {
                    echo "🚀 Deploying to Minikube..."

                    env.KUBECONFIG = "${MINIKUBE_HOME}/.kube/config"

                    try {
                        echo " - Setting KUBECONFIG=${env.KUBECONFIG}"

                        sh 'eval $(minikube -p minikube docker-env)'

                        // Add --insecure-skip-tls-verify to kubectl commands that connect to the API server
                        // This resolves the TLS certificate validation error.
                        // Also, ensure the context is correctly set up for the Jenkins user.
                        sh "kubectl config use-context minikube --kubeconfig=${env.KUBECONFIG}"
                        sh "kubectl config current-context --kubeconfig=${env.KUBECONFIG}"

                        echo "🗑️ Deleting old Kubernetes resources if they exist..."
                        sh "kubectl delete deployment ${K8S_DEPLOYMENT_NAME} --namespace=default --kubeconfig=${env.KUBECONFIG} --ignore-not-found=true --insecure-skip-tls-verify"
                        sh "kubectl delete service ${K8S_SERVICE_NAME} --namespace=default --kubeconfig=${env.KUBECONFIG} --ignore-not-found=true --insecure-skip-tls-verify"


                        echo "📝 Applying Kubernetes manifests..."
                        sh "kubectl delete deployment pz41-app-deployment --namespace=default --kubeconfig=/home/jenkins/.kube/config --ignore-not-found=true --insecure-skip-tls-verify=true"
                        sh "kubectl apply -f k8s/deployment.yaml --kubeconfig=/home/jenkins/.kube/config --insecure-skip-tls-verify=true"


                        echo "♻️ Triggering a rollout restart to apply the new image..."
                        sh "kubectl rollout restart deployment/${K8S_DEPLOYMENT_NAME} --namespace=default --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify"

                        echo "⏳ Waiting for deployment rollout to complete..."
                        timeout(time: 5, unit: 'MINUTES') {
                            sh "kubectl rollout status deployment/${K8S_DEPLOYMENT_NAME} --namespace=default --watch=true --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify"
                        }

                        echo "✅ Application deployed successfully to Minikube."
                        echo "🔗 Service URL:"
                        // minikube service doesn't directly take --insecure-skip-tls-verify, but it might resolve internal IPs.
                        // If it fails, you might need to manually construct the URL.
                        sh "minikube service ${K8S_SERVICE_NAME} --url --kubeconfig=${env.KUBECONFIG}"

                    } catch (e) {
                        echo "❌ Failed to deploy to Minikube: ${e.getMessage()}"
                        echo "--- DIAGNOSTIC INFORMATION ---"
                        echo "Retrieving deployment status:"
                        sh "kubectl describe deployment ${K8S_DEPLOYMENT_NAME} --namespace=default --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify || true"
                        echo "Retrieving pod statuses:"
                        sh "kubectl get pods -l app=${IMAGE_NAME} --namespace=default -o wide --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify || true"

                        echo "Retrieving logs from potentially problematic pods (adjust selector if needed):"
                        def podNames = sh(script: "kubectl get pods -l app=${IMAGE_NAME} --namespace=default -o jsonpath='{.items[*].metadata.name}' --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify || true", returnStdout: true).trim()
                        podNames.split(' ').each { podName ->
                            echo "--- Logs for pod: ${podName} ---"
                            sh "kubectl logs ${podName} --namespace=default --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify || true"
                            sh "kubectl describe pod ${podName} --namespace=default --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify || true"
                        }
                        echo "--- END DIAGNOSTIC INFORMATION ---"
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
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

                    // Set KUBECONFIG for Jenkins to use the Minikube context
                    // This assumes minikube is started and a kubeconfig exists for it.
                    // If running minikube as a separate step, ensure it generates the config.
                    env.KUBECONFIG = "${MINIKUBE_HOME}/.kube/config" // Using MINIKUBE_HOME as base

                    try {
                        echo " - Setting KUBECONFIG=${env.KUBECONFIG}"

                        // Optional: Ensure minikube's docker environment is still active for kubectl commands
                        // This helps if kubectl also needs to interact with the minikube's docker daemon for some operations
                        sh 'eval $(minikube -p minikube docker-env)'

                        // Ensure kubectl connects to the correct Minikube API server
                        // This might be redundant if the MINIKUBE_HOME/.kube/config is already correctly set up by minikube
                        // but it's good for explicit control in a CI environment.
                        def minikubeApiServerUrl = sh(script: "minikube -p minikube status --format '{{.Host}}'", returnStdout: true).trim()
                        echo " - Minikube API Server URL: ${minikubeApiServerUrl}"


                        // It's generally better to let `minikube update-context` handle context updates
                        // rather than manually setting individual config properties, if possible.
                        // However, if Jenkins's kubectl needs specific certs/keys, the manual method is necessary.
                        // For a clean Minikube setup, 'minikube update-context' usually works.
                        // For Jenkins, explicit paths are safer.
                        // If you are having issues with certificates, consider adding --insecure-skip-tls-verify
                        // BUT ONLY IN DEVELOPMENT/TESTING ENVIRONMENTS.
                        // For production, proper certificate handling is crucial.
                        sh "kubectl config use-context minikube --kubeconfig=${env.KUBECONFIG}"
                        // Verify context
                        sh "kubectl config current-context --kubeconfig=${env.KUBECONFIG}"


                        echo "🗑️ Deleting old Kubernetes resources if they exist..."
                        sh "kubectl delete deployment ${K8S_DEPLOYMENT_NAME} --namespace=default --kubeconfig=${env.KUBECONFIG} --ignore-not-found=true"
                        sh "kubectl delete service ${K8S_SERVICE_NAME} --namespace=default --kubeconfig=${env.KUBECONFIG} --ignore-not-found=true"


                        echo "📝 Applying Kubernetes manifests..."
                        // Remove --validate=false unless absolutely necessary. It hides real config errors.
                        // Add --insecure-skip-tls-verify back if you face authentication issues, but address the root cause for production.
                        sh "kubectl apply -f k8s/deployment.yaml --kubeconfig=${env.KUBECONFIG}"
                        sh "kubectl apply -f k8s/service.yaml --kubeconfig=${env.KUBECONFIG}"

                        echo "♻️ Triggering a rollout restart to apply the new image..."
                        sh "kubectl rollout restart deployment/${K8S_DEPLOYMENT_NAME} --namespace=default --kubeconfig=${env.KUBECONFIG}"

                        echo "⏳ Waiting for deployment rollout to complete..."
                        timeout(time: 5, unit: 'MINUTES') {
                            sh "kubectl rollout status deployment/${K8S_DEPLOYMENT_NAME} --namespace=default --watch=true --kubeconfig=${env.KUBECONFIG}"
                        }

                        echo "✅ Application deployed successfully to Minikube."
                        echo "🔗 Service URL:"
                        sh "minikube service ${K8S_SERVICE_NAME} --url --kubeconfig=${env.KUBECONFIG}" // Add kubeconfig here too

                    } catch (e) {
                        echo "❌ Failed to deploy to Minikube: ${e.getMessage()}"
                        echo "--- DIAGNOSTIC INFORMATION ---"
                        echo "Retrieving deployment status:"
                        sh "kubectl describe deployment ${K8S_DEPLOYMENT_NAME} --namespace=default --kubeconfig=${env.KUBECONFIG} || true"
                        echo "Retrieving pod statuses:"
                        sh "kubectl get pods -l app=${IMAGE_NAME} --namespace=default -o wide --kubeconfig=${env.KUBECONFIG} || true"

                        echo "Retrieving logs from potentially problematic pods (adjust selector if needed):"
                        def podNames = sh(script: "kubectl get pods -l app=${IMAGE_NAME} --namespace=default -o jsonpath='{.items[*].metadata.name}' --kubeconfig=${env.KUBECONFIG} || true", returnStdout: true).trim()
                        podNames.split(' ').each { podName ->
                            echo "--- Logs for pod: ${podName} ---"
                            sh "kubectl logs ${podName} --namespace=default --kubeconfig=${env.KUBECONFIG} || true"
                            sh "kubectl describe pod ${podName} --namespace=default --kubeconfig=${env.KUBECONFIG} || true"
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
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

        // !!! ВИДАЛИТИ ЦЮ СТАДІЮ !!!
        // stage('Install Minikube Tools') {
        //     steps {
        //         script {
        //             echo "Installing minikube and kubectl inside the agent..."
        //             sh 'curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64 && sudo install minikube-linux-amd64 /usr/local/bin/minikube'
        //             sh 'curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" && sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl'
        //         }
        //     }
        // }


stage('Build Docker Image') {
    steps {
        script {
            echo "⚙️ Building Docker image ${IMAGE_NAME}:${IMAGE_TAG} directly into Minikube's Docker daemon using minikube image build..."
            // This command builds the image directly into Minikube's Docker daemon.
            // No need for `eval $(minikube docker-env)` here for `minikube image build`.
            sh "minikube -p minikube image build -t ${IMAGE_NAME}:${IMAGE_TAG} ."
            echo "✅ Docker image ${IMAGE_NAME}:${IMAGE_TAG} built successfully in Minikube."
            echo "🔎 Verifying image presence in Minikube's Docker daemon:"
            // Use `minikube ssh` to execute `docker images` directly on the Minikube VM
            sh "minikube -p minikube ssh 'docker images | grep ${IMAGE_NAME}'"
        }
    }
}

stage('Deploy to Minikube') {
            steps {
                script {
                    echo "🚀 Deploying to Minikube..."

                    sh 'eval $(minikube -p minikube docker-env)'
                    try {
                        def minikubeInternalIp = sh(script: 'minikube -p minikube ip', returnStdout: true).trim()
                        echo "    - Minikube Internal IP: ${minikubeInternalIp}"

                        env.KUBECONFIG = "/home/jenkins/.kube/config"
                        echo "    - Setting KUBECONFIG=${env.KUBECONFIG}"

                        def minikubeApiServerUrl = sh(script: "KUBECONFIG=${env.KUBECONFIG} kubectl config view --minify --output jsonpath='{.clusters[?(@.name==\"minikube\")].cluster.server}' --insecure-skip-tls-verify", returnStdout: true).trim()
                        echo "    - Minikube API Server URL (from host's kubeconfig): ${minikubeApiServerUrl}"

                        def minikubeApiServerPort = (minikubeApiServerUrl =~ /:(\d+)$/)[0][1]
                        echo "    - Minikube API Server Port: ${minikubeApiServerPort}"

                        sh "kubectl config set-cluster minikube --server=https://host.docker.internal:${minikubeApiServerPort} --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify"
                        sh "kubectl config set-credentials minikube --client-certificate=/home/jenkins/.minikube/profiles/minikube/client.crt --client-key=/home/jenkins/.minikube/profiles/minikube/client.key --embed-certs=true --kubeconfig=${env.KUBECONFIG}"
                        sh "kubectl config set-cluster minikube --certificate-authority=/home/jenkins/.minikube/ca.crt --embed-certs=true --kubeconfig=${env.KUBECONFIG}"

                        echo "    - Verifying kubeconfig setup (with --insecure-skip-tls-verify)..."
                        sh "kubectl config current-context --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify"
                        sh "kubectl config get-contexts --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify"

                        echo "🗑️ Deleting old Kubernetes resources if they exist..."
                        // Видаляємо Deployment
                        sh "kubectl delete deployment ${K8S_DEPLOYMENT_NAME} --namespace=default --kubeconfig=${env.KUBECONFIG} --ignore-not-found=true --insecure-skip-tls-verify"
                        // Видаляємо Service
                        sh "kubectl delete service ${K8S_SERVICE_NAME} --namespace=default --kubeconfig=${env.KUBECONFIG} --ignore-not-found=true --insecure-skip-tls-verify"


                echo "📝 Applying Kubernetes manifests..."
                sh "kubectl apply -f k8s/deployment.yaml --kubeconfig=${env.KUBECONFIG} --validate=false --insecure-skip-tls-verify"
                sh "kubectl apply -f k8s/service.yaml --kubeconfig=${env.KUBECONFIG} --validate=false --insecure-skip-tls-verify"


                echo "♻️ Triggering a rollout restart to apply the new image..."
                sh "kubectl rollout restart deployment/${K8S_DEPLOYMENT_NAME} --namespace=default --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify"


                        echo "⏳ Waiting for deployment rollout to complete..."
                        timeout(time: 5, unit: 'MINUTES') {
                            sh "kubectl rollout status deployment/${K8S_DEPLOYMENT_NAME} --namespace=default --watch=true --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify"
                        }

                echo "⏳ Waiting for deployment rollout to complete..."
                timeout(time: 5, unit: 'MINUTES') {
                    sh "kubectl rollout status deployment/${K8S_DEPLOYMENT_NAME} --namespace=default --watch=true --kubeconfig=${env.KUBECONFIG} --insecure-skip-tls-verify"
                }
                echo "✅ Application deployed successfully to Minikube."
                                echo "🔗 Service URL:"
                                sh "minikube service ${K8S_SERVICE_NAME} --url"
                  }catch (e) {
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
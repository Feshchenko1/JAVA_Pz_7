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
                    // Крапка в кінці '.' означає, що контекст для збірки Dockerfile знаходиться в поточному каталозі.
                    sh "minikube -p minikube image build -t ${IMAGE_NAME}:${IMAGE_TAG} ."
                    echo "✅ Docker image ${IMAGE_NAME}:${IMAGE_TAG} built successfully in Minikube."
                }
            }
        }

        stage('Deploy to Minikube') {
            steps {
                script {
                    echo "🚀 Deploying to Minikube..."
                    try {
                        def minikubeInternalIp = sh(script: 'minikube -p minikube ip', returnStdout: true).trim()
                        echo "    - Minikube Internal IP: ${minikubeInternalIp}"

                        env.KUBECONFIG = "/home/jenkins/.kube/config"
                        echo "    - Setting KUBECONFIG=${env.KUBECONFIG}"

                        // Для kubectl, який працює всередині Jenkins агента,
                        // KUBECONFIG вже повинен вказувати на коректні файли,
                        // і host.docker.internal буде коректним для доступу до API Minikube на хості.
                        // Всі ці kubectl config set-cluster/credentials/certificate-authority
                        // повинні були бути зроблені minikube start на хості.
                        // Однак, ми намагаємося переналаштувати його для контейнера Jenkins агента.
                        // Зберігаємо зміну IP на host.docker.internal для KUBECONFIG.
                        def minikubeApiServerUrl = sh(script: "KUBECONFIG=${env.KUBECONFIG} kubectl config view --minify --output jsonpath='{.clusters[?(@.name==\"minikube\")].cluster.server}'", returnStdout: true).trim()
                        echo "    - Minikube API Server URL (from host's kubeconfig): ${minikubeApiServerUrl}"

                        def minikubeApiServerPort = (minikubeApiServerUrl =~ /:(\d+)$/)[0][1]
                        echo "    - Minikube API Server Port: ${minikubeApiServerPort}"

                        // Важливо: переконайтеся, що ці файли сертифікатів доступні всередині контейнера Jenkins агента
                        // через `-v "C:/Users/Bogdan/.minikube:/home/jenkins/.minikube"`.
                        // Якщо cert.pem не є root CA, а це ca.pem, то потрібно використовувати його.
                        // Судячи з вашого ls -l /home/jenkins/.minikube/certs та cat /home/jenkins/.minikube/ca.crt,
                        // ca.crt та ca.pem є вашими кореневими CA.
                        // Minikube зазвичай використовує ca.crt як кореневий сертифікат для kubectl.
                        sh "kubectl config set-cluster minikube --server=https://host.docker.internal:${minikubeApiServerPort} --kubeconfig=${env.KUBECONFIG}"
                        sh "kubectl config set-credentials minikube --client-certificate=/home/jenkins/.minikube/profiles/minikube/client.crt --client-key=/home/jenkins/.minikube/profiles/minikube/client.key --embed-certs=true --kubeconfig=${env.KUBECONFIG}"
                        sh "kubectl config set-cluster minikube --certificate-authority=/home/jenkins/.minikube/ca.crt --embed-certs=true --kubeconfig=${env.KUBECONFIG}"


                        echo "    - Verifying kubeconfig setup (with --insecure-skip-tls-verify)..."
                        // Пробуємо без --insecure-skip-tls-verify спочатку, якщо certs працюють
                        sh "kubectl config current-context --kubeconfig=${env.KUBECONFIG}"
                        sh "kubectl config get-contexts --kubeconfig=${env.KUBECONFIG}"

                        echo "📝 Applying Kubernetes manifests..."
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
                        sh "minikube service ${K8S_SERVICE_NAME} --url"

                    } catch (e) {
                        echo "❌ Failed to deploy to Minikube: ${e.getMessage()}"

                        // --- DIAGNOSTIC STEPS ADDED HERE ---
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
pipeline {
    agent any

    environment {
        IMAGE_NAME = "pz41-app"
        IMAGE_TAG = "latest"
        K8S_DEPLOYMENT_NAME = "pz41-app-deployment"
        K8S_SERVICE_NAME = "pz41-app-service"
        MINIKUBE_HOME = '/home/jenkins'
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
            // Вкажіть --shell bash, щоб отримати вивід у форматі Bash
            def dockerEnvOutput = sh(script: "minikube -p minikube docker-env --shell bash", returnStdout: true).trim()

            def dockerHost
            def dockerTlsVerify
            def dockerCertPath
            dockerEnvOutput.eachLine { line ->
                if (line.startsWith("export DOCKER_HOST=")) { // Використовуйте startsWith для більшої надійності
                    dockerHost = line.split("=")[1].replaceAll('"', '') // Видаліть всі подвійні лапки
                } else if (line.startsWith("export DOCKER_TLS_VERIFY=")) {
                    dockerTlsVerify = line.split("=")[1].replaceAll('"', '')
                } else if (line.startsWith("export DOCKER_CERT_PATH=")) {
                    dockerCertPath = line.split("=")[1].replaceAll('"', '')
                }
            }

            // Перевірка, чи змінні не null/порожні
            if (!dockerHost || !dockerTlsVerify || !dockerCertPath) {
                error "Failed to parse Minikube Docker environment. DOCKER_HOST: ${dockerHost}, DOCKER_TLS_VERIFY: ${dockerTlsVerify}, DOCKER_CERT_PATH: ${dockerCertPath}"
            }

            withEnv([
                "DOCKER_HOST=${dockerHost}",
                "DOCKER_TLS_VERIFY=${dockerTlsVerify}",
                "DOCKER_CERT_PATH=${dockerCertPath}"
            ]) {
                echo "⚙️ Building Docker image ${IMAGE_NAME}:${IMAGE_TAG}..."
                // Переконайтеся, що ви знаходитесь у корені вашого репозиторію, де лежить Dockerfile
                // Зазвичай це ${WORKSPACE}
                sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ."

                echo "✅ Docker image is now available inside Minikube. Verifying..."
                sh "docker images | grep ${IMAGE_NAME} || true"
            }
        }
    }
}
stage('Deploy to Minikube') {
    steps {
        script {
            echo "🚀 Deploying to Minikube..."

            // Видаліть це, якщо ви вже видалили 'server' з kubeconfig
            // env.KUBECONFIG = "${MINIKUBE_HOME}/.kube/config"

            // Замість eval $(minikube -p minikube docker-env)
            // Використовуйте змінну KUBECONFIG, яка вже має бути налаштована через Secret File.
            // НЕ РОБІТЬ sh 'eval $(minikube -p minikube docker-env)' ТУТ ЗНОВУ, ЦЕ ДЛЯ DOCKER, А НЕ KUBECTL.
            // Якщо KUBECONFIG встановлено через withCredentials, то він вже буде доступний.

            // Забезпечте, що KUBECONFIG використовується. Якщо ви не використовуєте withCredentials,
            // тоді можливо вам потрібно буде явно вказати KUBECONFIG=... для кожного kubectl виклику.
            // Але оскільки ви вже монтуєте .kube та .minikube, то Jenkins має знайти його.

            try {
                // Видаліть або закоментуйте рядок:
                // sh 'eval $(minikube -p minikube docker-env)' // Цей рядок встановлює Docker env, а не K8s env

                echo " - Setting KUBECONFIG=${env.KUBECONFIG}" // Це не встановлює KUBECONFIG, а виводить його значення.
                                                              // KUBECONFIG вже має бути встановлений через withCredentials

                // Якщо ви не використовуєте withCredentials (як у моєму попередньому прикладі),
                // тоді вам доведеться вказати KUBECONFIG для кожного виклику kubectl.
                // Або, якщо ви монтуєте C:/Users/Bogdan/.kube до /home/jenkins/.kube,
                // і ваш файл config знаходиться за цим шляхом, то kubectl має його знайти за замовчуванням.

                // Проте, оскільки ви використовуєте:
                // -v C:/Users/Bogdan/.kube:/home/jenkins/.kube
                // -v C:/Users/Bogdan/.minikube:/home/jenkins/.minikube
                // то файли доступні в Jenkins, і kubectl повинен їх знайти.

                // Перевірте, чи не конфліктує MINIKUBE_HOME = '/home/jenkins' з фактичним шляхом.
                // Якщо JenkinsAgent виконується як user 'jenkins', то /home/jenkins/.kube коректно.

                sh "kubectl config use-context minikube" // без --kubeconfig, якщо env.KUBECONFIG вже встановлено
                sh "kubectl config current-context" // без --kubeconfig

                echo "🗑️ Deleting old Kubernetes resources if they exist..."
                sh "kubectl delete deployment ${K8S_DEPLOYMENT_NAME} --namespace=default --ignore-not-found=true --insecure-skip-tls-verify"
                sh "kubectl delete service ${K8S_SERVICE_NAME} --namespace=default --ignore-not-found=true --insecure-skip-tls-verify"


                echo "📝 Applying Kubernetes manifests..."
                // Цей рядок дублює попередній delete
                // sh "kubectl delete deployment pz41-app-deployment --namespace=default --kubeconfig=/home/jenkins/.kube/config --ignore-not-found=true --insecure-skip-tls-verify=true"
                // Додайте apply service
                sh "kubectl apply -f k8s/service.yaml --namespace=default --insecure-skip-tls-verify=true"
                sh "kubectl apply -f k8s/deployment.yaml --namespace=default --insecure-skip-tls-verify=true"


                echo "♻️ Triggering a rollout restart to apply the new image..."
                sh "kubectl rollout restart deployment/${K8S_DEPLOYMENT_NAME} --namespace=default --insecure-skip-tls-verify"

                echo "⏳ Waiting for deployment rollout to complete..."
                timeout(time: 5, unit: 'MINUTES') {
                    sh "kubectl rollout status deployment/${K8S_DEPLOYMENT_NAME} --namespace=default --watch=true --insecure-skip-tls-verify"
                }

                echo "✅ Application deployed successfully to Minikube."
                echo "🔗 Service URL:"
                sh "minikube service ${K8S_SERVICE_NAME} --url"
                    } catch (e) {
                        echo "❌ Failed to deploy to Minikube: ${e.getMessage()}"
                        echo "--- DIAGNOSTIC INFORMATION ---"
                        echo "Retrieving deployment status:"
                        sh "kubectl describe deployment ${K8S_DEPLOYMENT_NAME} --namespace=default  --insecure-skip-tls-verify || true"
                        echo "Retrieving pod statuses:"
                        sh "kubectl get pods -l app=${IMAGE_NAME} --namespace=default -o wide  --insecure-skip-tls-verify || true"

                        echo "Retrieving logs from potentially problematic pods (adjust selector if needed):"
                        def podNames = sh(script: "kubectl get pods -l app=${IMAGE_NAME} --namespace=default -o jsonpath='{.items[*].metadata.name}'  --insecure-skip-tls-verify || true", returnStdout: true).trim()
                        podNames.split(' ').each { podName ->
                            echo "--- Logs for pod: ${podName} ---"
                            sh "kubectl logs ${podName} --namespace=default  --insecure-skip-tls-verify || true"
                            sh "kubectl describe pod ${podName} --namespace=default  --insecure-skip-tls-verify || true"
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
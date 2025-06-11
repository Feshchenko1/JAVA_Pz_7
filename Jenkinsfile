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
            // Використовуйте sh -c "eval" для Jenkins
            // Це дозволить змінним оточення встановитись у поточному shell
            def dockerEnvScript = "minikube -p minikube docker-env"
            // Виконати команду та отримати вивід, потім виконати 'eval' для встановлення змінних оточення
            sh "eval \"\$(${dockerEnvScript})\""


            echo "⚙️ Building Docker image ${IMAGE_NAME}:${IMAGE_TAG}..."
            // Переконайтеся, що ви знаходитесь у корені вашого репозиторію, де лежить Dockerfile
            // Поточна робоча директорія в Jenkinsfile - це корінь клонованого репозиторію.
            // Якщо Dockerfile знаходиться в корені, це правильно.
            sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ."

            echo "✅ Docker image is now available inside Minikube. Verifying..."
            sh "docker images | grep ${IMAGE_NAME} || true"

            echo "Caching Docker image in Minikube for direct use by Kubernetes..."
            sh "minikube cache add ${IMAGE_NAME}:${IMAGE_TAG}"
        }
    }
}
stage('Deploy to Minikube') {
    steps {
        script {
            echo "🚀 Deploying to Minikube..."

            env.KUBECONFIG = "${MINIKUBE_HOME}/.kube/config"

            sh "echo KUBECONFIG is set to: ${env.KUBECONFIG}"
            sh "ls -la ${env.KUBECONFIG} || true"

            // ДОДАЙТЕ ЦЕЙ РЯДОК ДЛЯ ДІАГНОСТИКИ:
            sh "echo 'Server URL from kubeconfig:'"
            sh "kubectl config view --minify --output jsonpath='{.clusters[0].cluster.server}' || true"


            try {
                sh "kubectl config use-context minikube"
                sh "kubectl config current-context"

                echo "🗑️ Deleting old Kubernetes resources if they exist..."
                sh "kubectl delete deployment ${K8S_DEPLOYMENT_NAME} --namespace=default --ignore-not-found=true --insecure-skip-tls-verify"
                sh "kubectl delete service ${K8S_SERVICE_NAME} --namespace=default --ignore-not-found=true --insecure-skip-tls-verify"

                echo "📝 Applying Kubernetes manifests..."
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
                sh "kubectl describe deployment ${K8S_DEPLOYMENT_NAME} --namespace=default --insecure-skip-tls-verify || true"
                echo "Retrieving pod statuses:"
                sh "kubectl get pods -l app=${IMAGE_NAME} --namespace=default -o wide --insecure-skip-tls-verify || true"

                echo "Retrieving logs from potentially problematic pods (adjust selector if needed):"
                def podNames = sh(script: "kubectl get pods -l app=${IMAGE_NAME} --namespace=default -o jsonpath='{.items[*].metadata.name}' --insecure-skip-tls-verify || true", returnStdout: true).trim()
                podNames.split(' ').each { podName ->
                    echo "--- Logs for pod: ${podName} ---"
                    sh "kubectl logs ${podName} --namespace=default --insecure-skip-tls-verify || true"
                    sh "kubectl describe pod ${podName} --namespace=default --insecure-skip-tls-verify || true"
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
pipeline {
    agent any

    environment {
        REPO_NAME = "shrodlnger"
        IMAGE_NAME = "${REPO_NAME}/pz41-app"
        IMAGE_TAG = "latest"

        GCP_PROJECT_ID = "minikube-462618"
        GKE_CLUSTER_NAME = "kuber"
        GKE_CLUSTER_ZONE = "europe-central2-a"

        K8S_DEPLOYMENT_NAME = "pz41-app-deployment"
        K8S_SERVICE_NAME = "pz41-app-service"

        DOCKER_HOST = "tcp://host.docker.internal:2375"
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Fetching code from repository...'
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
                echo 'Running tests...'
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
                echo 'Archiving the JAR file...'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    echo "Building Docker image ${IMAGE_NAME}:${IMAGE_TAG}..."
                    sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ."

                    echo "Docker image built locally. Verifying..."
                    sh "docker images | grep ${REPO_NAME}/${IMAGE_NAME.split('/')[1]} || true"
                }
            }
        }

        stage('Push Docker Image to Docker Hub') {
            steps {
                script {
                    echo "Logging into Docker Hub..."
                    withCredentials([usernamePassword(credentialsId: 'docker-hub-credentials', passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
                        sh "echo \"$DOCKER_PASSWORD\" | docker login -u \"$DOCKER_USERNAME\" --password-stdin"
                    }
                    echo "Pushing Docker image ${IMAGE_NAME}:${IMAGE_TAG} to Docker Hub..."
                    sh "docker push ${IMAGE_NAME}:${IMAGE_TAG}"
                    echo "Docker image pushed to Docker Hub."
                }
            }
        }

       stage('Deploy to GKE') {
                   steps {
                       script {
                           echo "Deploying to Google Kubernetes Engine..."

                           try {
                               withCredentials([file(credentialsId: 'gke-service-account-key', variable: 'GCP_KEY_FILE')]) {
                                   sh "gcloud auth activate-service-account --key-file=\"${GCP_KEY_FILE}\" --project=${GCP_PROJECT_ID}"
                                   sh "gcloud config set project ${GCP_PROJECT_ID}"
                                   sh "gcloud config set compute/zone ${GKE_CLUSTER_ZONE}"
                                   sh "gcloud container clusters get-credentials ${GKE_CLUSTER_NAME}"
                               }

                               sh "kubectl config current-context"
                               sh "kubectl config get-contexts"
                               sh "kubectl get nodes"

                               echo "Deleting old Kubernetes resources if they exist..."
                               sh "kubectl delete deployment ${K8S_DEPLOYMENT_NAME} --namespace=default --ignore-not-found=true"
                               sh "kubectl delete service ${K8S_SERVICE_NAME} --namespace=default --ignore-not-found=true"

                               echo "Applying Kubernetes manifests..."

                               sh "kubectl apply -f k8s/service.yaml --namespace=default"

                               sh "kubectl apply -f k8s/deployment.yaml --namespace=default"

                               echo "Triggering a rollout restart to apply the new image..."
                               sh "kubectl rollout restart deployment/${K8S_DEPLOYMENT_NAME} --namespace=default"

                               echo "Waiting for deployment rollout to complete..."
                               timeout(time: 5, unit: 'MINUTES') {
                                   sh "kubectl rollout status deployment/${K8S_DEPLOYMENT_NAME} --namespace=default --watch=true"
                               }

                               echo "Application deployed successfully to GKE."
                               echo "Service URL (will show external IP if service is LoadBalancer type):"
                               sh "kubectl get service ${K8S_SERVICE_NAME} --namespace=default -o wide"

                           } catch (e) {
                               echo "Failed to deploy to GKE: ${e.getMessage()}"
                               echo "--- DIAGNOSTIC INFORMATION ---"
                               echo "Retrieving deployment status:"
                               sh "kubectl describe deployment ${K8S_DEPLOYMENT_NAME} --namespace=default || true"
                               echo "Retrieving pod statuses:"
                               sh "kubectl get pods -l app=${IMAGE_NAME.split('/')[1]} --namespace=default -o wide || true"
                               echo "Retrieving logs from potentially problematic pods (adjust selector if needed):"
                               def podNames = sh(script: "kubectl get pods -l app=${IMAGE_NAME.split('/')[1]} --namespace=default -o jsonpath='{.items[*].metadata.name}' || true", returnStdout: true).trim()
                               podNames.split(' ').each { podName ->
                                   echo "--- Logs for pod: ${podName} ---"
                                   sh "kubectl logs ${podName} --namespace=default || true"
                                   sh "kubectl describe pod ${podName} --namespace=default || true"
                               }
                               echo "--- END DIAGNOSTIC INFORMATION ---"
                               error "GKE deployment failed"
                           }
                       }
                   }
               }
    }

    post {
        success {
            echo 'CI/CD Pipeline successfully completed! Application is deployed.'
        }
        failure {
            echo 'CI/CD Pipeline failed! Check above logs for details.'
        }
    }
}
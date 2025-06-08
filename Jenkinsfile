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
                          def minikubeIp = sh(script: "minikube ip", returnStdout: true).trim()
                          echo "Minikube IP detected: ${minikubeIp}"
                          def noProxy = "localhost,127.0.0.1,${minikubeIp},kubernetes.default.svc,kubernetes.default,.svc,.cluster.local"
                          withEnv(["NO_PROXY=${noProxy}", "no_proxy=${noProxy}"]) {
                              echo "📝 Applying Kubernetes manifests with NO_PROXY='${noProxy}'"
                              sh 'kubectl apply -f k8s/deployment.yaml'
                              sh 'kubectl apply -f k8s/service.yaml'
                              echo "♻️ Triggering a rollout restart to ensure the latest image is used..."
                              sh "kubectl rollout restart deployment/${K8S_DEPLOYMENT_NAME}"

                              echo "⏳ Waiting for deployment to complete..."
                              timeout(time: 5, unit: 'MINUTES') {
                                  sh "kubectl rollout status deployment/${K8S_DEPLOYMENT_NAME}"
                              }

                              echo "✅ Application deployed successfully to Minikube."
                          }
                          echo "🔗 Getting service URL..."
                          sh "minikube service ${K8S_SERVICE_NAME} --url"
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
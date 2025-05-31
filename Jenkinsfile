pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                echo 'Fetching code from repository...'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'Compiling the project...'
                sh 'chmod +x ./mvnw'
                sh './mvnw clean compile'
            }
        }

        stage('Test') {
            steps {
                echo 'Running tests...'
                sh './mvnw test'
            }
            post {
                always {
                    echo 'Archiving JUnit test results...'
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                echo 'Packaging the application...'
                sh './mvnw package -DskipTests'
            }
        }

        stage('Archive Artifacts') {
            steps {
                echo 'Archiving the JAR file...'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
    }

    post {
        success {
            echo 'Pipeline successfully completed! Artifacts are archived.'
        }
        failure {
            echo 'Pipeline failed. Check console output for details.'
        }
    }
}

pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                echo "Compiling..."
                sh "${tool name: 'sbt', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'}/bin/sbt compile"
            }
        }
        stage('Unit Test') {
            steps {
                echo "Testing..."
                sh "${tool name: 'sbt', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'}/bin/sbt coverage 'test-only * -- -F 4'"
                sh "${tool name: 'sbt', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'}/bin/sbt coverageReport"
                sh "${tool name: 'sbt', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'}/bin/sbt scalastyle || true"
            }
        }
        stage('Docker Publish') {
            steps {
                // Generate Jenkinsfile and prepare the artifact files.
                sh "${tool name: 'sbt', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'}/bin/sbt docker:stage"

                // Run the Docker tool to build the image
                script {
                    docker.withTool('docker') {
                        docker.build('${env.JOB_NAME}', 'target/docker/stage')
                    }
                }
            }
        }
        
    }
}
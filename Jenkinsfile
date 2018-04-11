pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                echo "Compiling..."
                sh "${tool name: 'sbt', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'}/bin/sbt compile"
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

                // Run the Docker push image to registry
                script {
                    docker.withRegistry('https://bigdata-registry.local:5043', 'ff494237-f391-4f89-957b-bb0bf680157f'){
                    docker.push("${env.BUILD_NUMBER}")
                    docker.push("latest")
                    }
                }
            }
        }
        
    }
}
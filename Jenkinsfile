pipeline {
    agent { node { label 'master' } }

    def app

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
                        app = docker.build("${env.BUILD_NUMBER}", 'target/docker/stage')
                    }
                }

            }
        }
        stage('Docker Push') {
            steps {
                script {
                    docker.withRegistry('https://bigdata-registry.local:5043', 'ff494237-f391-4f89-957b-bb0bf680157f'){
                        app.push("${env.BUILD_NUMBER}")
                        app.push("latest")
                    }
                }
            }
        }
        stage('Deploying'){

            steps {
                script {
                    sh "ssh root@172.27.11.161 'docker login -u admin -p 1nc0rrect bigdata-registry.local:5043'"
                    sh "ssh root@172.27.11.161 'docker pull bigdata-registry.local:5043/${env.JOB_NAME}:${env.BUILD_NUMBER}'"
                    sh "ssh root@172.27.11.161 'docker run -d --net=${env.JOB_NAME} -h ${env.JOB_NAME} --name ${env.JOB_NAME} -v /public/images/:/opt/bigdata-play/public/ ${env.JOB_NAME}'"
                    //sh "ssh root@10.0.1.201 'docker service create --name ${env.JOB_NAME} --mode global --publish mode=host,target=80,published=80 bigdata-registry.local:5043/${env.JOB_NAME}:${env.BUILD_NUMBER}'"
                    //sh "ssh root@10.0.1.201 'docker service update --image bigdata-registry.local:5043/${env.JOB_NAME}:${env.BUILD_NUMBER} ${env.JOB_NAME}'"
                } 
            }
        }
        
    }
}
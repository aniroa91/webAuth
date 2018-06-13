pipeline {

    agent { node { label 'webapp03' } }

    stages {
        stage('Build') {
            steps {
                echo "Compiling..."
                sh "${tool name: 'sbt', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'}/bin/sbt dist"
            }
        }
        stage('Docker Publish') {
            steps {
                // Generate Jenkinsfile and prepare the artifact files.
                sh "${tool name: 'sbt', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'}/bin/sbt docker:stage"

                script {
                    docker.withRegistry('https://bigdata-registry.local:5043', '010ed969-34b5-473b-bcd9-01a207e7e382') {
                        def app = docker.build("${env.JOB_NAME}:${env.BUILD_ID}")
                        /* Push the container to the custom Registry */
                        app.push()
                    }
                }
            }
        }
        stage('Deploying'){

            steps {
                sshagent (credentials: ['4a18733a-bef4-4220-84d7-30dd315c7483']) {
                    sh "ssh -o StrictHostKeyChecking=no root@172.27.11.161 'docker login -u admin -p 1nc0rrect bigdata-registry.local:5043'"
                    sh "ssh -o StrictHostKeyChecking=no root@172.27.11.161 'docker pull bigdata-registry.local:5043/${env.JOB_NAME}:${env.BUILD_ID}'"
                    sh "ssh -o StrictHostKeyChecking=no root@172.27.11.161 'docker rm -f ${env.JOB_NAME} && docker run -d -p 9000:9000 -h ${env.JOB_NAME} --name ${env.JOB_NAME} -v /public/images/:/opt/bigdata-play/public/ bigdata-registry.local:5043/${env.JOB_NAME}:${env.BUILD_ID}'"
                    //sh "ssh root@10.0.1.201 'docker service create --name ${env.JOB_NAME} --mode global --publish mode=host,target=80,published=80 bigdata-registry.local:5043/${env.JOB_NAME}:${env.BUILD_NUMBER}'"
                    //sh "ssh root@10.0.1.201 'docker service update --image bigdata-registry.local:5043/${env.JOB_NAME}:${env.BUILD_NUMBER} ${env.JOB_NAME}'"
                } 
            }
        }
        
    }
}
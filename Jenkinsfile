pipeline {

    agent { node { label 'agent16' } }

    environment {
        HTTP_PROXY = 'http://proxy.hcm.fpt.vn:80'
        HTTPS_PROXY = 'http://proxy.hcm.fpt.vn:80'
        NO_PROXY = '172.0.0.1,*.local,172.27.11.0/24'
        GIT_COMMIT = sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%h'").trim()
        K8S_SERVICE_NAME = 'noc-staging'
    }

    
    stages {
        stage('Build') {
            steps {
                echo "Compiling..."
                // sh 'printenv'
                sh "${tool name: 'sbt', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'}/bin/sbt dist"
            }
        }
        stage('Docker Publish') {
            steps {
                // Generate Jenkinsfile and prepare the artifact files.
                sh "${tool name: 'sbt', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'}/bin/sbt docker:stage"
                //cleanup current user docker credentials
                // sh 'rm  /var/jenkins_home/.dockercfg || true'
                script {
                    docker.withRegistry('https://bigdata-registry.local:5043', '010ed969-34b5-473b-bcd9-01a207e7e382') {
                        def app = docker.build("${env.JOB_NAME}:${env.BUILD_ID}-${env.BUILD_TIMESTAMP}-${GIT_COMMIT}")
                        /* Push the container to the custom Registry */
                        app.push()
                    }
                }
            }
        }
        stage('Deploying'){
            steps {
                kubernetesDeploy(configs: 'k8s_deploy.yaml', 
                enableConfigSubstitution: true,
                kubeConfig: [path: ''], kubeconfigId: 'admin_k8s_kubeconfig', secretName: '', 
                dockerCredentials: [[credentialsId: '010ed969-34b5-473b-bcd9-01a207e7e382', url: 'http://bigdata-registry.local:5043']]
                )
            }

        }
    }
}

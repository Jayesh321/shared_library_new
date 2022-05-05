def call ( String dockerCred = 'a', String dockerRepo = 'a', String docTag = 'a', String grepo = 'a', String gbranch = 'a', String gitcred = 'a'  ) {

pipeline {
    environment {
        dockerCredential = "${dockerCred}"
        dockerRepository = "${dockerRepo}"
        dockerTag = "${docTag}_$BUILD_NUMBER"
        gitRepo = "${grepo}"
        gitBranch = "${gbranch}"
        gitCred = "${gitcred}"
    }

    agent none

    triggers {
        pollSCM '* * * * * '
    }

    options {
    buildDiscarder(logRotator(numToKeepStr: '5', artifactNumToKeepStr: '5'))
    }

    stages {
        stage('POLL SCM') {
            agent{label 'docker_slave'}
            steps {
                checkout([$class: 'GitSCM', branches: [[name: "$gitBranch"]], extensions: [], userRemoteConfigs: [[credentialsId: "$gitCred", url: "$gitRepo"]]])
            }
        }

        stage('BUILD STAGE') {
            agent{label 'docker_slave'}
            steps {
                script {
                    //dockerimage = docker.build("'$dockerRepository':$dockerTag")
                    dockerimage = dockerImage = docker.build dockerRepository + ":$dockerTag" 

                }
            }
        }

        stage('PUSH TO DOCKER HUB') {
            agent{label 'docker_slave'}
            steps { 
                script {
                    docker.withRegistry('', "$dockerCredential") {
                        dockerImage.push()
                    }
                }
            }
        } 

        stage ('Deploy') {
            agent{label 'k8s_slave'}
            steps {
                sh 'kubctl set image deployment/webapp-deployment nodejs="$dockerRepository:$dockerTag" --record'
            }
        }
    }

}

}

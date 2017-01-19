node('local') {
    properties([[$class: 'GitLabConnectionProperty', gitLabConnection: 'https://git.lumc.nl/'], pipelineTriggers([[$class: 'GitHubPushTrigger']])])

    try {

        stage('Init') {
            tool 'JDK 8u102'
            tool 'Apache Maven 3.3.9'
        }

        stage('Checkout') {
            checkout scm
            sh 'git submodule update --init --recursive'
        }

        stage('Build and Test') {
            withMaven(maven: 'Apache Maven 3.3.9', jdk: 'JDK 8u102') {
                sh 'mvn -B -T 2 -Dmaven.test.failure.ignore clean package'
            }
        }

        stage('Report tests') {
            junit '*/target/surefire-reports/*.xml'
        }

        if(currentBuild.result == null || "SUCCESS".equals(currentBuild.result)) {
            currentBuild.result = "SUCCESS"
            slackSend (color: '#00FF00', message: "${currentBuild.result}: Job '${env.JOB_NAME} #${env.BUILD_NUMBER}' (<${env.BUILD_URL}|Open>)", channel: '#biopet-bot', teamDomain: 'lumc', tokenCredentialId: 'lumc')
        } else {
            slackSend (color: '#FFFF00', message: "${currentBuild.result}: Job '${env.JOB_NAME} #${env.BUILD_NUMBER}' (<${env.BUILD_URL}|Open>)", channel: '#biopet-bot', teamDomain: 'lumc', tokenCredentialId: 'lumc')
        }

    } catch (e) {
        if(currentBuild.result = null || "FAILED".equals(currentBuild.result)) {
            currentBuild.result = "FAILED"
        }
        slackSend (color: '#FF0000', message: "${currentBuild.result}: Job '${env.JOB_NAME} #${env.BUILD_NUMBER}' (<${env.BUILD_URL}|Open>)", channel: '#biopet-bot', teamDomain: 'lumc', tokenCredentialId: 'lumc')

        throw e
    }
}

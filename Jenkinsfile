node('local') {
    try {

        stage('Init') {
            tool 'JDK 8u102'
            tool 'Apache Maven 3.3.9'
        }

        timeout(30) {
            stage('Checkout') {
                checkout scm
                sh 'git submodule update --init --recursive'
            }
        }

        timeout(45) {
            stage('Build and Test') {
                withMaven(maven: 'Apache Maven 3.3.9', jdk: 'JDK 8u102') {
                    sh 'mvn -B -T 2 -Dmaven.test.failure.ignore clean package'
                }
            }
        }

        stage('Report Tests') {
            junit '*/target/surefire-reports/*.xml'
        }

        stage('Check git on changes') {
            sh 'if [ $(git diff | wc -l) -eq 0 ]; then true; else echo "[ERROR] Git is not clean anymore after build"; git diff; echo "[ERROR] This might be caused by reformated code, if so run maven locally"; false; fi'
        }

        stage('Check Documentation') {
            sh 'mkdocs build --clean --strict'
        }

        if (currentBuild.result == null || "SUCCESS".equals(currentBuild.result)) {
            currentBuild.result = "SUCCESS"
            slackSend(color: '#00FF00', message: "${currentBuild.result}: Job '${env.JOB_NAME} #${env.BUILD_NUMBER}' (<${env.BUILD_URL}|Open>)", channel: '#biopet-bot', teamDomain: 'lumc', tokenCredentialId: 'lumc')
        } else {
            slackSend(color: '#FFFF00', message: "${currentBuild.result}: Job '${env.JOB_NAME} #${env.BUILD_NUMBER}' (<${env.BUILD_URL}|Open>)", channel: '#biopet-bot', teamDomain: 'lumc', tokenCredentialId: 'lumc')
        }

    } catch (e) {
        if (currentBuild.result == null || "FAILED".equals(currentBuild.result)) {
            currentBuild.result = "FAILED"
        }
        slackSend(color: '#FF0000', message: "${currentBuild.result}: Job '${env.JOB_NAME} #${env.BUILD_NUMBER}' (<${env.BUILD_URL}|Open>)", channel: '#biopet-bot', teamDomain: 'lumc', tokenCredentialId: 'lumc')

        throw e
    }
}

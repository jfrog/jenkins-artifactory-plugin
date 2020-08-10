node('java') {
    cleanWs()
    def jdktool = tool name: "1.8.0_102"
    env.JAVA_HOME = jdktool

    if ("$P_BUILD_MODE".toString().equals("Pre build actions")) {
        git(
                url: 'git@github.com:jenkinsci/artifactory-plugin.git',
                credentialsId: 'jenkinsci-github-key'
        )

        stage('Initial install') {
            def rtMaven = Artifactory.newMavenBuild()
            rtMaven.tool = 'mvn-3.6.2'
            rtMaven.run pom: 'pom.xml', goals: 'clean install'
        }

        stage('Pull') {
            sh("git pull https://github.com/JFrog/jenkins-artifactory-plugin.git master")
        }

        stage('Install pulled code') {
            def rtMaven = Artifactory.newMavenBuild()
            rtMaven.tool = 'mvn-3.6.2'
            rtMaven.run pom: 'pom.xml', goals: 'clean install'
        }

        stage('Push') {
            sh("git push --set-upstream origin master")
        }

        stage ('Starting next job') {
            build 'artifactory-jenkins-plugin'
        }
    }

    if ("$P_BUILD_MODE".toString().equals("Post build merge")) {
        git(
                url: 'https://github.com/JFrog/jenkins-artifactory-plugin.git'
        )
        wrap([$class: 'MaskPasswordsBuildWrapper', varPasswordPairs: [[password: 'GITHUB_API_KEY', var: 'SECRET']]]) {
            sh("git pull https://github.com/jenkinsci/artifactory-plugin.git")
            sh("git fetch https://github.com/jenkinsci/artifactory-plugin.git --tags")
            sh("git push https://${GITHUB_USERNAME}:${GITHUB_API_KEY}@github.com/JFrog/jenkins-artifactory-plugin.git")
            sh("git push https://${GITHUB_USERNAME}:${GITHUB_API_KEY}@github.com/JFrog/jenkins-artifactory-plugin.git --tags")
        }
    }
}
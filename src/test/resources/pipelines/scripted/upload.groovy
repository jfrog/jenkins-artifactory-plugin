package pipelines.scripted

node {
    def rtServer = Artifactory.newServer url: "${env.JENKINS_ARTIFACTORY_URL}", username: "${env.JENKINS_ARTIFACTORY_USERNAME}", password: "${env.JENKINS_ARTIFACTORY_PASSWORD}"
    def uploadSpec = """{
      "files": [
        {
          "pattern": "${FILES_DIR}",
          "target": "${LOCAL_REPO}/"
        }
     ]
    }"""
    rtServer.upload spec: uploadSpec
}

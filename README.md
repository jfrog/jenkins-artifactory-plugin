[![Build status](https://ci.appveyor.com/api/projects/status/01cimn54er7nna82?svg=true)](https://ci.appveyor.com/project/jfrog-ecosystem/jenkins-artifactory-plugin)

# Artifactory Plugin for Jenkins

## General
The plugin integrates Jenkins and Artifactory to publish, resolve, promote and release traceable build artifacts.
For more information, including the release notes, please visit the [JFrog Artifactory Plugin documentation](https://www.jfrog.com/confluence/display/RTF/Jenkins+Artifactory+Plug-in)

## How to Contribute
JFrog welcomes community contribution through pull requests.

### Important:
The plugin code is stored in two github repositories:
https://github.com/jfrog/jenkins-artifactory-plugin and
https://github.com/jenkinsci/artifactory-plugin

Please make sure to submit pull requests to *https://github.com/jfrog/jenkins-artifactory-plugin* only.

## How to build the plugin code
To build the plugin, please use Maven with JDK 8 and run:
```console
> mvn clean install
```

## Tests
### Unit tests
To run unit tests execute the following command: 
```
> mvn clean test
```

### Integration tests
#### Running integration tests
Before running the integration tests, set the following environment variables.

*JENKINS_ARTIFACTORY_URL*
*JENKINS_ARTIFACTORY_USERNAME*
*JENKINS_ARTIFACTORY_PASSWORD*
*JENKINS_ARTIFACTORY_DOCKER_DOMAIN* (For example, server-docker-local.jfrog.io)
*JENKINS_ARTIFACTORY_DOCKER_REPO* (For example, docker-local)
*JENKINS_ARTIFACTORY_DOCKER_HOST* - Optional address of the docker daemon (For example, tcp://127.0.0.1:1234)
*MAVEN_HOME* - The local maven installation path.
*GRADLE_HOME* - The local gradle installation path).

To disable build scan with Xray integration tests, set *JENKINS_XRAY_TEST_ENABLE* to *false*.

Run the integration tests.
```
> mvn clean verify -DskipITs=false
```
#### Integration tests results and progress
The tests results are printed to the console (standard output) when the tests finish.
Since JUnit however does not indicate which tests are currently running, a file named *tests.log* is created in the current directory, which logs the tests progress. 
[![Build status](https://ci.appveyor.com/api/projects/status/01cimn54er7nna82?svg=true)](https://ci.appveyor.com/project/jfrog-ecosystem/jenkins-artifactory-plugin)

# Artifactory Plugin for Jenkins

## General
The plugin integrates Jenkins and Artifactory to publish, resolve, promote and release traceable build artifacts.
For more information please visit the [JFrog Artifactory Plugin documentation](https://www.jfrog.com/confluence/display/RTF/Jenkins+Artifactory+Plug-in)

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
* The *JENKINS_ARTIFACTORY_URL* environment variable should be set to the Artifactory URL.
* The *JENKINS_ARTIFACTORY_USERNAME* environment variable should be set to the Artifactory username.
* The *JENKINS_ARTIFACTORY_PASSWORD* environment variable should be set to the Artifactory password.
* The *MAVEN_HOME* environment variable should be set to the local maven installation path.
* The *GRADLE_HOME* environment variable should be set to the local gradle installation path.

To run integration tests execute the following command:
```
> mvn clean verify -DskipITs=false
```
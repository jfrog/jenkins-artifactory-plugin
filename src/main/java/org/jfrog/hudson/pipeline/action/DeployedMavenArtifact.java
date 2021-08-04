package org.jfrog.hudson.pipeline.action;

public class DeployedMavenArtifact extends DeployedArtifact {
    public DeployedMavenArtifact(String artifactoryUrl, String repository, String remotePath, String name) {
        super(artifactoryUrl, repository, remotePath, name);
    }
}

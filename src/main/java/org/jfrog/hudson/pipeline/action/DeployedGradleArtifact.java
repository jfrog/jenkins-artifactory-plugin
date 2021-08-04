package org.jfrog.hudson.pipeline.action;

public class DeployedGradleArtifact extends DeployedArtifact {
    public DeployedGradleArtifact(String artifactoryUrl, String repository, String remotePath, String name) {
        super(artifactoryUrl, repository, remotePath, name);
    }
}

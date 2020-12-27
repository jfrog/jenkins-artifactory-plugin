package org.jfrog.hudson.pipeline.action;

import java.io.Serializable;

public class DeployedMavenArtifact implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String artifactoryUrl;
    private final String repository;
    private final String remotePath;
    private final String name;

    public DeployedMavenArtifact(String artifactoryUrl, String repository, String remotePath, String name) {
        this.artifactoryUrl = artifactoryUrl;
        this.repository = repository;
        this.remotePath = remotePath;
        this.name = name;
    }

    public String getUrl() {
        return this.artifactoryUrl + "/" + this.repository + "/" + this.remotePath;
    }

    public String getDisplayName() {
        return this.name;
    }

    public String getName() {
        return name;
    }
}

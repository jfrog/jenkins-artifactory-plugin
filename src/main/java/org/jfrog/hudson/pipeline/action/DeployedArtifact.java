package org.jfrog.hudson.pipeline.action;

import java.io.Serializable;

/**
 * Deployed artifact to display as part of the pipeline's summary.
 */
public class DeployedArtifact implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String artifactoryUrl;
    private final String repository;
    private final String remotePath;
    private final String name;

    public DeployedArtifact(String artifactoryUrl, String repository, String remotePath, String name) {
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

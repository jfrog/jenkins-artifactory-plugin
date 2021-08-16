package org.jfrog.hudson.pipeline.action;

import hudson.model.Run;

import javax.annotation.Nonnull;

/**
 * Adds a list of the deployed maven artifacts in the summary of a pipeline job.
 */
public class DeployedMavenArtifactsAction extends DeployedArtifactsAction {
    public DeployedMavenArtifactsAction(@Nonnull Run<?, ?> build) {
        super(build);
    }

    @Override
    public String getDisplayName() {
        return "Maven Artifacts";
    }

    @Override
    public String getUrlName() {
        return "artifactory_maven_artifacts";
    }
}

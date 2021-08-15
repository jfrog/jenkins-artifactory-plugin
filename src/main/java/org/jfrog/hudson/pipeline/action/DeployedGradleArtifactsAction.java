package org.jfrog.hudson.pipeline.action;

import hudson.model.Run;

import javax.annotation.Nonnull;

/**
 * Adds a list of the deployed gradle artifacts in the summary of a pipeline job.
 */
public class DeployedGradleArtifactsAction extends DeployedArtifactsAction {
    public DeployedGradleArtifactsAction(@Nonnull Run build) {
        super(build);
    }

    public String getDisplayName() {
        return "Gradle Artifacts";
    }

    public String getUrlName() {
        return "artifactory_gradle_artifacts";
    }
}

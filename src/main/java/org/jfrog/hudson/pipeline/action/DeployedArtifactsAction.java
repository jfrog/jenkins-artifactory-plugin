package org.jfrog.hudson.pipeline.action;

import hudson.model.Run;
import jenkins.model.RunAction2;

import javax.annotation.Nonnull;

/**
 * Adds a list of the deployed artifacts in the summary of a pipeline job.
 */
public abstract class DeployedArtifactsAction implements RunAction2 {
    private Run build;

    public DeployedArtifactsAction(@Nonnull Run build) {
        this.build = build;
    }

    @Override
    public synchronized void onAttached(Run<?, ?> build) {
        this.build = build;
    }

    @Override
    public synchronized void onLoad(Run<?, ?> build) {
        this.build = build;
    }

    public Run getBuild() {
        return build;
    }

    public String getIconFileName() {
        return "/plugin/artifactory/images/artifactory-icon.png";
    }

    public abstract String getDisplayName();

    public abstract String getUrlName();
}

package org.jfrog.hudson.pipeline.action;

import hudson.model.Run;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Adds a list of the deployed gradle artifacts in the summary of a pipeline job.
 */
public class DeployedGradleArtifactsAction extends DeployedArtifactsAction {
    private final List<DeployedGradleArtifact> deployedGradleArtifacts = new CopyOnWriteArrayList<>();

    public DeployedGradleArtifactsAction(@Nonnull Run build) {
        super(build);
    }

    public String getDisplayName() {
        return "Gradle Artifacts";
    }

    public String getUrlName() {
        return "artifactory_gradle_artifacts";
    }

    /**
     * @return list of deployed gradle artifacts. Called from the UI.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public synchronized Collection<DeployedGradleArtifact> getDeployedGradleArtifacts() {
        return this.deployedGradleArtifacts;
    }

    public void appendDeployedGradleArtifacts(List<DeployedGradleArtifact> deployed) {
        deployedGradleArtifacts.addAll(deployed);
    }
}

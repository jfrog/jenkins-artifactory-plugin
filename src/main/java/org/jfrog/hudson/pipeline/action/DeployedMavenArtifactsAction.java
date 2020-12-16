package org.jfrog.hudson.pipeline.action;

import hudson.model.Run;
import jenkins.model.RunAction2;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Adds a list of the deployed maven artifacts in the summary of a pipeline job.
 */
public class DeployedMavenArtifactsAction implements RunAction2 {
    private Run build;
    private final List<DeployedMavenArtifact> deployedMavenArtifacts = new CopyOnWriteArrayList<>();

    public DeployedMavenArtifactsAction(@Nonnull Run build) {
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

    public String getDisplayName() {
        return "Maven Artifacts";
    }

    public String getUrlName() {
        return "artifactory_maven_artifacts";
    }

    /**
     * @return list of deployed maven artifacts. Called from the UI.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public synchronized Collection<DeployedMavenArtifact> getDeployedMavenArtifacts() {
        return this.deployedMavenArtifacts;
    }

    public void appendDeployedMavenArtifacts(List<DeployedMavenArtifact> deployed) {
        deployedMavenArtifacts.addAll(deployed);
    }
}
